@PLUGIN@ - /project/ REST API
===================+=========

This page describes the '/project/' REST endpoints that are added by
the @PLUGIN@ plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="project-endpoints"> @PLUGIN@ Endpoints
---------------------------------------------

### <a id="upload-image"> Upload Image
_POST /project/[\{project-name\}](../../../Documentation/rest-api-projects.html#project-name)/@PLUGIN@~images_

Uploads an image.

The image must be specified in the request body as a
[ImageInput](#image-input) entity.

Caller must have the
[Create Reference](../../../Documentation/access-control.html#category_create)
access right on the `refs/images/*` namespace of the project.

#### Request

```
  POST /config/server/@PLUGIN@~images HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "image_data": "data:image/png;base64,iVBORw0KGgoAAAAN..."
  }
```

#### Response

```
  HTTP/1.1 201 Created
```

<a id="json-entities">JSON Entities
-----------------------------------

### <a id="image-input"></a>ImageInput

The `ImageInput` entity contains the image that should be uploaded.

* _file\_name_: The name of the image file (optional).
* _image\_data_: The image data URL.

SEE ALSO
--------

* [Project related REST endpoints](../../../Documentation/rest-api-project.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
