package com.herodigital.wcm.ext.renditions.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.herodigital.wcm.ext.renditions.model.RenditionMeta;
import com.herodigital.wcm.ext.renditions.model.RenditionType;
import com.herodigital.wcm.ext.renditions.service.AssetRenditionResolver;

@Component(immediate=true, metatype=false, label="Asset Rendition Resolver Service")
@Service(AssetRenditionResolver.class)
public class AssetRenditionResolverImpl implements AssetRenditionResolver {
	
	private static final Logger log = LoggerFactory.getLogger(AssetRenditionResolverImpl.class);
	
	private static final String[] RENDITION_EXTENSIONS = new String[]{"jpeg", "png"};
	
	/**
	 * Returned {@link Rendition} is resolved with the following priority rules:
	 * <ol>
	 * <li>Rendition type (eg. web, thumbnail)</li>
	 * <li>Rendition extension (eg. png, jpeg)</li>
	 * </ol>
	 * First match is returned. Requested dimensions are always honored.
	 * <p>
	 * {@link RenditionType#ORIGINAL} is a special case which will always result in the "original"
	 * returned, or null if missing. No extra search rules are used.
	 */
	@Override
	public Rendition resolveRendition(Asset asset, RenditionMeta renditionMeta) {
		if (asset == null) return null;
		if (renditionMeta == null) return null;
		
		List<String> renditionPriorityList = buildSortedRenditions(renditionMeta);
		
		if (log.isTraceEnabled()) {
			log.trace("Searching for {} rendition in order of {}", asset.getPath(), renditionPriorityList);
		}
		
		Rendition rendition = null;
		for (String name : renditionPriorityList) {
			if (log.isTraceEnabled()) log.trace("Searching for {} for {}", name, asset.getPath());
			
			rendition = asset.getRendition(name);
			if (rendition != null) break;
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Resolved rendition {} for {} and {}", (rendition == null) ? "null" : rendition.getName(), asset.getPath(), renditionMeta);
		}
		return rendition;
	}
	
	private List<String> buildSortedRenditions(RenditionMeta renditionMeta) {
		List<String> renditions = new ArrayList<>();
		List<RenditionType> sortedTypes = buildSortedRenditionTypes(renditionMeta.getRenditionType());
		List<String> sortedExtensions = buildSortedExtensions(renditionMeta.getExtension());
		
		for (RenditionType renditionType : sortedTypes) {
			if (RenditionType.ORIGINAL == renditionType) {
				renditions.add(RenditionType.ORIGINAL.getName());
			} else {
				for (String extension : sortedExtensions) {
					StringBuilder sb = new StringBuilder();
					// cq5dam.web.1920.1080.png
					sb.append("cq5dam.")
						.append(renditionType.getName())
						.append(".")
						.append(renditionMeta.getWidth())
						.append(".")
						.append(renditionMeta.getHeight())
						.append(".")
						.append(extension);
					renditions.add(sb.toString());
				}
			}
		}
		return renditions;
	}
	
	/**
	 * Builds list of rendition types with {@code preference} as the first item.
	 * <p>
	 * {@link RenditionType#ORIGINAL} is special and is excluded from the result when it
	 * is not the {code preference}. When it is the {@code preference} it is the only item returned.
	 * 
	 * @param preference
	 * @return
	 */
	private List<RenditionType> buildSortedRenditionTypes(final RenditionType preference) {
		if (preference == RenditionType.ORIGINAL) return Arrays.asList(preference);
		
		List<RenditionType> list = RenditionType.getNonOriginalRenditionTypes();
		Collections.sort(list, new Comparator<RenditionType>() {
			public int compare(RenditionType o1, RenditionType o2) {
				if (preference == o1) return -1;
				if (preference == o2) return 1;
				return 0;
			}
		});
		return list;
	}
	
	private List<String> buildSortedExtensions(final String preference) {
		// protect against changes to RENDITION_EXTENSIONS
		List<String> list = new ArrayList<>(Arrays.asList(RENDITION_EXTENSIONS));
		Collections.sort(list, new Comparator<String>() {
			public int compare(String o1, String o2) {
				if (preference.equals(o1)) return -1;
				if (preference.equals(o2)) return 1;
				return 0;
			}
		});
		return list;
	}

}
