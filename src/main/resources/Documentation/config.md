Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    defaultProject = All-Images
```

<a id="default-project">
`plugin.@PLUGIN@.defaultProject`
:	The project to which images are uploaded per default.

<a id="link-decoration">
`plugin.@PLUGIN@.linkDecoration`
:	Decoration for image links in the Gerrit WebUI.
    `NONE`: no decoration, `TOOLTIP`: the image is shown as tooltip on
    mouse over an image link, `INLINE`: the image is inlined instead of
    the URL.

<a id="stage">
`plugin.@PLUGIN@.stage`
:	Whether images should be staged before upload.

<a id="enable-image-server">
`plugin.@PLUGIN@.enableImageServer`
:	Whether image server functionality should be enabled.
    If `true` images can be uploaded to Gerrit.
    When this parameter is changed the plugin must be reloaded.
    By default `true`.

<a id="pattern">
`plugin.@PLUGIN@.pattern`
:	URL pattern for image links. Matching images will be decorated.
    The URL pattern is a JavaScript regexp, the following characters
    must be escaped: `.`, `*`, `+`, `?`, `^`, `$`, `{`, `}`, `(`,
    `)`, `|`, `[`, `]`, `/`, `\`
    Images that match this pattern are rendered in the UI even when the
    corresponding mime type is not configured as safe in the Gerrit
    configuration.
    Must be set if an external image server is used.

<a id="upload-url">
`plugin.@PLUGIN@.uploadUrl`
:	Optional, URL for uploading images.
    May be set if an external image server is used.
    If Gerrit is used as image server the value is ignored.
