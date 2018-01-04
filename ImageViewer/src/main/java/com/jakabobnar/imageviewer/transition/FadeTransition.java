/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.transition;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jakabobnar.imageviewer.Transition;

/**
 * FadeTransition fades out the old image and fades in the new one.
 *
 * @author Jaka Bobnar
 *
 */
public class FadeTransition implements Transition {

    static final String NAME = "Fade";

    /*
     * (non-Javadoc)
     *
     * @see com.jakabobnar.imageviewer.Transition#draw(java.awt.Graphics2D, java.awt.image.BufferedImage,
     * java.awt.image.BufferedImage, float, int, int, boolean)
     */
    @Override
    public void draw(Graphics2D g, BufferedImage first, BufferedImage second, float transition, int width, int height,
            boolean forward) {
        // g.setComposite(AlphaComposite.SrcAtop.derive(transition));
        g.drawImage(first,null,null);
        g.setComposite(AlphaComposite.SrcOver.derive(1f - transition));
        g.drawImage(second,null,null);
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
