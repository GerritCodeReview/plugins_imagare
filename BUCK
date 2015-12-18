include_defs('//bucklets/gerrit_plugin.bucklet')

MODULE = 'com.googlesource.gerrit.plugins.imagare.Imagare'

gerrit_plugin(
  name = 'imagare',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  gwt_module = MODULE,
  manifest_entries = [
    'Gerrit-PluginName: imagare',
    'Gerrit-Module: com.googlesource.gerrit.plugins.imagare.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.imagare.HttpModule',
  ]
)

java_library(
  name = 'classpath',
  deps = GERRIT_GWT_API + GERRIT_PLUGIN_API + [
    ':imagare__plugin',
    '//lib/gwt:user',
  ],
)
