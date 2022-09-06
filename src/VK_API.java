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
import com.vk.api.sdk.objects.video.SaveResult;
import com.vk.api.sdk.objects.video.responses.VideoUploadResponse;
import com.vk.api.sdk.objects.wall.responses.PostResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VK_API {

    private static VkApiClient vk = Vk.getVk();
    //private static GroupActor groupActor = Vk.getGroupActor();
    private static UserActor userActor = Vk.getUserActor();

    public static void postStory(InstagramStory[] stories) throws ClientException, ApiException {
        if (stories.length == 1) postStory(stories[0]);
        else {
            List<String> attachments =  new ArrayList<>();
            boolean main = false;
            String captionForWall = "Сториз\n\n";

            for (int i = 0; i < stories.length; i++) {
                InstagramStory story = stories[i];
                String user = story.getUser();
                if (user.equals(Constants.main_account_username)) main = true;
                int version = story.getVersion();
                File media = story.getMedia();
                String formattedDate = Constants.dateFormat(story.getDate());
                String caption = "Стори " + user;
                captionForWall += (user.equals(Constants.main_account_username) ? Constants.getMain_account_name : user)
                        + " (" + formattedDate + ")\n";
                String attachment = postMediaLink(user, version, media, caption, formattedDate);
                attachments.add(attachment);
            }
            if (main)
                captionForWall += "\n" + Constants.main_account_tags;
            else
                captionForWall += "\n" + Constants.other_tags;

            vk.wall().post(userActor).fromGroup(true)
                    .ownerId(-Constants.vk_group_id).signed(false)
                    .message(captionForWall)
                    .attachments(attachments).execute();
        }
    }

    public static void postStory(InstagramStory story) throws ClientException, ApiException {
        String attachment;
        String user = story.getUser();
        int version = story.getVersion();
        File media = story.getMedia();
        String formattedDate = Constants.dateFormat(story.getDate());
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

    public static String postPost(InstagramPost post, Set<Integer> indexes) throws ClientException, ApiException {
        List<String> attachments =  new ArrayList<>();
        String user = post.getUser();
        String formattedDate = Constants.dateFormat(post.getDate());

        int[] versions = post.getVersions();
        File[] medias = post.getMedia();
        String message_in_post = post.getCaption();
        String caption = "Из поста " + user + ": " + message_in_post;

        for (int i : indexes) {
            i--;
            int currentMediaVersion = versions[i];
            File currentMedia = medias[i];
            attachments.add(postMediaLink(user, currentMediaVersion, currentMedia, caption, formattedDate));
        }

        String translatedMessage = message_in_post;
        if (message_in_post.length() > 1 )
            translatedMessage = post.getTranslatedCaption();

        String postCaption = "Медиа из поста " + user + "\n\n" + translatedMessage +
                "\n\n" + Constants.other_tags;

        PostResponse postVK = vk.wall().post(userActor).fromGroup(true)
                .ownerId(-Constants.vk_group_id).signed(false)
                .message(postCaption).attachments(attachments).execute();
        return Constants.postLink + postVK.getPostId();
    }

    public static String postPost(InstagramPost post) throws ClientException, ApiException {
        List<String> attachments =  new ArrayList<>();
        String user = post.getUser();
        String formattedDate = Constants.dateFormat(post.getDate());
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
            translatedMessage = post.getTranslatedCaption();

        String postCaption;
        if (user.equals(Constants.main_account_username)) {
            postCaption = "Пост " + Constants.getMain_account_name + "\n\n" + translatedMessage
                    + "\n\n" + Constants.main_account_tags;

        } else {
            postCaption = "Пост " + user + "\n\n" + translatedMessage
                    + "\n\n" + Constants.other_tags;

        }
        PostResponse postVK = vk.wall().post(userActor).fromGroup(true)
                .ownerId(-Constants.vk_group_id).signed(false)
                .message(postCaption).attachments(attachments).execute();
        return Constants.postLink + postVK.getPostId();
    }

    public static void postReel(InstagramReel reel) throws ClientException, ApiException {
        String user = reel.getUser();
        String formattedDate = Constants.dateFormat(reel.getDate());
        File media = reel.getVideo();
        String message_in_reel = reel.getCaption();
        String caption = "Рилс " + user + ": " + message_in_reel;
        String attachment = postMediaLink(user, 2, media, caption, formattedDate);

        String translatedMessage = message_in_reel;
        if (translatedMessage.length() > 1)
            translatedMessage = reel.getTranslatedCaption();

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

    public static void postVideoFromYouTube(String youtubeUrl, String message) throws ClientException, ApiException {
        SaveResult videoSaveResult = vk.videos().save(userActor)
                .link(youtubeUrl).groupId(Constants.vk_group_id)
                .execute();
        try {
            URL url = new URL(videoSaveResult.getUploadUrl());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            InputStreamReader streamReader = new InputStreamReader(con.getInputStream());
            try (BufferedReader lineReader = new BufferedReader(streamReader)) {
                StringBuilder responseBody = new StringBuilder();
                String line;
                while ((line = lineReader.readLine()) != null) {
                    responseBody.append(line);
                }
                System.out.println(responseBody);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        vk.wall().post(userActor).fromGroup(true).ownerId(-Constants.vk_group_id).signed(false)
                .message(message).attachments(Constants.groupVideos + videoSaveResult.getVideoId())
                .execute();
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
}
