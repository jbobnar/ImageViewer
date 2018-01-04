/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.colorprofile;

/**
 * LinuxColorProfileLoader provides the location of system color profiles and the default color profile on linux
 * operating systems.
 *
 * @author Jaka Bobnar
 *
 */
public class LinuxColorProfileLoader {

    private static final String DEFAULT_LOCATION = "/usr/share/color/icc/colord";

    /**
     * Returns the location where the system color profiles are stored.
     *
     * @return the path to the folder with color profile files
     */
    public static String getColorProfilesFolder() {
        return DEFAULT_LOCATION;
    }

    /**
     * The array of color profiles for all currently active devices.
     *
     * @return the color profiles
     */
    public static String[] getCurrentColorProfiles() {
        return new String[0];
    }
}
