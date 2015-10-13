AEM Renditions Servlet
==============================

## Background

AEM comes with two kinds of image renditions for DAM assets (web and thumbnail). This bundle provides a single
servlet which can be used to request DAM image renditions in a prioritized manner.

Special "original" rendition can also be retrieved. However, it is only returned if specifically queried or in the case
that `rendition.servlet.redirect.on.missing.rendition` functionality is enabled.

## Documentaton

Supported Extensions: jpg, jpeg, png, svg, gif

Supported Rendition Type Selectors: imgw, imgt, imgo

* imgw = web rendition
* imgt = thumbnail rendition
* imgo = original raw image

Supported DAM Rendition MIME Types:

* image/jpeg
* image/png

Rendition Search Priority:

0. Rendition must have matching dimensions
1. Closest matching rendition type (web, thumbnail)
2. Closest matching rendition extension (png, jpeg)

## Configuration

`rendition.servlet.redirect.on.wrong.type`: Enabled by default. If request is made to a rendition with the wrong extension, a 302 redirect is returned the the URL with the matching extension. If disabled, then 415 error response is returned.

`rendition.servlet.redirect.on.missing.rendition`: Disabled by default. If requested rendition is not found, 404 error response is returned. If enabled, 302 redirect is returned to the original rendition.

## Examples

### Image

```
/content/dam/path/to/image/example.png/jcr:content/renditions/cq5dam.thumbnail.48.48.png
/content/dam/path/to/image/example.png/jcr:content/renditions/cq5dam.web.1280.1280.jpeg
/content/dam/path/to/image/example.png/jcr:content/renditions/original
```

### Request 1:

Right selector, right extension, right dimensions.

```
/content/dam/path/to/image/example.png.imgw.1280.1280.jpg
```

**Result**

200 Response. 

Underlying image is `/content/dam/path/to/image/example.png/jcr:content/renditions/cq5dam.web.1280.1280.jpeg`.

### Request 2:

Wrong selector, wrong extension, wrong dimensions.

`rendition.servlet.redirect.on.wrong.type` = true

`rendition.servlet.redirect.on.missing.rendition` = true

```
/content/dam/path/to/image/example.png.imgt.1281.1281.gif
```

**Result**

302 redirect to `/content/dam/path/to/image/example.png.imgo.png`.

Underlying image is `/content/dam/path/to/image/example.png/jcr:content/renditions/orignal`.

### Request 3:

Wrong selector, wrong extension, right dimensions.

`rendition.servlet.redirect.on.wrong.type` = true

`rendition.servlet.redirect.on.missing.rendition` = true

```
/content/dam/path/to/image/example.png.imgt.1280.1280.gif
```

**Result**

302 redirect to `/content/dam/path/to/image/example.png.imgt.1280.1280.jpg`.

Underlying image is `/content/dam/path/to/image/example.png/jcr:content/renditions/cq5dam.web.1280.1280.jpeg`.

### Request 4:

Wrong selector, right extension, right dimensions.

`rendition.servlet.redirect.on.wrong.type` = true

`rendition.servlet.redirect.on.missing.rendition` = true


```
/content/dam/path/to/image/example.png.imgt.1280.1280.jpg
```

**Result**

200 response. However, underlying search order will check for a 1280x1280 thumbnail rendition first, before using the web rendition.

Underlying image is `/content/dam/path/to/image/example.png/jcr:content/renditions/cq5dam.web.1280.1280.jpeg`.
