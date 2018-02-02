/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.jakabobnar.imageviewer.components.OverlayDialog;
import com.jakabobnar.imageviewer.image.Histogram;
import com.jakabobnar.imageviewer.util.Utilities;

/**
 * HistogramDisplayer is an overlay displayer, which displays 1 to 5 histograms one below the other or one over the
 * other, depending on the settings. It displays the individual RGB histograms, the combined RGB histogram, and
 * luminosity histogram. Each histogram is normalized according to its own values, therefore comparison of histograms is
 * not meaningful.
 *
 * @author Jaka Bobnar
 *
 */
public class HistogramDisplayer extends OverlayDialog {

    private static final long serialVersionUID = 5173426152251229557L;

    private static class HistogramCanvas extends JPanel {
        private static final Map<RenderingHints.Key, Object> HINTS = new HashMap<>();

        static {
            HINTS.put(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            HINTS.put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            HINTS.put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
        }

        private static final long serialVersionUID = -4067362289910770271L;
        // draw the values as histogram bars or draw them as an area chart
        private static final boolean HISTOGRAM = false;
        private static final int OFFSET = 3;
        private static final int BOTTOM_OFFSET = 16;
        private static final Color BORDER_COLOR = new Color(90,90,90);
        private static final Color GRID_COLOR = new Color(160,160,160);
        private static final Map<Integer, Color> COLORS = new HashMap<>(5);
        private String caption = "";

        static {
            int a = 100;
            COLORS.put(Integer.valueOf(0),new Color(255,0,0,a));
            COLORS.put(Integer.valueOf(1),new Color(0,255,0,a));
            COLORS.put(Integer.valueOf(2),new Color(0,0,255,a));
            // luminosity is grayish
            COLORS.put(Integer.valueOf(3),new Color(60,60,60,a));
            // RGB is orange
            COLORS.put(Integer.valueOf(4),new Color(255,100,0,a));
        }

        private int[] indices = new int[0];
        private int[][] rgbl;

        /**
         * Set the histogram data. Fiist 3 arrays are individual RGB channels, 4th array is luminosity, and 5th array is
         * combined RGB.
         *
         * @param rgbl the data
         */
        public void setHistogram(int[][] rgbl) {
            this.rgbl = rgbl;
            repaint();
        }

        /**
         * Set the indices from the data array that this panel will display. All selected histograms are overlayed.
         *
         * @see #setHistogram(int[][])
         * @param indices the list of indices (1-5) to display
         * @param caption the histogram caption
         */
        public void setIndices(int[] indices, String caption) {
            this.indices = indices;
            this.caption = caption;
            repaint();
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (rgbl == null || rgbl[0].length == 0) {
                return;
            }

            int w = getWidth() - OFFSET - OFFSET;
            int h = getHeight() - OFFSET - BOTTOM_OFFSET;
            ((Graphics2D) g).setRenderingHints(HINTS);
            g.setColor(BORDER_COLOR);
            g.drawRect(OFFSET - 1,OFFSET - 1,w + 2,h + 2);
            g.drawString(caption,OFFSET,getHeight() - 2);

            ((Graphics2D) g)
                    .setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL,0,new float[] { 3 },0f));
            g.setColor(GRID_COLOR);
            g.drawLine(OFFSET,OFFSET + h / 4,OFFSET + w,OFFSET + h / 4);
            g.drawLine(OFFSET,OFFSET + h / 2,OFFSET + w,OFFSET + h / 2);
            g.drawLine(OFFSET,OFFSET + 3 * h / 4,OFFSET + w,OFFSET + 3 * h / 4);
            g.drawLine(OFFSET + w / 4,OFFSET,OFFSET + w / 4,OFFSET + h);
            g.drawLine(OFFSET + w / 2,OFFSET,OFFSET + w / 2,OFFSET + h);
            g.drawLine(OFFSET + 3 * w / 4,OFFSET,OFFSET + 3 * w / 4,OFFSET + h);

