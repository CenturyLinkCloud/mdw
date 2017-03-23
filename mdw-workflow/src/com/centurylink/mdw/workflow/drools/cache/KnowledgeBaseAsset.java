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
