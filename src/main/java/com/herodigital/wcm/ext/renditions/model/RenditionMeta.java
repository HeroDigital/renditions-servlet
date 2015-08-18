package com.herodigital.wcm.ext.renditions.model;

/**
 * Descriptor of all rendition facets needed when requesting a DAM image rendition. 
 * 
 * @author joelepps
 *
 */
public class RenditionMeta {
	
	private final RenditionType type;
	private final int width;
	private final int height;
	private final String extension;
	
	public RenditionMeta(RenditionType type, int width, int height, String extension) {
		this.type = type;
		this.width = width;
		this.height = height;
		this.extension = extension;
	}

	public RenditionType getRenditionType() {
		return type;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
	public String getExtension() {
		return extension;
	}
	
	@Override
	public String toString() {
		return "ImageMeta [type=" + type + ", width=" + width + ", height=" + height + ", extension=" + extension + "]";
	}
}
