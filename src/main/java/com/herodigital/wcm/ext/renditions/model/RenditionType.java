package com.herodigital.wcm.ext.renditions.model;

public enum RenditionType {
	WEB("web"), THUMBNAIL("thumbnail"), ORIGINAL("original");
	
	private final String name;
	
	private RenditionType(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
