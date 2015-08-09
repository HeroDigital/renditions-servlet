package com.herodigital.wcm.ext.renditions.service;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.herodigital.wcm.ext.renditions.model.RenditionMeta;

/**
 * Rendition resolution for a {@link Asset}. 
 * 
 * @author joelepps
 *
 */
public interface AssetRenditionResolver {

	/**
	 * Closest matching rendition for an {@link Asset} and {@link RenditionMeta} template.
	 * <p>
	 * Implementation decides resolution priority and rules.
	 * 
	 * @param asset Asset to resolve
	 * @param renditionMeta Rendition requirements.
	 * @return Rendition or null if no rendition matching criteria can be found.
	 */
	public Rendition resolveRendition(Asset asset, RenditionMeta renditionMeta);
	
}
