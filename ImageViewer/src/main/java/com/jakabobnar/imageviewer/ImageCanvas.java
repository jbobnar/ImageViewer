/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.jakabobnar.imageviewer.image.ImageUtil;
import com.jakabobnar.imageviewer.util.AbstractEventAdapter;
import com.jakabobnar.imageviewer.util.DismissableBlockingQueue;
import com.jakabobnar.imageviewer.util.ImageExecutor;
import com.jakabobnar.imageviewer.util.LMBAction;

/**
 * ImageCanvas is the canvas that draws the actual image. It also takes care of zooming.
 *
 * @author Jaka Bobnar
 *
 */
public class ImageCanvas extends JComponent {

    private static final long serialVersionUID = 8815682284094274014L;

    private static final Cursor NO_CURSOR = Toolkit.getDefaultToolkit()
            .createCustomCursor(new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB),new Point(0,0),"null");

    private Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();

    private transient BufferedImage image;
    private transient BufferedImage transitionToImage;
    private boolean transitionForward = true;
    private Timer transitionTimer;
    private Timer hideMouseTimer;
    private boolean autoHideMouseCursor = true;
    private boolean toolbarAutoHide = true;

    private boolean inTransition;
    private long transitionStartTime;
    private float transitionParameter;

    private Color backgroundColor = Color.BLACK;
    private float transitionSpeed = 2000;
    private final List<Transition> transitionEffects = new ArrayList<>(0);// .asList(new FadeTransition());
    private transient Transition selectedTransition;
    private final ZoomHandler zoomHandler;
    private final Toolbar applicationToolbar;

    private static final int NONE = 0;
    private static final int ZOOM = 1;
    private static final int ORIGINAL = 2;
    private static final int CURSOR = 3;
    private static final int PAINTING = 4;
    private static final int CURSOR_SIZE = 50;

    private class ZoomHandler extends AbstractEventAdapter implements Serializable {
        private static final long serialVersionUID = -2245928845012988054L;
        private final ExecutorService zoomExecutor = new ImageExecutor("CanvasZoom",1,
                new DismissableBlockingQueue<>(2));
        private float zoomFactor = 3f;
        private boolean doFastRescaling = false;

        private final Stroke paintStroke = new BasicStroke(5f,BasicStroke.JOIN_ROUND,BasicStroke.CAP_ROUND);
        private BufferedImage cursor;

        private Image zoomImage;
        private BufferedImage originalImage;
        private Point zoomOffset;
        private int zoomW, zoomH;
        private int zoomWorg, zoomHorg;
        private Point zoomOffsetOrg;
        private float zoomFactorOrg = 1f;
        private boolean originalWidthSmaller, originalHeightSmaller;
        private int zoomedIn = NONE;
        private Point zoomStart;
        private Point zoomTranslation;
        private Point zoomAnchor;
        private boolean enableZoom = true;
        // left mouse button shows highlight cursor or the original image
        private LMBAction leftMouseButtonAction = LMBAction.CURSOR;
        private Point previousPoint = null;
        private int lastMouseButtonDown;
        private boolean mouseButtonDown = false;
        private boolean keyPressed = false;
        private boolean firstTimePaint = true;

        private Color highlightColor;

        private final Optional<Robot> robot;

        ZoomHandler() {
            Robot theRobot;
            try {
                theRobot = new Robot();
            } catch (AWTException | SecurityException e) {
                theRobot = null;
            }
            this.robot = Optional.ofNullable(theRobot);
            setHighlightCursorColor(40 / 255f,100);
        }

        /**
         * Sets the hue of the highlight cursor color and its opacity.
         *
         * @param hue the hue (0 to 1)
         * @param opacity the opacity (0 to 255)
         */
        public void setHighlightCursorColor(float hue, int opacity) {
            cursor = new BufferedImage(CURSOR_SIZE,CURSOR_SIZE,BufferedImage.TYPE_INT_ARGB);
            Graphics2D gr = cursor.createGraphics();
            gr.setRenderingHints(ImageUtil.HINTS);
            Color c = Color.getHSBColor(hue,1f,1f);
            highlightColor = new Color(c.getRed(),c.getGreen(),c.getBlue(),opacity);
            gr.setColor(highlightColor);
            gr.fillOval(0,0,cursor.getWidth(),cursor.getHeight());
            gr.dispose();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (mouseButtonDown && !keyPressed) {
                handleStartEvent(getMousePosition(),MouseInfo.getPointerInfo().getLocation(),e.isControlDown(),
                        e.isAltDown(),lastMouseButtonDown);
                keyPressed = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keyPressed = false;
            previousPoint = null;
            if (zoomedIn != PAINTING) {
                zoomedIn = NONE;
                repaint();
            }
        }

        private boolean isToolbarClick(MouseEvent e) {
            return applicationToolbar.isVisible() && applicationToolbar.getHeight() > e.getY();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (inTransition) return;
            if (isToolbarClick(e)) return;
            previousPoint = null;
            if (e.getClickCount() == 2) {
                zoomedIn = NONE;
                repaint();
            } else {
                lastMouseButtonDown = SwingUtilities.isLeftMouseButton(e) ? MouseEvent.BUTTON1
                        : SwingUtilities.isRightMouseButton(e) ? MouseEvent.BUTTON3 : e.getButton();
                mouseButtonDown = true;
                keyPressed = e.isControlDown() || e.isAltDown();
                handleStartEvent(e.getPoint(),e.getLocationOnScreen(),e.isControlDown(),e.isAltDown(),
                        lastMouseButtonDown);
            }
        }

        private void handleStartEvent(Point location, Point locationOnScreen, boolean ctrlDown, boolean altDown,
                int button) {
            if (enableZoom) {
                float fact = 1f;
                Point offset = null;
                zoomAnchor = location;
                zoomStart = locationOnScreen;
                if (button == MouseEvent.BUTTON3) {
                    // start zooming if right mouse button is pressed
                    if (ctrlDown) {
                        zoomedIn = ORIGINAL;
                        fact = zoomFactorOrg;
                        offset = zoomOffsetOrg;
                    } else {
                        zoomedIn = ZOOM;
                        fact = zoomFactor;
                        offset = zoomOffset;
                    }
                } else if (button == MouseEvent.BUTTON1) {
                    // show original image if left buttons is pressed
                    if (leftMouseButtonAction == LMBAction.ZOOM) {
                        zoomedIn = ctrlDown ? PAINTING : altDown ? CURSOR : ORIGINAL;
                    } else if (leftMouseButtonAction == LMBAction.CURSOR) {
                        zoomedIn = ctrlDown ? PAINTING : CURSOR;
                    } else if (leftMouseButtonAction == LMBAction.PAINT) {
                        zoomedIn = altDown ? CURSOR : PAINTING;
                    } else {
                        zoomedIn = ORIGINAL;
                    }

                    fact = zoomFactorOrg;
                    offset = zoomOffsetOrg;
                }
                if (offset == null) {
                    return;
                }
                if (zoomedIn > NONE) {
                    if (zoomedIn < CURSOR) {
                        setCursor(NO_CURSOR);
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    if (zoomAnchor != null) {
                        zoomTranslation = new Point((int) (zoomAnchor.x * (1 - fact)) + offset.x,
                                (int) (zoomAnchor.y * (1 - fact)) + offset.y);
                        trimZoom();
                    }
                    repaint();
                }
            } else if (button == MouseEvent.BUTTON1) {
                zoomedIn = ctrlDown ? PAINTING : altDown ? CURSOR : NONE;
                if (zoomedIn > NONE) {
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            }
        }

        /**
         * Reset the zooming state.
         *
         * @param repaint true if repaint should be called afterwards
         */
        public void reset(boolean repaint) {
            mouseButtonDown = false;
            firstTimePaint = true;
            previousPoint = null;
            zoomedIn = NONE;
            if (repaint) {
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isToolbarClick(e)) return;
            mouseButtonDown = false;
            previousPoint = null;
            if (enableZoom) {
                if (zoomedIn > NONE && (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isLeftMouseButton(e))
                        && zoomStart != null) {
                    // reposition the mouse to the location, where the image was clicked and stop zooming
                    if (zoomedIn < CURSOR) {
                        robot.ifPresent(r -> r.mouseMove(zoomStart.x,zoomStart.y));
                    }
                    if (!autoHideMouseCursor) {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    if (zoomedIn != PAINTING) {
                        zoomedIn = NONE;
                        repaint();
                    }
                }
            } else if (zoomedIn != PAINTING && zoomedIn != NONE) {
                zoomedIn = NONE;
                repaint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (zoomedIn > NONE && enableZoom && zoomTranslation != null) {
                // when zoomed and mouse is dragged, move the image around to see different portions of the image
                Point p = e.getPoint();
                if (zoomAnchor != null) {
                    zoomTranslation = new Point(zoomTranslation.x + p.x - zoomAnchor.x,
                            zoomTranslation.y + p.y - zoomAnchor.y);
                    trimZoom();
                }
                zoomAnchor = p;
                if (zoomedIn < CURSOR) {
                    robot.ifPresent(theRobot -> {
                        // allow continuous dragging - if we are close to the border, move the mouse to the other side
                        // of the canvas
                        Rectangle r = getBounds();
                        if (r.width - p.x < 2 || p.x < 2 || r.height - p.y < 2 || p.y < 2) {
                            Point rr = getLocationOnScreen();
                            int xx = r.width - p.x < 2 ? 3 : p.x < 2 ? r.width - 3 : p.x;
                            int yy = r.height - p.y < 2 ? 3 : p.y < 2 ? r.height - 3 : p.y;
                            theRobot.mouseMove(xx + rr.x,yy + rr.y);
                            zoomAnchor = new Point(xx,yy);
                        }
                    });
                }
                repaint();
            } else if (!enableZoom && zoomedIn > NONE) {
                repaint();
            }
        }

        /**
         * Reposition to zoom-in coordinates to avoid zooming into the black border between the canvas and the image. We
         * do not need to zoom into the background area.
         */
        private void trimZoom() {
            int width = getWidth();
            int height = getHeight();
            int zw = 0, zh = 0;
            if (zoomedIn == ZOOM) {
                zw = zoomW;
                zh = zoomH;
            } else if (zoomedIn == ORIGINAL) {
                zw = zoomWorg;
                zh = zoomHorg;
            }
            if (zoomTranslation.x + zw < width) {
                zoomTranslation.x = width - zw;
            } else if (zoomTranslation.x > 0) {
                zoomTranslation.x = 0;
            }
            if (zoomTranslation.y + zh < height) {
                zoomTranslation.y = height - zh;
            } else if (zoomTranslation.y > 0) {
                zoomTranslation.y = 0;
            }
        }

        /**
         * Create the zoom in image in a background thread. The given original image is taken as the source
         *
         * @param orgImage the original image to zoom into
         */
        void createZoomImage(final BufferedImage orgImage) {
            synchronized (this) {
                zoomImage = null;
            }
            this.originalImage = orgImage;
            this.originalWidthSmaller = false;
            this.originalHeightSmaller = false;
            if (orgImage == null) {
                return;
            }
            if (enableZoom) {
                zoomExecutor.execute(() -> {
                    // first, "downsize" the image to the current canvas size
                    int width = getWidth();
                    int height = getHeight();
                    zoomWorg = orgImage.getWidth();
                    zoomHorg = orgImage.getHeight();
                    zoomFactorOrg = 1f;
                    zoomFactorOrg = Math.max(zoomWorg / (float) width,zoomHorg / (float) height);
                    if (zoomWorg < width || zoomHorg < height) {
                        // zoomFactorOrg = Math.max(zoomWorg / (float) width,zoomHorg / (float) height);
                        originalWidthSmaller = zoomWorg < width;
                        originalHeightSmaller = zoomHorg < height;
                    }
                    double ratio = (double) zoomWorg / zoomHorg;
                    zoomW = (int) (height * ratio);
                    zoomH = height;
                    if (zoomW > width) {
                        zoomW = width;
                        zoomH = (int) (width / ratio);
                    }
                    // zoom offset is needed for drawing the image, to avoid zooming into black borders
                    zoomOffset = new Point((int) (zoomFactor * (width - zoomW) / 2),
                            (int) (zoomFactor * (height - zoomH) / 2));
                    if (originalWidthSmaller || originalHeightSmaller) {
                        zoomOffsetOrg = new Point((width - zoomWorg) / 2,(height - zoomHorg) / 2);
                    } else {
                        zoomOffsetOrg = new Point((int) (zoomFactorOrg * (width - zoomW) / 2),
                                (int) (zoomFactorOrg * (height - zoomH) / 2));
                    }

                    // now, upsize the image to canvas size * zoom factor
                    zoomW *= zoomFactor;
                    zoomH *= zoomFactor;
                    if (zoomW != 0 && zoomH != 0) {
                        BufferedImage scaledImage = ImageUtil.getScaledImage(orgImage,Color.BLACK,zoomW,zoomH,
                                doFastRescaling);
                        Graphics g = getGraphics();
                        if (g != null) {
                            // do an off screen draw of the image. Without this the image takes a long time to render
                            // the first time it is used. Once it's been drawn somewhere, the next time it just falls
                            // through. We could use MediaTracker, but this is twice as fast.
                            g = g.create();
                            g.setClip(0,0,0,0);
                            g.drawImage(scaledImage,0,0,null);
                            g.dispose();
                        }

                        synchronized (this) {
                            zoomImage = scaledImage;
                            if (zoomedIn == ZOOM || zoomedIn == ORIGINAL) {
                                repaint();
                            }
                        }
                    }
                });
            }
        }

        /**
         * Enable or disable zooming.
         *
         * @param enable true to enable zooming or false to disable
         */
        void setEnableZoom(boolean enable) {
            this.enableZoom = enable;
            if (enable) {
                createZoomImage(originalImage);
            }
        }

        /**
         * Sets a new zoom factor and resizes the zoomed in image according to the new value.
         *
         * @param zoomFactor the new zoom factor
         * @param fast true to do a fast rescaling or false for smooth
         */
        void setZoomFactor(float zoomFactor, boolean fast) {
            if (Float.compare(this.zoomFactor, zoomFactor) != 0 || fast != doFastRescaling) {
                this.zoomFactor = zoomFactor;
                this.doFastRescaling = fast;
                createZoomImage(originalImage);
            }
        }

        /**
         * Paint the zoomed in image to the given graphics. The zoomed in image is properly translated according to the
         * current mouse position.
         *
         * @param g the graphics to draw the image on
         * @return true if the zoomed in image was drawn or false otherwise
         */
        boolean paintZoom(Graphics g) {
            if (inTransition) return false;
            // paint entire screen black to avoid artifacts when increasing the size of the frame
            if (zoomedIn > NONE) {
                Image im = null;
                synchronized (this) {
                    if (zoomedIn == ZOOM) {
                        im = zoomImage;
                    } else if (zoomedIn == ORIGINAL) {
                        im = originalImage;
                    }
                }
                if (zoomedIn < CURSOR) {
                    if (im != null && zoomTranslation != null) {
                        int x = zoomTranslation.x;
                        int y = zoomTranslation.y;
                        int w = im.getWidth(null);
                        int width = getWidth();
                        int h = im.getHeight(null);
                        int height = getHeight();
                        boolean paintBackground = false;
                        if (w <= width) {
                            x = (width - w) / 2;
                            paintBackground = true;
                        }
                        if (h <= height) {
                            y = (height - h) / 2;
                            paintBackground = true;
                        }
                        if (paintBackground) {
                            g.fillRect(0,0,screenDimension.width,screenDimension.height);
                        }
                        g.drawImage(im,x,y,null);
                    }
                    return true;
                } else if (zoomedIn == CURSOR) {
                    g.drawImage(image,0,0,null);
                    Point p = getMousePosition();
                    if (p == null) {
                        p = previousPoint;
                    }
                    if (p != null) {
                        g.drawImage(cursor,p.x - CURSOR_SIZE / 2,p.y - CURSOR_SIZE / 2,null);
                        previousPoint = p;
                    }
                    return true;
                } else if (zoomedIn == PAINTING) {
                    if (firstTimePaint) {
                        firstTimePaint = false;
                        return false;
                    }
                    Point p = getMousePosition();
                    if (p == null) {
                        p = previousPoint;
                    }
                    if (p != null) {
                        if (previousPoint != null) {
                            ((Graphics2D) g).setStroke(paintStroke);
                            g.setColor(highlightColor);
                            g.drawLine(previousPoint.x,previousPoint.y,p.x,p.y);
                        }
                        previousPoint = p;
                    }
                    return true;
                }
            } else {
                g.fillRect(0,0,screenDimension.width,screenDimension.height);
            }
            return false;
        }
    }

    /**
     * Construct a new image canvas.
     *
     * @param toolbar reference to the toolbar, which needs to be repainted after canvas is repainted
     */
    public ImageCanvas(Toolbar toolbar) {
        zoomHandler = new ZoomHandler();
        applicationToolbar = toolbar;
        addMouseListener(zoomHandler);
        addMouseMotionListener(zoomHandler);
        addKeyListener(zoomHandler);
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                hideMouseTimer.restart();
            }
        });
        transitionTimer = new Timer(10,e -> {
            long time = System.currentTimeMillis();
            long duration = time - transitionStartTime;
            if (duration < transitionSpeed) {
                inTransition = true;
                transitionParameter = 1f - duration / transitionSpeed;
            } else {
                transitionStartTime = -1;
                transitionTimer.stop();
                transitionParameter = 0f;
                image = transitionToImage;
                inTransition = false;
            }
            repaint();
        });
        hideMouseTimer = new Timer(1000,e -> {
            if (autoHideMouseCursor && zoomHandler.zoomedIn == NONE) {
                setCursor(NO_CURSOR);
            }
        });
        hideMouseTimer.start();
        setOpaque(true);
    }

    /**
     * Set the flag whether the toolbar is automatically hidden when mouse is moved away or not. This affects the way
     * how the canvas is repainted.
     *
     * @param toolbarAutoHide true if the toolbar is automatically hidden or false otherwise.
     */
    public void setToolbarAutoHide(boolean toolbarAutoHide) {
        this.toolbarAutoHide = toolbarAutoHide;
        repaint();
    }

    /**
     * Indicate if the mouse is automatically hidden after being still for a defined time period. The cursor is shown
     * again when the mouse is moved.
     *
     * @param autoHide true to hide the cursor or false to keep it showing
     */
    public void setAutoHideMouseCursor(boolean autoHide) {
        this.autoHideMouseCursor = autoHide;
        if (this.autoHideMouseCursor) {
            hideMouseTimer.restart();
        } else {
            hideMouseTimer.stop();
        }
    }

    /**
     * Set a new zooming factor and recreates the zoom-in image in the background.
     *
     * @param factor the zoom in factor
     * @param fast true to use fast algorithm or false for smooth algorithm
     */
    public void setZoomFactor(float factor, boolean fast) {
        zoomHandler.setZoomFactor(factor,fast);
    }

    /**
     * Returns the current zoom factor.
     *
     * @return the current zoom factor
     */
    public float getZoomFactor() {
        return zoomHandler.zoomFactor;
    }

    /**
     * Enable or disable zooming with left/right mouse button.
     *
     * @param enableZoom true to enable or false to disable zooming
     */
    public void setEnableZoom(boolean enableZoom) {
        zoomHandler.setEnableZoom(enableZoom);
    }

    /**
     * Set a new transition duration (speed) in milliseconds. Each transition to next or previous image will be
     * rescheduled to take the given amount of time.
     *
     * @param durationInMillis the duration of the transition in milliseconds
     */
    public void setTransitionDuration(int durationInMillis) {
        this.transitionSpeed = durationInMillis;
    }

    /**
     * Sets a new list of transition effects, which will be used randomly when transitioning to the next or previous
     * image.
     *
     * @param transitions the list of transitions to use
     */
    public void setTransitionEffects(List<Transition> transitions) {
        this.transitionEffects.clear();
        this.transitionEffects.addAll(transitions);
    }

    /**
     * Sets the action for the left mouse button click.
     *
     * @param action the action to use for the left mouse button click
     */
    public void setLMBAction(LMBAction action) {
        zoomHandler.leftMouseButtonAction = action;
    }

    /**
     * Sets the hue of the highlight cursor color and its opacity
     *
     * @param hue the hue (0 to 1)
     * @param opacity the opacity (0 to 255)
     */
    public void setHighlightCursorColor(float hue, int opacity) {
        zoomHandler.setHighlightCursorColor(hue,opacity);
    }

    /**
     * Set the background color.
     *
     * @param backgroundColor the background color
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        repaint();
    }

    /**
     * Disposes of all internal resources. This method has to be called, when there is no more intentions of using this
     * canvas. After disposing of the canvas, subsequent calls might (but not necessarily) throw exceptions.
     */
    public void dispose() {
        zoomHandler.zoomExecutor.shutdownNow();
    }

    /**
     * Returns true if the canvas in transitioning to the new image at the moment or false otherwise.
     *
     * @return true if in transition or false otherwise
     */
    public boolean isInTransition() {
        return inTransition;
    }

    /**
     * Set the new image and redraw the canvas.
     *
     * @param image the image that fits the canvas
     * @param orgImage the original full size image
     */
    public void setImage(BufferedImage image, BufferedImage orgImage) {
        zoomHandler.reset(false);
        skipTransition();
        this.image = image;
        zoomHandler.createZoomImage(orgImage);
        repaint();
    }

    /**
     * Skip the current transition and advance to the new image immediately.
     */
    public void skipTransition() {
        if (inTransition) {
            transitionTimer.stop();
            inTransition = false;
            // zoom image is already in the making, so there is no need to do anything else
            image = transitionToImage;
            repaint();
        }
    }

    /**
     * Transition from the current image to the new image.
     *
     * @param destinationImage the destination image
     * @param destinationOrgImage the original full size destination image
     * @param forward true to advance in a forward transition, false for backward transition
     */
    public void transitionTo(BufferedImage destinationImage, BufferedImage destinationOrgImage, boolean forward) {
        zoomHandler.reset(false);
        if (transitionEffects.isEmpty()) {
            setImage(destinationImage,destinationOrgImage);
        } else {
            if (inTransition) {
                // if already in transition, go to the final image if it is the same, or transition to the new one
                transitionTimer.stop();
                inTransition = false;
                if (destinationImage == this.transitionToImage) {
                    setImage(destinationImage,destinationOrgImage);
                } else {
                    transitionTo(destinationImage,destinationOrgImage,forward);
                }
            } else {
                this.selectedTransition = transitionEffects.get((int) (Math.random() * transitionEffects.size()));
                this.transitionToImage = destinationImage;
                this.transitionForward = forward;
                this.inTransition = true;
                // create the zoom-in image in the background immediately. It is not too expensive if we don't need it,
                // but is quite expensive if it is created lazily, when the user requests it
                zoomHandler.createZoomImage(destinationOrgImage);
                transitionStartTime = System.currentTimeMillis();
                transitionTimer.start();
            }
        }
    }

    /**
     * Repaint the canvas black.
     */
    public void clean() {
        zoomHandler.reset(false);
        drawEverything(getGraphics(),false);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.Component#repaint()
     */
    @Override
    public void repaint() {
        if (applicationToolbar.isVisible() && toolbarAutoHide) {
            repaint(0,applicationToolbar.getHeight(),getWidth(),getHeight() - applicationToolbar.getHeight());
        } else {
            super.repaint();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics graphics = g.create();
        drawEverything(graphics,true);
        graphics.dispose();
        if (applicationToolbar.isShowing() && toolbarAutoHide) {
            applicationToolbar.repaint();
        }
    }

    /**
     * Draw the black background and the image. The image can be drawn directly or in transition. If in zoom mode the
     * zoomed in image is drawn.
     *
     * @param g the graphics to draw on
     * @param drawImage true to draw the image or false to only draw the background
     */
    private void drawEverything(Graphics g, boolean drawImage) {
        g.setColor(backgroundColor);
        if (!drawImage) {
            g.setClip(null);
        }
        if (drawImage && image != null) {
            if (!zoomHandler.paintZoom(g)) {
                if (inTransition) {
                    ((Graphics2D) g).setRenderingHints(ImageUtil.NO_HINTS);
                    selectedTransition.draw((Graphics2D) g,image,transitionToImage,transitionParameter,getWidth(),
                            getHeight(),transitionForward);
                } else {
                    ((Graphics2D) g).setRenderingHints(ImageUtil.HINTS);
                    g.drawImage(image,0,0,null);
                }
            }
        } else {
            g.fillRect(0,0,screenDimension.width,screenDimension.height);
        }
    }

    /**
     * Reset any zooming and drawings.
     */
    public void resetZoomAndDrawing() {
        zoomHandler.reset(true);
    }

    /**
     * Update the screen dimension according to the current graphics device this canvas is displayed on.
     */
    public void updateScreenDimension() {
        Rectangle r = getGraphicsConfiguration().getBounds();
        screenDimension = new Dimension(r.width,r.height);
    }
}
