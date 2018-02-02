/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.colorprofile;

import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ColorProfileManager is a bridge to access the OS related mechanism to load the system default color profiles and their
 * location.
 *
 * @author Jaka Bobnar
 *
 */
public final class ColorProfileManager {

    private enum OS {
        WIN, NIX, MAC, UNKNOWN
    }
    
    private ColorProfileManager() {}

    /**
     * Returns the default color profile for the given graphical component. The component is expected to be displayed on
     * some device. The file containing the profile for that device is returned.
     *
     * @param component the component which identifies the device for which the profile should be loaded
     * @return the color profile file
     */
    public static File getColorProfileForComponent(Component component) {
        GraphicsDevice device = component.getGraphicsConfiguration().getDevice();
        return getColorProfileForDisplay(device);
    }

    /**
     * Returns the default color profile for the given device.
     *
     * @param device the device for which the profile is requested
     * @return the color profile file
     */
    public static File getColorProfileForDisplay(GraphicsDevice device) {
        return getColorProfileForDisplay(getScreenID(device));
    }

    /**
     * Returns the ID of the given display device.
     *
     * @param device the device, for which the id is requested
     * @return the id
     */
    private static int getScreenID(GraphicsDevice device) {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (int i = 0; i < devices.length; i++) {
            if (devices[i] == device) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the list of all available and supported color profile files.
     *
     * @return the color profile files that can be applied to the images
     */
    public static File[] getAvailableColorProfiles() {
        File folder = getColorProfileFolder();
        if (folder == null || !folder.exists()) {
            return new File[0];
        }
        File[] profiles = folder.listFiles();
        if (profiles == null) {
            return new File[0];
        }
        List<File> checkedList = new ArrayList<>();
        for (File f : profiles) {
            try (FileInputStream fis = new FileInputStream(f)) {
                ICC_Profile.getInstance(fis);
                checkedList.add(f);
            } catch (Exception e) {
                // ignore, file could not be read (corrupted?)
            }
        }
        return checkedList.toArray(new File[checkedList.size()]);
    }

    /**
     * Returns the folder in which the system default color profiles are stored.
     *
     * @return the folder containing the system default color profiles
     */
    private static File getColorProfileFolder() {
        switch (getOS()) {
        case WIN:
            return new File(WinColorProfileLoader.getColorProfilesFolder());
        case NIX:
            return new File(LinuxColorProfileLoader.getColorProfilesFolder());
        case MAC:
        case UNKNOWN:
        default:
            return null;
        }
    }

    /**
     * Returns the color profile for the display identified by the given ID.
     *
     * @param displayID the display ID
     * @return
     */
    private static File getColorProfileForDisplay(int displayID) {
        if (displayID < 0) {
            return null;
        }
        File folder = getColorProfileFolder();
        String[] files;
        switch (getOS()) {
        case WIN:
            files = WinColorProfileLoader.getCurrentColorProfiles();
            break;
        case NIX:
            files = LinuxColorProfileLoader.getCurrentColorProfiles();
            break;
        case MAC:
        case UNKNOWN:
        default:
            return null;
        }
        if (files.length > displayID) {
            return new File(folder, files[displayID]);
        }
        return null;
    }

    private static OS getOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.UK);
        if (os.contains("win")) {
            return OS.WIN;
        } else if (os.contains("linux") || os.contains("nix")) {
            return OS.NIX;
        } else if (os.contains("mac")) {
            return OS.MAC;
        }
        return OS.UNKNOWN;
    }
}
