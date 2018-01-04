/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Transition draws the steps of transition from old to new image.
 *
 * @author Jaka Bobnar
 *
 */
public interface Transition {

    /**
     * Draws the transition from the first to the second image. Only one transition step is draw, which is defined by
     * the given transition parameter, which has a value between 0 (start of transition) and 1 (end of transition). It
     * is not necessary that this method will be ever called with parameters 0 or 1, nor is it necessary that the
     * transition parameter changes in equidistant steps. Each call to this method should be treated independently.
     *
     * @param g the graphics to draw the images on
     * @param first the image to transition from
     * @param second the image to transition to
     * @param transition defines how far into the transition the drawing is (0 is start of transition, 1 is end of
     *            transition)
     * @param width the width of the canvas to draw on
     * @param height the height of the canvas to draw on
     * @param forward true if the transition goes to the next image, or false if the transition goes to the previous
     *            image (some transition might implement reverse transition for backward movements)
     */
    void draw(Graphics2D g, BufferedImage first, BufferedImage second, float transition, int width, int height,
            boolean forward);

    /**
     * Return a unique name for this transitions.
     *
     * @return the name of the transition
     */
    String getName();

    /**
     * Checks if the transition is equals to this transition. Transitions are equal if they have the same name.
     *
     * @param t the transition to check if equals
     * @return true if equals or false if not
     */
    default boolean equals(Transition t) {
        return t.getName().equalsIgnoreCase(getName());
    }
}
