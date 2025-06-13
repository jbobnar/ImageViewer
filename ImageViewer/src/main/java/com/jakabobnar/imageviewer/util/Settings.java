/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.util;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.jakabobnar.imageviewer.Transition;
import com.jakabobnar.imageviewer.image.Sorting;
import com.jakabobnar.imageviewer.transition.TransitionsMap;

/**
 * Settings object contains all program settings. It also provides methods to load and persist the settings.
 *
 * @author Jaka Bobnar
 *
 */
public class Settings implements Serializable {

    private static final long serialVersionUID = 6536150871347674562L;
    private static final String USE_MULTIPLE_CORES = "useMultipleCores";
    private static final String COLOR_MANAGE = "colorManage";
    private static final String SCALE_TO_FIT = "scaleSmallImagesToFit";
    private static final String SCALE_BEST_QUALITY = "scaleBestQuality";
    private static final String ROTATE_IMAGE = "rotateImage";
    private static final String CYCLE_WHEN_AT_END = "cycleWhenAtEnd";
    private static final String PLAY_SOUND_ON_CYCLE = "playSoundOnCycle";
    private static final String STEP_SIZE = "stepSize";
    private static final String ZOOM_FACTOR = "zoomFactor";
    private static final String MOUSE_BUTTON_ADVANCE = "mouseButtonAdvance";
    private static final String AUTO_HIDE_MOUSE = "autoHideMouse";
    private static final String WAIT_FOR_IMAGES_TO_LOAD = "waitForImagesToLoad";
    private static final String USE_SYSTEM_COLOR_PROFILE = "useSystemColorProfile";
    private static final String USE_DISPLAY_COLOR_PROFILE = "useDisplayColorProfile";
    private static final String COLOR_PROFILE_FILE = "colorProfileFile";
    private static final String USE_TRANSITION = "useTransitions";
    private static final String TRANSITION_DURATION = "transitionDuration";
    private static final String TRANSITIONS = "transitions";
    private static final String HISTO_SHOW_RGB = "histoShowRGB";
    private static final String HISTO_SHOW_CHANNELS = "histoShowChannels";
    private static final String HISTO_SHOW_LUMINOSITY = "histoShowLuminosity";
    private static final String HISTO_OVERLAY_CHARTS = "histoOverlayCharts";
    private static final String HISTO_OVERLAY_CHANNELS = "histoOverlayChannels";
    private static final String OVERLAY_OPACITY = "overlayOpacity";
    private static final String LEFT_MOUSE_BUTTON_ACTION = "leftMouseButtonAction";
    private static final String CURSOR_HUE = "cursorHue";
    private static final String CURSOR_OPACITY = "cursorOpacity";
    private static final String FRAME_BOUNDS = "frameBounds";
    private static final String FULL_FRAME = "fullFrame";
    private static final String WINDOW_STATE = "windowState";
    private static final String LAST_FILE = "lastFile";
    private static final String HISTOGRAM_VISIBLE = "histogramVisible";
    private static final String HISTOGRAM_LOCATION = "histogramLocation";
    private static final String EXIF_VISIBLE = "exifVisible";
    private static final String EXIF_LOCATION = "exifLocation";
    private static final String SLIDESHOW_DURATION = "slideShowDuration";
    private static final String REVERSE_BUTTONS = "reverseButtons";
    private static final String REVERSE_SCROLLING = "reverseScrolling";
    private static final String BACKGROUND_COLOR = "backgroundColor";
    private static final String TOOLBAR_AUTO_HIDE = "toolbarAutoHide";
    private static final String SHOW_TOOLBAR = "showToolbar";
    private static final String SORTING_ORDER = "sortingOrder";
    private static final String RECENT_FILES = "recentFiles";
    private static final String QUALITY_OVER_SPEED = "preferQualityOverSpeed";

