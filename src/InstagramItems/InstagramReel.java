package InstagramItems;

import java.io.File;

public class InstagramReel extends InstagramItem {

    private File video;
    private String caption;
    private String translatedCaption;

    public File getVideo() { return video; }
    public String getCaption() { return caption; }
    public String getTranslatedCaption() { return translatedCaption; }

    public void setVideo(File video) { this.video = video; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setTranslatedCaption(String translatedCaption) { this.translatedCaption = translatedCaption; }
}
