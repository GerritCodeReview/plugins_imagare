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

package com.googlesource.gerrit.plugins.imagare;

import com.google.gerrit.extensions.restapi.RestResource;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.project.RefControl;
import com.google.inject.TypeLiteral;

public class ImageResource implements RestResource {
  public static final TypeLiteral<RestView<ImageResource>> IMAGE_KIND =
      new TypeLiteral<RestView<ImageResource>>() {};

  private final RefControl refControl;

  ImageResource(RefControl refControl) {
    this.refControl = refControl;
  }

  public RefControl getControl() {
    return refControl;
  }

  public Project.NameKey getProject() {
    return refControl.getProjectControl().getProject().getNameKey();
  }

  public String getRef() {
    return refControl.getRefName();
  }

  public Branch.NameKey getBranchKey() {
    return new Branch.NameKey(getProject(), getRef());
  }
}
