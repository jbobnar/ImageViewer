/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.transition;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import com.jakabobnar.imageviewer.Transition;
import com.jakabobnar.imageviewer.image.ImageUtil;

/**
 * Zoom transition is a transition where the old image is zoomed out and faded out while the new image fades in.
 *
 * @author Jaka Bobnar
 */
public class ZoomTransition implements Transition {

    static final String NAME = "Zoom Out";

    /*
     * (non-Javadoc)
     *
     * @see com.jakabobnar.imageviewer.Transition#draw(java.awt.Graphics2D, java.awt.image.BufferedImage,
     * java.awt.image.BufferedImage, float, int, int, boolean)
     */
    @Override
    public void draw(Graphics2D g, BufferedImage first, BufferedImage second, float transition, int width, int height,
            boolean forward) {

        float theTransition = transition;
        if (forward) {
            int w = (int) (theTransition * first.getWidth());
            int h = (int) (theTransition * first.getHeight());
            if (w != 0 && h != 0) {
                g.setComposite(AlphaComposite.SrcAtop.derive(theTransition));
                Image ff = ImageUtil.getScaledImage(first,Color.BLACK,w,h,true);
                g.drawImage(ff,(width - w) / 2,(height - h) / 2,null);
            }
            g.setComposite(AlphaComposite.SrcOver.derive(1f - theTransition));
            g.drawImage(second,0,0,null);
        } else {
            g.setComposite(AlphaComposite.SrcAtop.derive(theTransition));
            g.drawImage(first,0,0,null);
            theTransition = 1f - theTransition;
            int w = (int) (theTransition * second.getWidth());
            int h = (int) (theTransition * second.getHeight());
            g.setComposite(AlphaComposite.SrcOver.derive(theTransition));
            Image sc = ImageUtil.getScaledImage(second,Color.BLACK,w,h,true);
            g.drawImage(sc,(width - w) / 2,(height - h) / 2,null);

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
