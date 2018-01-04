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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.jakabobnar.imageviewer.ViewerFrame;
import com.jakabobnar.imageviewer.util.Utilities;

/**
 * The about dialog for the application.
 *
 * @author Jaka Bobnar
 */
public class AboutDialog extends JDialog {

    private static final long serialVersionUID = 8854460891703087133L;

    private static class HyperLinkLabel extends JLabel {

        private static final long serialVersionUID = -4138736435292554589L;

        HyperLinkLabel(String hyperlink) {
            setText("<HTML><U>" + hyperlink + "</U></HTML>");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(Color.WHITE);
            addMouseListener(Utilities.onMouseClick(e -> {
                try {
                    Desktop.getDesktop().browse(new URI("http://" + getText()));
                } catch (URISyntaxException | IOException ex) {
                    // ignore
                }
            }));
        }
    }

    /**
     * Constructs a new about dialog.
     *
     * @param parent the owner
     */
    public AboutDialog(JFrame parent) {
        super(parent);
        setTitle("About Image Viewer");
        setType(Type.NORMAL);
        initialize();
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(320,300);
        setLocationRelativeTo(parent);
        setResizable(false);
        registerKeyStroke((JComponent) getContentPane(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),e -> dispose());
        setVisible(true);
    }

    private void initialize() {
        String version = "UNKNOWN";
        try {
            String mainName = ViewerFrame.class.getName();
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try (InputStream stream = resources.nextElement().openStream()) {
                    Manifest manifest = new Manifest(stream);
                    Attributes attr = manifest.getMainAttributes();
                    String main = attr.getValue("Main-Class");
                    if (mainName.equals(main)) {
                        version = attr.getValue("Implementation-Version");
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        } catch (IOException e) {
            // ignore
        }
        JPanel panel = new JPanel(new GridBagLayout());
        int y = 0;
        panel.add(new JLabel("Image Viewer: " + version),gbc(0,0,++y,1,1,0,NORTH,NONE,0,0,5,0));
        panel.add(new JLabel("Copyright \u00a9 Jaka Bobnar. All rights reserved."),
                gbc(0,++y,1,1,1,0,NORTH,NONE,0,0,2,0));
        panel.add(new HyperLinkLabel("www.jakabobnar.com"),gbc(0,++y,1,1,1,0,NORTH,NONE,0,0,5,0));
        JTextPane textArea = new JTextPane();
        textArea.setForeground(Color.WHITE);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setOpaque(false);
        StyledDocument doc = new DefaultStyledDocument();
        textArea.setStyledDocument(doc);
        Style style = doc.addStyle("topStyle",null);
        StyleConstants.setAlignment(style,StyleConstants.ALIGN_CENTER);
        StyleConstants.setForeground(style,Color.WHITE);
        Font font = new JLabel().getFont();
        StyleConstants.setFontFamily(style,font.getFamily());
        StyleConstants.setFontSize(style,font.getSize());
        String text = "This software is free for personnal, non-profit, and educational use. "
                + "If you wish to use it in commercial context, please contact the author.";
        textArea.setParagraphAttributes(style,true);
        try {
            doc.insertString(0,text,style);
        } catch (BadLocationException e1) {
            textArea.setText(text);
        }
        panel.add(textArea,gbc(0,++y,1,1,1,0,NORTH,BOTH,10,15,5,15));
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.add(panel,gbc(0,0,1,1,1,1,NORTH,BOTH,10,10,5,10));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        contentPanel.add(closeButton,gbc(0,1,1,1,1,0,EAST,NONE,0,5,5,5));
        setContentPane(contentPanel);
    }
}
