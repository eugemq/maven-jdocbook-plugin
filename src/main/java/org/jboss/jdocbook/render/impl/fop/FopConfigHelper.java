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
package org.jboss.jdocbook.render.impl.fop;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.MalformedURLException;

import org.apache.fop.fonts.FontCache;
import org.apache.fop.fonts.EmbedFontInfo;
import org.apache.fop.fonts.FontTriplet;
import org.apache.fop.fonts.FontResolver;
import org.apache.fop.fonts.FontSetup;
import org.apache.fop.fonts.FontEventListener;
import org.apache.fop.fonts.autodetect.FontInfoFinder;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.ConfigurationUtil;
import org.jboss.jdocbook.render.RenderingEnvironment;
import org.jboss.jdocbook.JDocBookProcessException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.DOMBuilder;

/**
 * Delegate to help manage building a FOP user-config
 *
 * @author Steve Ebersole
 */
public class FopConfigHelper {
	/**
	 * {@link FontCache} persistent cache file path, relative to jDocBook work directory.
	 */
	public static final String FONT_CACHE_FILE = "fop/fop-fonts.cache";

	private static FopConfigHelper INSTANCE;

	public static DefaultConfiguration getFopConfiguration(RenderingEnvironment environment) {
		if ( INSTANCE == null ) {
			environment.log().info( "creating FOP user-config DOM" );
			INSTANCE =  new FopConfigHelper( environment );
		}
		return INSTANCE.fopConfiguration;
	}

	private RenderingEnvironment environment;
	private DefaultConfiguration fopConfiguration;

	public FopConfigHelper(RenderingEnvironment environment) {
		this.environment = environment;
		this.fopConfiguration = buildFopConfiguration();
	}

	private DefaultConfiguration buildFopConfiguration() {
		DefaultConfiguration fopUserConfiguration = new DefaultConfiguration( "fop" );
		fopUserConfiguration.setAttribute( "version", "1.0" );

		DefaultConfiguration strictConfig = new DefaultConfiguration( "strict-configuration" );
		strictConfig.setValue( true );
		fopUserConfiguration.addChild( strictConfig );

		DefaultConfiguration renderersConfig = new DefaultConfiguration( "renderers" );
		fopUserConfiguration.addChild( renderersConfig );

		DefaultConfiguration pdfRendererConfig = new DefaultConfiguration( "renderer" );
		pdfRendererConfig.setAttribute( "mime", "application/pdf" );
		renderersConfig.addChild( pdfRendererConfig );

		pdfRendererConfig.addChild( buildFontsConfig() );

		dumpUserConfigToFile( fopUserConfiguration );

		return fopUserConfiguration;
	}

	@SuppressWarnings({ "unchecked" })
	private DefaultConfiguration buildFontsConfig() {
		DefaultConfiguration fontsConfig = new DefaultConfiguration( "fonts" );

// did not work for me :(
//			if ( environment.getFontDirectories() != null ) {
//				for ( File fontDirectory : environment.getFontDirectories() ) {
//					DefaultConfiguration fontDirectoryElement = new DefaultConfiguration( "directory" );
//					fontDirectoryElement.setValue( fontDirectory.getAbsolutePath() );
//					fontsConfig.addChild( fontDirectoryElement );
//				}
//			}

		// this code is mostly copied from
		// http://dev.plutext.org/trac/docx4j/browser/trunk/docx4j/src/main/java/org/docx4j/convert/out/pdf/viaXSLFO/Conversion.java
		//
		// Thanks to Jason Harrop from the fop-user list (and who wrote that code) for pointing it out
		for ( EmbedFontInfo embedFontInfo : locateEmbedFontInfos() ) {
			DefaultConfiguration fontConfig = new DefaultConfiguration( "font" );
			fontsConfig.addChild( fontConfig );
			fontConfig.setAttribute( "embed-url", embedFontInfo.getEmbedFile() );
			if ( embedFontInfo.getSubFontName() != null ) {
				fontConfig.setAttribute( "sub-font", embedFontInfo.getSubFontName() );
			}

			FontTriplet triplet = ( FontTriplet ) embedFontInfo.getFontTriplets().get( 0 );

			fontConfig.addChild(
					generateFontTripletConfig(
							triplet.getName(),
							triplet.getStyle(),
							triplet.getWeight()
					)
			);
			fontConfig.addChild(
					generateFontTripletConfig(
							triplet.getName(),
							"normal",
							"bold"
					)
			);
			fontConfig.addChild(
					generateFontTripletConfig(
							triplet.getName(),
							"italic",
							"bold"
					)
			);
			fontConfig.addChild(
					generateFontTripletConfig(
							triplet.getName(),
							"italic",
							"normal"
					)
			);
		}

		if ( environment.getOptions().isAutoDetectFontsEnabled() ) {
			DefaultConfiguration autoDetect = new DefaultConfiguration( "auto-detect" );
			fontsConfig.addChild( autoDetect );
		}

		return fontsConfig;
	}

