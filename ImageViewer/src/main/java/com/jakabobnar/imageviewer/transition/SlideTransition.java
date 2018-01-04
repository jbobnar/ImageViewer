/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.transition;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jakabobnar.imageviewer.Transition;

/**
 * MoveTransition slides the old image to the left, while the new image slides in from the right.
 *
 * @author Jaka Bobnar
 *
 */
public class SlideTransition implements Transition {

    static final String NAME = "Slide";

    /*
     * (non-Javadoc)
     *
     * @see com.jakabobnar.imageviewer.Transition#draw(java.awt.Graphics2D, java.awt.image.BufferedImage,
     * java.awt.image.BufferedImage, float, int, int, boolean)
     */
    @Override
    public void draw(Graphics2D g, BufferedImage first, BufferedImage second, float transition, int width, int height,
            boolean forward) {
        if (forward) {
            if (transition > 0.5) {
                g.drawImage(first,(int) (-2 * (1 - transition) * width),0,null);
            } else {
                g.drawImage(second,(int) (2 * transition * width),0,null);
            }
        } else {
            if (transition > 0.5) {
                g.drawImage(first,(int) (2 * (1 - transition) * width),0,null);
            } else {
                g.drawImage(second,(int) (-2 * transition * width),0,null);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jakabobnar.imageviewer.Transition#getName()
     */
    @Override
    public String getName() {
        return NAME;
    }
}
