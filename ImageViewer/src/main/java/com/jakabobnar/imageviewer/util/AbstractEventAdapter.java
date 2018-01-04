/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.util;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;

import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * AbstractEventAdapter is an adapter for multiple swing listeners.
 *
 * @author Jaka Bobnar
 *
 */
public abstract class AbstractEventAdapter extends MouseAdapter
        implements KeyListener, ComponentListener, HierarchyListener, PopupMenuListener {

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        // Do nothing
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // Do nothing
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        // Do nothing
    }

    @Override
    public void componentResized(ComponentEvent e) {
        // Do nothing
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        // Do nothing
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // Do nothing
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        // Do nothing
    }

    @Override
    public void componentShown(ComponentEvent e) {
        // Do nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Do nothing
    }
}
