/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.colorprofile;

import java.io.File;
import java.util.Locale;

/**
 * WindowsColorProfileLoader provides the native access to retrieve the location of the color profiles and the currently
 * applied color profile for all active graphics devices.
 *
 * @author Jaka Bobnar
 *
 */
public final class WinColorProfileLoader {

    private WinColorProfileLoader() {}
    
    private static final boolean LIB_LOADED;
    private static final String DEFAULT_LOCATION;

    static {
        // find out the what is the java architecture, to load the appropriate dll
        String s = System.getProperty("sun.arch.data.model");
        boolean loaded = false;
        if (s != null) {
            if ("64".equals(s.trim())) {
                loaded = loadLibrary(false);
            } else {
                loaded = loadLibrary(true);
            }
        }
        if (!loaded) {
            loaded = loadLibrary(true);
            if (!loaded) {
                loaded = loadLibrary(false);
            }

        }
        LIB_LOADED = loaded;

        // this is the default location on most systems, but just in case if windows is installed in a different
        // folder, rename this location to the actual one
        File file = new File("C:/Windows/system32/spool/drivers/color");
        String loc = null;
        if (file.exists()) {
            loc = file.getAbsolutePath();
        } else {
            s = System.getProperty("java.library.path");
            String[] paths = s.split("\\;");
            for (String p : paths) {
                String d = p.toLowerCase(Locale.UK);
                if (d.endsWith("\\system32\\") || d.endsWith("\\system32") || d.endsWith("/system32/")
                        || d.endsWith("/system32")) {
                    loc = p.replace("\\", "/");
                    break;
                }
            }
            if (loc != null) {
                if (loc.charAt(loc.length() - 1) != '/') {
                    loc = loc + '/';
                }
                loc = loc + "spool/drivers/color";
            }
        }
        DEFAULT_LOCATION = loc;
    }

    /**
     * Load the library.
     *
     * @param b32 true to load the 32-bit version or false to load the 64-bit version.
     *
     * @return true if the library was successfully loaded, or false otherwise
     */
    private static boolean loadLibrary(boolean b32) {
        if (b32) {
            try {
                System.loadLibrary("lib/ColorProfile32");
                return true;
            } catch (UnsatisfiedLinkError e) {
                // ignore, the method returns false
            }
        } else {
            try {
                System.loadLibrary("lib/ColorProfile64");
                return true;
            } catch (UnsatisfiedLinkError e) {
                // ignore, the method returns false
            }
        }
        return false;
    }

    private static native String getColorProfilesLocation();

    private static native String[] getColorProfiles();

    /**
     * Returns the folder in which the operating system holds all available color profiles. This is usually the same as
     * {@link #DEFAULT_LOCATION}.
     *
     * @return the color profiles folder
     */
    public static String getColorProfilesFolder() {
        String defaultLocation = DEFAULT_LOCATION;
        if (LIB_LOADED) {
            defaultLocation = getColorProfilesLocation().replace('\\', '/');
        }
        return defaultLocation;
    }

    /**
     * Returns the array of color profiles for all currently active graphics devices. The profiles are ordered by device
     * number.
     *
     * @return the array of color profiles
     */
    public static String[] getCurrentColorProfiles() {
        return LIB_LOADED ? getColorProfiles() : new String[0];
    }
}
