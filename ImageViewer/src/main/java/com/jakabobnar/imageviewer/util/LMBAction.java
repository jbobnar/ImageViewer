/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.util;

/**
 * LMBAction defines three possible actions that can be bound to the left mouse button. In zoom action, the left mouse
 * button is used for zooming the image, in paint action, the left button is used for drawing lines on the image and
 * in cursor mode a highlight cursor is displayed.
 *
 * @author Jaka Bobnar
 *
 */
public enum LMBAction {
    ZOOM, PAINT, CURSOR
}
