/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.sandbox;

import java.awt.Color;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.jakabobnar.imageviewer.ViewerFrame;
import com.jakabobnar.imageviewer.components.SettingsDialog;

import net.sf.tinylaf.Theme;
import net.sf.tinylaf.TinyLookAndFeel;
import net.sf.tinylaf.util.SBReference;

public class LAFManager {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new TinyLookAndFeel());
                if (true) {
                    Theme.buttonCheckColor = new SBReference(Color.ORANGE, 0, 0, 9);
                    Theme.radioFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.checkFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.textTextColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.textCaretColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.labelFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.comboTextColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.comboArrowColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.sliderThumbRolloverColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.buttonFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.sliderTickColor = new SBReference(Color.ORANGE.darker(), 0, 0, 7);
                    Theme.buttonRolloverColor = new SBReference(Color.ORANGE.darker(), 0, 0, 8);
                    Theme.listFocusBorderColor = new SBReference(Color.ORANGE.darker(), 0, 0, 8);
                    Theme.listTextColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.listSelectedBgColor = new SBReference(new Color(255, 200, 0, 100), 0, 0, 6);
                    Theme.listSelectedTextColor = new SBReference(Color.BLACK, 0, 0, 5);
                    Theme.scrollArrowColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.comboSelectedBgColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.menuFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.menuItemFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.menuItemRolloverColor = new SBReference(new Color(255, 200, 0, 100), 0, 0, 6);
                    Theme.menuSeparatorColor = new SBReference(Color.ORANGE.darker(), 0, 0, 8);
                    Theme.tabBorderColor = new SBReference(Color.ORANGE.darker(), 0, 0, 8);
                    Theme.tabFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.tabRolloverColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.tabSelectedColor = new SBReference(new Color(140,90,0), 0, 0, 10);
                    Theme.titledBorderColor = new SBReference(new Color(93,93,93), 0, 0, 8);
                    Theme.titledBorderFontColor = new SBReference(Color.ORANGE, 0, 0, 7);
                    Theme.saveTheme("src/main/resources/Default.theme");
                }
            } catch (Exception e) {
                // ignore
            }
            ViewerFrame frame = new ViewerFrame();
            new SettingsDialog(frame).open();
        });
    }
}
