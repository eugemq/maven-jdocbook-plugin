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

/**
 * Collection of utilities for dealing with i18n support, as defined by GNU gettext.
 *
 * @author Steve Ebersole
 */
public class I18nUtils {
	/**
	 * Is the given file a GNU gettext POT file?
	 * <p/>
	 * The determination here is made solely upon the file extension currently.
	 *
	 * @param file The file to check.
	 * @return True if it is considered a POT file; false otherwise.
	 */
	public static boolean isPotFile(File file) {
		return "pot".equals( FileUtils.getExtension( file.getName() ) );
	}

	/**
	 * Given a source file, determine its correspnding GNU gettext POT file name.
	 *
	 * @param source The source file.
	 * @return The corresponding POT file name.
	 */
	public static String determinePotFileName(File source) {
		return FileUtils.removeExtension( source.getName() ) + ".pot";
	}

	/**
	 * Given a source file (or a POT file), determine its correspnding GNU gettext PO file name.
	 *
	 * @param template The source (or POT) file.
	 * @return The corresponding PO file name.
	 */
	public static String determinePoFileName(File template) {
		return FileUtils.removeExtension( template.getName() ) + ".po";
	}

}
