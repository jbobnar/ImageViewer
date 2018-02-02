/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.components;

import static com.jakabobnar.imageviewer.util.Utilities.gbc;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.jakabobnar.imageviewer.ViewerFrame;

/**
 * OverlayDialog is the base dialog for all overlay dialogs that can be displayed above the {@link ViewerFrame}.
 *
 * @author Jaka Bobnar
 */
public abstract class OverlayDialog extends JDialog {

    private static final long serialVersionUID = -1448020813812555955L;
    private float focusOpacity = .9f;
    private float regularOpacity = .6f;
    protected transient MouseAdapter adapter = new MouseAdapter() {

        private Point offset = new Point(0,0);

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                offset = e.getPoint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                Point loc = e.getLocationOnScreen();
                setLocation(loc.x - offset.x,loc.y - offset.y);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                myLocation = getBounds();
                parentLocation = getParent().getBounds();
            } else {
                mouseEventReceiver.ifPresent(x -> x.mouseReleased(e));
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            targetOpacity = regularOpacity;
            timer.start();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            targetOpacity = focusOpacity;
            timer.start();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            mouseEventReceiver.ifPresent(x -> x.mouseWheelMoved(e));
        }

    };
    private float opacity = 0;
    private Timer timer;
    private Rectangle myLocation;
    private Rectangle parentLocation;
    private float targetOpacity = regularOpacity;
    private transient GradientPaint paint;
    private transient Optional<MouseAdapter> mouseEventReceiver;

    /**
     * Constructs a new dialog.
     *
     * @param parent the parent frame
     * @param width the default width of this dialog
     * @param height the default height of this dialog
     */
    protected OverlayDialog(JFrame parent, final int width, final int height) {
        super(parent);
        if (parent == null) {
            throw new NullPointerException("Parent cannot be null.");
        }
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setAlwaysOnTop(true);
        JPanel contentPanel = new JPanel(new GridBagLayout()) {

            private static final long serialVersionUID = 3228661113605432407L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setPaint(paint);
                int w = getWidth();
                int h = getHeight();
                g2d.fillRect(0,0,w,h);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(0,0,w - 1,h - 1);
            }
        };
        contentPanel.setOpaque(false);
        JComponent mainComponent = createMainComponent();
        mainComponent.addMouseMotionListener(adapter);
        mainComponent.addMouseListener(adapter);
        mainComponent.addMouseWheelListener(adapter);
        contentPanel.add(mainComponent,gbc(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,1));
        setContentPane(contentPanel);
        setOpacity(0f);
        setSize(width,height);
        setFocusable(false);
        setFocusableWindowState(false);
        int x = (int) Math.min(parent.getX() + parent.getWidth() * 0.8,parent.getX() + parent.getWidth() - width - 15.);
        int y = (int) Math.min(parent.getY() + parent.getHeight() * 0.8,
                parent.getY() + parent.getHeight() - height - 15.);
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        setLocation(x,y);
        parentLocation = parent.getBounds();
        myLocation = new Rectangle(x,y,width,height);
        timer = new Timer(20,e -> {
            // animate the appearance and disappearance of the dialog
            boolean down = false;
            if (opacity < targetOpacity) {
                opacity += 0.02;
                down = false;
            } else if (opacity >= targetOpacity) {
                opacity -= 0.02;
                down = true;
            }
            setOpacity(opacity);
            if (down && opacity <= targetOpacity || !down && opacity >= targetOpacity) {
                timer.stop();
            }
        });
        parent.addWindowListener(new WindowAdapter() {

            @Override
            public void windowDeactivated(WindowEvent e) {
                setAlwaysOnTop(false);
            }

            @Override
            public void windowActivated(WindowEvent e) {
                setAlwaysOnTop(true);
            }
        });
        // when moving and resizing the frame be smart how to reposition this dialog
        parent.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(ComponentEvent e) {
                if (myLocation.intersects(parentLocation)) {
                    Rectangle r = getParent().getBounds();
                    int dx = r.x - parentLocation.x;
                    int dy = r.y - parentLocation.y;
                    myLocation.translate(dx,dy);
                    SwingUtilities.invokeLater(() -> setLocation(myLocation.x,myLocation.y));
                }
                parentLocation = getParent().getBounds();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (myLocation.intersects(parentLocation)) {
                    Rectangle r = getParent().getBounds();
                    Point p = myLocation.getLocation();
                    p.translate(-parentLocation.x,-parentLocation.y);
                    float ww = p.x / (float) parentLocation.width;
                    float hh = p.y / (float) parentLocation.height;
                    p.x = (int) (r.width * ww);
                    p.y = (int) (r.height * hh);
                    p.translate(r.x,r.y);
                    if (p.x < r.x) {
                        p.x = r.x + 10;
                    } else if (p.x + getWidth() > r.x + r.width) {
                        p.x = r.x + r.width - getWidth() - 10;
                    }
                    if (p.y < r.y) {
                        p.y = r.y + 10;
                    } else if (p.y + getHeight() > r.y + r.height) {
                        p.y = r.y + r.height - getHeight() - 10;
                    }
                    myLocation.setLocation(p);
                    SwingUtilities.invokeLater(() -> setLocation(p));
                }
                parentLocation = getParent().getBounds();
            }
        });
    }

    @Override
    public void setLocation(Point p) {
        super.setLocation(p);
        myLocation = new Rectangle(p,getSize());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.Window#setSize(int, int)
     */
    @Override
    public void setSize(int width, int height) {
        super.setSize(width,height);
        paint = new GradientPaint(0,0,new Color(229,229,229),0,height,new Color(163,163,163));
    }

    /**
     * Receiver for wheel mouse events and button 4 and 5 click events.
     *
     * @param adapter the adapter to receive the special events
     */
    public void setMouseEventReceiver(MouseAdapter adapter) {
        this.mouseEventReceiver = Optional.ofNullable(adapter);
    }

    /**
     * Returns the main component (contents) that is displayed in this dialog.
     *
     * @return the contents of the dialog
     */
    protected abstract JComponent createMainComponent();

    /**
     * Shows or hides this displayer. If this dialog is already in the requested state, nothing happens.
     *
     * @param show true to show it or false to hide it.
     */
    public void setShowing(boolean show) {
        if (isShowing() != show) {
            toggle();
        }
    }

    /**
     * Toggle the visibility of this dialog. The visibility of the dialog always changes when this method is called.
     */
    public void toggle() {
        if (isShowing()) {
            dispose();
            opacity = 0;
            setOpacity(0f);
        } else {
            setOpacity(0f);
            targetOpacity = regularOpacity;
            Point p = getParent().getMousePosition();
            if (p != null) {
                Rectangle r = getBounds();
                if (r.x <= p.x && p.x <= r.x + r.width && r.y <= p.y && p.y <= r.y + r.height) {
                    targetOpacity = focusOpacity;
                }
            }
            setVisible(true);
            toFront();
            timer.start();
        }
    }

    /**
     * Set the opacity of this dialog when not in focus.
     *
     * @param regularOpacity the opacity
     */
    public void setRegularOpacity(float regularOpacity) {
        this.regularOpacity = regularOpacity;
        this.targetOpacity = regularOpacity;
        focusOpacity = regularOpacity > .9f ? 1f : .9f;
        setOpacity(targetOpacity);
    }

    /**
     * Returns the opacity of the dialog when not in focus.
     *
     * @return the opacity
     */
    public float getRegularOpacity() {
        return regularOpacity;
    }
}
