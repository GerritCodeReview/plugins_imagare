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

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.RefPermission;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ImagesCollection
    implements ChildCollection<ProjectResource, ImageResource> {
  private final DynamicMap<RestView<ImageResource>> views;
  private final Provider<PostImage> createImage;
  private final PermissionBackend permissionBackend;

  @Inject
  public ImagesCollection(
      DynamicMap<RestView<ImageResource>> views,
      Provider<PostImage> createImage,
      PermissionBackend permissionBackend) {
    this.views = views;
    this.createImage = createImage;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public RestView<ProjectResource> list() throws ResourceNotFoundException {
    throw new ResourceNotFoundException();
  }

  @Override
  public ImageResource parse(ProjectResource parent, IdString id) throws ResourceNotFoundException {
    Branch.NameKey branchName = new Branch.NameKey(parent.getNameKey(), id.get());
    if (permissionBackend
        .user(parent.getControl().getUser())
        .ref(branchName)
        .testOrFalse(RefPermission.READ)) {
      return new ImageResource(parent.getControl().controlForRef(branchName));
    }
    throw new ResourceNotFoundException(id);
  }

  @Override
  public DynamicMap<RestView<ImageResource>> views() {
    return views;
  }

  @Override
  @SuppressWarnings("unchecked")
  public PostImage post(ProjectResource parent) throws RestApiException {
    return createImage.get();
  }
}
