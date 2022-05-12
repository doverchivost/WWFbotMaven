package InstagramItems;

import java.io.File;

public class InstagramPost extends InstagramItem {

    private String caption;
    private String translatedCaption;
    private int mediaCount;
    private File[] media;
    private int[] versions;

    public String getCaption() { return caption; }
    public String getTranslatedCaption() { return translatedCaption; }
    public int getMediaCount() { return mediaCount; }
    public File[] getMedia() { return media; }
    public int[] getVersions() { return versions; }

    public void setCaption(String caption) { this.caption = caption; }
    public void setTranslatedCaption(String translatedCaption) { this.translatedCaption = translatedCaption; }
    public void setMediaCount(int mediaCount) { this.mediaCount = mediaCount; }
    public void setMedia(File[] media) { this.media = media; }
    public void setVersions(int[] versions) { this.versions = versions; }
}
