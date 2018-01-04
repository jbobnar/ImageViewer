/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.components;

import java.awt.Color;

import javax.swing.JTextArea;

/**
 * HelpArea is a transparent extension of wrappable JTextArea, with yellowish foreground.
 *
 * @author Jaka Bobnar
 *
 */
public class HelpArea extends JTextArea {
    private static final long serialVersionUID = -3346893114346740231L;

    /**
     * Create a new Help Area component.
     *
     * @param text the text to show in the area
     */
    public HelpArea(String text) {
        super(text);
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setFocusable(false);
        setForeground(Color.YELLOW.darker());
        setOpaque(false);
    }
}
