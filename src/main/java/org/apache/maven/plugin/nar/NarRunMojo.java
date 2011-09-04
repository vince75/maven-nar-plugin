package org.apache.maven.plugin.nar;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Runs NAR executables.
 *
 * @goal nar-run
 * @requiresProject
 * @requiresDependencyResolution runtime
 */
public class NarRunMojo
    extends AbstractRunMojo {

    public static final String ARGS = "run.args";

    public void narExecute() throws MojoFailureException, MojoExecutionException {
        for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
        {
            Library library = (Library) i.next();
            if ( library.getType().equals( Library.EXECUTABLE ) )
            {
                runExecutable( library, Artifact.SCOPE_RUNTIME, System.getProperty( ARGS ), System.in );
            }
        }
    }

    protected List getClasspathElements() {
        return Collections.emptyList();
    }
}
