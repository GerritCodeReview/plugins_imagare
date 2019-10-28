// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

let imagareRestApi;
let link_pattern;
let decoration;

function _createImage(url) {
  let img = document.createElement('img');
  img.setAttribute("src", url);
  img.setAttribute("style", "max-width: 100%; height: auto;");

  return img;
}

function _insertImages(node) {
  if (!node) {
    return;
  }

  for (const link of _getLinks(node)) {
    link.replaceWith(_createImage(link.href));
  }

  return node;
}

function _addTooltips(node) {
  if (!node) {
    return;
  }

  for (const link of _getLinks(node)) {
    link.onmouseover = (event) => {
      let img = _createImage(link.href);
      img.onmouseout = (event) => {
        event.target.replaceWith(link);
      }

      event.target.replaceWith(img);
    }
  }

  return node;
}

function _getLinks(node) {
  let links = [];
  for (const link of node.querySelectorAll("a")) {
    if (!link.href.match(link_pattern)) {
      continue;
    }
    links.push(link);
  }
  return links;
}

function decorateLinks(node) {
  if (decoration === "inline") {
    node = _insertImages(node);
  } else if (decoration === "tooltip") {
    node = _addTooltips(node);
  }
  return node;
}

Gerrit.install(plugin => {
  if (!window.Polymer) { return; }

  plugin.restApi('/accounts/self/imagare~preference')
    .get('')
    .then(prefs => {
      if (!prefs
        || !prefs.link_decoration
        || prefs.link_decoration.toLowerCase() === "none") {
        return;
      }

      link_pattern = prefs.pattern || '.*';
      decoration = prefs.link_decoration.toLowerCase();

      plugin.on('format-text', decorateLinks);

      Gerrit.emit('plugin-format-added');
    });
});
