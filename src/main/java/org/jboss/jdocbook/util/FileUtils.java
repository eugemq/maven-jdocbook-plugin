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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.jboss.jdocbook.JDocBookProcessException;
import org.jboss.jdocbook.ValueInjection;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import net.socialchange.doctype.DoctypeChangerStream;
import net.socialchange.doctype.DoctypeGenerator;
import net.socialchange.doctype.Doctype;
import net.socialchange.doctype.DoctypeImpl;

/**
 * Various {@link java.io.File file} and {@link java.io.File directory} related utilities.
 *
 * @author Steve Ebersole
 */
public class FileUtils extends org.codehaus.plexus.util.FileUtils {
	/**
	 * A filter which simply accepts any file about which it is asked.
	 */
	public static final FileFilter ACCEPT_ALL = new FileFilter() {
		/**
		 * {@inheritDoc}
		 */
		public boolean accept(File pathname) {
			return true;
		}
	};

	/**
	 * Create a SAXSource from a given <tt>file</tt>.
	 * <p/>
	 * NOTE: the result <b>is</b> {@link BufferedInputStream buffered}.
	 * 
	 * @param file The file from which to generate a SAXSource
	 * @param resolver An entity resolver to apply to the file reader.
	 * @param xincludeAware Should we handle XIncludes?
	 * @param valueInjections The values to be injected
	 *
	 * @return An appropriate SAXSource
	 */
	public static SAXSource createSAXSource(
			File file,
			EntityResolver resolver,
			boolean xincludeAware,
			final List<ValueInjection> valueInjections) {
		try {
			final boolean injectionsDefined = valueInjections != null && ! valueInjections.isEmpty();

        	SAXParserFactory factory = new SAXParserFactoryImpl();
        	factory.setXIncludeAware( xincludeAware  );

			XMLReader reader = factory.newSAXParser().getXMLReader();
			reader.setEntityResolver( resolver );

			// Disable DTD loading and validation
			reader.setFeature( Constants.DTD_LOADING_FEATURE, false );
			reader.setFeature( Constants.DTD_VALIDATION_FEATURE, false );

			try {
				InputStream inputStream = new BufferedInputStream( new FileInputStream( file ) );
				if ( injectionsDefined ) {
					DoctypeChangerStream changerStream = new DoctypeChangerStream( inputStream );
					changerStream.setGenerator(
							new DoctypeGenerator() {
								public Doctype generate(final Doctype doctype) {
									final String root = doctype == null ? null : doctype.getRootElement();
									final String pubId = doctype == null ? null : doctype.getPublicId();
									final String sysId = doctype == null ? null : doctype.getSystemId();

									StringBuffer internalSubset = new StringBuffer();
									buildInjectedInternalEntitySubset( internalSubset, valueInjections );
									if ( doctype != null && doctype.getInternalSubset() != null ) {
										internalSubset.append( doctype.getInternalSubset() ).append( '\n' );
									}
									return new DoctypeImpl( root, pubId, sysId, internalSubset.toString() );
								}
							}
					);
					inputStream = changerStream;
				}
				InputSource source = new InputSource( inputStream );
				source.setSystemId( file.toURI().toURL().toString() );
				return new SAXSource( reader, source );
			}
			catch ( FileNotFoundException e ) {
				throw new JDocBookProcessException( "unable to locate source file", e );
			}
			catch ( MalformedURLException e ) {
				throw new JDocBookProcessException( "unexpected problem converting file to URL", e );
			}
		}
		catch ( ParserConfigurationException e ) {
			throw new JDocBookProcessException( "unable to build SAX Parser/Factory [" + e.getMessage() + "]", e );
		}
		catch ( SAXException e ) {
			throw new JDocBookProcessException( "unable to build SAX Parser/Factory [" + e.getMessage() + "]", e );
		}

	}

	/**
	 * Determine the 'relativity' of a file in relation to the given basedir.  'relativity', is the relative path
	 * from the basedir to the file's parent (directory).
	 * <p/>
	 * For example, a file <tt>/home/steve/hibernate/tmp/Test.java</tt> would have a relativity of <tt>hibernate/tmp</tt>
	 * relative to <tt>/home/steve</tt> as the basedir.
	 *
	 * @param file The file for which to determine relativity.
	 * @param basedir The directory from which to base the relativity.
	 * @return The relativity.
	 * @throws RuntimeException Indicates that the given file was not found to be relative to basedir.
	 */
	public static String determineRelativity(File file, File basedir) {
		String basedirPath = resolveFullPathName( basedir );
		String directory = resolveFullPathName( file.getParentFile() );
		if ( basedirPath.equals( directory ) ) {
			return null;
		}
		int baseStart = directory.indexOf( basedirPath );
		if ( baseStart < 0 ) {
			throw new RuntimeException( "Included file did not seem to be relative to basedir!" );
		}
		String relativity = directory.substring( basedirPath.length() + 1 );
		while ( relativity.startsWith( "/" ) ) {
			relativity = relativity.substring( 1 );
		}
		return relativity;
	}

	/**
	 * (recursively) find the most recent timestamp in said directory.
	 *
	 * @param directory The directory to check.
	 * @param acceptor A filter telling which files to consider.
	 * @return The most recent {@link File#lastModified() timestamp} found.
	 */
	public static long findMostRecentTimestamp(File directory, FileFilter acceptor) {
		return findMostRecentTimestamp( 0L, directory, acceptor );
	}

	private static long findMostRecentTimestamp(long current, File directory, FileFilter acceptor) {
		long local = 0L;
		for ( File subPath : directory.listFiles() ) {
			final long temp;
			if ( subPath.isDirectory() ) {
				temp = findMostRecentTimestamp( current, directory, acceptor );
			}
			else if ( acceptor.accept( subPath ) ) {
				temp = subPath.lastModified();
			}
			else {
				temp = local;
			}

			if ( temp > local ) {
				local = temp;
			}
		}
		return local > current ? local : current;
	}

	/**
	 * (recursively) find the least recent timestamp in said directory.
	 *
	 * @param directory The directory to check.
	 * @param acceptor A filter telling which files to consider.
	 * @return The least recent {@link File#lastModified() timestamp} found.
	 */
	public static long findLeastRecentTimestamp(File directory, FileFilter acceptor) {
		return findLeastRecentTimestamp( Long.MAX_VALUE, directory, acceptor );
	}

	public static long findLeastRecentTimestamp(long current, File directory, FileFilter acceptor) {
		long local = Long.MAX_VALUE;
		for ( File subPath : directory.listFiles() ) {
			final long temp;
			if ( subPath.isDirectory() ) {
				temp = findLeastRecentTimestamp( current, directory, acceptor );
			}
			else if ( acceptor.accept( subPath ) ) {
				temp = subPath.lastModified();
			}
			else {
				temp = local;
			}

			if ( temp < local ) {
				local = temp;
			}
		}
		return local < current ? local : current;
	}

	/**
	 * Here for consistent handling of full path names.
	 *
	 * @param path The path
	 * @return The full path name.
	 */
	public static String resolveFullPathName(File path) {
		return path.getAbsolutePath();
	}

	private static void buildInjectedInternalEntitySubset(StringBuffer buffer, List<ValueInjection> valueInjections) {
		for ( ValueInjection injection : valueInjections ) {
			buffer.append( "<!ENTITY " )
					.append( injection.getName() )
					.append( " \"" )
					.append( injection.getValue() )
					.append( "\">\n" );
		}
	}

}
