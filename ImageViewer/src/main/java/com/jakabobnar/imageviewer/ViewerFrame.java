/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer;

import static com.jakabobnar.imageviewer.util.Utilities.registerKeyStroke;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.jakabobnar.colorprofile.ColorProfileManager;
import com.jakabobnar.imageviewer.components.HelpDialog;
import com.jakabobnar.imageviewer.components.SettingsDialog;
import com.jakabobnar.imageviewer.util.Settings;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sf.tinylaf.TinyLookAndFeel;

/**
 * ViewerFrame is the main application frame and entry point.
 *
 * @author Jaka Bobnar
 *
 *         TODO When scrolling prefer quality over speed. Invert mouse scrolling Bigger Controls and larger fonts Fast
 *         scaling method implementation
 */
public class ViewerFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    public static ViewerFrame instance;

    public static void main(String[] args) {
        ImageIO.setUseCache(false); // This speeds up the loading of images
        Logger.getLogger("").setLevel(Level.OFF);
        // don't use opengl, it has problem in full screen mode
        System.setProperty("sun.java2d.opengl","false");
        System.setProperty("sun.java2d.d3d","true");
        // System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.ddscale","true");
        System.setProperty("sun.java2d.translaccel","true");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new TinyLookAndFeel());
            } catch (Exception e) {
                // ignore
            }
            File f = args.length > 0 ? new File(args[0]) : null;
            Toolkit.getDefaultToolkit().setDynamicLayout(false);
            instance = new ViewerFrame(f);
        });
    }

    private Rectangle lastBounds;
    private Viewer viewer;
    private File file;
    private Settings settings;

    /**
     * Constructs a new frame.
     */
    public ViewerFrame() {
        this(null);
    }

    /**
     * Constructs a new frame and opens the file given by the parameter.
     *
     * @param file the file to open
     */
    public ViewerFrame(File file) {
        this.file = file;
        setTitle("Image Viewer");
        setIconImages(Arrays.asList("i256.png","i128.png","i64.png","i48.png","i32.png","i16.png").stream()
                .map(s -> new ImageIcon(getClass().getClassLoader().getResource(s)).getImage())
                .collect(Collectors.toList()));
        setSize(1000,600);
        // Instantiate the displayers, abusing the singleton pattern
        EXIFDisplayer.getInstance(this);
        HistogramDisplayer.getInstance(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        settings = new Settings();
        // If system color profile option is selected, load the profile
        if (settings.systemColorProfile) {
            settings.colorProfile = ColorProfileManager.getColorProfileForComponent(ViewerFrame.this);
        }
        viewer = new Viewer(file);
        viewer.addRecentFilesSelectionListener(f -> {
            settings.addRecentFile(f);
            viewer.updateRecentFiles(settings.getRecentFiles());
        });
        registerGlobalActions(viewer);
        setContentPane(viewer);
        applyDialogSettings(settings,true);
        setVisible(true);
        // In some cases on Win 10 the frame was not brought to front. This definitely brings the frame to the front.
        setAlwaysOnTop(true);
        setAlwaysOnTop(false);
        addNotify();
        // Click on the frame to force focus on the frame
        try {
            Robot robot = new Robot(getGraphicsConfiguration().getDevice());
            Rectangle b = getBounds();
            robot.mouseMove(b.x + b.width / 2,b.y + b.height / 2);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (AWTException e1) {
            // ignore
        }
        if (settings.fullFrame != isUndecorated()) {
            toggleFullScreen();
        }
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                updateColorProfile();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (!isUndecorated() && getExtendedState() != MAXIMIZED_BOTH) {
                    lastBounds = getBounds();
                }
                updateColorProfile();
                viewer.resetZoomAndDrawing();
                viewer.updateScreenDimension();
            }

            private void updateColorProfile() {
                if (settings.useDisplayColorProfile) {
                    boolean systemColorProfile = settings.systemColorProfile;
                    if (systemColorProfile) {
                        viewer.setDestinationColorProfile(
                                ColorProfileManager.getColorProfileForComponent(ViewerFrame.this));
                    }
                }
            }
        });

        SwingUtilities.invokeLater(() -> {
            // Do this last to avoid problems with file loading requests due to multithreading. In the end we need
            // this.file to be selected.
            viewer.applySettings(settings);
            if (settings.lastFile != null) {
                File lastFile = new File(settings.lastFile);
                if (lastFile.exists()) {
                    this.file = lastFile;
                    viewer.openFileOrFolder(this.file);
                }
            }
        });

        Thread saveSettings = new Thread(() -> setSettings(settings,true));
        saveSettings.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(saveSettings);
    }

    @Override
    public void dispose() {
        viewer.dispose();
        super.dispose();
    }

    /**
     * Apply the settings to the dialogs and frames.
     *
     * @param newSettings the settings to apply
     * @param applyBounds true if the main frame bounds should be applied or false otherwise
     */
    private void applyDialogSettings(Settings newSettings, boolean applyBounds) {
        HistogramDisplayer.getInstance().setShowChannels(newSettings.showChannels);
        HistogramDisplayer.getInstance().setShowLuminosity(newSettings.showLuminosity);
        HistogramDisplayer.getInstance().setShowRGB(newSettings.showRGB);
        HistogramDisplayer.getInstance().setOverlayChannels(newSettings.overlayChannels);
        HistogramDisplayer.getInstance().setOverlayCharts(newSettings.overlayCharts);
        EXIFDisplayer.getInstance().setRegularOpacity(newSettings.overlayOpacity / 100f);
        HistogramDisplayer.getInstance().setRegularOpacity(newSettings.overlayOpacity / 100f);
        if (applyBounds) {
            viewer.setShowHistogram(newSettings.histogramVisible);
            viewer.setShowEXIFData(newSettings.exifVisible);
            setExtendedState(newSettings.windowState);
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            if (devices.length > 0) {
                // check available screens if any of them intersects with the frame bounds loaded from the settings
                // if one does, apply the bounds. if none of the screens intersects with the bounds, use default
                for (GraphicsDevice d : devices) {
                    for (GraphicsConfiguration gc : d.getConfigurations()) {
                        Rectangle b = gc.getBounds().intersection(newSettings.frameBounds);
                        if (b.width > 10 && b.height > 10) {
                            lastBounds = newSettings.frameBounds;
                            setBounds(newSettings.frameBounds);
                        }
                        if (newSettings.histogramLocation != null) {
                            if (gc.getBounds().contains(newSettings.histogramLocation)) {
                                HistogramDisplayer.getInstance().setLocation(newSettings.histogramLocation);
                            }
                        }
                        if (newSettings.exifLocation != null) {
                            if (gc.getBounds().contains(newSettings.exifLocation)) {
                                EXIFDisplayer.getInstance().setLocation(newSettings.exifLocation);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Set all settings to the viewer (depending on the boolean parameter) and store the settings to the file.
     *
     * @param settings the settings to set and store
     * @param saveOnly true if the settings should only be stored or false if applied and stored
     */
    private void setSettings(Settings settings, boolean saveOnly) {
        this.settings = settings;
        if (!saveOnly) {
            viewer.applySettings(settings);
            applyDialogSettings(settings,false);
        }
        settings.fullFrame = isUndecorated();
        this.file = viewer.getOpenFile();
        settings.lastFile = this.file == null ? null : this.file.getAbsolutePath();
        settings.frameBounds = lastBounds;
        settings.histogramVisible = HistogramDisplayer.getInstance().isShowing();
        settings.exifVisible = EXIFDisplayer.getInstance().isShowing();
        settings.histogramLocation = HistogramDisplayer.getInstance().getLocation();
        settings.exifLocation = EXIFDisplayer.getInstance().getLocation();
        settings.windowState = getExtendedState();
        settings.toolbarAutoHide = viewer.isToolbarAutoHide();
        settings.showToolbar = viewer.isShowToolbar();
        try {
            // This method is normally called from the swing thread, but the Settings store is such a quick operation
            // that it doesn't matter if it happens on the UI thread.
            settings.store();
        } catch (IOException e) {
            // The method is also called from the shutdown hook. In that case do not do any UI stuff as it vetos the
            // shutdown.
            if (!saveOnly) {
                JOptionPane.showMessageDialog(this,"Could not store the settings to your home folder.","Settings Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Open the settings dialog.
     */
    void openSettings() {
        SettingsDialog.open(ViewerFrame.this,settings).ifPresent(s -> setSettings(s,false));
        viewer.resetZoomAndDrawing();
    }

    /**
     * Apply the settings to the viewer.
     *
     * @param settings the settings to apply
     */
    public void applySettings(Settings settings) {
        setSettings(settings,false);
        viewer.resetZoomAndDrawing();
    }

    private FileChooser chooser;
    private Stage stage;
    private JFXPanel panel;

    /**
     * Open the native file chooser and load the file that is selected. The Java FX file chooser is used as it is
     * the only true native file chooser in the java world.
     */
    void selectFile() {
        if (panel == null) {
            panel = new JFXPanel();
            Platform.setImplicitExit(false);
        }
        Platform.runLater(() -> {
            // use fx dialog, because that is the native file dialog, but it needs some mambo jambo, to behave like
            // a modal dialog for a swing frame, which is why an additional stage is used
            if (chooser == null) {
                chooser = new FileChooser();
                chooser.setTitle("Select File or Folder");
                stage = new Stage(StageStyle.UNDECORATED);
                stage.setWidth(0);
                stage.setHeight(0);
                // set position relative to parent
                stage.setX(ViewerFrame.this.getX() + 50);
                stage.setY(ViewerFrame.this.getY() + 50);
            }
            // do not allow to show another dialog, if one is already showing
            if (stage.isShowing()) return;
            try {
                // Disable the main frame and show the stage. The stage is undecorated and 0,0 in size, so it won't be
                // shown at all, but the chooser will appear to be modal to this invisible stage
                ViewerFrame.this.setEnabled(false);
                stage.show();
                // Start with the previously selected file or from the home folder
                if (this.file != null && this.file.exists()) {
                    if (this.file.isDirectory()) {
                        chooser.setInitialDirectory(this.file);
                    } else {
                        chooser.setInitialDirectory(this.file.getParentFile());
                        chooser.setInitialFileName(this.file.getName());
                    }
                } else {
                    String home = System.getProperty("user.home");
                    File initialDir = new File(home);
                    chooser.setInitialDirectory(initialDir);
                }
                File newFile = chooser.showOpenDialog(stage);
                stage.hide();
                ViewerFrame.this.setEnabled(true);
                ViewerFrame.this.toFront();
                if (newFile != null) {
                    this.file = newFile;
                    settings.addRecentFile(this.file);
                    viewer.updateRecentFiles(settings.getRecentFiles());
                    viewer.openFileOrFolder(this.file);
                }
            } finally {
                // Make sure that the frame is enabled and bring it to front no matter what
                stage.hide();
                ViewerFrame.this.setEnabled(true);
                ViewerFrame.this.toFront();
            }
        });
    }

    /**
     * Toggles the toolbar auto hide feature.
     */
    private void toggleToolbarAutoHide() {
        viewer.setToolbarAutoHide(!viewer.isToolbarAutoHide());
    }

    /**
     * Toggle the full screen visibility. If the application is currently running in window mode, make it full screen.
     * If the application is running in full screen, switch it to window mode.
     */
    private void toggleFullScreen() {
        boolean histo = HistogramDisplayer.getInstance().isShowing();
        boolean exif = EXIFDisplayer.getInstance().isShowing();
        HistogramDisplayer.getInstance().setShowing(false);
        EXIFDisplayer.getInstance().setShowing(false);
        if (isUndecorated()) {
            // normal window mode
            removeNotify();
            setUndecorated(false);
            if (lastBounds != null) {
                setBounds(lastBounds);
            }
            addNotify();
        } else {
            // full screen mode
            lastBounds = getBounds();
            removeNotify();
            setUndecorated(true);
            Rectangle r = ViewerFrame.this.getGraphicsConfiguration().getBounds();
            setSize(r.width,r.height);
            setLocation(r.x,r.y);
        }
        addNotify();
        toFront();
        // Delay scaling
        SwingUtilities.invokeLater(() -> viewer.scaleImages());
        HistogramDisplayer.getInstance().setShowing(histo);
        EXIFDisplayer.getInstance().setShowing(exif);
        setState(Frame.ICONIFIED);
        setState(Frame.NORMAL);
    }

    /**
     * Register global key actions on the provided viewer.
     * @param comp the component to act on when specific actions occur
     */
    @SuppressWarnings("unused")
    private void registerGlobalActions(final Viewer comp) {
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_H,InputEvent.CTRL_DOWN_MASK),e -> {
            new HelpDialog(ViewerFrame.this);
            comp.resetZoomAndDrawing();
        });
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK),e -> comp.toggleSlideShow());
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK),e -> toggleFullScreen());
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_I,0),e -> comp.setShowEXIFData(!comp.isShowEXIFData()));
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_H,0),e -> comp.setShowHistogram(!comp.isShowHistogram()));
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_P,0),e -> openSettings());
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_O,0),e -> selectFile());
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_ESCAPE,0),e -> System.exit(0));
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_F11,0),e -> toggleFullScreen());
        registerKeyStroke(comp,getKeyStroke(KeyEvent.VK_F12,0),e -> toggleToolbarAutoHide());
    }
}
