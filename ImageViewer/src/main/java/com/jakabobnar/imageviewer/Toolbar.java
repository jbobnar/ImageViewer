/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer;

import static com.jakabobnar.imageviewer.util.Utilities.gbc;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;

import com.jakabobnar.imageviewer.util.AbstractEventAdapter;
import com.jakabobnar.imageviewer.util.Utilities;

/**
 * Toolbar is a toolbar styled component, that shows the main menu and next and previous buttons. The currently
 * displayed file name and its index is also displayed.
 *
 * @author Jaka Bobnar
 */
public class Toolbar extends JPanel {

    private static final long serialVersionUID = 1L;
    private final JPopupMenu popup;
    private final JMenu recentFilesMenu;
    private boolean popupShowing = false;
    private JLabel fileInfo;
    private JLabel indexInfo;
    private transient Paint paint;
    private final List<ToolbarListener> toolbarListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new Toolbar.
     */
    public Toolbar() {
        super(new GridBagLayout());
        JButton menu = new JButton("Menu");
        popup = new JPopupMenu();
        popup.addPopupMenuListener(new AbstractEventAdapter() {

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                popupShowing = false;
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                popupShowing = false;
            }
        });
        JMenuItem openFile = new JMenuItem("Open File");
        openFile.addActionListener(a -> toolbarListeners.forEach(c -> c.openFile()));
        recentFilesMenu = new JMenu("Recent Files");
        recentFilesMenu.setEnabled(false);
        JMenuItem preferences = new JMenuItem("Preferences");
        preferences.addActionListener(a -> toolbarListeners.forEach(c -> c.openSettings()));
        JMenuItem help = new JMenuItem("Help");
        help.addActionListener(a -> toolbarListeners.forEach(c -> c.help()));
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(a -> toolbarListeners.forEach(c -> c.about()));
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(a -> System.exit(0));
        Utilities.addToPopupMenu(popup,openFile,recentFilesMenu,null,preferences,help,null,about,null,exit);
        menu.addActionListener(e -> {
            popupShowing = !popupShowing;
            if (popupShowing) {
                popup.show(menu,0,menu.getHeight());
            } else {
                popup.setVisible(false);
            }
        });
        JButton previous = new JButton("Previous");
        previous.setFocusable(false);
        previous.addActionListener(e -> toolbarListeners.forEach(c -> c.advanceImage(false)));
        JButton next = new JButton("Next");
        next.setFocusable(false);
        next.addActionListener(e -> toolbarListeners.forEach(c -> c.advanceImage(true)));
        fileInfo = new JLabel();
        fileInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        indexInfo = new JLabel();
        indexInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        int x = 0;
        int offset = 5;
        add(menu,gbc(x++,0,1,2,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,offset,5,offset,5));
        add(previous,gbc(x++,0,1,2,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,offset,2,offset,5));
        add(next,gbc(x++,0,1,2,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,offset,2,offset,5));
        add(fileInfo,gbc(x,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,2,2,0,2));
        add(indexInfo,gbc(x,1,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,0,2,2,2));
    }

    /**
     * Set the new image information to be displayed in the toolbar.
     *
     * @param fileName the name of the file
     * @param fileIndex the index of the file
     * @param totalImages number of all files
     */
    public void setImageInfo(String fileName, int fileIndex, int totalImages) {
        fileInfo.setText(fileName);
        indexInfo.setText(String.format("Image %d/%d",fileIndex + 1,totalImages));
    }

    /**
     * Sets the list of recent files which is displayed in the recent files sub menu of the main popup.
     *
     * @param files the files
     */
    public void setRecentFiles(File[] files) {
        recentFilesMenu.removeAll();
        Arrays.stream(files).map(f -> new JMenuItem(f.getAbsolutePath()))
                .peek(m -> m.addActionListener(e -> fireRecentFileSelected(m.getText()))).forEach(recentFilesMenu::add);
        recentFilesMenu.setEnabled(files.length > 0);
    }

    /**
     * Notify all registered listeners that a new recent file was selected.
     *
     * @param path the path to the selected file
     */
    private void fireRecentFileSelected(String path) {
        File file = new File(path);
        toolbarListeners.forEach(c -> c.recentFileSelected(file));
    }

    /**
     * Add a listener which is notified when an action is selected in this toolbar.
     *
     * @param listener the listener to notify on action
     */
    public void addToolbarListener(ToolbarListener listener) {
        toolbarListeners.add(listener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (paint == null) {
            paint = new GradientPaint(0,0,Color.BLACK,0,getHeight(),new Color(50,50,50));
        }
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setPaint(paint);
        g2d.fillRect(0,0,getWidth(),getHeight());
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag) {
            popup.setVisible(false);
            popupShowing = false;
        }
    }
}