    public Rectangle frameBounds = new Rectangle(0,0,1000,600);
    public boolean fullFrame = false;
    public int windowState = 0;
    public boolean toolbarAutoHide = true;
    public boolean showToolbar = true;
    public String lastFile = null;
    public boolean histogramVisible = false;
    public boolean exifVisible = false;
    public Point histogramLocation = null;
    public Point exifLocation = null;

    public boolean useMultipleCores = true;
    public boolean colorManage = true;
    public boolean scaleToFit = true;
    public boolean scaleBestQuality = true;
    public boolean rotateImage = true;
    public boolean cycleWhenAtEnd = false;
    public boolean playSoundOnCycle = false;
    public int stepSize = 5;
    public float zoomFactor = 3f;
    public boolean mouseButtonAdvance = false;
    public LMBAction leftMouseButtonAction = LMBAction.PAINT;
    public boolean reverseButtons = false;
    public boolean reverseScrollingDirection = false;
    public int cursorHue = 40;
    public int cursorOpacity = 100;
    public boolean autoHideMouse = true;
    public boolean waitForImagesToLoadWhenScrolling = true;
    public boolean preferQualityOverSpeedWhenScrolling = false;

    public boolean systemColorProfile = false;
    public boolean useDisplayColorProfile = true;
    public File colorProfile;

    public Sorting sortingOrder = Sorting.NAME;
    public boolean useTransitions = true;
    public int transitionDuration = 1500;
    public List<Transition> transitions = new ArrayList<>();
    public int slideShowDuration = 6000;

    public boolean showRGB = true;
    public boolean showLuminosity = false;
    public boolean showChannels = false;
    public boolean overlayCharts = false;
    public boolean overlayChannels = true;

    public int overlayOpacity = 60;
    public Color backgroundColor = new Color(0,0,0);

    private LinkedList<File> recentFiles = new LinkedList<>();

    private static final String PROPERTIES = ".imageviewer";

    public Settings() {
        load();
    }

