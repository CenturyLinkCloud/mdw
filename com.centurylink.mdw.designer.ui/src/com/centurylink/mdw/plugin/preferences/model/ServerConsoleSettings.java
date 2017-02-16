/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.preferences.model;

import java.io.File;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

import com.centurylink.mdw.plugin.MdwPlugin;

public class ServerConsoleSettings implements PreferenceConstants {
    private int bufferSize;

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int i) {
        bufferSize = i;
    }

    private FontData fontData;

    public FontData getFontData() {
        return fontData;
    }

    public void setFontData(FontData fd) {
        fontData = fd;
    }

    private RGB fontRgb;

    public RGB getFontRgb() {
        return fontRgb;
    }

    public void setFontRgb(RGB rgb) {
        this.fontRgb = rgb;
    }

    private RGB backgroundRgb;

    public RGB getBackgroundRgb() {
        return backgroundRgb;
    }

    public void setBackgroundRgb(RGB rgb) {
        backgroundRgb = rgb;
    }

    public enum ClientShell {
        Karaf, Putty
    }

    private ClientShell clientShell = ClientShell.Karaf;

    public ClientShell getClientShell() {
        return clientShell;
    }

    public void setClientShell(ClientShell clientShell) {
        this.clientShell = clientShell;
    }

    private File clientShellExe;

    public File getClientShellExe() {
        return clientShellExe;
    }

    public void setClientShellExe(File shellExe) {
        this.clientShellExe = shellExe;
    }

    public ServerConsoleSettings() {
        initialize();
    }

    /**
     * initialize attribute values from preferences
     */
    public void initialize() {
        setDefaultValues();

        IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
        bufferSize = store.getInt(PREFS_SERVER_CONSOLE_BUFFER_SIZE);
        if (bufferSize == 0)
            bufferSize = store.getDefaultInt(PREFS_SERVER_CONSOLE_BUFFER_SIZE);

        FontData fd = new FontData(store.getString(PREFS_SERVER_CONSOLE_FONT));
        if (fd.equals(new FontData("Courier New", 10, SWT.NORMAL)))
            fd = new FontData(store.getDefaultString(PREFS_SERVER_CONSOLE_FONT));
        setFontData(fd);
        int red = store.getInt(PREFS_SERVER_CONSOLE_FONT_RED);
        if (red == 0)
            red = store.getDefaultInt(PREFS_SERVER_CONSOLE_FONT_RED);
        int green = store.getInt(PREFS_SERVER_CONSOLE_FONT_GREEN);
        if (green == 0)
            green = store.getDefaultInt(PREFS_SERVER_CONSOLE_FONT_GREEN);
        int blue = store.getInt(PREFS_SERVER_CONSOLE_FONT_BLUE);
        if (blue == 0)
            blue = store.getDefaultInt(PREFS_SERVER_CONSOLE_FONT_BLUE);
        setFontRgb(new RGB(red, green, blue));
        red = store.getInt(PREFS_SERVER_CONSOLE_BG_RED);
        if (red == 255)
            red = store.getDefaultInt(PREFS_SERVER_CONSOLE_BG_RED);
        green = store.getInt(PREFS_SERVER_CONSOLE_BG_GREEN);
        if (green == 255)
            green = store.getDefaultInt(PREFS_SERVER_CONSOLE_BG_GREEN);
        blue = store.getInt(PREFS_SERVER_CONSOLE_BG_BLUE);
        if (blue == 255)
            blue = store.getDefaultInt(PREFS_SERVER_CONSOLE_BG_BLUE);
        setBackgroundRgb(new RGB(red, green, blue));

        String clientShell = store.getString(PREFS_SERVER_CLIENT_SHELL);
        if (clientShell == null || clientShell.isEmpty())
            clientShell = store.getDefaultString(PREFS_SERVER_CLIENT_SHELL);
        setClientShell(ClientShell.valueOf(clientShell));

        String clientShellExePath = store.getString(PREFS_SERVER_CLIENT_SHELL_EXE_PATH);
        if (clientShellExePath != null && !clientShellExePath.isEmpty())
            setClientShellExe(new File(clientShellExePath));
        else
            setClientShellExe(null);
    }

    /**
     * @return whether the prefs include all required information
     */
    public boolean isComplete() {
        return true;
    }

    /**
     * Specifies the default values for server console prefs. Will only be
     * active before the user has configured anything.
     */
    public static void setDefaultValues() {
        IPreferenceStore store = MdwPlugin.getDefault().getPreferenceStore();
        store.setDefault(PREFS_SERVER_CONSOLE_BUFFER_SIZE,
                PREFS_DEFAULT_SERVER_CONSOLE_BUFFER_SIZE);
        FontData fd = new FontData("Courier New", 10, SWT.NORMAL);
        store.setDefault(PREFS_SERVER_CONSOLE_FONT, fd.toString());
        store.setDefault(PREFS_SERVER_CONSOLE_FONT_RED, 0);
        store.setDefault(PREFS_SERVER_CONSOLE_FONT_GREEN, 0);
        store.setDefault(PREFS_SERVER_CONSOLE_FONT_BLUE, 0);
        store.setDefault(PREFS_SERVER_CONSOLE_BG_RED, 255);
        store.setDefault(PREFS_SERVER_CONSOLE_BG_GREEN, 255);
        store.setDefault(PREFS_SERVER_CONSOLE_BG_BLUE, 255);
        store.setDefault(PREFS_SERVER_CLIENT_SHELL, ClientShell.Karaf.toString());
    }
}
