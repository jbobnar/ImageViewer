/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.image;

import java.util.Collections;
import java.util.LinkedList;

/**
 * LinkBuffer is a semi synchronized version of the linked list, where the elements are always ordered. It also provides
 * a few additional usability features, such as returning null instead of throwing exception if the element does not
 * exist.
 *
 * @author Jaka Bobnar
 *
 */
public class LinkBuffer extends LinkedList<ImageFile> {

    private static final long serialVersionUID = -5336929986444673164L;

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#add(java.lang.Object)
     */
    @Override
    public synchronized boolean add(ImageFile e) {
        if (size() > 0) {
            if (getLast().id < e.id) {
                addLast(e);
                this.notifyAll();
                return true;
            } else if (getFirst().id > e.id) {
                addFirst(e);
                this.notifyAll();
                return true;
            }
        }
        boolean b = super.add(e);
        Collections.sort(this);
        this.notifyAll();
        return b;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#removeFirst()
     */
    @Override
    public synchronized ImageFile removeFirst() {
        return size() > 0 ? super.removeFirst() : null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#removeLast()
     */
    @Override
    public synchronized ImageFile removeLast() {
        return size() > 0 ? super.removeLast() : null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractCollection#isEmpty()
     */
    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#size()
     */
    @Override
    public synchronized int size() {
        return super.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#getLast()
     */
    @Override
    public synchronized ImageFile getLast() {
        return size() > 0 ? super.getLast() : null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#getFirst()
     */
    @Override
    public synchronized ImageFile getFirst() {
        return size() > 0 ? super.getFirst() : null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#clear()
     */
    @Override
    public synchronized void clear() {
        super.clear();
    }
}
