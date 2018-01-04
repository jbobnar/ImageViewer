/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer;

import static com.jakabobnar.imageviewer.util.Utilities.gbc;
import static com.jakabobnar.imageviewer.util.Utilities.registerKeyStroke;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.jakabobnar.colorprofile.ColorProfileManager;
import com.jakabobnar.imageviewer.image.EXIFData;
import com.jakabobnar.imageviewer.image.EXIFImage;
import com.jakabobnar.imageviewer.image.Histogram;
import com.jakabobnar.imageviewer.image.ImageFile;
import com.jakabobnar.imageviewer.image.ImageUtil;
import com.jakabobnar.imageviewer.image.LinkBuffer;
import com.jakabobnar.imageviewer.image.Sorting;
import com.jakabobnar.imageviewer.util.AbstractEventAdapter;
import com.jakabobnar.imageviewer.util.AudioPlayer;
import com.jakabobnar.imageviewer.util.DismissableBlockingQueue;
import com.jakabobnar.imageviewer.util.ImageExecutor;
import com.jakabobnar.imageviewer.util.LMBAction;
import com.jakabobnar.imageviewer.util.Settings;

/**
 * Viewer is the main class that takes care of all events and logic (image advance, loading, rewind etc.).
 *
 * @author Jaka Bobnar
 */
public class Viewer extends JPanel {

    private static final long serialVersionUID = 6590234775346107736L;
    // Number of preloaded images to either side of the currently showing image. If the buffer is too large, startup
    // loading will take too long and application will consume too much memory
    private static final int PRELOADING_BUFFER = 1;
    private static final int BUFFER_SIZE = 2 * PRELOADING_BUFFER + 1;
    private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
    // The minimum size for the buffer to store the images when doing fast reading/scrolling
    private static final int FAST_READ_BUFFER_MIN = 3 * NUM_CORES;
    // A no image constant, to avoid recreating the object too many times
    private static final EXIFImage NO_IMAGE = new EXIFImage(new EXIFData(),ImageUtil.NO_IMAGE,ImageUtil.NO_IMAGE);
    private static final Cursor NO_SCROLL_CURSOR;

    static {
        // Load the cursor when scrolling is attempted but now allowed, because it is disabled
        Cursor c;
        try (InputStream stream = ViewerFrame.class.getClassLoader().getResourceAsStream("Arrow.gif")) {
            c = Toolkit.getDefaultToolkit().createCustomCursor(ImageIO.read(stream),new Point(0,0),"noScroll");
        } catch (IOException e) {
            c = Cursor.getDefaultCursor();
        }
        NO_SCROLL_CURSOR = c;
    }

    private boolean reverseAdvanceButtons = false;
    private volatile boolean useMulticore = true;
    private volatile boolean colorManage = true;
    private volatile boolean scaleToFit = true;
    private volatile boolean rotateImage = true;
    private volatile boolean showEXIFData = false;
    private volatile boolean showHistogram = false;
    private volatile boolean cycleWhenAtEnd = false;
    private volatile boolean playSoundOnCycle = false;
    private Sorting sorting = Sorting.NAME;
    private int step = 10;
    private boolean mouseButtonAdvance = false;
    private boolean waitForImagesToLoadWhenScrolling = true;
    private ExecutorService worker;
    private ExecutorService imageReloader;
    private ExecutorService mtImageLoader;
    private Timer autoSlideShowTimer;
    private int transitionDuration;
    private int slideShowDuration;
    private final ImageCanvas canvas;
    private final Toolbar toolbar;
    // Original non profiled images, as loaded from file. If color management is selected, than this image is already
    // converted to sRGB color space (done by the image loading plugins)
    private BufferedImage[] originalImages;
    // Color managed images. If display profile is unknown, these are identical to original images
    private BufferedImage[] images;
    // Scaled images (fully color managed)
    private BufferedImage[] scaledImages;
    // Exif of the currently loaded images
    private EXIFData[] exif;
    // Files from which the above images were loaded
    private File[] imageFiles;
    // Currently applied non profiled image
    private volatile BufferedImage theOriginalNonProfiledImage;
    // File that contains the currently displayed image
    private volatile File loadedFile;
    private volatile EXIFData loadedEXIF = new EXIFData();
    private volatile Histogram loadedHistogram;
    // The index in the total files array, where we are currently located
    private volatile int fileIndex;
    // All image files at our disposal
    private File[] files;
    // For convenience only: always identical to files.length
    private int numFiles;
    private volatile LinkBuffer fastReadBuffer = new LinkBuffer();
    private volatile int lastLoadedId = 0;
    private volatile AtomicBoolean loaded = new AtomicBoolean(false);
    private volatile AtomicBoolean fullyLoaded = new AtomicBoolean(false);
    private volatile AtomicBoolean wheelInMotion = new AtomicBoolean(false);
    private ColorSpace colorSpace;
    private File colorProfileFile;
    private float trueZoomValue = 3f;
    private final Object mutex = new Object();
    private int currentWidth, currentHeight;
    private boolean disableScrolling = false;
    private boolean toolbarAutoHide = true;
    private boolean showToolbar = true;
    private Color backgroundColor = Color.BLACK;
    private final List<Consumer<File>> recentFilesListeners = new CopyOnWriteArrayList<>();

    private class CanvasEventAdapter extends AbstractEventAdapter {

        private final Timer timer;