	private List<EmbedFontInfo> locateEmbedFontInfos() {
		List<EmbedFontInfo> infoList = new ArrayList<EmbedFontInfo>();
		if ( environment.getFontDirectories() != null ) {
			FontCache fontCache = environment.getOptions().isUseFopFontCache()
					? FontCache.loadFrom( new File( getFopWorkDirectory(), "fop-font.cache" ) )
					: new FontCache();
			FontResolver fontResolver = FontSetup.createMinimalFontResolver();
			FontInfoFinder fontInfoFinder = new FontInfoFinder();
			fontInfoFinder.setEventListener(
					new FontEventListener() {
						public void fontSubstituted(Object source, FontTriplet requested, FontTriplet effective) {
							environment.log().info(
									"FOP font substitution : " + requested + " -> " + effective + "; source=" + source
							);
						}

						public void fontLoadingErrorAtAutoDetection(Object source, String fontURL, Exception e) {
							environment.log().info(
									"FOP autodetect font loading error : " + fontURL + "; source=" + source,
									e
							);
						}

						public void glyphNotAvailable(Object source, char ch, String fontName) {
							environment.log().trace(
									"Glyph not available for character [" + ch + "] in font " + fontName +
											"; source=" + source
							);
						}
					}
			);
			for ( File fontDirectory : environment.getFontDirectories() ) {
				for ( File fontFile : fontDirectory.listFiles() ) {
					EmbedFontInfo[] infos = fontInfoFinder.find( toURL( fontFile ), fontResolver, fontCache );
					if ( infos == null || infos.length == 0 ) {
						continue;
					}
					for ( EmbedFontInfo info : infos ) {
						if ( info.getEmbedFile() != null ) {
							infoList.add( info );
						}
					}
				}
			}
		}

		return infoList;
	}

	private URL toURL(File file) {
		try {
			return file.toURI().toURL();
		}
		catch ( MalformedURLException ignore ) {
		}
		return null;
	}

	private DefaultConfiguration generateFontTripletConfig(String name, String style, int weight) {
		return generateFontTripletConfig(
				name,
				style,
				weight >= 700 ? "bold" : "normal"
		);
	}

	private DefaultConfiguration generateFontTripletConfig(String name, String style, String weight) {
		DefaultConfiguration tripletConfig = new DefaultConfiguration( "font-triplet" );
		tripletConfig.setAttribute( "name", name );
		tripletConfig.setAttribute( "style", style );
		tripletConfig.setAttribute( "weight", weight );

		return tripletConfig;
	}

	private File fopWorkDirectory;

	private File getFopWorkDirectory() {
		if ( fopWorkDirectory == null ) {
			final File dir = new File( environment.getWorkDirectory(), "fop" );
			if ( dir.exists() ) {
				fopWorkDirectory = dir;
			}
			else {
				boolean created = dir.mkdirs();
				if ( created ) {
					fopWorkDirectory = dir;
				}
				else {
					environment.log().info( "Problem creating fop work directory" );
				}
			}
		}
		return fopWorkDirectory;
	}

	private void dumpUserConfigToFile(DefaultConfiguration fopUserConfiguration) {
		File dumpFile = new File( getFopWorkDirectory(), "generated-user-config.xml" );

		if ( ! dumpFile.exists() ) {
			try {
				//noinspection ResultOfMethodCallIgnored
				dumpFile.createNewFile();
			}
			catch ( IOException e ) {
				environment.log().error( "Unable to dump generated FOP user config", e );
			}
		}

		try {
			BufferedOutputStream outputStream = new BufferedOutputStream(
					new FileOutputStream( dumpFile )
			);

			XMLOutputter outputter = new XMLOutputter( Format.getCompactFormat().setIndent( "  " ) );
			outputter.output(
					new DOMBuilder().build( ConfigurationUtil.toElement( fopUserConfiguration ) ),
					outputStream
			);
		}
		catch ( FileNotFoundException e ) {
			// should never ever happen, see checks above..
			throw new JDocBookProcessException( "unable to open file for writing generated FOP user-config", e );
		}
		catch ( IOException e ) {
			environment.log().info( "Unable to write generated FOP user-config to file", e );
		}

	}
}
