/*
 * (C) Copyright 2017 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.image;

import java.awt.image.BufferedImage;

/**
 * EXIFImage is a container that combines the image and its EXIF data.
 *
 * @author Jaka Bobnar
 *
 */
public final class EXIFImage {

    /** The exif data */
    public final EXIFData data;
    /** Original image */
    public final BufferedImage originalImage;
    /** The image (scaled, profiled etc.) */
    public final BufferedImage profiledImage;

    /**
     * Construct a new container.
     *
     * @param data the exif data
     * @param image the original image
     */
    public EXIFImage(EXIFData data, BufferedImage originalImage) {
        this.data = data;
        this.originalImage = originalImage;
        this.profiledImage = null;
        // Overwrite the width and height with actual width and height, because for some images the metadata
        // extractor returns the size of the thumbnail
        data.put(EXIFData.TAG_IMAGE_WIDTH,String.valueOf(originalImage.getWidth()));
        data.put(EXIFData.TAG_IMAGE_HEIGHT,String.valueOf(originalImage.getHeight()));
    }

    /**
     * Construct a new container.
     *
     * @param data the exif data
     * @param originalImage the original image
     * @param profiledImage profiled image
     */
    public EXIFImage(EXIFData data, BufferedImage originalImage, BufferedImage profiledImage) {
        this.data = data;
        this.originalImage = originalImage;
        this.profiledImage = profiledImage;
        // Overwrite the width and height with actual width and height, because for some images the metadata
        // extractor returns the size of the thumbnail
        data.put(EXIFData.TAG_IMAGE_WIDTH,String.valueOf(originalImage.getWidth()));
        data.put(EXIFData.TAG_IMAGE_HEIGHT,String.valueOf(originalImage.getHeight()));
    }
}
