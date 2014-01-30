The @PLUGIN@ plugin allows Gerrit users to upload and share images.

The plugin installs a new menu `Tools` -> `Image Upload` where users
can upload images. After the upload the user gets an URL under which
the uploaded image is available. This is useful for sharing screenshots
and linking them from review comments.

<a id="setup"></a>
Setup
-----
The uploaded images are stored in the `refs/images/*` namespace of the
`All-Projects` project. To be able to upload images the user must have
the [Create Reference](../../../Documentation/access-control.html#category_create)
on this namespace. After the installation of the plugin the Gerrit
administrator must assign this permission to enable the upload of
images.
