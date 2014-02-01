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

SEE ALSO
--------

* [Config related REST endpoints](../../../Documentation/rest-api-config.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
