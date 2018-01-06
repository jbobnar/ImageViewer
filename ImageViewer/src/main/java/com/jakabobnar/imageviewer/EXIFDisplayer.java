/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer;

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.jakabobnar.imageviewer.components.OverlayDialog;
import com.jakabobnar.imageviewer.image.EXIFData;

/**
 * EXIFDisplayer is an overlay dialog, which displays the image exif information.
 *
 * @author Jaka Bobnar
 *
 */
public class EXIFDisplayer extends OverlayDialog {

    private static final long serialVersionUID = -1676562847366651873L;

    private static final int C_WIDTH = 380;
    private static final int C_HEIGHT = 78;

    private transient Style topStyle;
    private transient Style secondStyle;
    private JTextPane textPane;
    private EXIFData currentData;

    /**
     * Construct a new EXIFDisplayer with the given frame as its parent.
     *
     * @param frame the parent frame (cannot be null)
     */
    public EXIFDisplayer(JFrame frame) {
        super(frame,C_WIDTH,C_HEIGHT);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jakabobnar.imageviewer.OverlayDialog#createMainComponent()
     */
    @Override
    protected JTextPane createMainComponent() {
        textPane = new JTextPane();
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setFocusable(false);
        StyledDocument doc = new DefaultStyledDocument();
        textPane.setStyledDocument(doc);
        topStyle = doc.addStyle("topStyle",null);
        secondStyle = doc.addStyle("secondStyle",null);
        StyleConstants.setFontSize(topStyle,19);
        StyleConstants.setFontFamily(topStyle,"Tahoma");
        StyleConstants.setBold(topStyle,true);
        StyleConstants.setSpaceBelow(topStyle,5);
        StyleConstants.setFontSize(secondStyle,12);
        StyleConstants.setFontFamily(secondStyle,"Segoe");
        return textPane;
    }

    /**
     * Set the EXIF data to be displayed by this dialog.
     *
     * @param data the data to display
     */
    public void setData(EXIFData data) {
        if (currentData == data) {
            return;
        }
        this.currentData = data;
        if (data == null) {
            SwingUtilities.invokeLater(() -> textPane.setText(""));
        } else {
            final StringBuilder sb = new StringBuilder(1000);
            String obj = data.get(EXIFData.TAG_EXPOSURE_TIME);
            if (obj != null) {
                obj = obj.replace("sec","s");
                sb.append(obj).append(',').append(' ');
            }
            obj = data.get(EXIFData.TAG_F_NUMBER);
            if (obj != null) {
                obj = obj.replace(',','.');
                sb.append(obj).append(',').append(' ');
            }
            obj = data.get(EXIFData.TAG_ISO_SPEED);
            if (obj != null) {
                sb.append("ISO ").append(obj).append(',').append(' ');
            }
            obj = data.get(EXIFData.TAG_EXPOSURE_COMPENSATION);
            if (obj != null) {
                sb.append(obj).append('\n');
            }
            boolean hasShootingInfo = true;
            if (sb.length() == 0) {
                sb.append("(No Shooting Info)\n");
                hasShootingInfo = false;
            }

            int b = sb.length();
            if (hasShootingInfo) {
                obj = data.get(EXIFData.TAG_METERING_MODE);
                if (obj != null) {
                    sb.append("Meter: ").append(obj).append(',').append(' ');
                }
                obj = data.get(EXIFData.TAG_FLASH);
                if (obj != null) {
                    obj = obj.contains("did not") ? "No Flash" : obj;
                    sb.append(obj).append(',').append(' ');
                }
                obj = data.get(EXIFData.TAG_WHITE_BALANCE);
                if (obj != null) {
                    int idx = obj.toLowerCase().indexOf("white balance");
                    obj = idx < 0 ? obj : idx == 0 ? obj.substring(13).trim() : obj.substring(0,idx).trim();
                    obj += " WB";
                    sb.append(obj).append('\n');
                }
                obj = data.get(EXIFData.TAG_FOCAL_LENGTH);
                if (obj != null) {
                    int idx = obj.indexOf(',');
                    if (idx < 0) {
                        idx = obj.indexOf('.');
                    }
                    if (idx > 0) {
                        obj = obj.substring(0,idx) + "mm";
                    }
                    sb.append("FL: ").append(obj).append(',').append(' ');
                }
                obj = data.get(EXIFData.TAG_DATE_TIME);
                if (obj != null) {
                    sb.append(obj).append(',').append(' ');
                }
            }
            obj = data.get(EXIFData.TAG_PROFILE_DESCRIPTION);
            if (obj != null) {
                if (!hasShootingInfo) {
                    sb.append("Color Profile: ");
                }
                sb.append(obj).append('\n');
            } else {
                sb.append('\n');
            }

            obj = data.get(EXIFData.TAG_IMAGE_WIDTH);
            if (obj != null) {
                Object height = data.get(EXIFData.TAG_IMAGE_HEIGHT);
                if (height != null) {
                    String w = String.valueOf(obj);
                    if (w.indexOf(' ') > 0) {
                        w = w.substring(0,w.indexOf(' '));
                    }
                    String h = String.valueOf(height);
                    if (h.indexOf(' ') > 0) {
                        h = h.substring(0,h.indexOf(' '));
                    }
                    long ww = Long.parseLong(w);
                    long hh = Long.parseLong(h);
                    double size = (Math.round(ww * hh / 100000.)) / 10.;
                    sb.append(size).append("MP (").append(w).append('x').append(h).append(')').append(' ');
                }
            }

            obj = data.get(EXIFData.TAG_MODEL);
            if (obj != null) {
                sb.append(obj).append(',').append(' ');
            }
            obj = data.get(EXIFData.TAG_LENS_MODEL);
            if (obj == null) {
                obj = data.get(EXIFData.TAG_LENS_TYPE);
                if (obj != null) {
                    obj += " " + data.get(EXIFData.TAG_LENS);
                }
            }
            if (obj != null) {
                sb.append(obj);
            }

            int length = sb.length() - 1;
            char c = sb.charAt(length);
            while (c == ',' || c == ' ') {
                c = sb.charAt(--length);
            }
            final String secondText = sb.substring(b,length + 1);

            SwingUtilities.invokeLater(() -> {
                textPane.setText("");
                StyledDocument document = textPane.getStyledDocument();
                try {
                    document.insertString(0,sb.substring(0,b),topStyle);
                    document.insertString(b,secondText,secondStyle);
                } catch (BadLocationException e) {
                    // ignore
                }
            });
        }
    }
}
