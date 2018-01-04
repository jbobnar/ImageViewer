/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/**
 * Utilities provide general utility methods.
 *
 * @author Jaka Bobnar
 *
 */
public final class Utilities {

    /**
     * BooleanConsumer is a type of consumer which accepts a primitive boolean type.
     *
     * @author Jaka Bobnar
     *
     */
    @FunctionalInterface
    public static interface BooleanConsumer {
        void accept(boolean selected);
    }

    private Utilities() {

    }

    /**
     * Register a global key stroke with a specific action.
     *
     * @param component the component on which to register the action
     * @param stroke the key stroke
     * @param listener the action listener that is called when the action is being executed.
     */
    public static void registerKeyStroke(final JComponent component, KeyStroke stroke, final ActionListener listener) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke,stroke);
        component.getActionMap().put(stroke,new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }
        });
    }

    /**
     * Create grid bag constraints from the given parameters.
     *
     * @param gridx horizontal cell index
     * @param gridy vertical cell index
     * @param gridwidth width in cell numbers
     * @param gridheight height in cell numbers
     * @param weightx weight for horizontal component resize
     * @param weighty weight for vertical component resize
     * @param anchor the anchor for the component in this cell
     * @param fill the fill policy
     * @param inset the inset applied to all four sides
     * @return the grid bag constraints
     */
    public static GridBagConstraints gbc(int gridx, int gridy, int gridwidth, int gridheight, double weightx,
            double weighty, int anchor, int fill, int inset) {
        return new GridBagConstraints(gridx,gridy,gridwidth,gridheight,weightx,weighty,anchor,fill,
                new Insets(inset,inset,inset,inset),0,0);
    }

    /**
     * Create grid bag constraints from the given parameters.
     *
     * @param gridx horizontal cell index
     * @param gridy vertical cell index
     * @param gridwidth width in cell numbers
     * @param gridheight height in cell numbers
     * @param weightx weight for horizontal component resize
     * @param weighty weight for vertical component resize
     * @param anchor the anchor for the component in this cell
     * @param fill the fill policy
     * @param insettop top boundary inset
     * @param insetleft left boundary inset
     * @param insetbottom bottom boundary inset
     * @param insetright right boundary inset
     * @return the grid bag constraints
     */
    public static GridBagConstraints gbc(int gridx, int gridy, int gridwidth, int gridheight, double weightx,
            double weighty, int anchor, int fill, int insettop, int insetleft, int insetbottom, int insetright) {
        return new GridBagConstraints(gridx,gridy,gridwidth,gridheight,weightx,weighty,anchor,fill,
                new Insets(insettop,insetleft,insetbottom,insetright),0,0);
    }

    /**
     * Set the fixed size for the given component.
     *
     * @param c the component
     * @param width the width of the component
     * @param height the height of the component
     */
    public static void setFixedSize(Component c, int width, int height) {
        Dimension dim = new Dimension(width,height);
        c.setMinimumSize(dim);
        c.setMaximumSize(dim);
        c.setPreferredSize(dim);
    }

    /**
     * Add the menu items to the menu. If an item is null, add a separator.
     *
     * @param menu the destination menu
     * @param items the items to add
     */
    public static void addToPopupMenu(JPopupMenu menu, JMenuItem... items) {
        for (JMenuItem item : items) {
            if (item == null) {
                menu.addSeparator();
            } else {
                menu.add(item);
            }
        }
    }

    /**
     * Add the given components to the provided grid. The grid is expected to have the GridLayout already set.
     *
     * @param grid the panel to add the components to
     * @param components the components to add
     */
    public static void addToGrid(JPanel grid, JComponent... components) {
        if (!(grid.getLayout() instanceof GridLayout)) {
            throw new IllegalArgumentException("The provided component is not using a GridLayout.");
        }
        Arrays.stream(components).forEach(grid::add);
    }

    /**
     * Add the given buttons to a button group and return the group. The first button in the array is selected.
     *
     * @param buttons the buttons to add
     * @return the button group to which the buttons were added
     */
    public static ButtonGroup addToButtonGroup(AbstractButton... buttons) {
        ButtonGroup bg = new ButtonGroup();
        Arrays.stream(buttons).forEach(bg::add);
        buttons[0].setSelected(true);
        return bg;
    }

    /**
     * Returns a mouse listener where the provided consumer is invoked when the mouseClick event occurs.
     *
     * @param consumer the consumer to invoke on mouse click
     * @return the mouse listener
     */
    public static MouseListener onMouseClick(Consumer<MouseEvent> consumer) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                consumer.accept(e);
            }
        };
    }

    /**
     * Returns an item listener which notifies the consumer whether the item was selected (true) or deselected (false).
     * @param consumer consumer notified on selection/deselection
     * @return an item listener
     */
    public static ItemListener itemListener(BooleanConsumer consumer) {
        return e -> consumer.accept(e.getStateChange() == ItemEvent.SELECTED);
    }
}
