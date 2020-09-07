load("@npm_bazel_rollup//:index.bzl", "rollup_bundle")
load("//tools/bzl:js.bzl", "polygerrit_plugin")
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/js:eslint.bzl", "eslint")
load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "imagare",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: imagare",
        "Gerrit-Module: com.googlesource.gerrit.plugins.imagare.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.imagare.HttpModule",
    ],
    resources = glob(["src/main/**/*"]),
    resource_jars = [":gr-imagare-static"],
)

genrule2(
    name = "gr-imagare-static",
    srcs = [":gr-imagare"],
    outs = ["gr-imagare-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :gr-imagare) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

rollup_bundle(
    name = "imagare-bundle",
    srcs = glob(["gr-imagare/*.js"]),
    entry_point = "gr-imagare/gr-imagare.js",
    rollup_bin = "//tools/node_tools:rollup-bin",
    sourcemap = "hidden",
    format = 'iife',
    deps = [
        "@tools_npm//rollup-plugin-node-resolve",
    ],
)

polygerrit_plugin(
    name = "gr-imagare",
    app = "imagare-bundle.js",
    plugin_name = "imagare",
)

# Define the eslinter for the plugin
# The eslint macro creates 2 rules: lint_test and lint_bin
eslint(
    name = "lint",
    srcs = glob([
        "gr-imagare/**/*.js",
    ]),
    config = ".eslintrc.json",
    data = [],
    extensions = [
        ".js",
    ],
    ignore = ".eslintignore",
    plugins = [
        "@npm//eslint-config-google",
        "@npm//eslint-plugin-html",
        "@npm//eslint-plugin-import",
        "@npm//eslint-plugin-jsdoc",
    ],
)
