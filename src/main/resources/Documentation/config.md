Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    defaultProject = All-Images
```

<a id="block">
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
