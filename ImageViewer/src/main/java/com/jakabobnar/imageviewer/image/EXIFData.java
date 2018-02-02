/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * EXIFData represents the EXIF information loaded from an image file
 *
 * @author Jaka Bobnar
 *
 */
public class EXIFData extends HashMap<String, String> {
    private static final long serialVersionUID = -3417312284712032185L;

    public static final String TAG_FILE = "File";
    public static final String TAG_PROFILE_DESCRIPTION = "Profile Description";
    public static final String TAG_EXPOSURE_TIME = "Exposure Time";
    public static final String TAG_F_NUMBER = "F-Number";
    public static final String TAG_EXPOSURE_PROGRAM = "Exposure Program";
    public static final String TAG_ISO_SPEED = "ISO Speed Ratings";
    public static final String TAG_DATE_TIME = "Date/Time Original";
    public static final String TAG_EXPOSURE_COMPENSATION = "Exposure Bias Value";
    public static final String TAG_METERING_MODE = "Metering Mode";
    public static final String TAG_FLASH = "Flash";
    public static final String TAG_FOCAL_LENGTH = "Focal Length";
    public static final String TAG_EXPORUSE_MODE = "Exposure Mode";
    public static final String TAG_WHITE_BALANCE = "White Balance Mode";
    public static final String TAG_BODY_SERIAL_NUMBER = "Body Serial Number";
    public static final String TAG_LENS_MODEL = "Lens Model";
    public static final String TAG_MODEL = "Model";
    public static final String TAG_LENS = "Lens";
    public static final String TAG_LENS_TYPE = "Lens Type";
    public static final String TAG_IMAGE_WIDTH = "Image Width";
    public static final String TAG_IMAGE_HEIGHT = "Image Height";
    public static final String TAG_ORIENTATION = "Orientation";
    public static final String TAG_LAST_MODIFIED = "Modified";

    private static final ThreadLocal<SimpleDateFormat> FORMATTER = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy:MM:dd HH:mm:ss"));

    private Date dateTaken;

    /**
     * Constructs the EXIFData which contains the path to the given file, last modified data and the image size.
     *
     * @param file the file, which the exif theoretically belongs to
     * @param image the image from which the size will be extracted
     */
    public EXIFData(File file, BufferedImage image) {
        this(file);
        put(TAG_IMAGE_WIDTH,String.valueOf(image.getWidth()));
        put(TAG_IMAGE_HEIGHT,String.valueOf(image.getHeight()));
    }

    /**
     * Construct new EXIFData for the given file. The data contains the file path and the date of the last modification.
     *
     * @param file the file for which to create the exif
     */
    public EXIFData(File file) {
        put(TAG_FILE,file.getAbsolutePath());
        String date = FORMATTER.get().format(new Date(file.lastModified()));
        put(TAG_DATE_TIME,date);
        put(TAG_LAST_MODIFIED,date);
    }

    /**
     * Returns the date when the picture was taken. If the information does not exist, the current date is returned.
     *
     * @return the date when the photo was taken
     */
    public Date getDateTaken() {
        if (dateTaken == null) {
            try {
                dateTaken = FORMATTER.get().parse(get(TAG_DATE_TIME));
            } catch (ParseException e) {
                dateTaken = new Date();
            }
        }
        return dateTaken;
    }

    /**
     * Construct new EXIFData without any tags.
     */
    public EXIFData() {
        // nothing
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        return super.equals(obj);
    }    
}
