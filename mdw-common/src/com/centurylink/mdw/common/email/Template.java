/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.email;

import java.util.Date;

import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;

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
