/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.transition;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jakabobnar.imageviewer.Transition;

/**
 * SlideFadeTransition slides the old image to the left and fades it out, while the new image fades in and slides in
 * from the right.
 *
 * @author Jaka Bobnar
 *
 */
public class SlideFadeTransition implements Transition {

    static final String NAME = "Slide & Fade";

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
            g.setComposite(AlphaComposite.SrcAtop.derive(transition));
            g.drawImage(first,(int) ((1 - transition) * width),0,null);
            g.setComposite(AlphaComposite.SrcOver.derive(1f - transition));
            g.drawImage(second,(int) (+transition * width),0,null);
        } else {
            g.setComposite(AlphaComposite.SrcAtop.derive(transition));
            g.drawImage(first,(int) (-(1 - transition) * width),0,null);
            g.setComposite(AlphaComposite.SrcOver.derive(1f - transition));
            g.drawImage(second,(int) (-transition * width),0,null);
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
