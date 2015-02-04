The @PLUGIN@ plugin allows Gerrit users to upload and share images.

The plugin installs a new menu `Tools` -> `Image Upload` where users
can upload images. After the upload the user gets an URL under which
the uploaded image is available. This is useful for sharing screenshots
and linking them from review comments.

<a id="setup"></a>
Setup
-----
The uploaded images are stored in the `refs/images/*` namespace of the
`All-Projects` project.

After the installation of the plugin the Gerrit administrator must
assign permissions on this namespace:

* [Read](../../../Documentation/access-control.html#category_read) to
  enable users to see the uploaded images.

* Both [Create Reference](../../../Documentation/access-control.html#category_create)
  and [Push](../../../Documentation/access-control.html#category_push)
  to enable users to upload images.

In addition Gerrit must be configured to
[treat the image MIME types as safe](../../../Documentation/config-gerrit.html#mimetype.name.safe)
because otherwise they would not get rendered.

To allow deletions of own uploaded images the global capability
`Delete Own Images` can be granted.

To allow the deletion of any uploaded image the
[Force Push](../../../Documentation/access-control.html#category_push)
access right can be assigned on the images namespace.
