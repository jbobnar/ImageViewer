/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.components;

import static com.jakabobnar.imageviewer.util.Utilities.gbc;
import static com.jakabobnar.imageviewer.util.Utilities.itemListener;
import static com.jakabobnar.imageviewer.util.Utilities.registerKeyStroke;
import static com.jakabobnar.imageviewer.util.Utilities.setFixedSize;
import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTH;
import static java.awt.GridBagConstraints.NORTHEAST;
import static java.awt.GridBagConstraints.NORTHWEST;
import static java.awt.GridBagConstraints.WEST;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import com.jakabobnar.colorprofile.ColorProfileManager;
import com.jakabobnar.imageviewer.Transition;
import com.jakabobnar.imageviewer.ViewerFrame;
import com.jakabobnar.imageviewer.image.Sorting;
import com.jakabobnar.imageviewer.transition.TransitionsMap;
import com.jakabobnar.imageviewer.util.LMBAction;
import com.jakabobnar.imageviewer.util.Settings;
import com.jakabobnar.imageviewer.util.Utilities;

/**
 * SettingsDialog provide means to change the settings of the application. The settings include all settable image
 * loading, transition, performance, and other parameters.
 *
 * @author Jaka Bobnar
 *
 */
public class SettingsDialog extends JDialog {

    private static class HighlightPanel extends JPanel {
        private static final long serialVersionUID = 695098626341531836L;
        private Color color;
        private int opacity = 100;

        public HighlightPanel() {
            setBackground(new Color(200,200,200));
        }

        public void setHue(int hue) {
            color = Color.getHSBColor(hue / 255f,1f,1f);
            repaint();
        }

        public void setOpacity(int opacity) {
            this.opacity = opacity;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            ((Graphics2D) g).setComposite(AlphaComposite.SrcOver.derive(opacity / 255f));
            g.setColor(color);
            g.fillRect(0,0,getWidth(),getHeight());
        }
    }

