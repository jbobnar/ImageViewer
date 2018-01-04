/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;

import com.jakabobnar.imageviewer.Transition;
import com.jakabobnar.imageviewer.util.Utilities;

/**
 * TransitionPreview is a component which shows a single transition between two images in an infinite loop.
 *
 * @author Jaka Bobnar
 *
 */
public class TransitionPreview extends JComponent {

    private static final long serialVersionUID = 8546436982742498380L;
    private static final Map<RenderingHints.Key, Object> HINTS = new HashMap<>();

    static {
        HINTS.put(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        HINTS.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        HINTS.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        HINTS.put(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static final BufferedImage[] image = new BufferedImage[2];

    static {
        new Thread(() -> {
            try (InputStream stream = TransitionPreview.class.getClassLoader().getResourceAsStream("image_1.jpg");
                    InputStream stream2 = TransitionPreview.class.getClassLoader().getResourceAsStream("image_2.jpg")) {
                image[0] = ImageIO.read(stream);
                image[1] = ImageIO.read(stream2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Transition transition;
    private Timer timer;
    private final float transitionSpeed = 1500;
    private float transitionParameter = 0f;
    private long transitionStartTime;
    private boolean inTransition = false;
    private int c = 0;

    private BufferedImage im1, im2;
    private static final Dimension DIM = new Dimension(250,167);

    /**
     * Constructs a new transition preview component.
     */
    public TransitionPreview() {
        Utilities.setFixedSize(this,DIM.width,DIM.height);
        im1 = image[(++c) % 2];
        im2 = image[(++c) % 2];
        timer = new Timer(5,e -> {
            long time = System.currentTimeMillis();
            long duration = time - transitionStartTime;
            if (inTransition) {
                if (duration < transitionSpeed) {
                    transitionParameter = 1f - duration / transitionSpeed;
                } else {
                    inTransition = false;
                    transitionStartTime = time;
                    transitionParameter = 0f;
                    im1 = image[c % 2];
                    im2 = image[(++c) % 2];
                }
                repaint();
            } else {
                if (duration > transitionSpeed / 2) {
                    inTransition = true;
                    transitionStartTime = time;
                }
            }
        });
        timer.start();
        inTransition = true;
        addHierarchyListener(e -> {
            if (isShowing()) {
                timer.start();
            } else {
                timer.stop();
            }
        });
    }

    /**
     * Set the transition that will be previewed. This methods has to be called in the UI thread.
     *
     * @param t new transition
     */
    public void setTransition(Transition t) {
        transition = t;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g) {
        ((Graphics2D) g).setRenderingHints(HINTS);
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        // paint entire screen black to avoid artifacts when increasing the size of the frame
        g.fillRect(0,0,DIM.width,DIM.height);
        if (im1 != null && im2 != null) {
            if (transition != null && inTransition) {
                transition.draw((Graphics2D) g,im1,im2,transitionParameter,getWidth(),getHeight(),true);
            } else {
                g.drawImage(im1,0,0,null);
            }
        }
    }
}
