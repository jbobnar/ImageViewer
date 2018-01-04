/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.transition;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jakabobnar.imageviewer.Transition;

/**
 * DoorsTransition slides the old image half left and half right, while the new image fades in.
 *
 * @author Jaka Bobnar
 *
 */
public class DoorsTransition implements Transition {

    static final String NAME = "Sliding Doors";

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
            // g.setComposite(AlphaComposite.SrcOver.derive(transition));
            Graphics g1 = g.create((int) (-(1 - transition) * width),0,width / 2,height);
            Graphics g2 = g.create(width / 2 + (int) ((1 - transition) * width),0,width / 2,height);
            g1.drawImage(first,0,0,null);
            g2.drawImage(first,-width / 2,0,null);
            g1.dispose();
            g2.dispose();
            g.setComposite(AlphaComposite.SrcOver.derive(1f - transition));
            g.drawImage(second,0,0,null);
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
