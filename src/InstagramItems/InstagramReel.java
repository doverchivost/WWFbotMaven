package InstagramItems;

import java.io.File;

public class InstagramReel extends InstagramItem {

    private File video;
    private String caption;

    public File getVideo() { return video; }
    public String getCaption() { return caption; }

    public void setVideo(File video) { this.video = video; }
    public void setCaption(String caption) { this.caption = caption; }
}








//import java.io.File;
//import java.util.Date;
//
//public class InstagramReel extends InstagramItem {
//
//    private Date reel_date;
//                          private String reel_caption;
//    private String user_who_posted;
//                          private File media_from_reel;
//                          private int media_versions;
//    private long reel_pk;
//
//    public Date getReel_date() { return reel_date; }
//    public String getReel_caption() { return reel_caption; }
//    public String getUser_who_posted() { return user_who_posted; }
//    public File getMedia_from_reel() { return media_from_reel; }
//    public int getMedia_versions() { return media_versions; }
//    public long getReel_pk() { return reel_pk; }
//
//    public void setReel_date(long getTaken) { this.reel_date = new Date((long)getTaken*1000); }
//    public void setReel_caption(String reel_caption) { this.reel_caption = reel_caption; }
//    public void setUser_who_posted(String user_who_posted) { this.user_who_posted = user_who_posted; }
//    public void setMedia_from_reel(File media_from_reel) { this.media_from_reel = media_from_reel; }
//    public void setMedia_versions(int media_versions) { this.media_versions = media_versions; }
//    public void setReel_pk(long reel_pk) { this.reel_pk = reel_pk; }
//}