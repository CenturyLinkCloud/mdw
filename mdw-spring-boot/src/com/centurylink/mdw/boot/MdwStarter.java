package com.centurylink.mdw.boot;

import java.io.File;

public class MdwStarter {

    public MdwStarter(File bootDir) {
        this.bootDir = bootDir;
    }

    private File bootDir;
    public File getBootDir() {
        return bootDir;
    }


    /**
     * Path should begin with /
     */
    public File getFile(String path) {
        return new File(bootDir + path);
    }
}
