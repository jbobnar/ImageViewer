/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JPanel;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;
import com.twelvemonkeys.imageio.util.Constants;

/**
 * ImageUtil provides a set of utility methods for loading the images, scaling them to target size and converting them
 * to a destination color space. It also provides functions to read the EXIF data and calculate the histgram data.
 *
 * @author Jaka Bobnar
 *
 */
public class ImageUtil {

    /**
     * Orientation lists possible orientations in which an image can be. The description of the orientation is what the
     * metadata extractor knows.
     *
     * @author Jaka Bobnar
     *
     */
    public static enum Orientation {
        NORMAL("Top, left side (Horizontal / normal)"), //
        MIRROR_HORIZONTAL("Top, right side (Mirror horizontal)"), //
        MIRROR_VERTICAL("Bottom, left side (Mirror vertical)"), //
        ROTATE_90("Right side, top (Rotate 90 CW)"), //
        ROTATE_180("Bottom, right side (Rotate 180)"), //
        ROTATE_270("Left side, bottom (Rotate 270 CW)"), //
        MIRROR_HORIZONTAL_ROTATE_90("Right side, bottom (Mirror horizontal and rotate 90 CW)"), //
        MIRROR_HORIZONTAL_ROTATE_270("Left side, top (Mirror horizontal and rotate 270 CW)");

        public final String description;

        /**
         * Construct a new orientation.
         *
         * @param description the description of the orientation as used by the metadata extractor.
         */
        private Orientation(String description) {
            this.description = description;
        }
    }

    /** An image, which is shown when the image file could not be decoded into an image */
    public static final BufferedImage NO_IMAGE;
    private static volatile AtomicInteger imageId = new AtomicInteger(0);
    private static final ThreadLocal<MediaTracker> MEDIA_TRACKER = ThreadLocal
            .withInitial(() -> new MediaTracker(new JPanel()));
    private static final Map<String, Orientation> ORIENTATION_TO_CODE = new HashMap<>();
    private static final Map<File, EXIFData> EXIF_CACHE = new HashMap<>();
    public static final Map<RenderingHints.Key, Object> NO_HINTS = new HashMap<>();
    public static final Map<RenderingHints.Key, Object> HINTS = new HashMap<>();

