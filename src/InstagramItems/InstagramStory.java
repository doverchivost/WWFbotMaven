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







//
//import java.io.File;
//import java.util.Date;
//
//public class InstagramStory {
//
//    private Date story_date; //Fri Jul 02 18:17:42 YEKT 2021
//    private String user_who_posted;
//                          private File media_from_story;
//                          private int media_versions_of_story;
//    //1 - photo, 2 - video
//    private long story_pk;
//
//    public Date getStory_date() { return story_date; }
//    public String getUser_who_posted() { return user_who_posted; }
//    public File getMedia_from_story() { return media_from_story; }
//    public int getMedia_versions_of_story() { return media_versions_of_story; }
//    public long getStory_pk() { return story_pk; }
//
//    public void setStory_date(long takenAt) {
//        this.story_date = new Date((long)takenAt*1000);
//    }
//    public void setUser_who_posted(String user) { this.user_who_posted = user; }
//    public void setMedia_from_story(File media) {this.media_from_story = media; }
//    public void setMedia_versions_of_story(int version) {this.media_versions_of_story = version; }
//    public void setStory_pk(long story_pk) { this.story_pk = story_pk; }
//}