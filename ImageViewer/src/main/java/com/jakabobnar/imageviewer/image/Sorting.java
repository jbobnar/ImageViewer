package com.jakabobnar.imageviewer.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;

import com.drew.imaging.ImageProcessingException;

/**
 * Sorting defines available criteria which can be used for sorting files in the folder. The order of the files is used
 * for presentation when advancing to next or previous image.
 *
 * @author Jaka Bobnar
 *
 */
public enum Sorting {
    NAME("File Name"), CREATION_DATE("Creation Date"), MODIFICATION_DATE("Modification Date"), DATE_TAKEN("Date Taken");

    private final String name;

    Sorting(String name) {
        this.name = name;
    }

    /**
     * Returns the comparator, for this sorting criterion. The comparator can be used for sorting files according to the
     * criteria as specified by individual instances of Sorting.
     *
     * @return the comparator
     */
    public Comparator<File> getComparator() {
        switch (this) {

        case CREATION_DATE:
            return (a, b) -> {
                try {
                    return Files.readAttributes(a.toPath(),BasicFileAttributes.class).creationTime()
                            .compareTo(Files.readAttributes(b.toPath(),BasicFileAttributes.class).creationTime());
                } catch (IOException e) {
                    return 0;
                    // ignore
                }
            };
        case MODIFICATION_DATE:
            return (a, b) -> Long.compare(a.lastModified(),b.lastModified());
        case DATE_TAKEN:
            return (a, b) -> {
                try {
                    EXIFData adata = ImageUtil.readExifData(a);
                    EXIFData bdata = ImageUtil.readExifData(b);
                    return adata.getDateTaken().compareTo(bdata.getDateTaken());
                } catch (IOException | ImageProcessingException e) {
                    return 0;
                    // ignore
                }
            };
        case NAME:
        default:
            return (a, b) -> a.getName().compareToIgnoreCase(b.getName());

        }
    }

    /**
     * Returns the human readable name of this sorting criterion.
     *
     * @return the name of the sorting criterion
     */
    public String getDescription() {
        return name;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return name;
    }
}