    private static class ListRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 773781800689632667L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            return super.getListCellRendererComponent(list,((Transition) value).getName(),index,isSelected,
                    cellHasFocus);
        }
    }

    private static class Model extends AbstractListModel<Transition> {

        private static final long serialVersionUID = 1457045296394775997L;
        private List<Transition> list = new ArrayList<>();

        public void add(List<Transition> transitions) {
            list.addAll(transitions);
            list.sort((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()));
            if (!list.isEmpty()) {
                fireIntervalAdded(this,0,list.size() - 1);
            }
        }

        public void clear() {
            int size = list.size();
            list.clear();
            fireIntervalRemoved(this,0,size);
        }

        public void remove(List<Transition> transitions) {
            int size = list.size() - 1;
            list.removeAll(transitions);
            if (size > -1) {
                fireIntervalRemoved(this,0,size);
            }
        }

        @Override
        public int getSize() {
            return list.size();
        }

        @Override
        public Transition getElementAt(int index) {
            return list.get(index);
        }
    }

    private static class CaretUpdater implements CaretListener {
        private final IntConsumer consumer;

        CaretUpdater(IntConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            JTextField textField = (JTextField) e.getSource();
            try {
                int a = Integer.parseInt(textField.getText());
                if (a > 0) {
                    consumer.accept(a);
                }
            } catch (NumberFormatException x) {
                // ignore
            }
        }
    }

    private static final long serialVersionUID = 2613299115085489670L;
    private static final int HELP_OFFSET = 10;

    private Settings settings;
    private float previousOpacity;
    private final ViewerFrame viewerFrame;

    /**
     * Constructs a new settings dialog.
     *
     * @param frame the parent frame
     */
    public SettingsDialog(ViewerFrame frame) {
        super(frame);
        this.viewerFrame = frame;
        setTitle("Settings");
        setType(Type.NORMAL);
        initialize();
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800,600);
        setLocationRelativeTo(frame);
        setResizable(false);
        registerKeyStroke((JComponent) getContentPane(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),e -> cancel());
    }

    private void cancel() {
        settings = null;
        viewerFrame.getExifDisplayer().setRegularOpacity(previousOpacity);
        viewerFrame.getHistogramDisplayer().setRegularOpacity(previousOpacity);
        dispose();
    }

    /**
     * Open the dialog and return the new settings if they were confirmed or an empty object if the changes were
     * cancelled.
     *
     * @return the new settings if confirmed, or empty if cancelled
     */
    public Optional<Settings> open() {
        previousOpacity = viewerFrame.getExifDisplayer().getRegularOpacity();
        setVisible(true);
        return Optional.ofNullable(settings);
    }

    private void initialize() {

        final JPanel controls = createControlsPane();
        final JPanel transitions = createTransitionsPane();
        final JPanel general = createImagePane();
        final JPanel presentation = createPresentationPane();

        // Set all components but the provided one to invisible. The provided one is set to visible.
        Function<JPanel, ItemListener> toggleComponents = visibleComponent -> e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Arrays.asList(general,controls,transitions,presentation).forEach(comp -> comp.setVisible(false));
                visibleComponent.setVisible(true);
            }
        };

        JPanel mainContent = new JPanel(new GridBagLayout());
        JPanel leftPanel = new JPanel(new GridLayout(4,1,5,5));

        JToggleButton generalButton = new JToggleButton("Image");
        generalButton.addItemListener(toggleComponents.apply(general));
        JToggleButton controlsButton = new JToggleButton("Controls");
        controlsButton.addItemListener(toggleComponents.apply(controls));
        JToggleButton presentationButton = new JToggleButton("Presentation");
        presentationButton.addItemListener(toggleComponents.apply(presentation));
        JToggleButton transitionsButton = new JToggleButton("Transitions");
        transitionsButton.addItemListener(toggleComponents.apply(transitions));
        Utilities.addToButtonGroup(generalButton,controlsButton,presentationButton,transitionsButton);
        setFixedSize(controlsButton,120,50);
        Utilities.addToGrid(leftPanel,generalButton,controlsButton,presentationButton,transitionsButton);

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> ((ViewerFrame) getParent()).applySettings(settings));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dispose());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancel());
        JPanel buttonPanel = new JPanel(new GridLayout(1,2,5,5));
        Utilities.addToGrid(buttonPanel,applyButton,okButton,cancelButton);

        mainContent.add(leftPanel,gbc(0,0,1,1,0,1,NORTH,HORIZONTAL,5,5,5,10));
        mainContent.add(controls,gbc(1,0,1,1,1,1,NORTH,BOTH,5));
        mainContent.add(transitions,gbc(1,0,1,1,1,1,NORTH,BOTH,5));
        mainContent.add(general,gbc(1,0,1,1,1,1,NORTH,BOTH,5));
        mainContent.add(presentation,gbc(1,0,1,1,1,1,NORTH,BOTH,5));
        mainContent.add(buttonPanel,gbc(0,1,2,1,0,0,NORTHEAST,NONE,0,10,12,12));

        setContentPane(mainContent);
    }

    private JPanel createPresentationPane() {
        cycleWhenAtEnd = new JCheckBox("Cycle presentation when reaching the end");
        String cycleWhenAtEndHelp = "Rewind the presentation to the beginning after reaching the last image or "
                + "forward the presentation to the end after reaching the first image.";

        playSound = new JCheckBox("Play sound when presentation is rewound");

        cycleWhenAtEnd.addItemListener(itemListener(selected -> settings.cycleWhenAtEnd = selected));
        cycleWhenAtEnd.addItemListener(itemListener(playSound::setEnabled));

        playSound.addItemListener(itemListener(selected -> settings.playSoundOnCycle = selected));

        JLabel sortingOrderLabel = new JLabel("Sorting Order:");
        sortingOrder = new JComboBox<>(Sorting.values());
        sortingOrder.addItemListener(e -> settings.sortingOrder = (Sorting)sortingOrder.getSelectedItem());
        String sortingOrderHelp = "Select the criterion used for sorting the files in the same directory. The images "
                + "are advanced according to the chosen sorting order. Be aware that some criterion require parsing "
                + "of files which could introduce longer loading times.";

        JLabel stepSizeLabel = new JLabel("Fast forward / backward step size:");
        stepSize = new JTextField(6);

        JLabel stepSizeImages = new JLabel("images");
        String stepSizeHelp = "Number of images to skip when skipping forward or backward (Page Up/Page Down)";
        stepSize.addCaretListener(new CaretUpdater(val -> settings.stepSize = val));

        JLabel slideShowDurationLabel = new JLabel("When autoadvancing show image for:");
        slideShowDuration = new JTextField(6);
        JLabel slideShowDurationMillis = new JLabel("milliseconds");
        String slideShowDurationHelp = "Number of milliseconds how long each image is displayed, before advancing "
                + "to the next image. This does not include the time needed for imate transition and only applies "
                + "to autoadvanced slide shows.";
        slideShowDuration.addCaretListener(new CaretUpdater(val -> settings.slideShowDuration = val));

        JPanel stepSizePanel = new JPanel(new GridBagLayout());
        stepSizePanel.add(stepSize,gbc(0,0,1,1,0,0,WEST,NONE,0));
        stepSizePanel.add(stepSizeImages,gbc(1,0,1,1,1,0,WEST,NONE,0,5,0,0));
        JPanel slideShowPanel = new JPanel(new GridBagLayout());
        slideShowPanel.add(slideShowDuration,gbc(0,0,1,1,0,0,WEST,NONE,0));
        slideShowPanel.add(slideShowDurationMillis,gbc(1,0,1,1,1,0,WEST,NONE,0,5,0,0));

        JLabel backgroundColorLabel = new JLabel("Background color:");
        backgroundColor = new JTextField(6);
        backgroundColor.addPropertyChangeListener("background",
                e -> Optional.ofNullable(settings).ifPresent(s -> s.backgroundColor = (Color) e.getNewValue()));
        backgroundColor.setEditable(false);
        backgroundColor.setOpaque(true);
        backgroundColor.setFocusable(false);
        backgroundColor.setBackground(Color.BLACK);
        backgroundColor.setToolTipText("Double-click to select the background color");
        backgroundColor.addMouseListener(Utilities.onMouseClick(e -> {
            if (e.getClickCount() > 1) {
                Optional.ofNullable(JColorChooser.showDialog(SettingsDialog.this,"Select Background Color",
                        settings.backgroundColor)).ifPresent(backgroundColor::setBackground);
            }
        }));
        JButton backgroundButton = new JButton("...");
        backgroundButton.setToolTipText("Click to select background color");
        backgroundButton.setMargin(new Insets(1,1,1,1));
        backgroundButton.addActionListener(e -> Optional.ofNullable(
                JColorChooser.showDialog(SettingsDialog.this,"Select Background Color",settings.backgroundColor))
                .ifPresent(backgroundColor::setBackground));
        JPanel backgroundColorPanel = new JPanel(new GridBagLayout());
        backgroundColorPanel.add(backgroundColor,gbc(0,0,1,1,0,0,WEST,NONE,0));
        backgroundColorPanel.add(backgroundButton,gbc(1,0,1,1,0,0,WEST,NONE,0));

        int y = 0;
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.add(cycleWhenAtEnd,gbc(0,y,2,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(cycleWhenAtEndHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,2,15));
        contentPanel.add(playSound,gbc(0,++y,2,1,1,0,WEST,NONE,0,15,10,0));
        contentPanel.add(sortingOrderLabel,gbc(0,++y,1,1,0,0,WEST,NONE,0,5,2,5));
        contentPanel.add(sortingOrder,gbc(1,y,1,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(sortingOrderHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(stepSizeLabel,gbc(0,++y,1,1,0,0,WEST,NONE,0,5,2,5));
        contentPanel.add(stepSizePanel,gbc(1,y,1,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(stepSizeHelp),gbc(0,++y,2,1,1,0,NORTHWEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(slideShowDurationLabel,gbc(0,++y,1,1,0,0,WEST,NONE,0,5,2,5));
        contentPanel.add(slideShowPanel,gbc(1,y,1,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(slideShowDurationHelp),
                gbc(0,++y,2,1,1,0,NORTHWEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(backgroundColorLabel,gbc(0,++y,1,1,0,0,WEST,NONE,0,5,2,5));
        contentPanel.add(backgroundColorPanel,gbc(1,y,1,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new JPanel(),gbc(1,++y,1,1,1,1,WEST,BOTH,0,0,2,0));

        return contentPanel;
    }

    private JPanel createControlsPane() {
        showToolbar = new JCheckBox("Show toolbar");
        showToolbar.addItemListener(itemListener(selected -> settings.showToolbar = selected));
        String showToolbarHelp = "Show the toolbar at the top of the application. The toolbar can either be shown "
                + "continuously or when the mouse is in the area where the toolbar should be visible. The mode can "
                + "be switched by pressing F12.";

        autoHideMouse = new JCheckBox("Automatically hide mouse when not moving");
        autoHideMouse.addItemListener(itemListener(selected -> settings.autoHideMouse = selected));

        reverseAdvanceButtons = new JCheckBox("Reverse mouse buttons for next / previous image");
        reverseAdvanceButtons.addItemListener(itemListener(selected -> settings.reverseButtons = selected));

        mouseButtonsAdvance = new JRadioButton("Use left/right mouse buttons to advance or go back one image");
        mouseButtonsZoom = new JRadioButton("Use left/right mouse buttons to show original/zoomed image");
        String mouseButtonsHelp = "The left and right mouse buttons can be used to advance to the next image or return "
                + "to the previous image. If you have a multi button mouse, you can use the buttons 4 and 5 to go "
                + "forward or backward; the left mouse button can be used to show original size image (if no other "
                + "function is selected for left button) and right button can be used to zoom in for the selected "
                + "factor. At any time it is possible to show the original image by holding down CTRL + right mouse "
                + "button.";

        Utilities.addToButtonGroup(mouseButtonsAdvance,mouseButtonsZoom);

        zoomLabel = new JLabel("Right mouse button zoom [%]:");
        zoomSlider = new JSlider(SwingConstants.HORIZONTAL,50,500,300);
        zoomSlider.setFocusable(false);
        zoomSlider.setMajorTickSpacing(50);
        zoomSlider.setMinorTickSpacing(10);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        zoomSlider.createStandardLabels(100,100);
        String zoomHelp = "Set the right mouse button zoom in percentage. On right mouse click the image will be "
                + "zoomed in/out to the selected percentage of the current canvas size.";
        leftMouseButtonsPainting = new JCheckBox("Use left mouse button for painting");
        leftMouseButtonsCursor = new JCheckBox("Use left mouse button for highlight cursor");
        String leftMouseButtonsCursorHelp = "The left mouse button can be used to show the highlight cursor or "
                + "to draw on the image. These options are only available when the button is not used for advancing "
                + "the image. The features also override the ability to show the original image. Regardless of the "
                + "setting the highlight cursor can always be shown by holding the ALT and left mouse button at the "
                + "same time, while painting is possible while holding down the CTRL key.";
        JLabel hueLabel = new JLabel("Highlight cursor / drawing color:");
        highlightColor = new HighlightPanel();
        highlightColor.setOpaque(true);
        setFixedSize(highlightColor,30,30);
        hueSlider = new JSlider(SwingConstants.HORIZONTAL,0,255,40);
        hueSlider.setPaintTicks(false);
        hueSlider.setPaintLabels(false);
        hueSlider.setFocusable(false);
        hueSlider.addChangeListener(e -> {
            settings.cursorHue = hueSlider.getValue();
            highlightColor.setHue(settings.cursorHue);
        });
        JLabel cursorOpacityLabel = new JLabel("Highlight cursor / drawing opacity:");
        cursorOpacitySlider = new JSlider(SwingConstants.HORIZONTAL,0,255,40);
        cursorOpacitySlider.setPaintTicks(false);
        cursorOpacitySlider.setPaintLabels(false);
        cursorOpacitySlider.setFocusable(false);
        cursorOpacitySlider.addChangeListener(e -> {
            settings.cursorOpacity = cursorOpacitySlider.getValue();
            highlightColor.setOpacity(settings.cursorOpacity);
        });

        mouseButtonsAdvance.addItemListener(itemListener(selected -> settings.mouseButtonAdvance = selected));
        mouseButtonsZoom.addItemListener(itemListener(
                selected -> Arrays.asList(zoomSlider,zoomLabel,leftMouseButtonsCursor,leftMouseButtonsPainting)
                        .forEach(c -> c.setEnabled(selected))));
        zoomSlider.addChangeListener(e -> settings.zoomFactor = zoomSlider.getValue() / 100f);

        Consumer<Void> updateLeftMouseButtonAction = nothing -> {
            boolean cursor = leftMouseButtonsCursor.isSelected();
            boolean paint = leftMouseButtonsPainting.isSelected();
            if (cursor && paint) return;
            settings.leftMouseButtonAction = cursor ? LMBAction.CURSOR : paint ? LMBAction.PAINT : LMBAction.ZOOM;
        };
        leftMouseButtonsCursor.addItemListener(itemListener(selected -> {
            if (selected) {
                leftMouseButtonsPainting.setSelected(false);
            }
            updateLeftMouseButtonAction.accept(null);
        }));
        leftMouseButtonsPainting.addItemListener(itemListener(selected -> {
            if (selected) {
                leftMouseButtonsCursor.setSelected(false);
            }
            updateLeftMouseButtonAction.accept(null);
        }));

        JLabel opacityLabel = new JLabel("Overlay dialogs opacity [%]:");
        opacitySlider = new JSlider(SwingConstants.HORIZONTAL,0,100,60);
        opacitySlider.setMajorTickSpacing(20);
        opacitySlider.setMinorTickSpacing(5);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        opacitySlider.createStandardLabels(20,20);
        opacitySlider.setFocusable(false);
        String opacityHelp = "Set the opacity of the overlay information dialogs (EXIF, histogram).";
        opacitySlider.addChangeListener(e -> {
            settings.overlayOpacity = opacitySlider.getValue();
            viewerFrame.getExifDisplayer().setRegularOpacity(settings.overlayOpacity / 100f);
            viewerFrame.getHistogramDisplayer().setRegularOpacity(settings.overlayOpacity / 100f);
        });

        JPanel colorPanel = new JPanel(new GridBagLayout());
        colorPanel.add(hueLabel,gbc(0,0,1,1,0,1,WEST,HORIZONTAL,0,0,2,3));
        colorPanel.add(cursorOpacityLabel,gbc(0,1,1,1,0,1,WEST,HORIZONTAL,0,0,2,3));
        colorPanel.add(hueSlider,gbc(1,0,1,1,1,1,CENTER,HORIZONTAL,0,0,0,5));
        colorPanel.add(cursorOpacitySlider,gbc(1,1,1,1,1,1,CENTER,HORIZONTAL,0,0,0,5));
        colorPanel.add(highlightColor,gbc(2,0,1,2,0,0,CENTER,NONE,0,5,0,13));

        int y = 0;
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.add(showToolbar,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(showToolbarHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,2,15));
        contentPanel.add(autoHideMouse,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,10,0));
        contentPanel.add(reverseAdvanceButtons,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,10,0));
        contentPanel.add(mouseButtonsAdvance,gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,0,2,0));
        contentPanel.add(mouseButtonsZoom,gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,0,2,0));
        contentPanel.add(new HelpArea(mouseButtonsHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,2,15));
        contentPanel.add(zoomLabel,gbc(0,++y,1,1,0,0,WEST,HORIZONTAL,0,HELP_OFFSET,2,5));
        contentPanel.add(zoomSlider,gbc(1,y,1,1,1,0,WEST,HORIZONTAL,0,0,2,0));
        contentPanel.add(new HelpArea(zoomHelp),gbc(0,++y,2,1,1,0,NORTHWEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(leftMouseButtonsCursor,gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,0,2,0));
        contentPanel.add(leftMouseButtonsPainting,gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,0,2,0));
        contentPanel.add(new HelpArea(leftMouseButtonsCursorHelp),
                gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,2,15));
        contentPanel.add(colorPanel,gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,0,2,0));
        contentPanel.add(opacityLabel,gbc(0,++y,1,1,0,0,WEST,HORIZONTAL,0,0,2,5));
        contentPanel.add(opacitySlider,gbc(1,y,1,1,1,0,WEST,HORIZONTAL,0,0,2,0));
        contentPanel.add(new HelpArea(opacityHelp),gbc(0,++y,2,1,1,1,NORTHWEST,HORIZONTAL,0,HELP_OFFSET,10,15));

        return contentPanel;
    }

    private JPanel createTransitionsPane() {
        useTransitions = new JCheckBox("Enable transitions between images");
        String useTransitionsHelp = "Animated transitions are used when advancing to the next or going to the "
                + "previous image. Transitions are not used when scrolling.";

        transitionDurationLabel = new JLabel("Duration of transitions:");
        transitionDuration = new JTextField(6);
        transitionDurationMillis = new JLabel("milliseconds");
        String transitionDurationHelp = "Duration of each transition between the images (does not apply to scrolling)";
        transitionDuration.addCaretListener(new CaretUpdater(val -> settings.transitionDuration = val));

        preview = new TransitionPreview();
        availableTransitions = new JList<>(new Model());
        availableTransitions.setCellRenderer(new ListRenderer());
        selectedTransitions = new JList<>(new Model());
        selectedTransitions.setCellRenderer(new ListRenderer());
        availableTransitions.addMouseListener(Utilities.onMouseClick(e -> {
            if (e.getClickCount() > 1) {
                moveRight(availableTransitions,selectedTransitions,false);
            } else {
                preview.setTransition(availableTransitions.getSelectedValue());
            }

        }));
        selectedTransitions.addMouseListener(Utilities.onMouseClick(e -> {
            if (e.getClickCount() > 1) {
                moveLeft(availableTransitions,selectedTransitions,false);
            } else {
                preview.setTransition(selectedTransitions.getSelectedValue());
            }

        }));

        right = new JButton(">");
        right.addActionListener(e -> moveRight(availableTransitions,selectedTransitions,false));
        rightRight = new JButton(">>");
        rightRight.addActionListener(e -> moveRight(availableTransitions,selectedTransitions,true));
        left = new JButton("<");
        left.addActionListener(e -> moveLeft(availableTransitions,selectedTransitions,false));
        leftLeft = new JButton("<<");
        leftLeft.addActionListener(e -> moveLeft(availableTransitions,selectedTransitions,true));
        JPanel buttonPanel = new JPanel(new GridLayout(4,1,5,5));
        Utilities.addToGrid(buttonPanel,right,rightRight,left,leftLeft);

        JScrollPane availableScroll = new JScrollPane(availableTransitions);
        JScrollPane selectedScroll = new JScrollPane(selectedTransitions);
        setFixedSize(availableScroll,80,1);
        setFixedSize(selectedScroll,80,1);

        JPanel transitionsPanel = new JPanel(new GridBagLayout());
        transitionsPanel.add(new JLabel("Available Transitions"),gbc(0,0,1,1,1,0,CENTER,NONE,0,0,2,0));
        transitionsPanel.add(availableScroll,gbc(0,1,1,1,1,1,CENTER,BOTH,0));
        transitionsPanel.add(buttonPanel,gbc(1,1,1,1,0,0,CENTER,HORIZONTAL,0,5,0,5));
        transitionsPanel.add(new JLabel("Selected Transitions"),gbc(2,0,1,1,1,0,CENTER,NONE,0,0,2,0));
        transitionsPanel.add(selectedScroll,gbc(2,1,1,1,1,1,CENTER,BOTH,0));
        String transitionPanelHelp = "A randomly selected transition from the right list will be used every time when "
                + "the next or previous image is requested (it does not apply to scrolling). If no transition is "
                + "selected a new image will be shown immediately when requested.";

        useTransitions.addItemListener(itemListener(selected -> {
            settings.useTransitions = selected;
            Arrays.asList(transitionDurationLabel,transitionDuration,availableTransitions,selectedTransitions,
                    transitionDurationMillis,right,rightRight,left,leftLeft).forEach(c -> c.setEnabled(selected));
            if (!selected) {
                selectedTransitions.clearSelection();
                availableTransitions.clearSelection();
                preview.setTransition(null);
            }
        }));

        JPanel transitionDurationPanel = new JPanel(new GridBagLayout());
        transitionDurationPanel.add(transitionDuration,gbc(0,0,1,1,0,0,WEST,NONE,0));
        transitionDurationPanel.add(transitionDurationMillis,gbc(1,0,1,1,1,0,WEST,NONE,0,5,0,0));

        int y = 0;
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.add(useTransitions,gbc(0,y,2,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(useTransitionsHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(transitionDurationLabel,gbc(0,++y,1,1,0,0,WEST,NONE,0,5,2,5));
        contentPanel.add(transitionDurationPanel,gbc(1,y,1,1,1,0,WEST,BOTH,0,0,2,0));
        contentPanel.add(new HelpArea(transitionDurationHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(transitionsPanel,gbc(0,++y,2,1,1,1,WEST,BOTH,0,0,10,10));
        contentPanel.add(new HelpArea(transitionPanelHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(preview,gbc(0,++y,2,1,1,0,CENTER,NONE,0,0,2,0));
        return contentPanel;
    }

    private void moveRight(JList<Transition> availTransitions, JList<Transition> selTransitions, boolean all) {
        if (all) {
            List<Transition> transitions = TransitionsMap.getInstance().getRegisteredTransitions();
            Model model = (Model) availTransitions.getModel();
            model.clear();
            model = (Model) selTransitions.getModel();
            model.clear();
            settings.transitions.clear();
            model.add(transitions);
            settings.transitions.addAll(transitions);
        } else {
            List<Transition> transitions = availTransitions.getSelectedValuesList();
            Model model = (Model) availTransitions.getModel();
            model.remove(transitions);
            model = (Model) selTransitions.getModel();
            model.add(transitions);
            settings.transitions.addAll(transitions);
        }
    }

    private void moveLeft(JList<Transition> availTransitions, JList<Transition> selTransitions, boolean all) {
        if (all) {
            List<Transition> transitions = TransitionsMap.getInstance().getRegisteredTransitions();
            Model model = (Model) selTransitions.getModel();
            model.clear();
            settings.transitions.clear();
            model = (Model) availTransitions.getModel();
            model.clear();
            model.add(transitions);
        } else {
            List<Transition> transitions = selTransitions.getSelectedValuesList();
            Model model = (Model) selTransitions.getModel();
            settings.transitions.removeAll(transitions);
            model.remove(transitions);
            model = (Model) availTransitions.getModel();
            model.add(transitions);
        }
    }

    private JPanel createImagePane() {
        multipleCores = new JCheckBox("Utilize all CPU cores");
        String multipleCoresHelp = "Try to utilize all available CPU cores as much as possible. This can "
                + "significantly speed up the loading of images when scrolling, but will "
                + "consume more memory and CPU.";
        multipleCores.addItemListener(itemListener(selected -> settings.useMultipleCores = selected));

        waitForImagesToLoad = new JCheckBox("Wait for next image when scrolling");
        String waitForImagesToLoadHelp = "When scrolling with mouse you can wait for the next (in order) image "
                + "to load or you can advance to the next image that becomes available (some images might not be "
                + "shown at all while scrolling). If all images are shown, scrolling will be generally slower.";
        waitForImagesToLoad
                .addItemListener(itemListener(selected -> settings.waitForImagesToLoadWhenScrolling = selected));

        colorManage = new JCheckBox("Enable color management");
        String colorManageHelp = "Enable color management to respect the color profiles embedded in the images. "
                + "Enabling color management also allows you to select the display color profile. "
                + "You can select a custom profile or let the application to use whatever profile "
                + "is set as a default profile for your device. Display color profile is not used when scrolling";

        displayColorManage = new JCheckBox("Use display specific color profile");

        systemDefaultProfile = new JRadioButton("System default profile");
        File f = ColorProfileManager.getColorProfileForComponent(getParent());
        systemProfile = new JLabel(f == null ? "N/A" : f.getName());
        customProfile = new JRadioButton("Custom display profile");
        File[] profiles = ColorProfileManager.getAvailableColorProfiles();
        customProfileSelector = new JComboBox<>(profiles);
        customProfileSelector.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = -7330861370624330507L;

            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list,((File) value).getName(),index,isSelected,cellHasFocus);
            }
        });
        customProfileSelector
                .addItemListener(e -> settings.colorProfile = (File) customProfileSelector.getSelectedItem());
        Utilities.addToButtonGroup(systemDefaultProfile,customProfile);

        systemDefaultProfile.addItemListener(itemListener(selected -> settings.systemColorProfile = selected));
        systemDefaultProfile.addItemListener(itemListener(systemProfile::setEnabled));
        customProfile.addItemListener(itemListener(customProfileSelector::setEnabled));

        displayColorManage.addItemListener(itemListener(selected -> {
            settings.useDisplayColorProfile = selected;
            systemDefaultProfile.setEnabled(selected);
            systemProfile.setEnabled(selected && settings.systemColorProfile);
            customProfile.setEnabled(selected);
            customProfileSelector.setEnabled(selected && !settings.systemColorProfile);
        }));
        colorManage.addItemListener(itemListener(selected -> {
            settings.colorManage = selected;
            displayColorManage.setEnabled(selected);
            boolean colorManageAndUseDisplay = settings.useDisplayColorProfile && selected;
            systemDefaultProfile.setEnabled(colorManageAndUseDisplay);
            systemProfile.setEnabled(colorManageAndUseDisplay && settings.systemColorProfile);
            customProfile.setEnabled(colorManageAndUseDisplay);
            customProfileSelector.setEnabled(colorManageAndUseDisplay && !settings.systemColorProfile);
        }));

        scaleToFit = new JCheckBox("Scale small images to fit the window");
        String scaleToFitHelp = "Images that are smaller than current window size will be resized to fit the window.";
        scaleToFit.addItemListener(itemListener(selected -> settings.scaleToFit = selected));

        rotateImage = new JCheckBox("Rotate images according to information stored in image EXIF");
        rotateImage.addItemListener(itemListener(selected -> settings.rotateImage = selected));

        JLabel histogram = new JLabel("Histogram");
        String histogramHelp = "Select which histograms you wish to see in the histogram overlay panel. You can choose "
                + "among the histograms of individual channels (red, green, blue), combined RGB histogram and "
                + "luminosity histogram.";
        histoShowChannels = new JCheckBox("Show individual channels");
        histoShowChannels.addItemListener(itemListener(selected -> settings.showChannels = selected));
        histoShowRGB = new JCheckBox("Show RGB");
        histoShowRGB.addItemListener(itemListener(selected -> settings.showRGB = selected));
        histoShowLuminosity = new JCheckBox("Show luminosity");
        histoShowLuminosity.addItemListener(e -> settings.showLuminosity = e.getStateChange() == ItemEvent.SELECTED);
        String histogramOverlayHelp = "Histograms can be drawn one over the other or one below the other. "
                + "If the histograms are not overlayed, you may overlay only the channels histograms.";
        histoOverlayChannels = new JCheckBox("Overlay channels");
        histoOverlayChannels.addItemListener(e -> settings.overlayChannels = e.getStateChange() == ItemEvent.SELECTED);
        histoOverlayCharts = new JCheckBox("Overlay all");
        histoOverlayCharts.addItemListener(itemListener(selected -> {
            settings.overlayCharts = selected;
            histoOverlayChannels.setEnabled(!selected);
        }));

        int y = 0;
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.add(multipleCores,gbc(0,y,2,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(multipleCoresHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(waitForImagesToLoad,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(waitForImagesToLoadHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(colorManage,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(colorManageHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(displayColorManage,gbc(0,++y,2,1,1,0,WEST,NONE,0,15,2,0));
        contentPanel.add(systemDefaultProfile,gbc(0,++y,1,1,1,0,WEST,NONE,0,15,2,5));
        contentPanel.add(systemProfile,gbc(1,y,1,1,1,0,WEST,NONE,0,2,2,5));
        contentPanel.add(customProfile,gbc(0,++y,1,1,0,0,WEST,NONE,0,15,10,5));
        contentPanel.add(customProfileSelector,gbc(1,y,1,1,1,0,WEST,HORIZONTAL,0,0,10,10));
        contentPanel.add(scaleToFit,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,2,0));
        contentPanel.add(new HelpArea(scaleToFitHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(rotateImage,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,10,0));
        contentPanel.add(histogram,gbc(0,++y,2,1,1,0,WEST,NONE,0,0,5,0));
        contentPanel.add(histoShowChannels,gbc(0,++y,2,1,1,0,WEST,NONE,0,15,2,0));
        contentPanel.add(histoShowRGB,gbc(0,++y,2,1,1,0,WEST,NONE,0,15,2,0));
        contentPanel.add(histoShowLuminosity,gbc(0,++y,2,1,1,0,WEST,NONE,0,15,2,0));
        contentPanel.add(new HelpArea(histogramHelp),gbc(0,++y,2,1,1,0,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        contentPanel.add(histoOverlayCharts,gbc(0,++y,2,1,1,0,WEST,NONE,0,15,2,0));
        contentPanel.add(histoOverlayChannels,gbc(0,++y,2,1,1,0,NORTHWEST,NONE,0,15,2,0));
        contentPanel.add(new HelpArea(histogramOverlayHelp),gbc(0,++y,2,1,1,1,WEST,HORIZONTAL,0,HELP_OFFSET,10,15));
        return contentPanel;
    }

    /**
     * Apply the settings to the UI components of this dialog.
     *
     * @param newSettings the settings to apply
     */
    public void applySettings(Settings newSettings) {
        this.settings = newSettings;
        sortingOrder.setSelectedItem(newSettings.sortingOrder);
        playSound.setSelected(newSettings.playSoundOnCycle);
        cycleWhenAtEnd.setSelected(newSettings.cycleWhenAtEnd);
        playSound.setEnabled(newSettings.cycleWhenAtEnd);
        slideShowDuration.setText(String.valueOf(newSettings.slideShowDuration));
        stepSize.setText(String.valueOf(newSettings.stepSize));
        backgroundColor.setBackground(newSettings.backgroundColor);

        showToolbar.setSelected(newSettings.showToolbar);
        autoHideMouse.setSelected(newSettings.autoHideMouse);
        reverseAdvanceButtons.setSelected(newSettings.reverseButtons);
        mouseButtonsAdvance.setSelected(newSettings.mouseButtonAdvance);
        mouseButtonsZoom.setSelected(!newSettings.mouseButtonAdvance);
        Arrays.asList(zoomSlider,zoomLabel,leftMouseButtonsCursor,leftMouseButtonsPainting)
                .forEach(c -> c.setEnabled(!newSettings.mouseButtonAdvance));
        zoomSlider.setValue((int) (newSettings.zoomFactor * 100));
        leftMouseButtonsCursor.setSelected(newSettings.leftMouseButtonAction == LMBAction.CURSOR);
        leftMouseButtonsPainting.setSelected(newSettings.leftMouseButtonAction == LMBAction.PAINT);
        hueSlider.setValue(newSettings.cursorHue);
        cursorOpacitySlider.setValue(newSettings.cursorOpacity);
        highlightColor.setHue(newSettings.cursorHue);
        highlightColor.setOpacity(newSettings.cursorOpacity);
        opacitySlider.setValue(newSettings.overlayOpacity);

        useTransitions.setSelected(newSettings.useTransitions);
        transitionDuration.setText(String.valueOf(newSettings.transitionDuration));
        Arrays.asList(transitionDurationLabel,transitionDuration,transitionDurationMillis,availableTransitions,
                selectedTransitions,right,rightRight,left,leftLeft)
                .forEach(c -> c.setEnabled(newSettings.useTransitions));

        List<Transition> transitions = TransitionsMap.getInstance().getRegisteredTransitions();
        Model model = (Model) availableTransitions.getModel();
        model.clear();
        transitions.removeAll(newSettings.transitions);
        model.add(transitions);
        model = (Model) selectedTransitions.getModel();
        model.clear();
        model.add(newSettings.transitions);
        if (newSettings.useTransitions) {
            if (newSettings.transitions.isEmpty()) {
                availableTransitions.setSelectedIndex(0);
                preview.setTransition(availableTransitions.getSelectedValue());
            } else {
                selectedTransitions.setSelectedIndex(0);
                preview.setTransition(selectedTransitions.getSelectedValue());
            }
        }

        multipleCores.setSelected(newSettings.useMultipleCores);
        waitForImagesToLoad.setSelected(newSettings.waitForImagesToLoadWhenScrolling);
        systemDefaultProfile.setSelected(newSettings.systemColorProfile);
        customProfileSelector.setSelectedItem(newSettings.colorProfile);
        customProfile.setSelected(!newSettings.systemColorProfile);
        displayColorManage.setSelected(newSettings.useDisplayColorProfile);
        colorManage.setSelected(newSettings.colorManage);
        displayColorManage.setEnabled(newSettings.colorManage);
        boolean colorManageAndDisplayProfile = newSettings.useDisplayColorProfile && newSettings.colorManage;
        systemDefaultProfile.setEnabled(colorManageAndDisplayProfile);
        systemProfile.setEnabled(colorManageAndDisplayProfile && newSettings.systemColorProfile);
        customProfile.setEnabled(newSettings.useDisplayColorProfile && newSettings.colorManage);
        customProfileSelector.setEnabled(colorManageAndDisplayProfile && !newSettings.systemColorProfile);
        scaleToFit.setSelected(newSettings.scaleToFit);
        rotateImage.setSelected(newSettings.rotateImage);
        histoShowChannels.setSelected(newSettings.showChannels);
        histoShowRGB.setSelected(newSettings.showRGB);
        histoShowLuminosity.setSelected(newSettings.showLuminosity);
        histoOverlayChannels.setSelected(newSettings.overlayChannels);
        histoOverlayCharts.setSelected(newSettings.overlayCharts);
        histoOverlayChannels.setEnabled(!newSettings.overlayCharts);
    }

    private JCheckBox cycleWhenAtEnd;
    private JCheckBox playSound;
    private JComboBox<Sorting> sortingOrder;
    private JTextField stepSize;
    private JTextField slideShowDuration;
    private JTextField backgroundColor;
    private JCheckBox autoHideMouse;
    private JCheckBox reverseAdvanceButtons;
    private JCheckBox showToolbar;
    private JRadioButton mouseButtonsAdvance;
    private JRadioButton mouseButtonsZoom;
    private JLabel zoomLabel;
    private JSlider zoomSlider;
    private JCheckBox leftMouseButtonsPainting;
    private JCheckBox leftMouseButtonsCursor;
    private JSlider hueSlider;
    private JSlider cursorOpacitySlider;
    private HighlightPanel highlightColor;
    private JSlider opacitySlider;
    private JCheckBox useTransitions;
    private JTextField transitionDuration;
    private JLabel transitionDurationLabel;
    private JLabel transitionDurationMillis;
    private JButton right;
    private JButton rightRight;
    private JButton left;
    private JButton leftLeft;
    private JList<Transition> availableTransitions;
    private JList<Transition> selectedTransitions;
    private JCheckBox multipleCores;
    private JCheckBox waitForImagesToLoad;
    private JCheckBox colorManage;
    private JCheckBox displayColorManage;
    private JRadioButton systemDefaultProfile;
    private JLabel systemProfile;
    private JRadioButton customProfile;
    private JComboBox<File> customProfileSelector;
    private JCheckBox scaleToFit;
    private JCheckBox rotateImage;
    private JCheckBox histoShowChannels;
    private JCheckBox histoShowRGB;
    private JCheckBox histoShowLuminosity;
    private JCheckBox histoOverlayChannels;
    private JCheckBox histoOverlayCharts;
    private TransitionPreview preview;
}
