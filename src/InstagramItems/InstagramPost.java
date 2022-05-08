package InstagramItems;

import java.io.File;
import java.util.Date;

public class InstagramPost extends InstagramItem {

    private String caption;
    private int mediaCount;
    private File[] media;
    private int[] versions;

    public String getCaption() { return caption; }
    public int getMediaCount() { return mediaCount; }
    public File[] getMedia() { return media; }
    public int[] getVersions() { return versions; }

    public void setCaption(String caption) { this.caption = caption; }
    public void setMediaCount(int mediaCount) { this.mediaCount = mediaCount; }
    public void setMedia(File[] media) { this.media = media; }
    public void setVersions(int[] versions) { this.versions = versions; }
}
