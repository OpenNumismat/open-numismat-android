package org.opennumismat.uscommemoratives;

import java.io.File;

/**
 * Created by v.ignatov on 20.01.2016.
 */
public class DownloadEntry {
    private final String title;
    private final String date;
    private final String size;
    private final String file_name;
    private final String url;
    private File file;

    public DownloadEntry(String title, String date, String size, String file, String url) {
        this.title = title;
        this.date = date;
        this.size = size;
        this.file_name = file;
        this.url = url;
    }

    public String title() {
        return title;
    }

    public String url() {
        return url;
    }

    public File file() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }

    public String file_name() {
        return file_name;
    }

    public String getDescription() {
        return file_name + ", " + size + ", " + date;
    }
}
