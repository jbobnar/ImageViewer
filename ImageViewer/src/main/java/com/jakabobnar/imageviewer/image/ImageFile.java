/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.image;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * ImageFile is a wrapper around the file, the image read from that file, scaled image, the id of the file, meta data
 * etc. It is used merely as a container to avoid tracking multiple arrays when dealing with images.
 *
 * @author Jaka Bobnar
 *
 */
public final class ImageFile implements Comparable<ImageFile> {

    /** The file from which the image was read */
    public final File file;
    /** The image adapted for presentation on the screen */
    public final BufferedImage profiledImage;
    /** The original image as read from the file */
    public final BufferedImage originalImage;
    /** The exif information */
    public final EXIFData exif;
    /** Unique id for this image, used for sorting purposes, may be 0 */
    public final int id;

    /**
     * ImageFile is a container, which connects the image and the file which the image was loaded from.
     *
     * @param file the file
     * @param originalImage the original image, before applying the display color profile
     * @param profiledImage the image that is adapted to be shown on the screen
     * @param exif the image exif data
     * @param id the id of the image, which is usually the index of the file in the array of all available files
     */
    public ImageFile(File file, BufferedImage originalImage, BufferedImage profiledImage, EXIFData exif, int id) {
        this.profiledImage = profiledImage;
        this.originalImage = originalImage;
        this.exif = exif;
        this.file = file;
        this.id = id;
        // Overwrite the width and height with actual width and height, because for some images the metadata
        // extractor returns the size of the thumbnail
        exif.put(EXIFData.TAG_IMAGE_WIDTH,String.valueOf(originalImage.getWidth()));
        exif.put(EXIFData.TAG_IMAGE_HEIGHT,String.valueOf(originalImage.getHeight()));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ImageFile o) {
        return id - o.id;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ImageFile other = (ImageFile) obj;
        return id == other.id;
    }

}
