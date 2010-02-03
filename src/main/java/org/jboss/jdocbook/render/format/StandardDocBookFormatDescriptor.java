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
package org.jboss.jdocbook.render.format;

/**
 * Contract for descriptors about standard DocBook formats.
 *
 * @author Steve Ebersole
 */
public interface StandardDocBookFormatDescriptor {
	/**
	 * The name of this format.
	 *
	 * @return The format name.
	 */
	public String getName();

	/**
	 * The standard file extension used for this format.
	 *
	 * @return The file extension
	 */
	public String getStandardFileExtension();

	/**
	 * The standard DocBook stylesheet for this format (as a classpath-relative resource name).
	 *
	 * @return The DocBook stylesheet resource name
	 */
	public String getStylesheetResource();

	/**
	 * The standard DocBook stylesheet for this format for 'profiling' (as a classpath-relative resource name).
	 *
	 * @return The DocBook 'profiling' stylesheet resource name
	 */
	public String getProfiledStylesheetResource();

	/**
	 * Does this format require setting the 'img.src.path' DocBook XSLT parameter?
	 *
	 * @return Is setting 'img.src.path' required?
	 */
	public boolean isImagePathSettingRequired();

	/**
	 * Does this format require copying the images over to the target directory?
	 *
	 * @return Is copying images to the target directory required?
	 */
	public boolean isImageCopyingRequired();

	/**
	 * Is this format doing chunking?
	 *
	 * @return Doing chunking?
	 */
	public boolean isDoingChunking();
}