    /**
     * Load the contents from the properties file in the user's home folder. Default settings are applied if the file is
     * missing or does not contain the necessary information.
     */
    public void load() {
        Properties properties = new Properties();
        File file = new File(System.getProperty("user.home"));
        file = new File(file,PROPERTIES);
        if (file.exists()) {
            try (FileInputStream stream = new FileInputStream(file)) {
                properties.load(stream);
            } catch (IOException e) {
                // ignore
            }
        }
        useMultipleCores = Boolean.parseBoolean(properties.getProperty(USE_MULTIPLE_CORES,"true"));
        colorManage = Boolean.parseBoolean(properties.getProperty(COLOR_MANAGE,"true"));
        scaleToFit = Boolean.parseBoolean(properties.getProperty(SCALE_TO_FIT,"true"));
        scaleBestQuality = Boolean.parseBoolean(properties.getProperty(SCALE_BEST_QUALITY,"true"));
        rotateImage = Boolean.parseBoolean(properties.getProperty(ROTATE_IMAGE,"true"));
        cycleWhenAtEnd = Boolean.parseBoolean(properties.getProperty(CYCLE_WHEN_AT_END,"false"));
        playSoundOnCycle = Boolean.parseBoolean(properties.getProperty(PLAY_SOUND_ON_CYCLE,"true"));
        try {
            stepSize = Integer.parseInt(properties.getProperty(STEP_SIZE,"5"));
        } catch (NumberFormatException e) {
            stepSize = 5;
        }
        try {
            zoomFactor = Float.parseFloat(properties.getProperty(ZOOM_FACTOR,"3.0"));
        } catch (NumberFormatException e) {
            zoomFactor = 5;
        }

        reverseButtons = Boolean.parseBoolean(properties.getProperty(REVERSE_BUTTONS,"false"));
        reverseScrollingDirection = Boolean.parseBoolean(properties.getProperty(REVERSE_SCROLLING,"false"));
        mouseButtonAdvance = Boolean.parseBoolean(properties.getProperty(MOUSE_BUTTON_ADVANCE,"false"));
        autoHideMouse = Boolean.parseBoolean(properties.getProperty(AUTO_HIDE_MOUSE,"true"));
        waitForImagesToLoadWhenScrolling = Boolean.parseBoolean(properties.getProperty(WAIT_FOR_IMAGES_TO_LOAD,"true"));
        preferQualityOverSpeedWhenScrolling = Boolean.parseBoolean(properties.getProperty(QUALITY_OVER_SPEED,"false"));
        systemColorProfile = Boolean.parseBoolean(properties.getProperty(USE_SYSTEM_COLOR_PROFILE,"true"));
        String profile = properties.getProperty(COLOR_PROFILE_FILE);
        if (profile != null) {
            useDisplayColorProfile = Boolean.parseBoolean(properties.getProperty(USE_DISPLAY_COLOR_PROFILE,"true"));
            colorProfile = new File(profile);
        } else {
            useDisplayColorProfile = false;
        }
        try {
            sortingOrder = Sorting.valueOf(properties.getProperty(SORTING_ORDER,Sorting.NAME.name()));
        } catch (IllegalArgumentException e) {
            sortingOrder = Sorting.NAME;
        }
        useTransitions = Boolean.parseBoolean(properties.getProperty(USE_TRANSITION,"true"));
        try {
            transitionDuration = Integer.parseInt(properties.getProperty(TRANSITION_DURATION,"1500"));
        } catch (NumberFormatException e) {
            transitionDuration = 1500;
        }
        String trans = properties.getProperty(TRANSITIONS);
        transitions = new ArrayList<>();
        if (trans != null) {
            String[] t = trans.split("\\,");
            for (String s : t) {
                Transition tr = TransitionsMap.getInstance().get(s);
                if (tr != null) {
                    transitions.add(tr);
                }
            }
        }
        try {
            slideShowDuration = Integer.parseInt(properties.getProperty(SLIDESHOW_DURATION,"5000"));
        } catch (NumberFormatException e) {
            slideShowDuration = 5000;
        }

        showRGB = Boolean.parseBoolean(properties.getProperty(HISTO_SHOW_RGB,"true"));
        showLuminosity = Boolean.parseBoolean(properties.getProperty(HISTO_SHOW_LUMINOSITY,"false"));
        showChannels = Boolean.parseBoolean(properties.getProperty(HISTO_SHOW_CHANNELS,"false"));
        overlayCharts = Boolean.parseBoolean(properties.getProperty(HISTO_OVERLAY_CHARTS,"false"));
        overlayChannels = Boolean.parseBoolean(properties.getProperty(HISTO_OVERLAY_CHANNELS,"true"));

        try {
            overlayOpacity = Integer.parseInt(properties.getProperty(OVERLAY_OPACITY,"60"));
        } catch (NumberFormatException e) {
            overlayOpacity = 60;
        }

        String action = properties.getProperty(LEFT_MOUSE_BUTTON_ACTION,"PAINT");
        try {
            leftMouseButtonAction = LMBAction.valueOf(action);
        } catch (RuntimeException e) {
            leftMouseButtonAction = LMBAction.PAINT;
        }
        try {
            cursorHue = Integer.parseInt(properties.getProperty(CURSOR_HUE,"40"));
        } catch (NumberFormatException e) {
            cursorHue = 40;
        }
        try {
            cursorOpacity = Integer.parseInt(properties.getProperty(CURSOR_OPACITY,"100"));
        } catch (NumberFormatException e) {
            cursorOpacity = 100;
        }

        String bounds = properties.getProperty(FRAME_BOUNDS);
        if (bounds != null) {
            String[] b = bounds.split("\\,");
            if (b.length == 4) {
                int[] defBounds = new int[] { 0, 0, 1000, 600 };
                for (int i = 0; i < 4; i++) {
                    try {
                        defBounds[i] = Integer.parseInt(b[i].trim());
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                frameBounds = new Rectangle(defBounds[0],defBounds[1],defBounds[2],defBounds[3]);
            }
        }

        fullFrame = Boolean.parseBoolean(properties.getProperty(FULL_FRAME,"false"));
        try {
            windowState = Integer.parseInt(properties.getProperty(WINDOW_STATE,"0"));
        } catch (NumberFormatException e) {
            windowState = 0;
        }
        toolbarAutoHide = Boolean.parseBoolean(properties.getProperty(TOOLBAR_AUTO_HIDE,"true"));
        showToolbar = Boolean.parseBoolean(properties.getProperty(SHOW_TOOLBAR,"true"));
        lastFile = properties.getProperty(LAST_FILE);

        histogramVisible = Boolean.parseBoolean(properties.getProperty(HISTOGRAM_VISIBLE,"false"));
        exifVisible = Boolean.parseBoolean(properties.getProperty(EXIF_VISIBLE,"false"));

        String location = properties.getProperty(EXIF_LOCATION);
        if (location != null) {
            String[] loc = location.split("\\,");
            if (loc.length == 2) {
                try {
                    int x = Integer.parseInt(loc[0]);
                    int y = Integer.parseInt(loc[1]);
                    exifLocation = new Point(x,y);
                } catch (NumberFormatException e) {
                    // ignore, exif dialog location unknown
                }
            }
        }

        location = properties.getProperty(HISTOGRAM_LOCATION);
        if (location != null) {
            String[] loc = location.split("\\,");
            if (loc.length == 2) {
                try {
                    int x = Integer.parseInt(loc[0]);
                    int y = Integer.parseInt(loc[1]);
                    histogramLocation = new Point(x,y);
                } catch (NumberFormatException e) {
                    // ignore, histogram dialog location unknown
                }
            }
        }

        String bgColor = properties.getProperty(BACKGROUND_COLOR,"0,0,0");
        String[] rgb = bgColor.split("\\,");
        if (rgb.length == 3) {
            int red = Integer.parseInt(rgb[0].trim()) % 256;
            int green = Integer.parseInt(rgb[1].trim()) % 256;
            int blue = Integer.parseInt(rgb[2].trim()) % 256;
            backgroundColor = new Color(red,green,blue);
        }

        String recent = properties.getProperty(RECENT_FILES);
        if (recent != null) {
            String[] files = recent.split("\\;");
            Arrays.stream(files).map(File::new).forEach(recentFiles::add);
        }
    }

    /**
     * Store the contents of this settings object to the properties file in the user's home folder.
     *
     * @throws IOException if settings could not be written to file
     */
    public void store() throws IOException {
        Properties properties = new Properties();
        properties.put(USE_MULTIPLE_CORES,String.valueOf(useMultipleCores));
        properties.put(COLOR_MANAGE,String.valueOf(colorManage));
        properties.put(SCALE_TO_FIT,String.valueOf(scaleToFit));
        properties.put(SCALE_BEST_QUALITY,String.valueOf(scaleBestQuality));
        properties.put(ROTATE_IMAGE,String.valueOf(rotateImage));
        properties.put(CYCLE_WHEN_AT_END,String.valueOf(cycleWhenAtEnd));
        properties.put(PLAY_SOUND_ON_CYCLE,String.valueOf(playSoundOnCycle));
        properties.put(STEP_SIZE,String.valueOf(stepSize));
        StringBuilder colorSB = new StringBuilder(11).append(backgroundColor.getRed()).append(',')
                .append(backgroundColor.getGreen()).append(',').append(backgroundColor.getBlue());
        properties.put(BACKGROUND_COLOR,colorSB.toString());
        properties.put(ZOOM_FACTOR,String.valueOf(zoomFactor));
        properties.put(MOUSE_BUTTON_ADVANCE,String.valueOf(mouseButtonAdvance));
        properties.put(REVERSE_BUTTONS,String.valueOf(reverseButtons));
        properties.put(REVERSE_SCROLLING,String.valueOf(reverseScrollingDirection));
        properties.put(AUTO_HIDE_MOUSE,String.valueOf(autoHideMouse));
        properties.put(WAIT_FOR_IMAGES_TO_LOAD,String.valueOf(waitForImagesToLoadWhenScrolling));
        properties.put(QUALITY_OVER_SPEED,String.valueOf(preferQualityOverSpeedWhenScrolling));
        properties.put(USE_SYSTEM_COLOR_PROFILE,String.valueOf(systemColorProfile));
        properties.put(USE_DISPLAY_COLOR_PROFILE,String.valueOf(useDisplayColorProfile));
        if (colorProfile != null) {
            properties.put(COLOR_PROFILE_FILE,colorProfile.getAbsolutePath());
        }
        properties.put(SORTING_ORDER,sortingOrder.name());
        properties.put(USE_TRANSITION,String.valueOf(useTransitions));
        properties.put(TRANSITION_DURATION,String.valueOf(transitionDuration));
        properties.put(SLIDESHOW_DURATION,String.valueOf(slideShowDuration));
        properties.put(HISTO_SHOW_CHANNELS,String.valueOf(showChannels));
        properties.put(HISTO_SHOW_RGB,String.valueOf(showRGB));
        properties.put(HISTO_SHOW_LUMINOSITY,String.valueOf(showLuminosity));
        properties.put(HISTO_OVERLAY_CHARTS,String.valueOf(overlayCharts));
        properties.put(HISTO_OVERLAY_CHANNELS,String.valueOf(overlayChannels));
        properties.put(OVERLAY_OPACITY,String.valueOf(overlayOpacity));
        properties.put(LEFT_MOUSE_BUTTON_ACTION,String.valueOf(leftMouseButtonAction));
        properties.put(CURSOR_HUE,String.valueOf(cursorHue));
        properties.put(CURSOR_OPACITY,String.valueOf(cursorOpacity));

        if (lastFile != null) {
            properties.put(LAST_FILE,lastFile);
        }
        properties.put(FULL_FRAME,String.valueOf(fullFrame));
        properties.put(WINDOW_STATE,String.valueOf(windowState));
        properties.put(FRAME_BOUNDS,
                frameBounds.x + "," + frameBounds.y + "," + frameBounds.width + "," + frameBounds.height);
        properties.put(TOOLBAR_AUTO_HIDE,String.valueOf(toolbarAutoHide));
        properties.put(SHOW_TOOLBAR,String.valueOf(showToolbar));
        properties.put(HISTOGRAM_VISIBLE,String.valueOf(histogramVisible));
        properties.put(EXIF_VISIBLE,String.valueOf(exifVisible));
        if (histogramLocation != null) {
            properties.put(HISTOGRAM_LOCATION,histogramLocation.x + "," + histogramLocation.y);
        }
        if (exifLocation != null) {
            properties.put(EXIF_LOCATION,exifLocation.x + "," + exifLocation.y);
        }

        StringBuilder sb = new StringBuilder(transitions.size() * 20);
        transitions.forEach(e -> sb.append(e.getName()).append(','));
        String d = transitions.isEmpty() ? "" : sb.substring(0,sb.length() - 1);
        properties.put(TRANSITIONS,d);

        String recent = recentFiles.stream().map(f -> f.getAbsolutePath()).collect(Collectors.joining(";"));
        properties.put(RECENT_FILES,recent);

        File file = new File(System.getProperty("user.home"));
        file = new File(file,PROPERTIES);
        try (FileOutputStream stream = new FileOutputStream(file)) {
            properties.store(stream,"Image viewer settings.");
        }
    }

    /**
     * Add the file to the list of recent files. Removes the oldest from the list
     *
     * @param file the file to add
     */
    public void addRecentFile(File file) {
        recentFiles.remove(file);
        recentFiles.addFirst(file);
        while (recentFiles.size() > 10) {
            recentFiles.removeLast();
        }
    }

    /**
     * Returns the list of recently opened files.
     *
     * @return the list of recent files
     */
    public File[] getRecentFiles() {
        return recentFiles.toArray(new File[recentFiles.size()]);
    }
}
