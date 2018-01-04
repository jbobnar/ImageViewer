/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.transition;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.bric.image.transition.ReversedTransition;
import com.jakabobnar.imageviewer.Transition;

/**
 * TransitionWrapper is a wrapper for a bric transition to be able to include them in the image viewer.
 *
 * @author Jaka Bobnar
 *
 */
public class TransitionWrapper implements Transition {

    private final com.bric.image.transition.Transition[] forwardTransition;
    private final ReversedTransition[] backwardTransition;
    private final String name;
    private final boolean reverse;
    private int random = 0;
    private float previousTransition = 0f;

    /**
     * Constructs a new wrapper around the bric transition or transitions.
     *
     * @param name the name of the transition
     * @param transition the transitions to wrap
     */
    public TransitionWrapper(String name, com.bric.image.transition.Transition... transition) {
        this(name,false,transition);
    }

    /**
     * Constructs a new wrapper around the bric transition.
     *
     * @param name the name of the transition
     * @param reverse indicates if this transition should be applied in reverse
     * @param transition the transition to wrap
     */
    public TransitionWrapper(String name, boolean reverse, com.bric.image.transition.Transition... transition) {
        this.name = name;
        this.reverse = reverse;
        this.forwardTransition = transition;
        this.backwardTransition = new ReversedTransition[forwardTransition.length];
        for (int i = 0; i < forwardTransition.length; i++) {
            this.backwardTransition[i] = new ReversedTransition(forwardTransition[i]);
        }
    }

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
            if (previousTransition < transition) {
                random = (int) (Math.random() * forwardTransition.length);
            }
            previousTransition = transition;
            forwardTransition[random].paint(g,first,second,reverse ? transition : 1 - transition);
        } else {
            backwardTransition[random].paint(g,first,second,reverse ? transition : 1 - transition);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jakabobnar.imageviewer.Transition#getName()
     */
    @Override
    public String getName() {
        return name;
    }
}
