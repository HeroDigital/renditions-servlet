package com.herodigital.wcm.ext.renditions.model;

import java.util.Arrays;
import java.util.List;

public enum RenditionType {
	/** The web rendition is often a jpeg (which may be compressed). */
	WEB("web"),
	/** AEM uses thumbnail PNG renditions internally in author mode. Publish may use these also, however, web renditions are preferred. */
	THUMBNAIL("thumbnail"), 
	/** Special "rendition" with no specific dimensions or extension associated (unlike WEB and THUMBNAIL). Therefore, code must typically treat this enum as a special case. */
	ORIGINAL("original");
	
	private final String name;
	
	private RenditionType(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Gets all renditions except for the special ORIGINAL rendition.
	 * 
	 * @return
	 */
	public static List<RenditionType> getNonOriginalRenditionTypes() {
		return Arrays.asList(WEB, THUMBNAIL);
	}
}
