Hero Digital Rendition Servlet
==============================

## Background

AEM comes with two kinds of image renditions for DAM assets (web and thumbnail). This bundle provides a single
servlet which can be used to request DAM image renditions in a prioritized manner.

Special "original" rendition can also be retrieved. However, it is only returned if specifically asked for and is not included in normal rendition resolution.

## Documentaton

Supported Extensions: jpg, jpeg, png

Supported Rendition Type Selectors: imgw, imgt, imgo

* imgw = web rendition
* imgt = thumbnail rendition
* imgo = original raw image

Rendition Search Priority:

0. Rendition must have matching dimensions
1. Closest matching rendition type (web, thumbnail)
2. Closest matching rendition extension (png, jpeg)

## Configuration

`rendition.servlet.redirect.on.wrong.type`: Enabled by default. If request is made to a rendition with the wrong extension, a 302 redirect is returned the the URL with the matching extension. If disabled, then 415 error response is returned.

`rendition.servlet.redirect.on.missing.rendition`: Disabled by default. If requested rendition is not found, 404 error response is returned. If enabled, 302 redirect is returned to the original rendition.

## Examples

All examples are based on default configuration.

```
/content/dam/path/to/image/example.imgw.1920.1080.jpg
OR
/content/dam/path/to/image/example.imgw.1920.1080.jpeg
```
This will query for a DAM rendition in the following search order:

1. cq5dam.web.1920.1080.jpeg
2. cq5dam.web.1920.1080.png (redirect to .jpg)
3. cq5dam.thumbnail.1920.1080.jpeg
4. cq5dam.thumbnail.1920.1080.png (redirect to .jpg)
5. 404 response

```
/content/dam/path/to/image/example.imgt.1920.1080.png
```
This will query for a DAM rendition in the following search order:

1. cq5dam.thumbnail.1920.1080.png
2. cq5dam.thumbnail.1920.1080.jpeg (redirect to .png)
3. cq5dam.web.1920.1080.png
4. cq5dam.web.1920.1080.jpeg (redirect to .png)
5. 404 response

```
/content/dam/path/to/image/example.imgo.png
```
This will query for the original DAM rendition in the following search order:

1. original
2. 404 response
