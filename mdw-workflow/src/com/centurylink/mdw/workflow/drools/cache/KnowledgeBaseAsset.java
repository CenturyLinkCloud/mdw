/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.drools.cache;

import org.drools.KnowledgeBase;

import com.centurylink.mdw.model.asset.Asset;

/**
 * Represents a compiled Drools KnowledgeBase (along with its underlying Asset).
 */
public class KnowledgeBaseAsset {

  private Asset asset;
  public Asset getAsset() { return asset; }

  private KnowledgeBase knowledgeBase;
  public KnowledgeBase getKnowledgeBase() { return knowledgeBase; }

  public KnowledgeBaseAsset(KnowledgeBase knowledgeBase, Asset asset) {
      this.knowledgeBase = knowledgeBase;
      this.asset = asset;
  }

}