            int length = 256;
            double step = w / 256.;
            double r = 256. / length;
            for (int j : indices) {
                int[] x, y;
                double max = 0;
                if (HISTOGRAM) {
                    x = new int[4 * length];
                    y = new int[4 * length];
                    for (int i = 0; i < 4 * length; i += 4) {
                        x[i] = (int) (i / 4. * step);
                        x[i + 1] = x[i];
                        x[i + 2] = (int) (i / 4. * step + step);
                        x[i + 3] = x[i + 2];
                        y[i] = 0;
                        y[i + 1] = rgbl[j][(int) (i / 4. * r)];
                        y[i + 2] = y[i + 1];
                        y[i + 3] = 0;
                        max = Math.max(y[i + 1],max);
                    }
                } else {
                    x = new int[2 * length + 2];
                    y = new int[2 * length + 2];
                    for (int i = 0; i < 2 * length; i += 2) {
                        x[i + 1] = (int) (i / 2. * step);
                        x[i + 2] = (int) (i / 2. * step + step);
                        y[i + 1] = rgbl[j][(int) (i / 2. * r)];
                        y[i + 2] = y[i + 1];
                        max = Math.max(y[i + 1],max);
                    }
                    x[0] = 0;
                    y[0] = 0;
                    x[2 * length + 1] = x[2 * length];
                    y[2 * length + 1] = 0;
                }
                if (max == 0) {
                    Arrays.fill(y,h + OFFSET);
                    for (int i = 0; i < x.length; i++) {
                        x[i] = x[i] + OFFSET;
                    }
                } else {
                    for (int i = 0; i < y.length; i++) {
                        y[i] = h - (int) ((y[i] / max) * h) + OFFSET;
                        x[i] += OFFSET;
                    }
                }
                g.setColor(COLORS.get(Integer.valueOf(j)));
                g.drawPolyline(x,y,y.length);
                y[0] += 1;
                y[y.length - 1] += 1;
                g.fillPolygon(x,y,y.length);
            }
        }
    }

    public static final int C_WIDTH = 266;

    private boolean showRGB = true;
    private boolean showLuminosity = true;
    private boolean showChannels = true;
    private boolean overlayCharts = false;
    private boolean overlayChannels = true;
    private HistogramCanvas[] displayers;
    private JPanel histoPanel;
    private int histSize = 150;

    /**
     * Constructs a new HistogramDisplayer for the given parent.
     *
     * @param frame the parent frame (cannot be null)
     */
    public HistogramDisplayer(JFrame frame) {
        super(frame,300,150);
        setUp();
    }

    /**
     * Set the flag whether the combined RGB histogram should be displayed or not.
     *
     * @param show true to show the combined RGB or false to hide it
     */
    public void setShowRGB(boolean show) {
        if (this.showRGB == show) return;
        this.showRGB = show;
        setUp();
    }

    /**
     * Set the flag whether the individual RGB channels histograms are displayed or not.
     *
     * @param show true to show the individual RGB channels or false to hide them
     */
    public void setShowChannels(boolean show) {
        if (this.showChannels == show) return;
        this.showChannels = show;
        setUp();
    }

    /**
     * Set the flag whether the luminosity histogram is displayer or not.
     *
     * @param show true to show display the luminosity histogram or false to hide it
     */
    public void setShowLuminosity(boolean show) {
        if (this.showLuminosity == show) return;
        this.showLuminosity = show;
        setUp();
    }

    /**
     * Set the flag whether all three RGB histograms are overlayed one above the other or displayed separately one below
     * the other. If the {@link #setShowChannels(boolean)} is set to false, this setting has no effect.
     *
     * @param overlayChannels true to overlay them or false otherwise
     */
    public void setOverlayChannels(boolean overlayChannels) {
        if (this.overlayChannels == overlayChannels) return;
        this.overlayChannels = overlayChannels;
        setUp();
    }

    /**
     * Set the flag whether all histograms are overlayed one above the other or displayed separately one below the
     * other.
     *
     * @param overlayCharts true to overlay histograms or false to display them separately
     */
    public void setOverlayCharts(boolean overlayCharts) {
        if (this.overlayCharts == overlayCharts) return;
        this.overlayCharts = overlayCharts;
        setUp();
    }

    private void setUpIndices() {
        if (overlayCharts) {
            List<Integer> indices = new ArrayList<>(5);
            List<String> caption = new ArrayList<>(3);
            if (showChannels) {
                indices.add(Integer.valueOf(0));
                indices.add(Integer.valueOf(1));
                indices.add(Integer.valueOf(2));
                caption.add("Channels");
            }
            if (showLuminosity) {
                indices.add(Integer.valueOf(3));
                caption.add("Luminosity");
            }
            if (showRGB) {
                indices.add(Integer.valueOf(4));
                caption.add("RGB");
            }
            StringBuilder sb = new StringBuilder(30);
            caption.forEach(s -> sb.append(s).append(',').append(' '));
            displayers[0].setIndices(indices.stream().mapToInt(Integer::intValue).toArray(),
                    sb.length() > 0 ? sb.substring(0,sb.length() - 2) : "");
        } else {
            if (showChannels) {
                if (overlayChannels) {
                    displayers[0].setIndices(new int[] { 0, 1, 2 },"Channels");
                } else {
                    displayers[0].setIndices(new int[] { 0 },"Red");
                    displayers[1].setIndices(new int[] { 1 },"Green");
                    displayers[2].setIndices(new int[] { 2 },"Blue");
                }
            }
            if (showLuminosity) {
                displayers[3].setIndices(new int[] { 3 },"Luminosity");
            }
            if (showRGB) {
                displayers[4].setIndices(new int[] { 4 },"RGB");
            }
        }
    }

    private int countCharts() {
        if (overlayCharts) {
            return 1;
        }
        int c = 0;
        if (showChannels) {
            if (overlayChannels) {
                c++;
            } else {
                c += 3;
            }
        }
        if (showLuminosity) {
            c++;
        }
        if (showRGB) {
            c++;
        }
        return c;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.jakabobnar.imageviewer.OverlayDialog#createMainComponent()
     */
    @Override
    protected JComponent createMainComponent() {
        displayers = new HistogramCanvas[5];
        JPanel mainPanel = new JPanel(new GridBagLayout());
        histoPanel = new JPanel();
        mainPanel.add(histoPanel,Utilities.gbc(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,3));
        histoPanel.setOpaque(false);
        mainPanel.setOpaque(false);
        for (int i = 0; i < 5; i++) {
            displayers[i] = new HistogramCanvas();
            displayers[i].setOpaque(false);
        }
        setUp();
        return mainPanel;
    }

    private void setUp() {
        histoPanel.removeAll();
        int count = countCharts();
        histoPanel.setLayout(new GridLayout(count,1,5,5));
        if (overlayCharts) {
            histoPanel.add(displayers[0]);
        } else {
            if (showChannels) {
                if (overlayChannels) {
                    histoPanel.add(displayers[0]);
                } else {
                    histoPanel.add(displayers[0]);
                    histoPanel.add(displayers[1]);
                    histoPanel.add(displayers[2]);
                }
            }
            if (showRGB) {
                histoPanel.add(displayers[4]);
            }
            if (showLuminosity) {
                histoPanel.add(displayers[3]);
            }
        }
        setSize(C_WIDTH,count * histSize + 6 + (count - 1) * 5);
        setUpIndices();
    }

    /**
     * Set the histogram data to display.
     *
     * @param histogramData the histogram data
     */
    public void setHistogram(Histogram histogramData) {
        int[][] rgbl = histogramData == null ? null : histogramData.getRgblRGB();
        SwingUtilities.invokeLater(() -> Arrays.asList(displayers).forEach(c -> c.setHistogram(rgbl)));
    }
}
