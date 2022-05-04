import Constants.Constants;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import Singletons.Vk;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.responses.PhotoUploadResponse;
import com.vk.api.sdk.objects.video.responses.VideoUploadResponse;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class VK_API {

    private static VkApiClient vk = Vk.getVk();
    //private static GroupActor groupActor = Vk.getGroupActor();
    private static UserActor userActor = Vk.getUserActor();

    public static void postStory(InstagramStory story) throws ClientException, ApiException {
        String attachment;
        String user = story.getUser();
        int version = story.getVersion();
        File media = story.getMedia();
        String formattedDate = dateFormat(story.getDate());
        String caption = "Стори " + user;
        attachment = postMediaLink(user, version, media, caption, formattedDate);

        String postCaption;
        if (user.equals(Constants.main_account_username)) {
            postCaption = "Стори " + Constants.getMain_account_name +
                    " (" + formattedDate + ")\n\n" + Constants.main_account_tags;

        } else {
            postCaption = "Стори " + user +
                    " (" + formattedDate + ")\n\n" + Constants.other_tags;
        }
        vk.wall().post(userActor).fromGroup(true)
                .ownerId(-Constants.vk_group_id).signed(false)
                .message(postCaption)
                .attachments(attachment).execute();
    }

    public static void postPost(InstagramPost post) throws ClientException, ApiException {
        List<String> attachments =  new ArrayList<>();
        String user = post.getUser();
        String formattedDate = dateFormat(post.getDate());
        int counter = post.getMediaCount();
        int[] versions = post.getVersions();
        File[] medias = post.getMedia();
        String message_in_post = post.getCaption();
        String caption = "Пост " + user + ": " + message_in_post;

        for (int i = 0; i < counter; i++) {
            int currentMediaVersion = versions[i];
            File currentMedia = medias[i];
            attachments.add(postMediaLink(user, currentMediaVersion, currentMedia, caption, formattedDate));
        }

        String translatedMessage = message_in_post;
        if (message_in_post.length() > 1 )
            translatedMessage = Translator.translateTextToRussian(message_in_post).replace("\\n", "\n");

        String postCaption;
        if (user.equals(Constants.main_account_username)) {
            postCaption = "Пост " + Constants.getMain_account_name + "\n\n" + translatedMessage
                    + "\n\n" + Constants.main_account_tags;

        } else {
            postCaption = "Пост " + user + "\n\n" + translatedMessage
                    + "\n\n" + Constants.other_tags;

        }
        vk.wall().post(userActor).fromGroup(true)
                .ownerId(-Constants.vk_group_id).signed(false)
                .message(postCaption).attachments(attachments).execute();
    }

    public static void postReel(InstagramReel reel) throws ClientException, ApiException {
        String user = reel.getUser();
        String formattedDate = dateFormat(reel.getDate());
        File media = reel.getVideo();
        String message_in_reel = reel.getCaption();
        String caption = "Рилс " + user + ": " + message_in_reel;
        String attachment = postMediaLink(user, 2, media, caption, formattedDate);

        String translatedMessage = message_in_reel;
        if (translatedMessage.length() > 1)
            translatedMessage = Translator.translateTextToRussian(message_in_reel).replace("\\n", "\n");;

        String postCaption;
        if (user.equals(Constants.main_account_username)) {
            postCaption = "Рилс " + Constants.getMain_account_name + "\n\n" + translatedMessage
                    + "\n\n" + Constants.main_account_tags;

        } else {
            postCaption = "Рилс " + user + "\n\n" + translatedMessage
                    + "\n\n" + Constants.other_tags;
        }

        vk.wall().post(userActor).fromGroup(true)
                .ownerId(-Constants.vk_group_id).signed(false)
                .message(postCaption).attachments(attachment).execute();
    }

    private static String postMediaLink (String user, int version, File media,
                                         String caption, String date) {
        String attachment = null;
        if (version == 1) {
            //photo
            int photoAlbumID;
            if (user.equals(Constants.main_account_username))
                photoAlbumID = Constants.photo_album_main;
            else
                photoAlbumID = Constants.photo_album_others;

            try {
                String photoInAlbumLink = vk.photos().getUploadServer(userActor)
                        .groupId(Constants.vk_group_id).albumId(photoAlbumID).execute().getUploadUrl();

                PhotoUploadResponse response = vk.upload().photo(photoInAlbumLink, media).execute();

                List<Photo> photoList = vk.photos().save(userActor).photosList(response.getPhotosList())
                        .groupId(Constants.vk_group_id).albumId(photoAlbumID)
                        .server(response.getServer()).hash(response.getHash())
                        .caption(caption + "\n\n" + date).execute();
                attachment = Constants.groupPhotos + photoList.get(0).getId();
            } catch (ApiException | ClientException e) {
                e.printStackTrace();
            }
        }
        else {
            //video
            try {
                String nameOfVideo = user + " " + date;
                String videoLink = vk.videos().save(userActor)
                        .name(nameOfVideo).description(caption)
                        .groupId(Constants.vk_group_id).repeat(true)
                        .execute().getUploadUrl();

                VideoUploadResponse response = vk.upload().video(videoLink, media).execute();
                attachment = Constants.groupVideos + response.getVideoId();
            } catch (ApiException  | ClientException e) {
                e.printStackTrace();
            }
        }
        return attachment;
    }

    private static String dateFormat (Date date) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm z");
        format.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        return format.format(date);
    }
}
