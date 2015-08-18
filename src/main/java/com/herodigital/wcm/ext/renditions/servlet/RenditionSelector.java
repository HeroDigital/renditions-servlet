package com.herodigital.wcm.ext.renditions.servlet;

import com.herodigital.wcm.ext.renditions.model.RenditionType;

/**
 * Mapping between the {@link ImageRenditionServlet} selectors and DAM
 * {@link RenditionType}. 
 * 
 * @author joelepps
 *
 */
enum RenditionSelector {
	IMG(ImageRenditionServlet.SELECTOR_RENDITION_WEB, RenditionType.WEB),
	THUMBNAIL(ImageRenditionServlet.SELECTOR_RENDITION_THUMB, RenditionType.THUMBNAIL),
	ORIGINAL(ImageRenditionServlet.SELECTOR_RENDITION_ORIGINAL, RenditionType.ORIGINAL);
	
	private final String selector;
	private final RenditionType renditionType;
	
	private RenditionSelector(String selector, RenditionType renditionType) {
		this.selector = selector;
		this.renditionType = renditionType;
	}

	public String getSelector() {
		return selector;
	}

	public RenditionType getRenditionType() {
		return renditionType;
	}
	
	/**
	 * @param selector
	 * @return The corresponding {@link RenditionSelector} or null.
	 */
	public static RenditionSelector fromSelector(String selector) {
		for (RenditionSelector candidate : RenditionSelector.values()) {
			if (candidate.getSelector().equals(selector)) return candidate;
		}
		return null;
	}
}
