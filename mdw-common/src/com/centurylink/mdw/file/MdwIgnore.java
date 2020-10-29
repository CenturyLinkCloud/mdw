package com.centurylink.mdw.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Currently only supports a straight dir/file list (no wildcards or subpaths).
 * TODO: proper ignore pattern processing (like .gitignore)
 */
public class MdwIgnore {

    public static final String MDW_IGNORE = ".mdwignore";

    List<File> excludes = new ArrayList<>();

    public MdwIgnore(File dir) throws IOException {
        File ignoreFile = new File(dir + "/" + MDW_IGNORE);
        if (ignoreFile.exists()) {
            String list = new String(Files.readAllBytes(Paths.get(ignoreFile.getPath()))).trim();
            for (String line : list.split("\n")) {
                line = line.trim();
                if (!line.startsWith("#"))
                    excludes.add(new File(dir + "/" + line));
            }
        }
    }

    /**
     * Hardcoded for node_modules and Mac's annoying .DS_Store files.
     */
    public boolean isIgnore(File file) {
        return excludes.contains(file) ||
                ".DS_Store".equals(file.getName()) ||
                ("node_modules".equals(file.getName()) && file.isDirectory());
    }
}
