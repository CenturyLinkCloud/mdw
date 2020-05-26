package com.centurylink.mdw.model.asset;

public class AssetPath {

    public final String pkg;
    public final String asset;

    public AssetPath(String path) {
        int lastSlash = path.lastIndexOf("/");
        if (lastSlash < 1 || lastSlash > path.length() - 2)
            throw new BadPathException(path);
        this.pkg = path.substring(0, lastSlash);
        this.asset = path.substring(lastSlash + 1);
    }

    public String ext() {
        int lastDot = asset.lastIndexOf(".");
        if (lastDot < 1 || lastDot > asset.length() - 2)
            return "";
        return asset.substring(lastDot + 1);
    }

    public String rootName() {
        int lastDot = asset.lastIndexOf(".");
        if (lastDot < 1 || lastDot > asset.length() - 2)
            return asset;
        return asset.substring(0, lastDot);
    }

    public String pkgPath() {
        return pkg.replace('.', '/');
    }

    public String toPath() {
        return pkgPath() + "/" + asset;
    }

    public String toString() {
        return pkg + "/" + asset;
    }

    public static class BadPathException extends RuntimeException {
        BadPathException(String message) {
            super(message);
        }
    }
}
