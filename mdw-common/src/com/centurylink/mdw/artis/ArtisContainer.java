/**
 * Copyright (c) 2015 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.artis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.qwest.artis.Artis;
import com.qwest.artis.utilities.ArtisUtility;
import com.centurylink.mdw.common.ApplicationContext;
import com.centurylink.mdw.common.constant.PropertyNames;
import com.centurylink.mdw.common.exception.PropertyException;
import com.centurylink.mdw.common.utilities.property.PropertyManager;

public class ArtisContainer {

    private static final Log LOG = LogFactory.getLog(ArtisContainer.class);

    private Artis artis = null;
    private String artisKey = "";
    private String configLocation = "";

    private boolean forceAll = false;
    private boolean forceWithConfig = false;
    private static ArtisContainer singleton = null;

    public ArtisContainer() {

    }

    private static synchronized ArtisContainer getSingleton() throws PropertyException {

        if (singleton == null) {
            singleton = new ArtisContainer();
            singleton.load();
        }

        return singleton;
    }

    public static ArtisContainer getArtisContainer() throws PropertyException {
        return getSingleton();
    }

    public void load() throws PropertyException {

        String key = PropertyManager.getProperty(PropertyNames.MDW_ARTIS_KEY);
        String forceAll = PropertyManager.getProperty(PropertyNames.MDW_ARTIS_FORCEALL);
        String forceWithConfig = PropertyManager
                .getProperty(PropertyNames.MDW_ARTIS_FORCE_WITH_CONFIG);

        this.artisKey = key;

        if (ApplicationContext.isWar())
            this.configLocation = new java.io.File(System.getProperty(PropertyManager.MDW_CONFIG_LOCATION) + "/artis.properties").getAbsolutePath();
        else
            this.configLocation = PropertyManager.getProperty(PropertyNames.MDW_ARTIS_CONF_LOCATION);

        setForceAll(Boolean.getBoolean(forceAll));
        setForceWithConfig(Boolean.getBoolean(forceWithConfig));
        startUp();
    }

    public void startUp() {
        LOG.debug("In ArtisContainer.instanciateArtis(): artisKey: " + this.artisKey
                + " configLocation: " + this.configLocation);
        this.artis = ArtisUtility.getArtis(artisKey, configLocation);
        LOG.debug("Key " + artis.getArtisKey());
    }

    public Artis getArtis() {
        LOG.debug("In ArtisContainer.getArtis()");
        return this.artis;
    }

    public void cleanUp() {
        LOG.debug("In ArtisContainer.cleanUp()");
        artis.close();
        ArtisUtility.cleanUp();
    }

    public boolean isForceAll() {
        return forceAll;
    }

    public void setForceAll(boolean forceAll) {
        this.forceAll = forceAll;
    }

    public boolean isForceWithConfig() {
        return forceWithConfig;
    }

    public void setForceWithConfig(boolean forceWithConfig) {
        this.forceWithConfig = forceWithConfig;
    }

}
