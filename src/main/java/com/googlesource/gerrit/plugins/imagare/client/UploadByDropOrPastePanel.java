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

package com.googlesource.gerrit.plugins.imagare.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class UploadByDropOrPastePanel extends VerticalPanel {

  UploadByDropOrPastePanel() {
    init0();

    setStyleName("imagare-image-upload-panel");
    getElement().setAttribute("contenteditable", "true");
    getElement().setAttribute("onpaste", "imagarePasteHandler(this, event)");
    getElement().setAttribute("ondrop", "imagareDropHandler(this, event)");
    getElement().setAttribute("onkeypress", "imagarePreventKeyPress(event)");

    add(new Label("drag and drop image here"));
    add(new Label("or"));
    add(new Label("paste image from clipboard with " + (isMac() ? "Cmd+V" : "Ctrl+V")));
  }

  private static native boolean isMac() /*-{
    return navigator.platform.toUpperCase().indexOf('MAC') >= 0;
  }-*/;

  private static native void init0() /*-{
    $wnd.imagarePreventKeyPress = function preventKeys(event) {
      event = event || window.event;
      var ctrlDown = event.ctrlKey || event.metaKey;
      if (!ctrlDown) {
        event.preventDefault();
      }
    }

    $wnd.imagarePasteHandler = function handlePaste(elem, e) {
      var savedContent = elem.innerHTML;

      var clipboardData = e.clipboardData || e.originalEvent.clipboardData;
      var items = clipboardData.items;

      if (JSON.stringify(items)) {
        // Chrome

        var blob;
        for (var i = 0; i < items.length; i++) {
          if (items[i].type.indexOf("image") === 0) {
            blob = items[i].getAsFile();
          }
        }

        if (blob !== null) {
          var reader = new FileReader();
          reader.onload = function(e) {
            @com.googlesource.gerrit.plugins.imagare.client.ImageUploader::stageImage(Ljava/lang/String;)(e.target.result);
          };
          reader.readAsDataURL(blob);
        }
      } else if (e && e.clipboardData && e.clipboardData.getData) {
        // Webkit

        if ((/text\/html/.test(e.clipboardData.types[0]))
          || (/text\/plain/.test(e.clipboardData.types[0]))) {
          elem.innerHTML = '<div>' + e.clipboardData.getData(e.clipboardData.types[0]) + '</div>';
        } else {
          elem.innerHTML = "";
        }

        waitOnPaste(10, elem, savedContent);
      } else {
        // other browser

        elem.innerHTML = "";
        waitOnPaste(10, elem, savedContent);
      }
    }

    function waitOnPaste(max, elem, savedContent) {
      if (elem.childNodes && elem.childNodes.length > 0) {
        stageImage(elem, savedContent);
      } else if (max > 0) {
        that = {
          m: max - 1,
          e: elem,
          s: savedContent
        }
        that.callself = function () {
          waitOnPaste(that.m, that.e, that.s)
        }
        setTimeout(that.callself, 20);
      }
    }

    function stageImage(elem, savedContent) {
      var imageData = elem.childNodes[0].getAttribute("src");
      elem.innerHTML = savedContent;
      @com.googlesource.gerrit.plugins.imagare.client.ImageUploader::stageImage(Ljava/lang/String;)(imageData);
    }

    $wnd.imagareDropHandler = function handleDrop(elem, event) {
      if (window.chrome) {
        event.preventDefault();
      }
      var savedContent = elem.innerHTML;
      var f = event.dataTransfer.files[0];
      if (f) {
        var r = new FileReader();
        r.onload = function(e) {
          elem.innerHTML = savedContent;
          if (f.type.match('image/.*')) {
            @com.googlesource.gerrit.plugins.imagare.client.ImageUploader::stageImage(Ljava/lang/String;Ljava/lang/String;)(e.target.result, f.name);
          } else {
            $wnd.Gerrit.showError('no image file');
          }
        }
        r.readAsDataURL(f);
      } else {
        $wnd.Gerrit.showError('Failed to load file.');
      }
    }
  }-*/;

  public void focus() {
    focus(getElement());
  }

  private static native void focus(Element elem) /*-{
    var range = document.createRange();
    var sel = window.getSelection();
    range.selectNode(elem);
    range.collapse(true);
    sel.removeAllRanges();
    sel.addRange(range);
    elem.focus();
  }-*/;

}
