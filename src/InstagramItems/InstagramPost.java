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






//import java.io.File;
//import java.util.Date;
//
//public class InstagramPost {
//
//                      private int media_count;
//    private Date post_date;
//                     private String post_caption;
//    private String user_who_posted;
//                     private File[] media_from_post;
//                      private int[] media_versions_in_order;
//    private long post_pk;
//
//    public int getMedia_count() { return media_count; }
//    public Date getPost_date() { return post_date; }
//    public String getPost_caption() { return post_caption; }
//    public File[] getMedia_from_post() { return media_from_post; }
//    public String getUser_who_posted() { return user_who_posted; }
//    public int[] getMedia_versions_in_order() { return media_versions_in_order; }
//    public long getPost_pk() { return post_pk; }
//
//    public void setMedia_count(int media_count) { this.media_count = media_count; }
//    public void setPost_date(long getTaken) { this.post_date = new Date((long)getTaken*1000); }
//    public void setPost_caption(String caption) {this.post_caption = caption; }
//    public void setUser_who_posted(String username) {this.user_who_posted = username; }
//    public void setMedia_from_post(File[] medias) {this.media_from_post = medias; }
//    public void setMedia_versions_in_order(int[] versions) {this.media_versions_in_order = versions; }
//    public void setPost_pk(long post_pk) { this.post_pk = post_pk; }
//}