package com.jakabobnar.imageviewer;

import java.io.File;

/**
 * 
 * <code>ToolbarListener</code> is a listener that can be added to a {@link Toolbar}. Whenever an action on the toolbar
 * is selected the listener is notified via the appropriate method.
 *
 * @author Jaka Bobnar
 *
 */
public interface ToolbarListener {
    
    /**
     * Invoked when the settings/preferences action is selected in the toolbar. Ths listener should show the settings
     * dialog. 
     */
    void openSettings();
    
    /**
     * Invoked when the open file action is selected. The listener should show the file chooser and allow selection of
     * a file to open and display.
     */
    void openFile();
    
    /**
     * Invoked when help action is selected. The listener should show the help dialog.
     */
    void help();
    
    /**
     * Invoked when the about action is selected. The listener should show the about dialog.
     */
    void about();
    
    /**
     * Invoked when the next or previous action is selected. The viewer should advance to the next or previous image,
     * depending on the parameter. The image should always be shown in best quality.
     * 
     * @param forward true for the next image or false for previous
     */
    void advanceImage(boolean forward);
 
    /**
     * Invoked when a recent file is selected. The file should be opened and put to the top of the recent files list.
     * 
     * @param file the file that was selected
     */
    void recentFileSelected(File file);
}
