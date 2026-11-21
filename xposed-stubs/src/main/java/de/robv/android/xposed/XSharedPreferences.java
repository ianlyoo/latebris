package de.robv.android.xposed;

import java.io.File;

/**
 * Compile-time stub of the methods Latebris calls; the framework (LSPosed or Vector)
 * provides the real class, which also implements android.content.SharedPreferences.
 */
public final class XSharedPreferences {
    public XSharedPreferences(String packageName, String prefFileName) {
        throw new UnsupportedOperationException("stub");
    }

    public File getFile() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean hasFileChanged() {
        throw new UnsupportedOperationException("stub");
    }

    public void reload() {
        throw new UnsupportedOperationException("stub");
    }

    public String getString(String key, String defValue) {
        throw new UnsupportedOperationException("stub");
    }
}
