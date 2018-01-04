package com.jakabobnar.imageviewer.util;

/**
 * <code>AudioException</code> is an exception that describes an error while playing the audio.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class AudioException extends Exception {

    private static final long serialVersionUID = 8791958772887648086L;

    /**
     * Construct a new audio exception.
     * 
     * @param message the exception message
     */
    public AudioException(String message) {
        super(message);
    }

    /**
     * Construct a new audio exception.
     * 
     * @param message the exception message
     * @param cause the cause of the exception
     */
    public AudioException(String message, Throwable cause) {
        super(message,cause);
    }
}
