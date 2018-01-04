/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.image;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Histogram represents image histogram data. It provides the individual rgb channels, combined RGB and luminosity.
 *
 * @see ImageUtil#imageHistogram(BufferedImage)
 * @author Jaka Bobnar
 */
public class Histogram {

    private final int[][] rgblRGB;
    private final File file;

    /**
     * Constructs a new histogram.
     *
     * @param file the image file
     * @param rgblRGB 5 histograms: red, green, blue, intensity, combined RGB
     */
    public Histogram(File file, int[][] rgblRGB) {
        this.rgblRGB = rgblRGB;
        this.file = file;
    }

    /**
     * Returns the file, which the histogram belongs to.
     *
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the array of histograms, orderer: red, gree, blue, luminosty, RGB.
     *
     * @return the histograms
     */
    public int[][] getRgblRGB() {
        return rgblRGB;
    }
}
