/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.email;

import java.util.Date;

import com.centurylink.mdw.model.asset.AssetVersionSpec;

public class Template
{
  public enum Format
  {
    Facelet,
    HTML
  }

  private Long assetId;
  public Long getAssetId() { return assetId; }

  private String name;
  public String getName() { return name; }

  private AssetVersionSpec templateAssetVerSpec;
  public AssetVersionSpec getTemplateAssetVerSpec() { return templateAssetVerSpec; }

  private Format format;
  public Format getFormat() { return format; }

  private String content;
  public String getContent() { return content; }

  private Date loaded;
  public Date getLoaded() { return loaded; }

  public Template(Long assetId, String name, Format format, String content)
  {
    this.assetId = assetId;
    this.name = name;
    this.format = format;
    this.content = content;
    this.loaded = new Date();
  }

  public Template(Long assetId, AssetVersionSpec templateAssetVerSpec, Format format, String content)
  {
    this.assetId = assetId;
    this.templateAssetVerSpec = templateAssetVerSpec;
    this.format = format;
    this.content = content;
    this.loaded = new Date();
  }
}
