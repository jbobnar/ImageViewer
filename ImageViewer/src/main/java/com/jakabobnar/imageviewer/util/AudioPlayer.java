/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * <code>AudioPlayer</code> plays a file when requested.
 *
 * @author Jaka Bobnar
 */
public final class AudioPlayer {

    private static final String BEEP_FILE = "beep.wav";
    private static final AudioPlayer INSTANCE = new AudioPlayer();
    private ExecutorService executor;
    private AtomicBoolean playing = new AtomicBoolean(false);
    private InputStream beepInputStream;

    private AudioPlayer() {
        // prevent instantiation
    }

    private ExecutorService getExecutor() {
        if (executor == null) {
            executor = new ImageExecutor("Audio",1,new DismissableBlockingQueue<>(1));
        }
        return executor;
    }

    /**
     * Play the beep.wav asynchronously, but only if the playing is not in queue or underway.
     */
    public static void playAsync() {
        INSTANCE.doPlayAsync();
    }

    /**
     * Play the beep file asynchronously, but only if playing is not already in queue or already playing.
     */
    public void doPlayAsync() {
        if (playing.compareAndSet(false,true)) {
            getExecutor().execute(() -> {
                try {
                    doPlaySync();
                } catch (AudioException e) {
                    //TODO message?
                }
                playing.set(false);
            });
        }
    }

    /**
     * Read the beep file into a local buffer to avoid parsing the file every time a beep is needed. In addition,
     * feeding the file from jar directly into the audio stream is not possible, because mark/reset is not supported.
     * 
     * @throws AudioException if the audio file cannot be read
     */
    private void checkBeepStream() throws AudioException {
        if (beepInputStream == null) {
            try (InputStream stream = AudioPlayer.class.getClassLoader().getResourceAsStream(BEEP_FILE)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
                byte[] buffer = new byte[1024];
                int siz = 0;
                while ((siz = stream.read(buffer)) != -1) {
                    bos.write(buffer,0,siz);
                }
                beepInputStream = new ByteArrayInputStream(bos.toByteArray());
            } catch (IOException e) {
                throw new AudioException("Cannot read audio file.",e);
            }
        }
        try {
            beepInputStream.reset();
        } catch (IOException e) {
            throw new AudioException("Cannot read audio file.",e);
        }
    }

    /**
     * Play the beep file synchronously (in the calling thread). If audio file is already playing the calling thread
     * will be blocked until previous playing finishes.
     * 
     * @throws AudioException if playing of the audio failed or the audio file could not be read
     */
    public synchronized void doPlaySync() throws AudioException {
        checkBeepStream();
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(beepInputStream)) {
            AudioFormat af = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,af);
            if (!AudioSystem.isLineSupported(info)) {
                throw new AudioException("Cannot play audio file. Audio system not available.");
            }
            int bufSize = (int)(af.getFrameRate() * af.getFrameSize() * 1.5);
            try (SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info)) {
                line.open(af,bufSize);
                line.start();
                byte[] data = new byte[bufSize];
                int bytesRead = 0;
                while ((bytesRead = ais.read(data,0,data.length)) != -1) {
                    line.write(data,0,bytesRead);
                }
                line.drain();
                line.stop();
            }
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            throw new AudioException("Error while playing audio file.",e);
        }
    }
}
