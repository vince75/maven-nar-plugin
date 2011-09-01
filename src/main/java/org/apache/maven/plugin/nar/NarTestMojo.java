package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Tests NAR files. Runs Native Tests and executables if produced.
 *
 * @goal nar-test
 * @phase test
 * @requiresProject
 * @requiresDependencyResolution
 * @author Mark Donszelmann
 */
public class NarTestMojo
    extends AbstractRunMojo
{
    /**
     * The classpath elements of the project being tested.
     * 
     * @parameter expression="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List classpathElements;

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // run all tests
        for ( Iterator i = getTests().iterator(); i.hasNext(); )
        {
            runTest( (Test) i.next() );
        }

        for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
        {
            Library library = (Library) i.next();
            if ( library.getType().equals( Library.EXECUTABLE ) && library.shouldRun() )
            {
                runExecutable( library, null, null );
            }
        }
    }

    private void runTest( Test test )
        throws MojoExecutionException, MojoFailureException
    {
        // run if requested
        if ( test.shouldRun() )
        {
            // NOTE should we use layout here ?
            String name = test.getName() + (getOS().equals( OS.WINDOWS ) ? ".exe" : "");
            File path = new File( getTestTargetDirectory(), "bin" );
            path = new File( path, getAOL().toString() );
            path = new File( path, name );
            if ( !path.exists() )
            {
                getLog().warn( "Skipping non-existing test " + path );
                return;
            }
            
            File workingDir = new File( getTestTargetDirectory(), "test-reports" );
            workingDir.mkdirs();
            getLog().info( "Running test " + name + " in " + workingDir );

            List args = test.getArgs();
            int result =
                NarUtil.runCommand( path.toString(), (String[]) args.toArray( new String[args.size()] ), workingDir,
                                    generateEnvironment(), getLog() );
            if ( result != 0 )
            {
                throw new MojoFailureException( "Test " + name + " failed with exit code: " + result + " 0x"
                    + Integer.toHexString( result ) );
            }
        }
    }

    protected List getClasspathElements() {
        return classpathElements;
    }
}
