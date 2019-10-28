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

function _createImage(url) {
  let img = document.createElement('img');
  img.setAttribute("src", url);
  img.setAttribute("style", "max-width: 100%; height: auto;");

  return img;
}

function _insertImage(link) {
  if (!link) {
    return;
  }

  link.replaceWith(_createImage(link.href));
}

function _addTooltip(link) {
  if (!link) {
    return;
  }

  link.onmouseover = (event) => {
    let img = _createImage(link.href);
    img.onmouseout = (event) => {
      event.target.replaceWith(link);
    }

    event.target.replaceWith(img);
  }
}

function _getAccountPrefs() {
  return
}

function _replaceLinks(element, link_pattern, decoration) {
  let links = element.querySelectorAll("a");
  for (const link of links) {
    if (!link.href.match(link_pattern)) {
      continue;
    }

    if (decoration === "inline") {
      _insertImage(link);
    } else if (decoration === "tooltip") {
      _addTooltip(link);
    }
  }
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

      let link_pattern = prefs.pattern || '.*';
      let decoration = prefs.link_decoration.toLowerCase();

      let changeObserver = new MutationObserver(mutations => {
        mutations.forEach(mut => {
          _replaceLinks(mut.target, link_pattern, decoration);
        });
      });

      plugin.hook('formatted-text').onAttached(element => {
        if (!element.content.hasChildNodes()) {
          return;
        }

        _replaceLinks(element.content, link_pattern, decoration);

        changeObserver.observe(element.content, { childList: true });
      });
    });
});
