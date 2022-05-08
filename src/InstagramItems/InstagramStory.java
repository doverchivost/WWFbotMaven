package InstagramItems;

import java.io.File;

public class InstagramStory extends InstagramItem {

    private File media;
    private int version;

    public File getMedia() { return media; }
    public int getVersion() { return version; }

    public void setMedia(File media) { this.media = media; }
    public void setVersion(int version) { this.version = version; }
}
