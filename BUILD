load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "imagare",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/**/*"]),
    gwt_module = "com.googlesource.gerrit.plugins.imagare.Imagare",
    manifest_entries = [
        "Gerrit-PluginName: imagare",
        "Gerrit-Module: com.googlesource.gerrit.plugins.imagare.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.imagare.HttpModule",
    ]
)
