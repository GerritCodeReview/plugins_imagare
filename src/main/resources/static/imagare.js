// Copyright (C) 2014 The Android Open Source Project
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

Gerrit.install(function(self) {
    function onHistory(t) {
      Gerrit.get('/accounts/self/preference', function(r) {
        if ('TOOLTIP' === r.link_decoration) {
          addTooltips();
        } else if ('INLINE' === r.link_decoration) {
          inlineImages();
        }
      });
    }

    function inlineImages() {
      var l = document.links;
      for(var i = 0; i < l.length; i++) {
        if (isImage(l[i].href)) {
          var a = document.createElement('a');
          a.setAttribute('href', l[i].href);
          var img = document.createElement('img');
          img.setAttribute('src', l[i].href);
          img.setAttribute('style', 'border: 1px solid #B3B2B2;');
          a.appendChild(img);
          l[i].parentNode.replaceChild(a, l[i]);
        }
      }
    }

    function addTooltips() {
      var l = document.links;
      for(var i = 0; i < l.length; i++) {
        if (isImage(l[i].href)) {
          l[i].onmouseover = function (evt) {
            var img = document.createElement('img');
            img.setAttribute('src', this.href);
            img.setAttribute('style', 'border: 1px solid #B3B2B2; position: absolute; bottom: ' + (this.offsetHeight + 3) + 'px');
            this.parentNode.insertBefore(img, this);
            this.onmouseout = function (evt) {
              this.parentNode.removeChild(this.previousSibling);
            }
          }
        }
      }
    }

    function isImage(href) {
      return href.match(window.location.hostname + '.*src/.*/rev/.*/.*\.(jpg|jpeg|png|gif|bmp|ico|svg|tif|tiff)')
    }

    Gerrit.on('history', onHistory);
  });
