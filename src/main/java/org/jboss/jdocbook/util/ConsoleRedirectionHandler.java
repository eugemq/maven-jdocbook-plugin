/*
 * jDocBook, processing of DocBook sources as a Maven plugin
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.jboss.jdocbook.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.jboss.jdocbook.JDocBookProcessException;

/**
 * Handles redirection of <tt>sysout</tt> and <tt>syserr</tt>.  The initial
 * impl handles redirection specifically to a file.
 *
 * @author Steve Ebersole
 */
public class ConsoleRedirectionHandler {
	private final File redirectionFile;

	private PrintStream redirectionStream;
	private PrintStream sysout;
	private PrintStream syserr;

	public ConsoleRedirectionHandler(File redirectionFile) {
		this.redirectionFile = redirectionFile;
	}

	public void start() {
		System.out.println( "redirecting output to file [" + redirectionFile.getAbsolutePath() + "]" );
		if ( !redirectionFile.exists() ) {
			//noinspection ResultOfMethodCallIgnored
			redirectionFile.getParentFile().mkdirs();
			try {
				//noinspection ResultOfMethodCallIgnored
				redirectionFile.createNewFile();
			}
			catch ( IOException e ) {
				throw new JDocBookProcessException(
						"Unable to create console redirection file [" + redirectionFile.getAbsolutePath() + "]",
						e
				);
			}
		}
		try {
			redirectionStream = new PrintStream( new FileOutputStream( redirectionFile ) );
		}
		catch ( FileNotFoundException e ) {
			// should never ever happen, see above...
			throw new JDocBookProcessException( "Unable to open console redirect file for output", e );
		}

		sysout = System.out;
		syserr = System.err;
		System.setOut( redirectionStream );
		System.setErr( redirectionStream );
	}

	public void stop() {
		System.setOut( sysout );
		System.setErr( syserr );

		redirectionStream.flush();
		redirectionStream.close();
	}

	public PrintStream sysout() {
		return sysout;
	}

	public PrintStream syserr() {
		return syserr;
	}
}