    static {
        // Hints for quality rendering
        HINTS.put(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        HINTS.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        HINTS.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        HINTS.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        HINTS.put(RenderingHints.KEY_ALPHA_INTERPOLATION,RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        // hints for fast rendering
        NO_HINTS.put(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_SPEED);
        NO_HINTS.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
        NO_HINTS.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_SPEED);
        NO_HINTS.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        NO_HINTS.put(RenderingHints.KEY_ALPHA_INTERPOLATION,RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        NO_HINTS.put(RenderingHints.KEY_DITHERING,RenderingHints.VALUE_DITHER_DISABLE);
        NO_HINTS.put(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        NO_HINTS.put(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        NO_HINTS.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        // Create no image
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        NO_IMAGE = new BufferedImage(dim.width,dim.height,BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = NO_IMAGE.createGraphics();
        Font font = g.getFont();
        g.setFont(font.deriveFont(30f));
        String message = "UNKNOWN IMAGE FORMAT";
        int width = g.getFontMetrics().stringWidth(message);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString(message,(dim.width - width) / 2,dim.height / 2);
        g.dispose();
        Arrays.stream(Orientation.values()).forEach(c -> ORIENTATION_TO_CODE.put(c.description,c));
    }

    /**
     * Clear the cache to free memory.
     */
    public static void clearCache() {
        EXIF_CACHE.clear();
    }

    /**
     * Scale the given image to the given dimensions, but preserving the original aspect ratio. Depending on the value
     * of the fast parameter the image is scaled either using the best performance or best quality parameters.
     *
     * @param image the image to scale
     * @param backgroundColor the background color (the color of the image border)
     * @param width the target width
     * @param height the target height
     * @param fast true for as fast as possible scaling or false for smooth scaling
     * @return the scaled image
     */
    public static BufferedImage getScaledImage(BufferedImage image, Color backgroundColor, int width, int height,
            boolean fast) {
        if (image == null) {
            return null;
        }
        double ratio = (double) image.getWidth() / image.getHeight();
        int w = (int) (height * ratio);
        int h = height;
        if (w > width) {
            w = width;
            h = (int) (width / ratio);
        }
        if (w <= 0 || h <= 0) {
            return image;
        }
        BufferedImage bi = null;
        if (image.getWidth() == width && image.getHeight() == height) {
            // the image is already the proper size
            bi = image;
        } else if (image.getWidth() == w && image.getHeight() == h) {
            // the image is of proper size, but need to add the black border, so that it matches target size,
            bi = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bi.createGraphics();
            g.setColor(backgroundColor);
            g.fillRect(0,0,width,height);
            g.drawImage(image,(width - w) / 2,(height - h) / 2,null);
            g.dispose();
        } else {
            if (fast) {
                bi = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bi.createGraphics();
                g.setRenderingHints(NO_HINTS);
                g.setColor(backgroundColor);
                g.fillRect(0,0,width,height);
                g.drawImage(image,(width - w) / 2,(height - h) / 2,w,h,null);
                g.dispose();
            } else {
                // When top quality is required, this is still the fastest way to resize an image
                Image im = image.getScaledInstance(w,h,Image.SCALE_SMOOTH);
//                Image im = Scalr.resize(image,Method.BALANCED,w,h);
                bi = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bi.createGraphics();
                g.setRenderingHints(HINTS);
                g.setColor(backgroundColor);
                g.fillRect(0,0,width,height);
                g.drawImage(im,(width - w) / 2,(height - h) / 2,null);
                g.dispose();
            }
        }
        return bi;
    }

    /**
     * Loads the image from the given file taking into account its embedded profile if color manage is true.
     *
     * @param file the file to load the image from
     * @param colorManage true to respect the embedded color profile or false to ignore it
     * @param displaySpace the destination color space
     * @param rotateImage rotate image according to information in the exif
     * @return the image and its exif data or null if the image could not be read.
     * @throws IOException in case of an IO related error
     * @throws ImageProcessingException
     */
    public static EXIFImage loadImage(File file, boolean colorManage, ColorSpace displaySpace, boolean rotateImage)
            throws IOException {
        // In multithreaded applications this could interfere with other threads, but we don't care.
        // If another thread triggered loading of image with different management, than whatever the previous thread
        // is doing is completely irrelevant. That image won't be used anywhere. THe only problem is that previous
        // thread needs to do more than needed, but that's life.
        System.setProperty(Constants.READ_EMBEDDED_PROFILE,String.valueOf(colorManage));
        System.setProperty(Constants.DO_COLOR_MANAGEMENT,String.valueOf(displaySpace == null));
        // This stream seems to work the fastest
        BufferedImage image = ImageIO.read(new BufferedInputStream(Files.newInputStream(file.toPath())));
        if (image == null) {
            return null;
        }
        EXIFData exif = null;
        try {
            exif = readExifData(file);
            if (rotateImage) {
                Orientation orientation = ORIENTATION_TO_CODE.get(exif.get(EXIFData.TAG_ORIENTATION));
                image = rotateImage(image,orientation);
            }
        } catch (ImageProcessingException e) {
            // ignore, exif does not exist
            exif = new EXIFData(file,image);
        }

        BufferedImage profiledImage = image;
        if (displaySpace != null && colorManage) {
            profiledImage = ImageUtil.convertImageToColorSpaceFast(image,displaySpace);
        }
        return new EXIFImage(exif,image,profiledImage);
    }

    /**
     * Rotate and mirror the image according to the given orientation parameter.
     *
     * @param image the image to rotate or mirror
     * @param orientation the orientation in which the image currently is
     * @return transformed image
     */
    private static BufferedImage rotateImage(BufferedImage image, Orientation orientation) {
        if (orientation != null) {
            switch (orientation) {
            case NORMAL:
                // ignore, return image
                return image;
            case ROTATE_270: {
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage retImage = new BufferedImage(h,w,image.getType());
                Graphics2D g = retImage.createGraphics();
                g.translate((h - w) / 2,(w - h) / 2);
                g.rotate(3 * Math.PI / 2,w / 2,h / 2);
                g.drawRenderedImage(image,null);
                g.dispose();
                return retImage;
            }
            case ROTATE_180: {
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage retImage = new BufferedImage(w,h,image.getType());
                Graphics2D g = retImage.createGraphics();
                g.rotate(Math.PI,w / 2,h / 2);
                g.drawRenderedImage(image,null);
                g.dispose();
                return retImage;
            }
            case ROTATE_90: {
                // rotate right 90 deg
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage retImage = new BufferedImage(h,w,image.getType());
                Graphics2D g = retImage.createGraphics();
                g.translate((h - w) / 2,(w - h) / 2);
                g.rotate(Math.PI / 2,w / 2,h / 2);
                g.drawRenderedImage(image,null);
                g.dispose();
                return retImage;
            }
            case MIRROR_VERTICAL: {
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage retImage = new BufferedImage(w,h,image.getType());
                Graphics2D g = retImage.createGraphics();
                g.scale(1,-1);
                g.translate(0,-h);
                g.drawRenderedImage(image,null);
                g.dispose();
                return retImage;
            }
            case MIRROR_HORIZONTAL: {
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage retImage = new BufferedImage(w,h,image.getType());
                Graphics2D g = retImage.createGraphics();
                g.scale(-1,1);
                g.translate(-w,0);
                g.drawRenderedImage(image,null);
                g.dispose();
                return retImage;
            }
            case MIRROR_HORIZONTAL_ROTATE_90: {
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage retImage = new BufferedImage(h,w,image.getType());
                Graphics2D g = retImage.createGraphics();
                g.scale(-1,1);
                g.translate(-h,0);
                g.translate((h - w) / 2,(w - h) / 2);
                g.rotate(Math.PI / 2,w / 2,h / 2);
                g.drawRenderedImage(image,null);
                g.dispose();
                return retImage;
            }
            case MIRROR_HORIZONTAL_ROTATE_270: {
                int w = image.getWidth();
                int h = image.getHeight();
                BufferedImage retImage = new BufferedImage(h,w,image.getType());
                Graphics2D g = retImage.createGraphics();
                g.scale(-1,1);
                g.translate(-h,0);
                g.translate((h - w) / 2,(w - h) / 2);
                g.rotate(3 * Math.PI / 2,w / 2,h / 2);
                g.drawRenderedImage(image,null);
                g.dispose();
                return retImage;
            }
            default: {
                return image;
            }
            }
        }
        return image;
    }

    /**
     * Reads the image from the given input stream by down sized it to the given size. The down sizing is coarse, so the
     * image is only useful for quick preview. But the algorithm is relatively fast.
     *
     * @param file the file to load the image from
     * @param width the maximum width of the down sized image
     * @param height the maximum height of the down sized image
     * @param tryToolkitIfSmall true to try loading the image with toolkit if the image is small enough (it is faster)
     * @return the image down sampled to the given width and height
     * @throws IOException
     */
    private static BufferedImage subsampleImage(File file, int width, int height, boolean tryWithToolkitIfSmall)
            throws IOException, InterruptedException {
        // It is generally faster to read all bytes and then subsample then to use a real stream
        try (ImageInputStream stream = new ByteArrayImageInputStream(Files.readAllBytes(file.toPath()))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            ImageReadParam imageReaderParams = reader.getDefaultReadParam();
            reader.setInput(stream);
            int w = reader.getWidth(0);
            int h = reader.getHeight(0);
            if (tryWithToolkitIfSmall && w <= 2000 && h <= 1500) {
                BufferedImage image = loadImageWithToolkit(file);
                if (image != null) {
                    return image;
                }
            }
            int subsampling = 1;
            if (w > width) {
                subsampling = w / width;
            } else if (h > height) {
                subsampling = h / height;
            }
            imageReaderParams.setSourceSubsampling(subsampling,subsampling,0,0);
            return reader.read(0,imageReaderParams);
        }
    }

    /**
     * Reads the full image as quickly as possible, but does not color manage it (not even embedded profile). This
     * method uses native sun code to load the image, but if it fails it falls back to
     * {@link #loadImage(File, boolean, ColorSpace, boolean)} with no color space and fast parameter set to
     * <code>true</code>.
     *
     * @param file the source of the image
     * @param rotateImage true to rotate the image according to exif or false otherwise
     * @return the image file
     * @throws IOException if the reading of the image failed
     */
    public static EXIFImage loadImageAsFastAsPossible(File file, boolean rotateImage) throws IOException {
        System.setProperty(Constants.READ_EMBEDDED_PROFILE,String.valueOf(false));
        System.setProperty(Constants.DO_COLOR_MANAGEMENT,String.valueOf(false));
        BufferedImage image = null;
        try {
            image = subsampleImage(file,800,600,true);
        } catch (InterruptedException e) {
            // if reading was interrupted, return null
        }
        if (image == null) {
            return null;
        } else {
            EXIFData exif = null;
            try {
                exif = readExifData(file);
                // Overwrite the width and height with actual width and height, because for some images the metadata
                // extractor returns the size of the thumbnail
                if (rotateImage) {
                    Orientation orientation = ORIENTATION_TO_CODE.get(exif.get(EXIFData.TAG_ORIENTATION));
                    image = rotateImage(image,orientation);
                }

            } catch (ImageProcessingException e) {
                // Ignore, exif does not exist
                exif = new EXIFData(file,image);
            }
            return new EXIFImage(exif,image,image);
        }
    }

    /**
     * Reads the full image relatively quickly but does not color manage it. This is marginally slower than than
     * {@link #loadImage(File, FastColorSpace, boolean)} when setting <code>fast</code> to <code>true</code>, with the
     * exception that this method produces the full blown image. On the other hand it is 30 per cent faster when the
     * <code>fast</code> is set to <code>false</code> and no color space conversion is requested. But it only supports
     * standard color profiles.
     *
     * @param file the source of the image
     * @return the image file
     * @throws InterruptedException if the reading of the image was interrupted
     * @deprecated In case of color space not being sRGB this method internally throws and catches an exception and
     *             prints the stack trace (courtesy of sun.awt.image). If that happens this method returns null, but the
     *             stack trace in System.err might still be annoying.
     */
    @Deprecated
    private static BufferedImage loadImageWithToolkit(File file) throws InterruptedException {
        // this is fast but is not color managed
        Image image = Toolkit.getDefaultToolkit().createImage(file.getAbsolutePath());
        if (image == null) {
            return NO_IMAGE;
        }
        int id = imageId.incrementAndGet();
        MediaTracker mt = MEDIA_TRACKER.get();
        mt.addImage(image,id);
        mt.waitForID(id);
        if (mt.isErrorID(id)) {
            mt.removeImage(image);
            return null;
        }
        mt.removeImage(image);
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w < 1 || h < 1) {
            return NO_IMAGE;
        }
        BufferedImage bif = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bif.createGraphics();
        g2d.drawImage(image,0,0,null);
        g2d.dispose();
        return bif;
    }

    /**
     * Transform the given image from its original space to the destination space. If the image is stored in an ICC
     * color space, than transformation happens very fast. If the image is an different non standard color space, this
     * methods falls back to {@link #convertImageToColorSpace(BufferedImage, ColorSpace)}.
     *
     * @param image the image to convert
     * @param destSpace the destination space
     * @return converted image
     */
    public static BufferedImage convertImageToColorSpaceFast(BufferedImage image, ColorSpace destSpace) {
        try {
            int w = image.getWidth();
            int h = image.getHeight();
            BufferedImage ret = new BufferedImage(w,h,image.getType());
            Object property = image.getProperty(Constants.ICC_PROFILE);
            ICC_Profile profile = property instanceof ICC_Profile ? (ICC_Profile) property : null;
            ColorSpace srcSpace = profile == null ? image.getColorModel().getColorSpace() : new ICC_ColorSpace(profile);
            ColorConvertOp cop = new ColorConvertOp(srcSpace,destSpace,null);
            cop.filter(image.getData(),ret.getRaster());
            return ret;
        } catch (Throwable t) {
            // ignore
            System.out.println(t);
        }
        return image;
    }

    /**
     * Reads the EXIF data from the image file and returns it. The tags that are interested are specified by the
     * constants in this class.
     *
     * @param file the file to red
     * @return EXIFData
     * @throws IOException in case of an error
     * @throws ImageProcessingException if meta data could not be read from the given file
     */
    public static EXIFData readExifData(File file) throws IOException, ImageProcessingException {
        EXIFData exif = EXIF_CACHE.get(file);
        if (exif == null) {
            exif = new EXIFData(file);
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            Collection<ExifDirectoryBase> exifDirectory = metadata.getDirectoriesOfType(ExifDirectoryBase.class);
            for (Directory directory : metadata.getDirectories()) {
                if (!exifDirectory.contains(directory)) {
                    for (Tag tag : directory.getTags()) {
                        exif.put(tag.getTagName(),tag.getDescription());
                    }
                }
            }
            // EXIF should overwrite whatever else has the same name
            for (Directory directory : exifDirectory) {
                for (Tag tag : directory.getTags()) {
                    exif.put(tag.getTagName(),tag.getDescription());
                }
            }
            EXIF_CACHE.put(file,exif);
        }
        return exif;
    }

    /**
     * Create 5 histograms for the given image. The histograms are returned in an array: 1st red channel, 2nd green
     * channel, 3rd blue channel, 4th luminosity, 5th combined RGB.
     *
     * @param input the image for which the histogram is created
     * @return the histograms
     */
    public static int[][] imageHistogram(BufferedImage input) {
        if (input == null) {
            return new int[5][256];
        }
        int[] rhistogram = new int[256];
        int[] ghistogram = new int[256];
        int[] bhistogram = new int[256];
        int[] lhistogram = new int[256];
        int[] rgbhistogram = new int[256];
        int w = input.getWidth();
        int h = input.getHeight();

        int length = w * h;
        int[] array = new int[length];
        input.getRGB(0,0,w,h,array,0,w);
        int r, g, b;
        for (int i = 0; i < length; i++) {
            r = (array[i] >> 16) & 0xFF;
            g = (array[i] >> 8) & 0xFF;
            b = array[i] & 0xFF;
            rhistogram[r]++;
            ghistogram[g]++;
            bhistogram[b]++;
            lhistogram[(int) (0.2125 * r + 0.7154 * g + 0.072 * b)]++;
            rgbhistogram[r]++;
            rgbhistogram[g]++;
            rgbhistogram[b]++;
        }

        int[][] hist = new int[5][];
        hist[0] = rhistogram;
        hist[1] = ghistogram;
        hist[2] = bhistogram;
        hist[3] = lhistogram;
        hist[4] = rgbhistogram;
        return hist;
    }
}
