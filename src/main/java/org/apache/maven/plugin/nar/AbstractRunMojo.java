package org.apache.maven.plugin.nar;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public abstract class AbstractRunMojo
    extends AbstractCompileMojo
{
    /**
     * Whether to use Valgrind.
     *
     * @parameter expression="${valgrind}"
     */
    protected boolean valgrind;

    protected void runExecutable( Library library, String scope, String args, InputStream in )
        throws MojoExecutionException, MojoFailureException
    {
        if ( !library.getType().equals( Library.EXECUTABLE ) )
        {
            throw new MojoFailureException( library + " is not executable" );
        }

        MavenProject project = getMavenProject();
        // FIXME NAR-90, we could make sure we get the final name from layout
        String extension = getOS().equals( OS.WINDOWS ) ? ".exe" : "";
        File executable =
            new File( getLayout().getBinDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
                                                   getMavenProject().getVersion(), getAOL().toString() ),
                      project.getArtifactId() + extension );
        if ( !executable.exists() )
        {
            getLog().warn( "Skipping non-existing executable " + executable );
            return;
        }
        getLog().info( "Running executable " + executable );
        LinkedList argList = new LinkedList( args == null ? library.getArgs() : Collections.singletonList( args ) );
        if ( valgrind )
        {
            argList.addFirst( executable.getPath() );
            executable = new File( "valgrind" );
        }
        int result =
            NarUtil.runCommand( executable.getPath(), (String[]) argList.toArray( new String[argList.size()] ), null,
                                generateEnvironment( scope ), getLog(), in );
        if ( result != 0 )
        {
            throw new MojoFailureException( "Executable " + executable + " failed with exit code: " +
                                                result + " 0x" + Integer.toHexString( result ) );
        }
    }

    protected String[] generateEnvironment( String scope )
        throws MojoExecutionException, MojoFailureException
    {
        List env = new ArrayList();

        Set/* <File> */sharedPaths = new HashSet();

        // add all shared libraries of this package
        for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
        {
            Library lib = (Library) i.next();
            if ( lib.getType().equals( Library.SHARED ) )
            {
                // Library artifacts should always reside in directory corresponding to compile scope,
                // since they are produced by current build.
                File path =
                    getLayout().getLibDirectory( getTargetDirectory( Artifact.SCOPE_COMPILE ), getMavenProject().getArtifactId(),
                                                 getMavenProject().getVersion(), getAOL().toString(), lib.getType() );
                getLog().debug( "Adding path to shared library: " + path );
                sharedPaths.add( path );
            }
        }

        // add dependent shared libraries
        String classifier = getAOL() + "-shared";
        List narArtifacts = getNarManager().getNarDependencies( scope );
        List dependencies = getNarManager().getAttachedNarDependencies( narArtifacts, classifier );
        for ( Iterator d = dependencies.iterator(); d.hasNext(); )
        {
            Artifact dependency = (Artifact) d.next();
            getLog().debug( "Looking for dependency " + dependency );

            // FIXME reported to maven developer list, isSnapshot
            // changes behaviour
            // of getBaseVersion, called in pathOf.
            dependency.isSnapshot();

            File libDirectory =
                getLayout().getLibDirectory( getUnpackDirectory( scope ), dependency.getArtifactId(), dependency.getVersion(),
                                             getAOL().toString(), Library.SHARED );
            sharedPaths.add( libDirectory );
        }

        // set environment
        if ( sharedPaths.size() > 0 )
        {
            String sharedPath = "";
            for ( Iterator i = sharedPaths.iterator(); i.hasNext(); )
            {
                sharedPath += ((File) i.next()).getPath();
                if ( i.hasNext() )
                {
                    sharedPath += File.pathSeparator;
                }
            }

            String sharedEnv = NarUtil.addLibraryPathToEnv( sharedPath, null, getOS() );
            env.add( sharedEnv );
        }

        // necessary to find WinSxS
        if ( getOS().equals( OS.WINDOWS ) )
        {
            env.add( "SystemRoot=" + NarUtil.getEnv( "SystemRoot", "SystemRoot", "C:\\Windows" ) );
        }

        // add CLASSPATH
        env.add( "CLASSPATH=" + StringUtils.join( getClasspathElements().iterator(), File.pathSeparator ) );

        return env.size() > 0 ? (String[]) env.toArray( new String[env.size()] ) : null;
    }

    protected File getTargetDirectory( String scope )
    {
        if ( Artifact.SCOPE_TEST.equals( scope ) )
        {
            return getTestTargetDirectory();
        }
        return getTargetDirectory();
    }

    protected File getUnpackDirectory( String scope )
    {
        if ( Artifact.SCOPE_TEST.equals( scope ) )
        {
            return getTestUnpackDirectory();
        }
        return getUnpackDirectory();
    }

    protected abstract List getClasspathElements();

}
