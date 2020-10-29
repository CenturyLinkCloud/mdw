package com.centurylink.mdw.drools;

import org.kie.api.KieBase;

import com.centurylink.mdw.model.asset.Asset;

/**
 * Represents a compiled Drools KnowledgeBase (along with its underlying Asset).
 */
public class KnowledgeBaseAsset {

  private Asset asset;
  public Asset getAsset() { return asset; }

  private KieBase knowledgeBase;
  public KieBase getKnowledgeBase() { return knowledgeBase; }

  public KnowledgeBaseAsset(KieBase knowledgeBase, Asset asset) {
      this.knowledgeBase = knowledgeBase;
      this.asset = asset;
  }

}
