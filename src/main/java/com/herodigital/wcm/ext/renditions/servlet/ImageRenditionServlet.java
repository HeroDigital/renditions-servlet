package com.herodigital.wcm.ext.renditions.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.herodigital.wcm.ext.renditions.model.RenditionMeta;
import com.herodigital.wcm.ext.renditions.service.AssetRenditionResolver;

/**
 * Handles fetching of an asset image rendition. Primary advantage over out of the box AEM
 * thumbnail servlet is the support of both web, thumbnail, and original renditions.
 * <p>
 * Resolution of rendition is handled by {@link AssetRenditionResolver}.
 * <p>
 * 404 response may be returned if requested asset or rendition does not exist (depending on configuration).
 * 
 * @author joelepps
 *
 */
@Component(metatype=true, immediate=true, label="Web Image Rendition Servlet", description="Servlet which returns the web image rendition")
@Service(Servlet.class)
@Properties({
		@Property(name = "service.description", value = "Servlet which returns the web image rendtions"),
		@Property(name = "service.vendor", value = "Hero Digital"),
		@Property(name = "sling.servlet.resourceTypes", value="sling/servlet/default", propertyPrivate=true),
		@Property(name = "sling.servlet.selectors", value={ImageRenditionServlet.SELECTOR_RENDITION_WEB, ImageRenditionServlet.SELECTOR_RENDITION_THUMB, ImageRenditionServlet.SELECTOR_RENDITION_ORIGINAL }, propertyPrivate=true),
		@Property(name = "sling.servlet.extensions", value={ "png", "jpg", "jpeg", "svg", "gif" }, propertyPrivate=true),
		@Property(name = "sling.servlet.methods", value={ "GET" }, propertyPrivate=true),
})
public class ImageRenditionServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 7272927969196706776L;
	
	private static final Logger log = LoggerFactory.getLogger(ImageRenditionServlet.class);
	
	protected static final String SELECTOR_RENDITION_WEB = "imgw";
	protected static final String SELECTOR_RENDITION_THUMB = "imgt";
	protected static final String SELECTOR_RENDITION_ORIGINAL = "imgo";
	
	protected static final String FILE_REFERENCE = "fileReference";
	
	private static enum Selector { TYPE, WIDTH, HEIGHT } // used for ordinal position of selectors
	
	@Property(label = "Redirect On Unsupported Type", description = "Enabled by default. If request is made to a rendition with the wrong extension, a 302 redirect is returned the the URL with the matching extension. If disabled, then 415 error response is returned.", boolValue = true)
	public static final String REDIRECT_ON_WRONG_TYPE = "rendition.servlet.redirect.on.wrong.type";
	private boolean redirectOnWrongType;
	
	@Property(label = "Redirect On Missing Rendition", description = "Disabled by default. If requested rendition is not found, 404 error response is returned. If enabled, 302 redirect is returned to the original rendition.", boolValue = false)
	public static final String REDIRECT_ON_MISSING_RENDITION = "rendition.servlet.redirect.on.missing.rendition";
	private boolean redirectOnMissingRendition;
	
	@Reference
	private AssetRenditionResolver assetRenditionResolver;
	
	@Activate
	@SuppressWarnings("unchecked")
	public void activate(ComponentContext context) {
		Dictionary<String, ?> properties = context.getProperties();
		redirectOnWrongType = toBoolean(properties.get(REDIRECT_ON_WRONG_TYPE));
		redirectOnMissingRendition = toBoolean(properties.get(REDIRECT_ON_MISSING_RENDITION));
	}
	
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
		// Convert request to convenience object
		RenditionMeta renditionMeta = buildRenditionMeta(request);
		if (renditionMeta == null) {
			log.debug("Failed to build rendition meta for {}", request.getPathInfo());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// Resolve image resource
		Resource resource = request.getResource();
		if (resource == null) {
			log.debug("Missing dam asset at {}", request.getPathInfo());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// Adapt image resource to dam object, supports node with fileReference to image
		Asset damAsset = resource.adaptTo(Asset.class);
		if (damAsset == null) {
			damAsset = extractAssetFromFileReference(resource);
		}
		if (damAsset == null) {
			log.debug("Cannot resolve dam asset at {} for {}", resource.getPath(), request.getPathInfo());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		// Resolve dam asset + meta data to actual rendition
		Rendition rendition = assetRenditionResolver.resolveRendition(damAsset, renditionMeta);
		if (rendition == null) {
			if (redirectOnMissingRendition) {
				sendRedirectToOriginalRendition(request, response, damAsset);
				return;
			} else {
				log.debug("Missing rendition for {} and {}", renditionMeta, damAsset.getPath());
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
		
		// If extension does not match rendition mime type, redirect or 415 error
		if (!rendition.getMimeType().contains(renditionMeta.getExtension())) {
			if (redirectOnWrongType) {
				sendRedirectToProperExtension(request, response, rendition, renditionMeta);
				return;
			} else {
				log.debug("Wrong extension for {} and request {}", rendition.getMimeType(), renditionMeta.getExtension());
				response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
				return;
			}
		}
		
		writeResponse(response, rendition);
	}
	
	private static RenditionMeta buildRenditionMeta(SlingHttpServletRequest request) {
		String[] selectors = request.getRequestPathInfo().getSelectors();
		String extension = request.getRequestPathInfo().getExtension();
		
		RenditionSelector renditionSelector = null;
		int width = 0;
		int height = 0;
		if (selectors.length >= 1 && SELECTOR_RENDITION_ORIGINAL.equals(selectors[Selector.TYPE.ordinal()])) {
			// original rendition does not need or parse width/height
			renditionSelector = RenditionSelector.fromSelector(selectors[Selector.TYPE.ordinal()]);
		} else if (selectors.length == 3) {
			renditionSelector = RenditionSelector.fromSelector(selectors[Selector.TYPE.ordinal()]);
			width = NumberUtils.toInt(selectors[Selector.WIDTH.ordinal()]);
			height = NumberUtils.toInt(selectors[Selector.HEIGHT.ordinal()]);
		} else {
			log.debug("Selectors size is not 1 or 3: {}", Arrays.toString(selectors));
			return null;
		}
		
		if (extension.equals("jpg")) {
			log.trace("Updating extension jpg to jpeg");
			extension = "jpeg";
		}
		
		if (renditionSelector == null) {
			log.debug("Rendition type was not recognized: {}", Arrays.toString(selectors));
			return null;
		}
		
		RenditionMeta meta = new RenditionMeta(renditionSelector.getRenditionType(), width, height, extension);
		log.trace("Build rendition meta: {}", meta);
		
		return meta;
	}

	private static void writeResponse(SlingHttpServletResponse response, Rendition rendition) throws IOException {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			inputStream = rendition.getStream();
			outputStream = response.getOutputStream();
			
			// NOTE: This does not matter if dispatcher serves image	
			// Dispatcher will use extension of file, it does not save HTTP Content-Type header
			response.setContentType(rendition.getMimeType());
			
			IOUtils.copy(inputStream, outputStream);
		} finally {
			if (inputStream != null) inputStream.close();
			if (outputStream != null) outputStream.close();
		}
	}
	
	private static void sendRedirectToOriginalRendition(SlingHttpServletRequest request, SlingHttpServletResponse response, Asset asset) throws IOException {
		String requestURL = request.getRequestURL().toString();
		String path = asset.getPath();
		String ext = getExtension(asset.getMimeType());
		String redirect = path + "." + SELECTOR_RENDITION_ORIGINAL + "." + ext;
		
		log.warn("Requested {}, however, rendition not found. Redirecting to {}", requestURL, redirect);
		response.sendRedirect(redirect);
	}
	
	private static void sendRedirectToProperExtension(SlingHttpServletRequest request, SlingHttpServletResponse response, Rendition rendition, RenditionMeta renditionMeta) throws IOException {
		String requestURL = request.getRequestURL().toString();
		String requestExt = request.getRequestPathInfo().getExtension();
		String newExt = getExtension(rendition.getMimeType());
		String redirect = requestURL;
		redirect = redirect.replaceAll(requestExt+"$", newExt);
		
		log.warn("Requested {}, however, mime type of rendition is {}. Redirecting to {}", requestURL, rendition.getMimeType(), redirect);
		response.sendRedirect(redirect);
	}
	
	private static Asset extractAssetFromFileReference(Resource resource) {
		if (resource == null) return null;
		
		ValueMap valueMap = resource.adaptTo(ValueMap.class);
		if (valueMap != null) {
			String fileReference = valueMap.get(FILE_REFERENCE, String.class);
			if (fileReference != null) {
				Resource candidateResource = resource.getResourceResolver().getResource(fileReference);
				if (candidateResource != null) {
					return candidateResource.adaptTo(Asset.class);
				}
			}
		}
		return null;
	}
	
	private static String getExtension(String mimeType) throws IOException {
		if (mimeType != null && mimeType.contains("png")) {
            return "png";
        } else if (mimeType != null && mimeType.contains("jpeg")) {
            return "jpg";
        } else if (mimeType != null && mimeType.contains("svg")) {
            return "svg";
        } else if (mimeType != null && mimeType.contains("gif")) {
        	return "gif";
        } else {
        	throw new IOException("Unsupported mime type " + mimeType);
        }
	}
	
	private static boolean toBoolean(Object obj) {
		if (obj == null) return false;
		if (obj instanceof Boolean) return (Boolean) obj;
		return BooleanUtils.toBoolean(obj.toString());
	}

}
