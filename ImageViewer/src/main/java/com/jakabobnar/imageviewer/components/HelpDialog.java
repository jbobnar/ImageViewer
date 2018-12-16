/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.components;

import static com.jakabobnar.imageviewer.util.Utilities.gbc;
import static com.jakabobnar.imageviewer.util.Utilities.registerKeyStroke;
import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTH;
import static java.awt.GridBagConstraints.NORTHWEST;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * HelpDialog shows all control options that exist in the application.
 *
 * @author Jaka Bobnar
 */
public class HelpDialog extends JDialog {

    private static final long serialVersionUID = 8854460891703087133L;

    /**
     * Constructs a new help dialog.
     *
     * @param parent the owner
     */
    public HelpDialog(JFrame parent) {
        super(parent);
        setTitle("Help");
        setType(Type.NORMAL);
        initialize();
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(550,475);
        setLocationRelativeTo(parent);
        setResizable(false);
        registerKeyStroke((JComponent) getContentPane(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),e -> dispose());
        setVisible(true);
    }

    private static GridBagConstraints labelGBC(int y) {
        return gbc(0,y,1,1,0,0,NORTHWEST,NONE,0,0,2,15);
    }

    private static GridBagConstraints helpGBC(int y) {
        return gbc(1,y,1,1,1,0,NORTHWEST,BOTH,0,0,2,0);
    }

    private void initialize() {
        JPanel panel = new JPanel(new GridBagLayout());
        int y = 0;
        panel.add(new JLabel("CTRL + H"),labelGBC(y));
        panel.add(new HelpArea("Show this help dialog"),helpGBC(y));
        panel.add(new JLabel("I"),labelGBC(++y));
        panel.add(new HelpArea("Show / hide current image meta-information (EXIF)"),helpGBC(y));
        panel.add(new JLabel("H"),labelGBC(++y));
        panel.add(new HelpArea("Show / hide current image histogram"),helpGBC(y));
        panel.add(new JLabel("P"),labelGBC(++y));
        panel.add(new HelpArea("Show program preferences dialog"),helpGBC(y));
        panel.add(new JLabel("O"),labelGBC(++y));
        panel.add(new HelpArea("Show open file/folder dialog"),helpGBC(y));
        panel.add(new JLabel("Escape"),labelGBC(++y));
        panel.add(new HelpArea("Exit application or close any open dialog"),helpGBC(y));
        panel.add(new JLabel("F11"),labelGBC(++y));
        panel.add(new HelpArea("Toggle full screen/normal mode"),helpGBC(y));
        panel.add(new JLabel("F12"),labelGBC(++y));
        panel.add(new HelpArea("Toggle the toolbar visibility"),helpGBC(y));
        panel.add(new JLabel("Home"),labelGBC(++y));
        panel.add(new HelpArea("Rewind to the first image in the selected folder"),helpGBC(y));
        panel.add(new JLabel("End"),labelGBC(++y));
        panel.add(new HelpArea("Forward to the last image in the selected folder"),helpGBC(y));
        panel.add(new JLabel("Page Up"),labelGBC(++y));
        panel.add(new HelpArea("Jump forward several images"),helpGBC(y));
        panel.add(new JLabel("Page Down"),labelGBC(++y));
        panel.add(new HelpArea("Jump backward several images"),helpGBC(y));
        panel.add(new JLabel("Left Mouse Button"),labelGBC(++y));
        panel.add(new HelpArea("Show original image / Show highlight cursor / Draw on image / Show next image"),
                helpGBC(y));
        panel.add(new JLabel("ALT + Left Mouse Button"),labelGBC(++y));
        panel.add(new HelpArea("Show highlight cursor"),helpGBC(y));
        panel.add(new JLabel("CTRL + Left Mouse Button"),labelGBC(++y));
        panel.add(new HelpArea("Draw on the image"),helpGBC(y));
        panel.add(new JLabel("Left Mouse Button Doubleclick"),labelGBC(++y));
        panel.add(new HelpArea("Remove all drawings from the image"),helpGBC(y));
        panel.add(new JLabel("Right Mouse Button"),labelGBC(++y));
        panel.add(new HelpArea("Zoom image / Show previous image"),helpGBC(y));
        panel.add(new JLabel("CTRL + Right Mouse Button"),labelGBC(++y));
        panel.add(new HelpArea("Show original size image"),helpGBC(y));
        panel.add(new JLabel("4th Mouse Button (Side)"),labelGBC(++y));
        panel.add(new HelpArea("Show next image"),helpGBC(y));
        panel.add(new JLabel("5th Mouse Button (Side)"),labelGBC(++y));
        panel.add(new HelpArea("Show previous image"),helpGBC(y));
        panel.add(new JLabel("Mouse Wheel Scroll"),labelGBC(++y));
        panel.add(new HelpArea("Scroll images back and forth"),helpGBC(y));
        panel.add(new JLabel("Middle Mouse Button"),labelGBC(++y));
        panel.add(new HelpArea("Disable / enable mouse wheel scrolling"),helpGBC(y));
        panel.add(new JLabel("ALT + Right Button + Mouse Wheel"),labelGBC(++y));
        panel.add(new HelpArea("Temporary zoom in and zoom out"),helpGBC(y));
        panel.add(new JLabel("CTRL + S"),labelGBC(++y));
        panel.add(new HelpArea("Start / stop automatic slide show"),helpGBC(y));

        panel.add(new JPanel(),gbc(0,++y,2,1,0,1,NORTHWEST,NONE,0,0,0,0));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.add(panel,gbc(0,0,1,1,1,1,NORTH,BOTH,10,10,5,10));
        contentPanel.add(closeButton,gbc(0,1,1,1,1,0,EAST,NONE,0,10,12,12));
        setContentPane(contentPanel);
    }
}