        public CanvasEventAdapter() {
            timer = new Timer(500,e -> {
                ((Timer) e.getSource()).stop();
                stopAllImageLoading();
                reloadImages();
            });
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!loaded.get()) return;
            JFrame frame = (JFrame) SwingUtilities.getRoot(Viewer.this);
            frame.toFront();
            int button = e.getButton();
            if (SwingUtilities.isMiddleMouseButton(e)) {
                // disable scrolling on middle button release
                disableScrolling = !disableScrolling;
            } else if (button == 4 || button == 5) {
                // advance the image when the side buttons are pressed, do full blown rendering
                advanceImage(reverseAdvanceButtons ? button == 5 : button == 4,false);
            } else if (mouseButtonAdvance && !e.isControlDown() && !e.isAltDown()
                    && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))) {
                // advance image in the case of left/right button but only if additional options are not selected
                advanceImage(reverseAdvanceButtons ? SwingUtilities.isRightMouseButton(e)
                        : SwingUtilities.isLeftMouseButton(e),false);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (toolbarAutoHide && showToolbar) {
                // Solves the repainting issue when the toolbar is shown
                int y = e.getY();
                int loc = getLocation().y;
                int height = toolbar.getHeight();
                if (height == 0) height = 28;
                boolean visible = toolbar.isVisible();
                if (y < loc + height) {
                    toolbar.setVisible(true);
                } else {
                    toolbar.setVisible(false);
                    canvas.requestFocus();
                    if (visible) {
                        canvas.resetZoomAndDrawing();
                    }
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            canvas.requestFocus();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (disableScrolling) {
                canvas.setCursor(NO_SCROLL_CURSOR);
                return;
            }
            if (!loaded.get()) return;
            if (e.isAltDown() && SwingUtilities.isRightMouseButton(e)) {
                // Change zooming with the wheel if alt is down
                float zoom = canvas.getZoomFactor();
                zoom += e.getWheelRotation() / 20f;
                if (zoom < 0.5) zoom = 0.5f;
                else if (zoom > 5) zoom = 5f;
                canvas.setZoomFactor(zoom,true);
            } else {
                timer.restart();
                wheelInMotion.compareAndSet(false,true);
                // advance the image as quickly as possible
                advanceImage(e.getWheelRotation() > 0,true);
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (!loaded.get()) {
                if (getWidth() != currentWidth || getHeight() != currentHeight) {
                    currentHeight = getHeight();
                    currentWidth = getWidth();
                    initialLoad();
                }
            } else if (fullyLoaded.get()) {
                stopAllImageLoading();
                canvas.clean();
                scaleImages();
            }
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            canvas.requestFocus();
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                canvas.setZoomFactor(trueZoomValue,false);
            }
        }
    }

    /**
     * Constructs a new viewer, with default settings.
     */
    public Viewer() {
        this(null);
    }

    /**
     * Constructs a new viewer.
     *
     * @param file the file which is initially displayed in the viewer and all its siblings loaded
     */
    public Viewer(File file) {
        super(new GridBagLayout());
        imageFiles = new File[BUFFER_SIZE];
        originalImages = new BufferedImage[BUFFER_SIZE];
        images = new BufferedImage[BUFFER_SIZE];
        scaledImages = new BufferedImage[BUFFER_SIZE];
        exif = new EXIFData[BUFFER_SIZE];
        openFileOrFolder(file);
        toolbar = new Toolbar(this);
        toolbar.addRecentFileSelectionListener(this::openFileOrFolder);
        toolbar.addRecentFileSelectionListener(f -> recentFilesListeners.forEach(c -> c.accept(f)));
        canvas = new ImageCanvas(toolbar);
        add(toolbar,gbc(0,1,1,1,1,0,GridBagConstraints.NORTH,GridBagConstraints.HORIZONTAL,0));
        add(canvas,gbc(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,0));
        toolbar.setVisible(false);
        CanvasEventAdapter eventAdapter = new CanvasEventAdapter();
        canvas.addComponentListener(eventAdapter);
        canvas.addMouseListener(eventAdapter);
        canvas.addMouseMotionListener(eventAdapter);
        canvas.addMouseWheelListener(eventAdapter);
        canvas.addKeyListener(eventAdapter);
        canvas.addHierarchyListener(eventAdapter);
        registerKeyStrokes();
        HistogramDisplayer.getInstance().setMouseEventReceiver(eventAdapter);
        EXIFDisplayer.getInstance().setMouseEventReceiver(eventAdapter);
        Thread th = new Thread(() -> dispose());
        th.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(th);
    }

    /**
     * Adds a listener which is notified when a recent file was selected.
     *
     * @param listener the listener to be notified
     */
    public void addRecentFilesSelectionListener(Consumer<File> listener) {
        recentFilesListeners.add(listener);
    }

    /**
     * Set the recent files list as the new list that is available in the main menu.
     *
     * @param files the files to show in the recent files menu
     */
    public void updateRecentFiles(File[] files) {
        toolbar.setRecentFiles(files);
    }

    /**
     * Apply the settings to the viewer.
     *
     * @param settings the settings to apply
     */
    public void applySettings(Settings settings) {
        setColorManage(settings.colorManage);
        setCycleWhenAtEnd(settings.cycleWhenAtEnd);
        setAutoHideMouseCursor(settings.autoHideMouse);
        setReverseAdvanceButtons(settings.reverseButtons);
        setPlaySoundOnCycle(settings.playSoundOnCycle);
        setWaitForImagesToLoadWhenScrolling(settings.waitForImagesToLoadWhenScrolling);
        setMouseButtonAdvance(settings.mouseButtonAdvance);
        setScaleSmallImagesToFit(settings.scaleToFit);
        setRotateImage(settings.rotateImage);
        setStepSize(settings.stepSize);
        if (settings.useDisplayColorProfile) {
            boolean systemColorProfile = settings.systemColorProfile;
            if (systemColorProfile) {
                setDestinationColorProfile(ColorProfileManager.getColorProfileForComponent(this));
            } else {
                File colorProfile = settings.colorProfile;
                setDestinationColorProfile(colorProfile);
            }
        } else {
            setDestinationColorProfile(null);
        }
        boolean useTransitions = settings.useTransitions;
        if (useTransitions) {
            setTransitionDuration(settings.transitionDuration);
            setTransitions(settings.transitions);
        } else {
            setTransitionDuration(0);
            setTransitions(new ArrayList<>());
        }
        setSlideShowDuration(settings.slideShowDuration);
        setUseMultipleCPUCores(settings.useMultipleCores);
        setZoomFactor(settings.zoomFactor);
        setLMBAction(settings.leftMouseButtonAction);
        setHighlightCursorColor(settings.cursorHue / 255f,settings.cursorOpacity);
        setBackgroundColor(settings.backgroundColor);
        setToolbarAutoHide(settings.toolbarAutoHide);
        setShowToolbar(settings.showToolbar);
        setSorting(settings.sortingOrder);
        updateRecentFiles(settings.getRecentFiles());
    }

    private void registerKeyStrokes() {
        registerKeyStroke(this,getKeyStroke(KeyEvent.VK_RIGHT,0),e -> advanceImage(true,false));
        registerKeyStroke(this,getKeyStroke(KeyEvent.VK_LEFT,0),e -> advanceImage(false,false));
        registerKeyStroke(this,getKeyStroke(KeyEvent.VK_HOME,0),e -> moveToIndex(0));
        registerKeyStroke(this,getKeyStroke(KeyEvent.VK_END,0),e -> moveToIndex(numFiles - 1));
        registerKeyStroke(this,getKeyStroke(KeyEvent.VK_PAGE_DOWN,0),e -> increaseByStep(true));
        registerKeyStroke(this,getKeyStroke(KeyEvent.VK_PAGE_UP,0),e -> increaseByStep(false));
    }

    /**
     * Stop all threads and dispose of allocated resources.
     */
    public void dispose() {
        stopAllImageLoading();
        if (imageReloader != null) {
            imageReloader.shutdownNow();
        }
        canvas.dispose();
        synchronized (mutex) {
            // Null the buffers to help the GC
            for (int i = 0; i < BUFFER_SIZE; i++) {
                imageFiles[i] = null;
                originalImages[i] = null;
                images[i] = null;
                scaledImages[i] = null;
                exif[i] = null;
            }
        }
    }

    /**
     * Start or stop the automatic slide show.
     */
    public void toggleSlideShow() {
        if (autoSlideShowTimer.isRunning()) {
            autoSlideShowTimer.stop();
        } else {
            autoSlideShowTimer.start();
        }
    }

    /**
     * Update slide show with new parameters (e.g. slideshow advance timer tick length change).
     */
    private void updateSlideShow() {
        boolean start = false;
        if (autoSlideShowTimer != null) {
            start = autoSlideShowTimer.isRunning();
            autoSlideShowTimer.stop();
        }
        autoSlideShowTimer = new Timer(slideShowDuration + transitionDuration,e -> advanceImage(true,false));
        if (start) {
            autoSlideShowTimer.start();
        }
    }

    /**
     * Set the new duration for how long an image is displayed when auto slide show is turned on.
     *
     * @param duration the time in millis how long to show an image
     */
    public void setSlideShowDuration(int duration) {
        this.slideShowDuration = duration;
        updateSlideShow();
    }

    /**
     * Sets the action for the left mouse button click.
     *
     * @param action left mouse button click action
     */
    public void setLMBAction(LMBAction action) {
        canvas.setLMBAction(action);
    }

    /**
     * Reset any drawing and zooming that might exist on the current image.
     */
    public void resetZoomAndDrawing() {
        canvas.resetZoomAndDrawing();
    }

    /**
     * Sets the hue of the highlight cursor color and its opacity.
     *
     * @param hue the hue (0 to 1)
     * @param opacity the opacity (0 to 255)
     */
    public void setHighlightCursorColor(float hue, int opacity) {
        canvas.setHighlightCursorColor(hue,opacity);
    }

    /**
     * Sets the flag whether every single image in the folder will be shown before advancing to the next one, when
     * scrolling through images. If this parameter is false, scrolling will be faster, but some images might be skipped.
     *
     * @param show true to show all images or false to skip images
     */
    public void setWaitForImagesToLoadWhenScrolling(boolean show) {
        if (this.waitForImagesToLoadWhenScrolling == show) return;
        stopAllImageLoading();
        this.waitForImagesToLoadWhenScrolling = show;
    }

    /**
     * Sets the flag whether a short beep is played when the images are cycled.
     *
     * @param playSound true to play a beep or false otherwise
     */
    public void setPlaySoundOnCycle(boolean playSound) {
        this.playSoundOnCycle = playSound;
    }

    /**
     * Sets the sorting order used for sorting the files in the selected folder.
     *
     * @param sorting the sorting order
     */
    public void setSorting(Sorting sorting) {
        if (this.sorting == sorting) return;
        this.sorting = sorting;
        File file;
        synchronized (mutex) {
            file = loadedFile;
            if (file == null && numFiles > 0 && fileIndex > -1) {
                file = files[fileIndex];
            }
        }
        openFileOrFolder(file);
    }

    /**
     * If true the slide show will continue with the first image after the last one, and the last one will be shown if
     * the previous is requested when the first one is shown.
     *
     * @param cycle true for cycling or false to stop at last/first image
     */
    public void setCycleWhenAtEnd(boolean cycle) {
        this.cycleWhenAtEnd = cycle;
    }

    /**
     * Sets the flag whether the exif data should be loaded and shown or hidden. When hidden it is not loaded at all to
     * save on the image loading time.
     *
     * @param show true to show the exif or false otherwise
     */
    public void setShowEXIFData(boolean show) {
        this.showEXIFData = show;
        if (show) {
            getMTImageLoader().execute(() -> {
                EXIFData exif;
                synchronized (mutex) {
                    exif = this.loadedEXIF;
                }
                EXIFDisplayer.getInstance().setData(exif);
            });
        }
        EXIFDisplayer.getInstance().setShowing(show);
    }

    /**
     * Sets the flag whether the histogram data should be loaded and shown or hidden. When hidden it is not loaded at
     * all to save on the image loading time.
     *
     * @param show true to show the histogram or false otherwise
     */
    public void setShowHistogram(boolean show) {
        this.showHistogram = show;
        if (show) {
            getMTImageLoader().execute(() -> {
                Histogram histogram;
                File file;
                BufferedImage image;
                synchronized (mutex) {
                    histogram = this.loadedHistogram;
                    file = loadedFile;
                    image = theOriginalNonProfiledImage;
                }
                if (histogram == null
                        || file != null && !file.getAbsolutePath().equals(histogram.getFile().getAbsolutePath())) {
                    histogram = new Histogram(file,ImageUtil.imageHistogram(image));
                    synchronized (mutex) {
                        loadedHistogram = histogram;
                    }
                }
                HistogramDisplayer.getInstance().setHistogram(histogram);
            });
        }
        HistogramDisplayer.getInstance().setShowing(show);
    }

    /**
     * Returns true if histogram is currently showing or false otherwise.
     *
     * @return true if image histogram showing or false if not
     */
    public boolean isShowHistogram() {
        return showHistogram;
    }

    /**
     * Returns true if exif data is displayed or false otherwise.
     *
     * @return true if exif is displayed
     */
    public boolean isShowEXIFData() {
        return showEXIFData;
    }

    /**
     * Set a new zooming factor and recreates the zoom-in image in the background.
     *
     * @param factor the zoom in factor
     */
    public void setZoomFactor(float factor) {
        trueZoomValue = factor;
        canvas.setZoomFactor(factor,false);
    }

    /**
     * Indicate if the mouse is automatically hidden after being still for a defined time period. The cursor is shown
     * again when the mouse is moved.
     *
     * @param autoHide true to hide the cursor or false to keep it showing
     */
    public void setAutoHideMouseCursor(boolean autoHide) {
        canvas.setAutoHideMouseCursor(autoHide);
    }

    /**
     * Reverse the buttons next and previous image.
     *
     * @param reverse true to reverse the buttons or false to use the default order
     */
    public void setReverseAdvanceButtons(boolean reverse) {
        this.reverseAdvanceButtons = reverse;
    }

    /**
     * Sets the duration of a single transition between images in milliseconds.
     *
     * @param duration transition duration in milliseconds
     */
    public void setTransitionDuration(int duration) {
        this.transitionDuration = duration;
        canvas.setTransitionDuration(duration);
        updateSlideShow();
    }

    /**
     * Sets the size of the step when advancing the images (in any direction) using the page up and page down keys. The
     * view is advanced for this many images. If step is 5, every fifth image will be displayed when pressing the page
     * keys.
     *
     * @param stepSize the size of advancing step
     */
    public void setStepSize(int stepSize) {
        this.step = stepSize;
    }

    /**
     * Indicate if the images that are too small for the current canvas size, should be scaled to fit into the window
     * preserving the aspect ration.
     *
     * @param scaleToFit true to increase the size of small images to fit, or false to show them in original size
     */
    public void setScaleSmallImagesToFit(boolean scaleToFit) {
        if (this.scaleToFit == scaleToFit) return;
        this.scaleToFit = scaleToFit;
        reloadImages();
    }

    /**
     * Indicate if the images that are not taken horizontally should be rotated according to the data in the exif info.
     *
     * @param rotateImage true to rotate the image or false otherwise
     */
    public void setRotateImage(boolean rotateImage) {
        if (this.rotateImage == rotateImage) return;
        this.rotateImage = rotateImage;
        reloadImages();
    }

    /**
     * Set the transitions that will be used when transitioning to the next or previous image. If the list is empty, the
     * image will be set immediately when requested, without any animation.
     *
     * @param transitionsToUse the list of transitions to use
     */
    public void setTransitions(List<Transition> transitionsToUse) {
        canvas.setTransitionEffects(transitionsToUse);
    }

    /**
     * Sets the flag whether multiple CPU cores will be utilized as much as possible.
     *
     * @param useMultipleCores true to utilize multiple cores, or do everything sequentially
     */
    public void setUseMultipleCPUCores(boolean useMultipleCores) {
        this.useMulticore = useMultipleCores && NUM_CORES > 1;
    }

    /**
     * Sets the flag whether the left/right mouse buttons advance and go back between the images rather the being used
     * for zoom. This is useful, when the mouse does not have additional side buttons.
     *
     * @param advance true to advance and go back or false to use them for zoom
     */
    public void setMouseButtonAdvance(boolean advance) {
        if (this.mouseButtonAdvance == advance) return;
        this.mouseButtonAdvance = advance;
        canvas.setEnableZoom(!advance);
    }

    /**
     * Sets the flag whether the images are color managed or not. If the images are color managed than the destination
     * color profile will also be used to transform to the monitor color space (if the profile is selected).
     *
     * @param colorManage true to color manage the photos or false otherwise
     */
    public void setColorManage(boolean colorManage) {
        if (this.colorManage == colorManage) return;
        this.colorManage = colorManage;
        reloadImages();
    }

    /**
     * Set the image background color.
     *
     * @param backgroundColor the bacground color
     */
    public void setBackgroundColor(Color backgroundColor) {
        if (Objects.equals(this.backgroundColor,backgroundColor)) return;
        this.backgroundColor = backgroundColor;
        canvas.setBackgroundColor(backgroundColor);
        reloadImages();
    }

    /**
     * Returns true if the toolbar is shown or false otherwise.
     *
     * @return true if the toolbar is shown or false otherwise
     */
    public boolean isShowToolbar() {
        return showToolbar;
    }

    /**
     * Toggle the visibility of the toolbar.
     *
     * @param showToolbar true to show the toolbar or false to hide it permanently
     */
    public void setShowToolbar(boolean showToolbar) {
        this.showToolbar = showToolbar;
        toolbar.setVisible(showToolbar);
    }

    /**
     * Set the flag whether the toolbar is automatically hidden when mouse moves away or always shown.
     *
     * @param toolbarAutoHide true if the toolbar is automatically hidden or false otherwise
     */
    public void setToolbarAutoHide(boolean toolbarAutoHide) {
        if (this.toolbarAutoHide == toolbarAutoHide) return;
        this.toolbarAutoHide = toolbarAutoHide;
        int y = toolbarAutoHide ? 1 : 0;
        remove(toolbar);
        remove(canvas);
        add(toolbar,gbc(0,y,1,1,1,0,GridBagConstraints.NORTH,GridBagConstraints.HORIZONTAL,0));
        add(canvas,gbc(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,0));
        toolbar.setVisible(!toolbarAutoHide && showToolbar);
        canvas.setToolbarAutoHide(toolbarAutoHide);
    }

    /**
     * Returns true if toolbar is in auto hide mode or false if it is shown all the time.
     *
     * @return true if toolbar is auto hiding or false otherwise
     */
    public boolean isToolbarAutoHide() {
        return toolbarAutoHide;
    }

    /**
     * Update the screen dimension according to the current graphics device this canvas is displayed on.
     */
    public void updateScreenDimension() {
        canvas.updateScreenDimension();
    }

    /**
     * Load the color profile from the given file and set it as a destination color profile. The
     * {@link #setColorManage(boolean)} has to be set to <code>true</code> in order for the destination color profile to
     * be used at all. If the file is null or does not exist, only the conversion from the embedded color profile to the
     * sRGB will be performed.
     *
     * @param colorProfile the icc profile file
     */
    public void setDestinationColorProfile(File colorProfile) {
        if (Objects.equals(this.colorProfileFile,colorProfile)) return;
        synchronized (this) {
            if (colorProfile == null || !colorProfile.exists()) {
                this.colorSpace = null;
            } else {
                try (FileInputStream fis = new FileInputStream(colorProfile)) {
                    this.colorSpace = new ICC_ColorSpace(ICC_Profile.getInstance(fis));
                } catch (Exception e) {
                    this.colorSpace = null;
                    System.err.println("Cannot load the color profile from: " + colorProfile);
                }
            }
            colorProfileFile = colorProfile;
        }
        reloadImages();
    }

    /**
     * Returns the file that is currently being open in the viewer.
     *
     * @return the currently open file
     */
    public File getOpenFile() {
        return loadedFile;
    }

    /**
     * Scan the working folder for all available images.
     *
     * @param file the file at which to start and continue with all its siblings (but no subfolders)
     */
    public void openFileOrFolder(File file) {
        stopAllImageLoading();
        synchronized (mutex) {
            if (file == null) {
                this.files = new File[0];
                this.fileIndex = 0;
            } else {
                ImageUtil.clearCache();
                File folder = file.isFile() ? file.getParentFile() : file;
                File[] ff = folder.listFiles();
                if (ff != null) {
                    final Comparator<File> comparator = sorting.getComparator();
                    this.files = Arrays.asList(ff).stream().filter(f -> {
                        if (f == null) return false;
                        try {
                            return Files.probeContentType(f.toPath()).substring(0,5).equalsIgnoreCase("image");
                        } catch (IOException e) {
                            return false;
                        } catch (NullPointerException e) {
                            return f.getName().toLowerCase(Locale.UK).endsWith(".psd");
                        }
                    }).sorted(comparator).toArray(File[]::new);
                } else {
                    this.files = new File[0];
                }
                this.fileIndex = file.isFile() ? indexOf(file,this.files,0,false) : 0;
            }
            numFiles = this.files.length;
        }
        currentHeight = getHeight();
        currentWidth = getWidth();
        if (currentWidth != 0 && currentHeight != 0 && numFiles > 0) {
            fullyLoaded.compareAndSet(true,false);
            loaded.compareAndSet(true,false);
            initialLoad();
        }
    }

    private void stopAllImageLoading() {
        wheelInMotion.compareAndSet(true,false);
        synchronized (this) {
            if (worker != null) {
                worker.shutdownNow();
                worker = null;
            }
            if (mtImageLoader != null) {
                mtImageLoader.shutdownNow();
                mtImageLoader = null;
            }
            if (imageReloader != null) {
                imageReloader.shutdownNow();
                imageReloader = null;
            }
        }
        lastLoadedId = fileIndex;
        fastReadBuffer.clear();
        getMTImageLoader().execute(() -> System.gc());
    }

    private void increaseByStep(boolean forward) {
        stopAllImageLoading();
        int idx;
        synchronized (mutex) {
            idx = fileIndex;
        }
        moveToIndex(forward ? idx + step : idx - step);
    }

    private void moveToIndex(int idx) {
        if (numFiles == 0) return;
        int index = idx;
        if (index >= numFiles) {
            index = numFiles - 1;
        } else if (index < 0) {
            index = 0;
        }
        stopAllImageLoading();
        synchronized (mutex) {
            fileIndex = index;
            loadedFile = files[index];
            if (cycleWhenAtEnd) {
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    int a = (index + i - PRELOADING_BUFFER) % numFiles;
                    if (a < 0) a += numFiles;
                    imageFiles[i] = files[a];
                }
            } else {
                if (index >= PRELOADING_BUFFER && index < numFiles - PRELOADING_BUFFER) {
                    for (int i = 0; i < BUFFER_SIZE && i < numFiles; i++) {
                        imageFiles[i] = files[index + i - PRELOADING_BUFFER];
                    }
                } else if (index < PRELOADING_BUFFER) {
                    for (int i = 0; i < BUFFER_SIZE && i < numFiles; i++) {
                        imageFiles[i] = files[i];
                    }
                } else if (index >= numFiles - PRELOADING_BUFFER) {
                    int k = numFiles >= BUFFER_SIZE ? numFiles - BUFFER_SIZE : 0;
                    for (int i = 0; i < BUFFER_SIZE && i < numFiles; i++) {
                        imageFiles[i] = files[k + i];
                    }
                }
            }
        }
        reloadImages();
    }

    /**
     * Play the sound at the end of cycle if the settings match.
     */
    private void playSoundIfEndOfCycle() {
        if (cycleWhenAtEnd && playSoundOnCycle) {
            AudioPlayer.playAsync();
        }
    }

    /**
     * Advances to the next image and refills the buffer by loading further images.
     *
     * @param forward direction of advance; true for next image, false for previous
     * @param fast indicates if the loading of images should be done fast (true) or best quality (false)
     */
    public void advanceImage(boolean forward, boolean fast) {
        if (canvas.isInTransition()) {
            canvas.skipTransition();
        } else {
            getWorker().execute(() -> {
                int idx = showNextImage(forward,fast);
                if (idx == -1) return;
                idx = (forward ? idx - 1 : idx + 1);
                if (idx >= numFiles) {
                    idx = idx % numFiles;
                    playSoundIfEndOfCycle();
                } else if (idx < 0) {
                    idx += numFiles;
                    playSoundIfEndOfCycle();
                }
                updateImageBuffers(idx,forward,fast);

            });
        }
    }

    /**
     * Shows the next image and returns the index in the files array of that image.
     *
     * @param forward true for next, or false for previous.
     * @param fast indicates if advancing should be fast or best quality (fast advance does not use transitions)
     * @return the index of the loaded image in the files array (in single threaded mode, this is always the fileIndex)
     */
    private int showNextImage(boolean forward, boolean fast) {
        int index = 0;
        int idx;
        synchronized (mutex) {
            idx = fileIndex;
        }
        if (forward) {
            if (cycleWhenAtEnd) {
                index = PRELOADING_BUFFER + 1;
            } else {
                if (idx == numFiles - 1) {
                    if (loadedFile == files[idx]) {
                        return -1;
                    }
                    index = 2 * PRELOADING_BUFFER;
                } else if (idx <= PRELOADING_BUFFER) {
                    index = idx + 1;
                } else {
                    index = PRELOADING_BUFFER + 1;
                }
            }
        } else {
            if (cycleWhenAtEnd) {
                index = PRELOADING_BUFFER - 1;
            } else {
                if (idx == 0) {
                    if (loadedFile == files[0]) {
                        return -1;
                    }
                    index = 0;
                } else if (idx <= PRELOADING_BUFFER) {
                    index = idx - 1;
                } else if (idx > numFiles - PRELOADING_BUFFER - 1) {
                    index = BUFFER_SIZE - numFiles + idx - 1;
                } else {
                    index = PRELOADING_BUFFER - 1;
                }
            }
        }
        final BufferedImage scaled, profiled, original;
        final EXIFData exifData;
        final File file;
        synchronized (mutex) {
            loadedFile = imageFiles[index];
            original = originalImages[index];
            profiled = images[index];
            scaled = scaledImages[index];
            exifData = exif[index];
            fileIndex = indexOf(loadedFile,files,fileIndex,true);
            idx = fileIndex;
            file = loadedFile;
        }
        if (profiled == null) {
            advanceImage(forward,fast);
            return -1;
        }
        applyImage(file,idx,original,profiled,scaled,exifData,true,forward,fast);
        return idx;
    }

    /**
     * Updates the image buffers, by loading the next or previous image to fill in the void in the buffer. If multiple
     * cores are used and fast parameter is true, this method loads several images concurrently for fastest performance.
     *
     * @param idx the index of the image that has just been sent to the display
     * @param forward true if we are sent the the next image, or false if we sent the previous
     * @param fast true to update the buffers fast, or false to use best quality
     */
    private void updateImageBuffers(int idx, boolean forward, boolean fast) {
        if (idx < 0 || idx > numFiles) return;
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        if (forward) {
            // If cycling is disabled and we're at the end of the list, there is nothing to do
            if (!cycleWhenAtEnd && idx == numFiles - 1) {
                return;
            }
            // If not at the end of the list, check if there are any remaining
            // files to load or if all are already in the buffer
            if (cycleWhenAtEnd || (idx < numFiles - PRELOADING_BUFFER - 1 && idx >= PRELOADING_BUFFER)) {
                synchronized (mutex) {
                    // Rotate the buffers
                    for (int i = 0; i < BUFFER_SIZE - 1; i++) {
                        imageFiles[i] = imageFiles[i + 1];
                        originalImages[i] = originalImages[i + 1];
                        images[i] = images[i + 1];
                        scaledImages[i] = scaledImages[i + 1];
                        exif[i] = exif[i + 1];
                    }
                }
                // Check if we reach the end and play the sound.
                if (idx + 1 >= numFiles) {
                    playSoundIfEndOfCycle();
                }
                // Identify the index of the new file to add to the buffer
                int a = (idx + 1 + PRELOADING_BUFFER) % numFiles;
                if (a < 0) {
                    a += numFiles;
                }
                File file = files[a];
                if (fast) {
                    // In fast image loading (mouse scrolling), get the latest from the buffer and fill up the read
                    // buffer with new images
                    ImageFile imf = null;
                    if (useMulticore) {
                        if (waitForImagesToLoadWhenScrolling) {
                            // If waiting for images, take the first image, and remove all others
                            // The first image might not be always the first one that follows the current.
                            // If Next image is huge and the one after very small, the one after might be loaded first,
                            // which means that it will also be shown.
                            imf = fastReadBuffer.removeFirst();
                            while (imf != null && imf.id <= a) {
                                imf = fastReadBuffer.removeFirst();
                            }
                        } else {
                            // Otherwise shutdown the loader, we will restart it, and take the last image from the
                            // buffer
                            synchronized (Viewer.this) {
                                getMTImageLoader().shutdown();
                                mtImageLoader = null;
                            }
                            imf = fastReadBuffer.removeLast();
                            fastReadBuffer.clear();
                        }
                        if (fastReadBuffer.size() <= FAST_READ_BUFFER_MIN) {
                            // Fill up the buffer with as many images as there is free room, taking into consideration
                            // the number of cores
                            int fastReadFileIndex = fastReadBuffer.isEmpty() ? idx + 2 + PRELOADING_BUFFER
                                    : fastReadBuffer.getLast().id + 1;
                            for (int i = 0; i < NUM_CORES - 1; i++) {
                                int m = fastReadFileIndex + i;
                                if (lastLoadedId >= m) {
                                    continue;
                                }
                                if (cycleWhenAtEnd) {
                                    m = m % numFiles;
                                } else if (m >= numFiles) {
                                    break;
                                }
                                final int k = m;
                                lastLoadedId = k;
                                getMTImageLoader().execute(() -> {
                                    EXIFImage loadedImage = loadImage(files[k],fast);
                                    if (Thread.currentThread().isInterrupted()) return;
                                    ImageFile f = new ImageFile(files[k],loadedImage.originalImage,
                                            loadedImage.profiledImage,loadedImage.data,k);
                                    if (waitForImagesToLoadWhenScrolling) {
                                        try {
                                            synchronized (fastReadBuffer) {
                                                ImageFile ff = fastReadBuffer.getLast();
                                                if (Thread.currentThread().isInterrupted()) return;
                                                if (ff != null && ff.id == f.id - 1) {
                                                    fastReadBuffer.add(f);
                                                } else {
                                                    fastReadBuffer.wait(1);
                                                }
                                            }
                                        } catch (InterruptedException e) {
                                            return;
                                        }
                                    } else {
                                        fastReadBuffer.add(f);
                                    }
                                });
                            }
                        }
                    }
                    if (imf == null) {
                        // Do not parallelize or no image ready yet.
                        EXIFImage loadedImage = loadImage(file,fast);
                        scaleAndSet(file,loadedImage.originalImage,loadedImage.profiledImage,loadedImage.data,width,
                                height,fast,BUFFER_SIZE - 1);
                    } else {
                        scaleAndSet(imf.file,imf.originalImage,imf.profiledImage,imf.exif,width,height,fast,
                                BUFFER_SIZE - 1);
                    }
                } else {
                    EXIFImage loadedImage = loadImage(file,fast);
                    scaleAndSet(file,loadedImage.originalImage,loadedImage.profiledImage,loadedImage.data,width,height,
                            fast,BUFFER_SIZE - 1);
                }
            }
        } else {
            // This is similar as in the forward case, except that index travels in the opposite direction
            if (!cycleWhenAtEnd && idx == 0) {
                return;
            }
            if (cycleWhenAtEnd || (idx > PRELOADING_BUFFER && idx <= numFiles - PRELOADING_BUFFER - 1)) {
                synchronized (mutex) {
                    for (int i = BUFFER_SIZE - 1; i > 0; i--) {
                        imageFiles[i] = imageFiles[i - 1];
                        originalImages[i] = originalImages[i - 1];
                        images[i] = images[i - 1];
                        scaledImages[i] = scaledImages[i - 1];
                        exif[i] = exif[i - 1];
                    }
                }
                if (idx - 1 < 0) {
                    playSoundIfEndOfCycle();
                }
                int a = (idx - 1 - PRELOADING_BUFFER) % numFiles;
                if (a < 0) {
                    a += numFiles;
                }
                File file = files[a];
                if (fast) {
                    ImageFile imf = null;
                    if (useMulticore) {
                        if (waitForImagesToLoadWhenScrolling) {
                            imf = fastReadBuffer.removeLast();
                            while (imf != null && imf.id >= a) {
                                imf = fastReadBuffer.removeLast();
                            }
                        } else {
                            synchronized (Viewer.this) {
                                getMTImageLoader().shutdown();
                                mtImageLoader = null;
                            }
                            imf = fastReadBuffer.removeFirst();
                            fastReadBuffer.clear();
                        }
                        if (fastReadBuffer.size() <= FAST_READ_BUFFER_MIN) {
                            int fastReadFileIndex = fastReadBuffer.isEmpty() ? idx - 2 - PRELOADING_BUFFER
                                    : fastReadBuffer.getFirst().id - 1;
                            for (int i = 0; i < NUM_CORES - 1; i++) {
                                int m = fastReadFileIndex - i;
                                if (lastLoadedId <= m) continue;
                                if (cycleWhenAtEnd) {
                                    m = m % numFiles;
                                    if (m < 0) {
                                        m += numFiles;
                                    }
                                } else if (m < 0) {
                                    break;
                                }
                                final int k = m;
                                lastLoadedId = m;
                                getMTImageLoader().execute(() -> {
                                    EXIFImage loadedImage = loadImage(files[k],fast);
                                    if (Thread.currentThread().isInterrupted()) return;
                                    ImageFile f = new ImageFile(files[k],loadedImage.originalImage,
                                            loadedImage.profiledImage,loadedImage.data,k);
                                    if (waitForImagesToLoadWhenScrolling) {
                                        try {
                                            synchronized (fastReadBuffer) {
                                                ImageFile ff = fastReadBuffer.getFirst();
                                                if (Thread.currentThread().isInterrupted()) return;
                                                if (ff != null && ff.id == f.id + 1) {
                                                    fastReadBuffer.add(f);
                                                } else {
                                                    fastReadBuffer.wait(1);
                                                }
                                            }
                                        } catch (InterruptedException e) {
                                            return;
                                        }
                                    } else {
                                        fastReadBuffer.add(f);
                                    }
                                });
                            }
                        }
                    }
                    if (imf == null) {
                        EXIFImage loadedImage = loadImage(file,fast);
                        scaleAndSet(file,loadedImage.originalImage,loadedImage.profiledImage,loadedImage.data,width,
                                height,fast,0);
                    } else {
                        scaleAndSet(imf.file,imf.originalImage,imf.profiledImage,imf.exif,width,height,fast,0);
                    }
                } else {
                    EXIFImage loadedImage = loadImage(file,fast);
                    scaleAndSet(file,loadedImage.originalImage,loadedImage.profiledImage,loadedImage.data,width,height,
                            fast,0);
                }
            }
        }
    }

    private void scaleAndSet(File file, BufferedImage original, BufferedImage profiledImage, EXIFData data, int width,
            int height, boolean fast, int destIndex) {
        // convenience method to avoid repetitive code
        // Scale the profiled image and set it on the buffers
        BufferedImage scaled = getScaledImage(profiledImage,width,height,fast);
        synchronized (mutex) {
            imageFiles[destIndex] = file;
            originalImages[destIndex] = original;
            images[destIndex] = profiledImage;
            scaledImages[destIndex] = scaled;
            exif[destIndex] = data;
        }
    }

    /**
     * Loads initial images.
     */
    public void initialLoad() {
        if (numFiles == 0) return;
        final int idx;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            idx = fileIndex;
        }
        getWorker().execute(() -> {
            if (useMulticore) {
                initialLoadMT(idx);
            } else {
                initialLoadST(idx);
            }
        });
    }

    /**
     * Loads the initial images in the single thread.
     *
     * @param idx the index of the initially selected file
     */
    private void initialLoadST(int idx) {
        // no synchronization needed for the first image, because this only happens at startup and nothing can be
        // advanced before the loaded boolean is set
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        int index = getBufferIndexForFileIndex(idx);
        // load the first image as quickly as possible
        File file;
        synchronized (mutex) {
            imageFiles[index] = files[idx];
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (i == index) continue;
                int k = (idx + i - index) % numFiles;
                if (k < 0) k += numFiles;
                imageFiles[i] = files[k];
            }
            file = imageFiles[index];
        }
        EXIFImage firstImage = loadImage(file,true);
        width = canvas.getWidth();
        height = canvas.getHeight();
        BufferedImage scaledImage = getScaledImage(firstImage.profiledImage,width,height,true);
        synchronized (mutex) {
            originalImages[index] = firstImage.originalImage;
            images[index] = firstImage.profiledImage;
            scaledImages[index] = scaledImage;
            exif[index] = firstImage.data;
        }
        loadedFile = file;
        fileIndex = idx;
        applyImage(loadedFile,idx,firstImage.originalImage,firstImage.profiledImage,scaledImage,firstImage.data,false,
                true,true);
        loaded.compareAndSet(false,true);
        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
        EXIFImage[] loadedImages = new EXIFImage[BUFFER_SIZE];
        BufferedImage[] scaledLoadedImages = new BufferedImage[BUFFER_SIZE];
        File[] loadedFiles = new File[BUFFER_SIZE];
        if (cycleWhenAtEnd) {
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                int k = (idx - index + i) % numFiles;
                if (k < 0) k += numFiles;
                loadedFiles[i] = files[k];
                loadedImages[i] = loadImage(loadedFiles[i],false);
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                width = canvas.getWidth();
                height = canvas.getHeight();
                scaledLoadedImages[i] = getScaledImage(loadedImages[i].profiledImage,width,height,false);
                if (i == index) {
                    applyImage(loadedFiles[i],k,loadedImages[i].originalImage,loadedImages[i].profiledImage,
                            scaledLoadedImages[i],loadedImages[i].data,false,true,false);
                }
            }
        } else {
            for (int i = 0; i < BUFFER_SIZE && i < numFiles; i++) {
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                int k = idx - index + i;
                loadedFiles[i] = files[k];
                loadedImages[i] = loadImage(loadedFiles[i],false);
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                width = canvas.getWidth();
                height = canvas.getHeight();
                scaledLoadedImages[i] = getScaledImage(loadedImages[i].profiledImage,width,height,false);
                if (i == index) {
                    applyImage(loadedFiles[i],idx,loadedImages[i].originalImage,loadedImages[i].profiledImage,
                            scaledLoadedImages[i],loadedImages[i].data,false,true,false);
                }
            }
        }
        boolean rescale = false;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) {
                fullyLoaded.compareAndSet(false,true);
                return;
            }
            width = canvas.getWidth();
            height = canvas.getHeight();
            for (int i = 0; i < BUFFER_SIZE; i++) {
                imageFiles[i] = loadedFiles[i];
                originalImages[i] = loadedImages[i].originalImage;
                images[i] = loadedImages[i].profiledImage;
                scaledImages[i] = scaledLoadedImages[i];
                exif[i] = loadedImages[i].data;
                if (!rescale && scaledImages[i] != null
                        && (scaledImages[i].getWidth() != width || scaledImages[i].getHeight() != height)) {
                    rescale = true;
                }
            }
            fullyLoaded.compareAndSet(false,true);
        }
        if (rescale) {
            scaleImages();
        }
    }

    /**
     * Loads the first few images to fill up the buffer delegating this task to multiple threads to utilize all cores.
     *
     * @param idx the index of the initially selected file
     */
    private void initialLoadMT(final int idx) {
        // no synchronization needed initially, because this only happens at startup and nothing can be advanced before
        // the loaded boolean is set
        int wwidth = canvas.getWidth();
        int hheight = canvas.getHeight();
        if (wwidth == 0 || hheight == 0) {
            return;
        }
        final int index = getBufferIndexForFileIndex(idx);
        final EXIFImage[] loadedImages = new EXIFImage[BUFFER_SIZE];
        final BufferedImage[] scaledLoadedImages = new BufferedImage[BUFFER_SIZE];
        final File[] loadedFiles = new File[BUFFER_SIZE];
        synchronized (mutex) {
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (i == index) continue;
                int k = (idx + i - index) % numFiles;
                if (k < 0) k += numFiles;
                imageFiles[i] = files[k];
            }
        }
        getMTImageLoader().execute(() -> {
            // do a fast load to show an image as quickly as possible
            synchronized (mutex) {
                imageFiles[index] = files[idx];
            }
            EXIFImage imm = loadImage(files[idx],true);
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            BufferedImage im = getScaledImage(imm.profiledImage,width,height,true);
            synchronized (mutex) {
                originalImages[index] = imm.originalImage;
                images[index] = imm.profiledImage;
                scaledImages[index] = im;
                exif[index] = imm.data;
                loadedFile = files[idx];
            }
            fileIndex = idx;
            applyImage(loadedFile,idx,imm.originalImage,imm.profiledImage,im,imm.data,false,true,true);
            loaded.compareAndSet(false,true);
        });
        // delegate image creation to multiple workers and wait for all images to be loaded
        int[] c = { 0 };
        boolean[] imageShown = { false };
        synchronized (c) {
            int expected = 0;
            if (cycleWhenAtEnd) {
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    expected++;
                    final int k = i;
                    getMTImageLoader().execute(() -> {
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        int a = (idx - index + k) % numFiles;
                        if (a < 0) a += numFiles;
                        loadedFiles[k] = files[a];
                        loadedImages[k] = loadImage(loadedFiles[k],false);
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        int width = canvas.getWidth();
                        int height = canvas.getHeight();
                        scaledLoadedImages[k] = getScaledImage(loadedImages[k].profiledImage,width,height,false);
                        if (k == index && loaded.get()) {
                            imageShown[0] = true;
                            applyImage(loadedFiles[index],a,loadedImages[index].originalImage,
                                    loadedImages[index].profiledImage,scaledLoadedImages[index],
                                    loadedImages[index].data,false,true,false);
                        }
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        synchronized (c) {
                            c[0]++;
                            c.notifyAll();
                        }
                    });
                }
            } else {
                for (int i = 0; i < BUFFER_SIZE && i < numFiles; i++) {
                    if (idx - index + i >= numFiles) break;
                    expected++;
                    final int k = i;
                    getMTImageLoader().execute(() -> {
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        loadedFiles[k] = files[idx - index + k];
                        loadedImages[k] = loadImage(loadedFiles[k],false);
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        int width = canvas.getWidth();
                        int height = canvas.getHeight();
                        scaledLoadedImages[k] = getScaledImage(loadedImages[k].profiledImage,width,height,false);
                        if (k == index && loaded.get()) {
                            imageShown[0] = true;
                            applyImage(loadedFiles[index],idx,loadedImages[index].originalImage,
                                    loadedImages[index].profiledImage,scaledLoadedImages[index],
                                    loadedImages[index].data,false,true,false);
                        }
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        synchronized (c) {
                            c[0]++;
                            c.notifyAll();
                        }
                    });
                }
            }
            try {
                // image loader is never aborted, when doing initial load, because scrolling is not allowed before
                // the initial load is complete
                int min = Math.min(expected,Math.min(BUFFER_SIZE,numFiles));
                while (c[0] != min && !wheelInMotion.get() && !Thread.currentThread().isInterrupted()) {
                    c.wait(10);
                }
                if (!imageShown[0] && loadedImages[index] != null) {
                    applyImage(loadedFiles[index],idx,loadedImages[index].originalImage,
                            loadedImages[index].profiledImage,scaledLoadedImages[index],loadedImages[index].data,false,
                            true,false);
                }
                boolean rescale = false;
                synchronized (mutex) {
                    if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) {
                        fullyLoaded.compareAndSet(false,true);
                        return;
                    }
                    int width = canvas.getWidth();
                    int height = canvas.getHeight();
                    for (int i = 0; i < BUFFER_SIZE; i++) {
                        if (loadedImages[i] == null) continue;
                        imageFiles[i] = loadedFiles[i];
                        originalImages[i] = loadedImages[i].originalImage;
                        images[i] = loadedImages[i].profiledImage;
                        scaledImages[i] = scaledLoadedImages[i];
                        exif[i] = loadedImages[i].data;
                        if (!rescale && scaledImages[i] != null
                                && (scaledImages[i].getWidth() != width || scaledImages[i].getHeight() != height)) {
                            rescale = true;
                        }
                    }
                    fullyLoaded.compareAndSet(false,true);
                }
                if (rescale) {
                    scaleImages();
                }
            } catch (InterruptedException e) {
                // ignore - may happen in the case if the wheel loader kicked in
            }
        }
    }

    private int getBufferIndexForFileIndex(int index) {
        if (cycleWhenAtEnd) {
            return PRELOADING_BUFFER;
        } else if (index <= PRELOADING_BUFFER) {
            return index;
        } else if (index > numFiles - PRELOADING_BUFFER - 1) {
            return BUFFER_SIZE - numFiles + index;
        } else {
            return PRELOADING_BUFFER;
        }

    }

    /**
     * Rescale images in the current image buffer to the fit size.
     */
    public void scaleImages() {
        if (wheelInMotion.get()) return;
        getImageReloader().execute(() -> {
            if (useMulticore) {
                scaleImagesMT();
            } else {
                scaleImagesST();
            }
        });
    }

    /**
     * Rescale all images from the current image buffer to the fit size in a single thread.
     */
    private void scaleImagesST() {
        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int index;
        int fIndex;
        final BufferedImage profiledImage, orgiginalImage;
        final File file;
        final EXIFData exifData;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            index = getBufferIndexForFileIndex(fileIndex);
            file = imageFiles[index];
            orgiginalImage = originalImages[index];
            profiledImage = images[index];
            exifData = exif[index];
            fIndex = fileIndex;
        }
        final BufferedImage scaledImage = getScaledImage(profiledImage,width,height,false);
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            scaledImages[index] = scaledImage;
        }
        applyImage(file,fIndex,orgiginalImage,profiledImage,scaledImage,exifData,false,true,false);
        BufferedImage image;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (i == index) continue;
            synchronized (mutex) {
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                image = images[i];
            }
            if (image == null) continue;
            image = getScaledImage(image,width,height,false);
            synchronized (mutex) {
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                scaledImages[i] = image;
            }
        }
    }

    /**
     * Rescale all images from the current image buffer to the fit size using multiple threads.
     */
    private void scaleImagesMT() {
        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        final int in;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            in = fileIndex;
        }
        final int index = getBufferIndexForFileIndex(in);
        final BufferedImage profiledImage, originalImage;
        final File file;
        final EXIFData exifData;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            file = imageFiles[index];
            originalImage = originalImages[index];
            profiledImage = images[index];
            exifData = exif[index];
        }
        getMTImageLoader().execute(() -> {
            final BufferedImage im = getScaledImage(profiledImage,width,height,false);
            synchronized (mutex) {
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                scaledImages[index] = im;
            }
            applyImage(file,in,originalImage,profiledImage,im,exifData,false,true,false);
        });
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (i == index) continue;
            final int k = i;
            getMTImageLoader().execute(() -> {
                BufferedImage scaledImage;
                synchronized (mutex) {
                    if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                    scaledImage = images[k];
                }
                if (scaledImage == null) return;
                scaledImage = getScaledImage(scaledImage,width,height,false);
                synchronized (mutex) {
                    if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                    scaledImages[k] = scaledImage;
                }
            });
        }
    }

    /**
     * Reload all images from the current image buffer and scaled the to the fit size. The action can be executed in a
     * single thread or concurrently using multiple threads to utilize all cores.
     */
    public void reloadImages() {
        if (numFiles == 0) return;
        if (!loaded.get()) return;
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        getImageReloader().execute(() -> {
            if (!loaded.get()) return;
            if (useMulticore) {
                reloadImagesMT(width,height);
            } else {
                reloadImagesST(width,height);
            }
        });
    }

    /**
     * Reloads images in the current image buffer and scales them to working size, preserving aspect ratio. The method
     * uses multiple threads to utilize all available cores.
     *
     * @param width the width of the canvas, to which the image is scaled
     * @param height the height of the canvas, to which the image is scaled
     */
    private void reloadImagesMT(int width, int height) {
        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
        int in;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            in = fileIndex;
        }
        int[] idx = { in };
        int[] index = { getBufferIndexForFileIndex(in) };
        final EXIFImage[] loadedImages = new EXIFImage[BUFFER_SIZE];
        final BufferedImage[] scaledLoadedImages = new BufferedImage[BUFFER_SIZE];
        final File[] fis = new File[BUFFER_SIZE];
        getMTImageLoader().execute(() -> {
            File file;
            synchronized (mutex) {
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                file = loadedFile;
            }
            final EXIFImage loadedImage = loadImage(file,false);
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            final BufferedImage scaledImage = getScaledImage(loadedImage.profiledImage,width,height,false);
            int i;
            synchronized (mutex) {
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                imageFiles[index[0]] = file;
                originalImages[index[0]] = loadedImage.originalImage;
                images[index[0]] = loadedImage.profiledImage;
                scaledImages[index[0]] = scaledImage;
                exif[index[0]] = loadedImage.data;
                loadedFile = file;
                fileIndex = indexOf(loadedFile,files,fileIndex,true);
                i = fileIndex;
            }
            applyImage(file,i,loadedImage.originalImage,loadedImage.profiledImage,scaledImage,loadedImage.data,false,
                    true,false);
            loadedImages[index[0]] = loadedImage;
            scaledLoadedImages[index[0]] = scaledImage;
            fis[index[0]] = file;
        });
        int[] c = { 0 };
        synchronized (c) {
            int expected = 0;
            if (cycleWhenAtEnd) {
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    if (i == index[0]) continue;
                    final int k = i;
                    int d = (idx[0] - index[0] + i) % numFiles;
                    if (d < 0) d += numFiles;
                    expected++;
                    fis[i] = files[d];
                    getMTImageLoader().execute(() -> {
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        loadedImages[k] = loadImage(fis[k],false);
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        scaledLoadedImages[k] = getScaledImage(loadedImages[k].profiledImage,width,height,false);
                        synchronized (c) {
                            c[0]++;
                            c.notifyAll();
                        }
                    });
                }
            } else {
                for (int i = 0; i < BUFFER_SIZE && i < numFiles; i++) {
                    if (i == index[0]) continue;
                    final int k = i;
                    int d = idx[0] - index[0] + i;
                    if (d >= numFiles) break;
                    expected++;
                    fis[i] = files[d];
                    getMTImageLoader().execute(() -> {
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        loadedImages[k] = loadImage(fis[k],false);
                        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                        scaledLoadedImages[k] = getScaledImage(loadedImages[k].profiledImage,width,height,false);
                        synchronized (c) {
                            c[0]++;
                            c.notifyAll();
                        }
                    });
                }
            }
            try {
                // wheel in motion takes care of the case when the scrolling might kick in
                int min = Math.min(expected,Math.min(BUFFER_SIZE - 1,numFiles - 1));
                while (c[0] != min && !wheelInMotion.get() && !Thread.currentThread().isInterrupted()) {
                    c.wait(10);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (loadedImages[i] == null) continue;
                imageFiles[i] = fis[i];
                originalImages[i] = loadedImages[i].originalImage;
                images[i] = loadedImages[i].profiledImage;
                scaledImages[i] = scaledLoadedImages[i];
                exif[i] = loadedImages[i].data;
            }
        }
    }

    /**
     * Reloads all images in the current buffer and scale them to target size. Image aspect ratio is preserved when
     * scaling. Everything happens in the calling thread.
     *
     * @param width the width of the canvas, to which the images are scaled
     * @param height the height of the canvas, to which the images are scaled
     */
    private void reloadImagesST(int width, int height) {
        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
        int idx;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            idx = fileIndex;
        }
        int index = getBufferIndexForFileIndex(idx);
        File file;
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            file = loadedFile;
        }
        final EXIFImage loadedImage = loadImage(file,false);
        if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
        final BufferedImage scaledImage = getScaledImage(loadedImage.profiledImage,width,height,false);
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            originalImages[index] = loadedImage.originalImage;
            images[index] = loadedImage.profiledImage;
            scaledImages[index] = scaledImage;
            exif[index] = loadedImage.data;
            imageFiles[index] = file;
            loadedFile = file;
            fileIndex = indexOf(loadedFile,files,fileIndex,true);
            idx = fileIndex;
        }
        applyImage(file,idx,loadedImage.originalImage,loadedImage.profiledImage,scaledImage,loadedImage.data,false,true,
                false);
        EXIFImage[] loadedImages = new EXIFImage[BUFFER_SIZE];
        BufferedImage[] scaledLoadedImages = new BufferedImage[BUFFER_SIZE];
        File[] fis = new File[BUFFER_SIZE];
        loadedImages[index] = loadedImage;
        scaledLoadedImages[index] = scaledImage;
        fis[index] = file;
        EXIFImage imm;
        if (cycleWhenAtEnd) {
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (i == index) continue;
                int a = (idx - index + i) % numFiles;
                if (a < 0) a += numFiles;
                fis[i] = files[a];
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                imm = loadImage(fis[i],false);
                loadedImages[i] = imm;
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                scaledLoadedImages[i] = getScaledImage(loadedImages[i].profiledImage,width,height,false);
            }
        } else {
            for (int i = 0; i < BUFFER_SIZE && i < numFiles; i++) {
                if (i == index) continue;
                fis[i] = files[idx - index + i];
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                imm = loadImage(fis[i],false);
                loadedImages[i] = imm;
                if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
                scaledLoadedImages[i] = getScaledImage(loadedImages[i].profiledImage,width,height,false);
            }
        }
        synchronized (mutex) {
            if (wheelInMotion.get() || Thread.currentThread().isInterrupted()) return;
            for (int i = 0; i < BUFFER_SIZE; i++) {
                imageFiles[i] = fis[i];
                originalImages[i] = loadedImages[i].originalImage;
                images[i] = loadedImages[i].profiledImage;
                scaledImages[i] = scaledLoadedImages[i];
                exif[i] = loadedImages[i].data;
            }
        }
    }

    /**
     * Find the index of the given file in the array of files. The files in the array are expected to be sorted by name
     * (ignoring case). First the file at <code>guess</code> index is checked and compared to the given file. It is then
     * used as the basis for the further search: if guess file is smaller than file, than we search from guess to the
     * end, otherwise from guess to the start of the array. Except for the guess file, the files are compare using the
     * object equality, rather than {@link Object#equals(Object)}, because it is faster.
     *
     * @param file the file to search for
     * @param files the list of available files
     * @param guess the initial index at which to start
     * @return the index of the file
     */
    private int indexOf(File file, File[] files, int guess, boolean strict) {
        if (file == null) {
            return guess;
        }
        File f = files[guess];
        if (guess > -1 && files[guess] == file) {
            return guess;
        }
        int c = sorting.getComparator().compare(f,file);
        if (strict) {
            if (c < 0) {
                for (int i = guess; i < files.length; i++) {
                    if (files[i] == file) {
                        return i;
                    }
                }
            } else {
                for (int i = guess; i > -1; i--) {
                    if (files[i] == file) {
                        return i;
                    }
                }
            }
        } else {
            String name = file.getName();
            if (c < 0) {
                for (int i = guess; i < files.length; i++) {
                    if (files[i].getName().equalsIgnoreCase(name)) {
                        return i;
                    }
                }
            } else {
                for (int i = guess; i > -1; i--) {
                    if (files[i].getName().equalsIgnoreCase(name)) {
                        return i;
                    }
                }
            }
        }
        // Maybe comparator returned that files are equal. In that case rescan the entire array and find the file.
        for (int i = 0; i < files.length; i++) {
            if (files[i] == file) {
                return i;
            }
        }
        return guess;
    }

    /**
     * Construct a single thread executor, which should be used for general task execution.
     *
     * @return the worker executor
     */
    private synchronized ExecutorService getWorker() {
        if (worker == null) {
            worker = new ImageExecutor("Worker",1,new DismissableBlockingQueue<>(2));
        }
        return worker;
    }

    /**
     * Constructs the image loading executor, which is used to exploit the multi-core characteristics of the CPU.
     *
     * @return the image loading executor
     */
    private synchronized ExecutorService getMTImageLoader() {
        if (mtImageLoader == null) {
            mtImageLoader = new ImageExecutor("MultiCoreLoader",NUM_CORES,new LinkedBlockingQueue<>(4 * NUM_CORES));
        }
        return mtImageLoader;
    }

    /**
     * Returns the executor used for reloading and rescaling images.
     *
     * @return the reloading and rescaling executor
     */
    private synchronized ExecutorService getImageReloader() {
        if (imageReloader == null) {
            imageReloader = new ImageExecutor("ImageReloader",1,new DismissableBlockingQueue<>(2));
        }
        return imageReloader;
    }

    /**
     * Apply the provided image to the displayer. If needed calculate the histogram from the original image.
     *
     * @param file the file from which the image was loaded
     * @param currentFileIndex the index of the file in the selected folder
     * @param original the original image which is not manager according to selected profile (but the embedded profile
     *            is valued, if such preference is selected)
     * @param profiled the original image changed according to the display profile (used for zooming)
     * @param scaled the scaled profiled image (the actual displayed image)
     * @param transition true if the transition between current and new image should be done according to the selected
     *            transition (normal advance) or false if it is a direct change
     * @param forward true to apply forward transition or false ot apply backward transition
     * @param fast true if the advance to the next image should be as fast as possible
     */
    private synchronized void applyImage(File file, int currentFileIndex, BufferedImage original,
            BufferedImage profiled, BufferedImage scaled, EXIFData exif, boolean transition, boolean forward,
            boolean fast) {
        if (file == null) return;
        Histogram hist;
        synchronized (mutex) {
            loadedEXIF = exif;
            hist = loadedHistogram;
            theOriginalNonProfiledImage = original;
        }
        if (showEXIFData) {
            EXIFDisplayer.getInstance().setData(exif);
        }
        if (showHistogram) {
            if (hist == null || hist.getFile() == null
                    || !file.getAbsolutePath().equals(hist.getFile().getAbsolutePath())) {
                hist = new Histogram(file,ImageUtil.imageHistogram(original));
                synchronized (mutex) {
                    loadedHistogram = hist;
                }
            }
            HistogramDisplayer.getInstance().setHistogram(hist);
        }
        SwingUtilities.invokeLater(() -> {
            toolbar.setImageInfo(file.getAbsolutePath(),currentFileIndex,numFiles);
            if (fast) {
                canvas.setImage(scaled,null);
            } else if (transition) {
                canvas.transitionTo(scaled,profiled,forward);
            } else {
                canvas.setImage(scaled,profiled);
            }
        });
    }

    /**
     * Convenience method to load the image from the given file, which catches exceptions and return NO_IMAGE in case
     * exception happens.
     *
     * @param file the file to load
     * @param fast true for fast loading or false for quality image
     * @return the original image (in sRGB color space) and the image converted to display profile, together with the
     *         image exif info
     */
    private EXIFImage loadImage(File file, boolean fast) {
        if (file == null) {
            return NO_IMAGE;
        }
        try {
            ColorSpace destinationSpace;
            synchronized (this) {
                destinationSpace = this.colorSpace;
            }
            EXIFImage image;
            if (fast) {
                image = ImageUtil.loadImageAsFastAsPossible(file,rotateImage);
            } else {
                image = ImageUtil.loadImage(file,colorManage,destinationSpace,rotateImage);
            }
            return image == null ? NO_IMAGE : image;
        } catch (IIOException e) {
            System.err.println("IIO Exception (" + e.getClass() + "):" + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO Exception (" + e.getClass() + "): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception (" + e.getClass() + "): " + e.getMessage());
        }
        return NO_IMAGE;
    }

    /**
     * Scale the image to the target size using the fast or quality algorithm. If the image is smaller than the target
     * size, the value of {@link #scaleToFit} is taken into account. If the image should not be scaled it is drawn on
     * the center of the returned image. The returned image is always of the given size.
     *
     * @param source the source image to scale
     * @param width the maximum target image width
     * @param height the maximum target image height
     * @param fast true for fast scaling or false for slow scaling
     * @return the scaled image
     */
    private BufferedImage getScaledImage(BufferedImage source, int width, int height, boolean fast) {
        if (scaleToFit || source.getWidth() > width || source.getHeight() > height) {
            return ImageUtil.getScaledImage(source,backgroundColor,width,height,fast);
        }
        BufferedImage bi = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.drawImage(source,(width - source.getWidth()) / 2,(height - source.getHeight()) / 2,null);
        g.dispose();
        return bi;
    }
}
