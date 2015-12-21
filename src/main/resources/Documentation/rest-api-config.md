@PLUGIN@ - /config/ REST API
============================

This page describes the '/config/' REST endpoints that are added by the
@PLUGIN@ plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="project-endpoints"> @PLUGIN@ Endpoints
--------------------------------------------

### <a id="get-config"> Get Config
_GET /config/server/@PLUGIN@~config_

Gets the configuration of the @PLUGIN@ plugin.

#### Request

```
  GET /config/server/@PLUGIN@~config HTTP/1.0
```

As response a [ConfigInfo](#config-info) entity is returned that
contains the configuration of the @PLUGIN@ plugin.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "default_project": "All-Images",
    "enable\_image_server": true
  }
```

### <a id="put-config"> Put Config
_PUT /config/server/@PLUGIN@~config_

Sets the configuration of the @PLUGIN@ plugin.

The new configuration must be specified as a [ConfigInfo](#config-info)
entity in the request body. Not setting a parameter leaves the
parameter unchanged.

#### Request

```
  PUT /config/server/@PLUGIN@~config HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "default_project": "All-Images"
  }
```

<a id="json-entities">JSON Entities
-----------------------------------

### <a id="config-info"></a>ConfigInfo

The `ConfigInfo` entity contains the configuration of the @PLUGIN@
plugin.

* _default\_project_: The project to which images should be uploaded by
  default.
* _link\_decoration_: Decoration for image links in the Gerrit WebUI.
  `NONE`: no decoration, `TOOLTIP`: the image is shown as tooltip on
  mouse over an image link, `INLINE`: the image is inlined instead of
  the URL.
* _stage_: Whether images should be staged before upload.
* _enable\_image\_server_: Whether Gerrit is used as image server.
* _pattern_: JavaScript Regular expression to match URLs of images
  that should be embedded.
* _upload\_url_: URL to upload images.

SEE ALSO
--------

* [Config related REST endpoints](../../../Documentation/rest-api-config.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
