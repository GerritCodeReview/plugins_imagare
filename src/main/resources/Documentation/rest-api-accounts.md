@PLUGIN@ - /accounts/ REST API
==============================

This page describes the '/accounts/' REST endpoints that are added by
the @PLUGIN@ plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="project-endpoints"> @PLUGIN@ Endpoints
--------------------------------------------

### <a id="get-config"> Get Preferences
_GET /accounts/[\{account-id\}](../../../Documentation/rest-api-accounts.html#account-id)/@PLUGIN@~preference_

Gets the preferences of a user for the @PLUGIN@ plugin.

#### Request

```
  GET /accounts/self/@PLUGIN@~preference HTTP/1.0
```

As response a [ConfigInfo](rest-api-config.html#config-info) entity is
returned that contains the preferences of a user for the @PLUGIN@
plugin.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8

  )]}'
  {
    "default_project": "All-Images",
    "link_decoration": "INLINE"
  }
```

### <a id="put-config"> Put Preferences
_PUT /accounts/[\{account-id\}](../../../Documentation/rest-api-accounts.html#account-id)/@PLUGIN@~preference_

Sets the configuration of the @PLUGIN@ plugin.

The new preferences must be specified as a [ConfigInfo](rest-api-config.html#config-info)
entity in the request body. Not setting a parameter means that the
parameter is unset and that the global setting for this parameter
applies again.

#### Request

```
  PUT /accounts/self/@PLUGIN@~preference HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "default_project": "All-Images"
  }
```

SEE ALSO
--------

* [Account related REST endpoints](../../../Documentation/rest-api-accounts.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
