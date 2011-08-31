package org.apache.maven.plugin.nar;

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
 * @requiresDependencyResolution
 */
public class NarRunMojo
    extends AbstractRunMojo {
    public void narExecute() throws MojoFailureException, MojoExecutionException {
        for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
        {
            Library library = (Library) i.next();
            if ( library.getType().equals( Library.EXECUTABLE ) )
            {
                runExecutable( library, System.in );
            }
        }
    }

    protected List getClasspathElements() {
        return Collections.emptyList();
    }
}
