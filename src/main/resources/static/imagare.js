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
      if (!startsWith(t, "/c/")) {
        return;
      }

      var prefs = getPrefsFromCookie();
      if (prefs !== null) {
        convertImageLinks(prefs);
      } else {
        Gerrit.get('/accounts/self/' + self.getPluginName() + '~preference', function(prefs) {
          storePrefsInCookie(prefs);
          convertImageLinks(prefs);
        });
      }
    }

    function startsWith(s, p) {
      return s.slice(0, p.length) == p;
    }

    function storePrefsInCookie(prefs) {
      var date = new Date();
      date.setTime(date.getTime() + (1 * 24 * 60 * 60 * 1000)); // 1 day
      document.cookie = getCookieName()
          + "="
          + JSON.stringify(prefs)
          + "; expires=" + date.toGMTString()
          + "; path=/";
    }

    function getPrefsFromCookie() {
      var cookie = document.cookie;
      if (cookie.length > 0) {
        var cookieName = getCookieName();
        var start = cookie.indexOf(cookieName + "=");
        if (start != -1) {
            start = start + cookieName.length + 1;
            var end = document.cookie.indexOf(";", start);
            if (end == -1) {
                end = document.cookie.length;
            }
            return JSON.parse(unescape(document.cookie.substring(start, end)));
        }
      }
      return null;
    }

    function getCookieName() {
      return self.getPluginName() + "~prefs";
    }

    function convertImageLinks(prefs) {
      if (!prefs.pattern) {
        return;
      }

      if ('TOOLTIP' === prefs.link_decoration) {
        addTooltips(prefs.pattern);
      } else if ('INLINE' === prefs.link_decoration) {
        inlineImages(prefs.pattern);
      }
    }

    function inlineImages(pattern) {
      var l = document.links;
      for(var i = 0; i < l.length; i++) {
        if (l[i].href.match(pattern)) {
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

    function addTooltips(pattern) {
      var l = document.links;
      for(var i = 0; i < l.length; i++) {
        if (l[i].href.match(pattern)) {
          l[i].onmouseover = function (evt) {
            var img = document.createElement('img');
            img.setAttribute('src', this.href);
            img.setAttribute('style', 'border: 1px solid #B3B2B2; position: absolute; top: ' + (this.offsetTop + this.offsetHeight) + 'px');
            this.parentNode.insertBefore(img, this);
            this.onmouseout = function (evt) {
              this.parentNode.removeChild(this.previousSibling);
            }
          }
        }
      }
    }

    Gerrit.on('history', onHistory);
  });
