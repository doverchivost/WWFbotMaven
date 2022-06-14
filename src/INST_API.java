import Constants.Constants;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import Singletons.Instagram;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.exceptions.IGResponseException;
import com.github.instagram4j.instagram4j.models.media.reel.ReelImageMedia;
import com.github.instagram4j.instagram4j.models.media.reel.ReelMedia;
import com.github.instagram4j.instagram4j.models.media.reel.ReelVideoMedia;
import com.github.instagram4j.instagram4j.models.media.timeline.*;
import com.github.instagram4j.instagram4j.requests.IGRequest;
import com.github.instagram4j.instagram4j.requests.accounts.AccountsCurrentUserRequest;
import com.github.instagram4j.instagram4j.requests.discover.DiscoverTopicalExploreRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedTimelineRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserRequest;
import com.github.instagram4j.instagram4j.requests.feed.FeedUserStoryRequest;
import com.github.instagram4j.instagram4j.requests.media.MediaInfoRequest;
import com.github.instagram4j.instagram4j.requests.users.UsersUsernameInfoRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserStoryResponse;
import com.github.instagram4j.instagram4j.responses.media.MediaInfoResponse;
import com.github.instagram4j.instagram4j.responses.users.UserResponse;
import com.github.instagram4j.instagram4j.utils.IGUtils;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class INST_API {

    private static long main_account_pk = 0;
    private static IGClient instagram = Instagram.getInstagram();

    public static void start() {
        System.out.println("Method: start - beginning");

        main_account_pk = getUserPk(Constants.main_account_username);
        //ДЛЯ ВОНДЖЕ:
        //main_account_pk = 1819739099;

        List<TimelineMedia> posts = getPostsByUserPk(main_account_pk);
        if (posts != null) {
            int postCount = 4;
            if (posts.size() < 5) postCount = posts.size() - 1;
            for (int i = postCount; i >= 0; i--)
                Constants.post5latestPk.put(posts.get(i).getPk(), new Date());
            Constants.latestPostPk = posts.get(0).getPk();
        } else {
            Constants.latestPostPk = 0;
            Constants.post5latestPk.put(Constants.latestPostPk, new Date());
        }
        
        if (Constants.latestPostPk != 0)
            Constants.post5latestPk.put(Constants.latestPostPk, new Date());

        List<ReelMedia> stories = getStoriesByUserPk(main_account_pk);
        if (stories != null) {
            for (ReelMedia story : stories)
                Constants.story24HoursPk.put(story.getPk(), new Date(story.getTaken_at() * 1000));
            Constants.latestStoryPk = stories.get(stories.size() - 1).getPk();
        } else
            Constants.latestStoryPk = 0;

        if (Constants.latestStoryPk != 0)
            Constants.story24HoursPk.put(Constants.latestStoryPk, new Date());

        System.out.println("Method: start - ending");
    }

    private static long getUserPk(String username)  {
        System.out.println("Method: getUserPk");
        IGRequest request = new UsersUsernameInfoRequest(username);
        UserResponse response = (UserResponse) sendRequest(request);
        long userPk = response.getUser().getPk();
        return userPk;
    }

    private static List<TimelineMedia> getPostsByUserPk(long pk) {
        System.out.println("Method: getPostsByUserPk");
        IGRequest request = new FeedUserRequest(pk);
        FeedUserResponse response = (FeedUserResponse) sendRequest(request);
        if (response.getItems() != null && !response.getItems().isEmpty())
            return response.getItems();
        return null;
    }

    private static List<ReelMedia> getStoriesByUserPk(long pk) {
        System.out.println("Method: getStoriesByUserPk");
        IGRequest request = new FeedUserStoryRequest(pk);
        FeedUserStoryResponse response = (FeedUserStoryResponse) sendRequest(request);
        if (response.getReel() != null && !response.getReel().getItems().isEmpty())
            return response.getReel().getItems();
        return null;
    }

    public static InstagramPost checkForPostUpdates() throws ClientException, ApiException {
        System.out.println("Method: checkForPostUpdates");
        System.out.println("Method: checkForPostUpdates - getPostsByUserPk");
        List<TimelineMedia> posts = getPostsByUserPk(main_account_pk);
        if (posts == null) return null;

        ArrayList<InstagramPost> notUpdatedPosts = new ArrayList<>();
        int postsAmount = 3;
        if (posts.size() < 4) postsAmount = posts.size()-1;

        for (int i = postsAmount; i >= 0; i--) {
            TimelineMedia currentPost = posts.get(i);
            if (!Constants.post5latestPk.containsKey(currentPost.getPk())) {
                InstagramPost newPost = createInstagramPost(currentPost);
                while (newPost.getUser() == null)
                    newPost = createInstagramPost(currentPost);
                notUpdatedPosts.add(newPost);
                Constants.post5latestPk.put(newPost.getPk(), newPost.getDate());
            }
        }
        System.out.println("Method: checkForPostUpdates - ending");
        if (notUpdatedPosts.size() >= 1) {
            for (int i = 0; i < notUpdatedPosts.size() - 1; i++)
                TELEGRAM_API.postingPostNotification(notUpdatedPosts.get(i));//VK_API.postPost(notUpdatedPosts.get(i));
            return notUpdatedPosts.get(notUpdatedPosts.size()-1);
        }

        return null;
    }

    public static InstagramStory checkForStoryUpdates() throws ClientException, ApiException {
        System.out.println("Method: checkForStoryUpdates");
        if (Constants.story24HoursPk.isEmpty())
            Constants.story24HoursPk.put(Constants.latestStoryPk, new Date());
        List<ReelMedia> stories = getStoriesByUserPk(main_account_pk);
        ArrayList<InstagramStory> notUpdatedStories = new ArrayList<>();
        if (stories == null) return null;

        for (int i = 0; i < stories.size(); i++) {
            long currentStoryPk = stories.get(i).getPk();
            if (!Constants.story24HoursPk.containsKey(currentStoryPk)) {
                InstagramStory currentStory = createInstagramStory(stories.get(i));
                while (currentStory.getUser() == null)
                    currentStory = createInstagramStory(stories.get(i));
                Constants.story24HoursPk.put(currentStoryPk, currentStory.getDate());
                notUpdatedStories.add(currentStory);
            }
        }
        System.out.println("Method: checkForStoryUpdates - ending");
        if (notUpdatedStories.size() >= 1) {
            for (int i = 0; i < notUpdatedStories.size() - 1; i++)
                TELEGRAM_API.postingStoryNotification(notUpdatedStories.get(i));//VK_API.postStory(notUpdatedStories.get(i));
            return notUpdatedStories.get(notUpdatedStories.size()-1);
        }

        return null;
    }

    private static InstagramPost createInstagramPost(TimelineMedia media) {
        InstagramPost instPost = new InstagramPost();

        String username = media.getUser().getUsername();
        instPost.setUser(username);
        long date = media.getTaken_at();
        instPost.setDate(date);
        String postCode = media.getCode();
        instPost.setPk(media.getPk());

        String caption = "";
        try {
            if (media.getCaption().getText() != null) {
                caption = spaceAfterHashtagsAndMentions(media.getCaption().getText());
            }
        } catch (NullPointerException e) {
            //there was no text in the post
        }
        instPost.setCaption(caption);
        instPost.setTranslatedCaption(spaceAfterHashtagsAndMentions(Translator.translateTextToRussian(caption)));

        int type = Integer.parseInt(media.getMedia_type());
        if (type == 1) {
            //photo
            TimelineImageMedia mediaImage = ((TimelineImageMedia) media);
            instPost.setMediaCount(1);
            String url = mediaImage.getImage_versions2().getCandidates().get(0).getUrl();
            File f = convertURLintoFILE(url, postCode, type);
            instPost.setMedia(new File[]{f});
            instPost.setVersions(new int[]{1});
        }
        else if (type == 2) {
            //video
            TimelineVideoMedia mediaVideo = ((TimelineVideoMedia) media);
            instPost.setMediaCount(1);
            String url = mediaVideo.getVideo_versions().get(0).getUrl();
            File f = convertURLintoFILE(url, postCode, type);
            instPost.setMedia(new File[]{f});
            instPost.setVersions(new int[]{2});
        }
        else {
            //carousel
            TimelineCarouselMedia mediaCarousel = ((TimelineCarouselMedia) media);
            List<CarouselItem> carouselItemList = mediaCarousel.getCarousel_media();
            int counter = carouselItemList.size();
            instPost.setMediaCount(counter);
            File[] medias = new File[counter];
            int[] versions = new int[counter];

            for (int i = 0; i < counter; i++) {
                CarouselItem carouselItem = carouselItemList.get(i);
                type = Integer.parseInt(carouselItem.getMedia_type());
                String url = null;
                if (type == 1) {
                    //photo
                    ImageCarouselItem imageCarousel = ((ImageCarouselItem) carouselItem);
                    url = imageCarousel.getImage_versions2().getCandidates().get(0).getUrl();
                }
                else if (type == 2) {
                    //video
                    VideoCarouselItem videoCarousel = ((VideoCarouselItem) carouselItem);
                    url = videoCarousel.getVideo_versions().get(0).getUrl();
                }
                else {
                    System.out.println("UKNOWN MEDIA TYPE IN CAROUSEL");
                }
                File f = convertURLintoFILE(url, postCode + i, type);
                versions[i] = type;
                medias[i] = f;
            }
            instPost.setVersions(versions);
            instPost.setMedia(medias);
        }
        return instPost;
    }

    private static InstagramStory createInstagramStory(ReelMedia reel) {
        System.out.println("Method: createInstagramStory - beginning");
        InstagramStory story = new InstagramStory();

        story.setDate(reel.getTaken_at());
        long pk = reel.getPk();
        story.setPk(pk);
        story.setUser(reel.getUser().getUsername());
        int mediaType = Integer.parseInt(reel.getMedia_type());
        story.setVersion(mediaType);
        try {
            if (mediaType == 1) {
                //photo
                ReelImageMedia reelImage = ((ReelImageMedia) reel);
                String url = reelImage.getImage_versions2().getCandidates().get(0).getUrl();
                story.setMedia(convertURLintoFILE(url, reel.getCode(), 1));
            } else if (mediaType == 2) {
                //video
                ReelVideoMedia reelVideo = ((ReelVideoMedia) reel);
                String url = reelVideo.getVideo_versions().get(0).getUrl();
                story.setMedia(convertURLintoFILE(url, reel.getCode(), 2));
            }
        }
        catch (ClassCastException e) {
            System.out.println("Wrong casting in story!");
            e.printStackTrace();
        }
        System.out.println("Method: getInstagramStory - ending");
        return story;
    }

    public static InstagramPost getUserPostByLink(String link) {
        String postCode = link.split("/")[4];
        System.out.println("Method: getUserPostByLink - response");
        IGRequest request =  new MediaInfoRequest(IGUtils.fromCode(postCode) + "");
        MediaInfoResponse response = (MediaInfoResponse) sendRequest(request);
        TimelineMedia post = response.getItems().get(0);
        System.out.println("Method: getUserPostByLink - ending");
        return createInstagramPost(post);
    }

    public static InstagramStory getUserStoryByLink(String link) {
        String storyCode = link.split("/")[5].split("\\?")[0];
        System.out.println("Method: getUserStoryByLink");
        IGRequest request = new MediaInfoRequest(storyCode + "");
        MediaInfoResponse response = (MediaInfoResponse) sendRequest(request);
        TimelineMedia reel = response.getItems().get(0);
        return getInstagramStory(reel);
    }

    public static InstagramReel getUserReelByLink(String link) {
        String reelCode = link.split("/")[4];
        IGRequest request = new MediaInfoRequest(IGUtils.fromCode(reelCode) + "");
        MediaInfoResponse response = (MediaInfoResponse) sendRequest(request);
        TimelineMedia reel = response.getItems().get(0);
        return getInstagramReel(reel);
    }

    private static InstagramReel getInstagramReel(TimelineMedia media) {
        InstagramReel instReel = new InstagramReel();
        instReel.setDate(media.getTaken_at());
        long pk = media.getPk();
        instReel.setPk(pk);
        instReel.setUser(media.getUser().getUsername());
        int mediaType = Integer.parseInt(media.getMedia_type());

        String caption = "";
        try {
            if (media.getCaption().getText() != null) {
                caption = spaceAfterHashtagsAndMentions(media.getCaption().getText());
            }
        } catch (NullPointerException e) {
            //there was no text in the post
        }
        instReel.setCaption(caption);
        instReel.setTranslatedCaption(spaceAfterHashtagsAndMentions(Translator.translateTextToRussian(caption)));

        try {
            if (mediaType == 2) {
                //video
                TimelineVideoMedia reelVideo = ((TimelineVideoMedia) media);
                String url = reelVideo.getVideo_versions().get(0).getUrl();
                instReel.setVideo(convertURLintoFILE(url, media.getCode(), 2));
            }
            else {
                System.out.println("UKNOWN MEDIA VERSION IN REELS");
            }
        }
        catch (ClassCastException e) {
            System.out.println("Wrong casting in story!");
            e.printStackTrace();
        }
        System.out.println("Method: getInstagramStory - ending");
        return instReel;
    }

    private static String spaceAfterHashtagsAndMentions(String caption) {
        return caption.replace("@", "@ ").replace("#", "# ");
    }

    private static InstagramStory getInstagramStory(TimelineMedia media) {
        System.out.println("Method: createInstagramStory - beginning");
        InstagramStory story = new InstagramStory();
        story.setDate(media.getTaken_at());
        long pk = media.getPk();
        story.setPk(pk);
        story.setUser(media.getUser().getUsername());
        int mediaType = Integer.parseInt(media.getMedia_type());
        story.setVersion(mediaType);
        try {
            if (mediaType == 1) {
                //photo
                TimelineImageMedia reelImage = ((TimelineImageMedia) media);
                String url = reelImage.getImage_versions2().getCandidates().get(0).getUrl();
                story.setMedia(convertURLintoFILE(url, media.getCode(), 1));
            } else if (mediaType == 2) {
                //video
                TimelineVideoMedia reelVideo = ((TimelineVideoMedia) media);
                String url = reelVideo.getVideo_versions().get(0).getUrl();
                story.setMedia(convertURLintoFILE(url, media.getCode(), 2));
            }
        }
        catch (ClassCastException e) {
            System.out.println("Wrong casting in story!");
            e.printStackTrace();
        }
        System.out.println("Method: getInstagramStory - ending");
        return story;
    }

    private static IGResponse sendRequest(IGRequest<IGResponse> request) {
        IGResponse response = null;
        //CompletableFuture<IGResponse> req = null;
        int status = 0;
        int counter = 0;
        while (status != 200) {
            try {
                Thread.sleep(randomSleep(), randomSleep());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            response = request.execute(instagram).join();
            status = response.getStatusCode();
/*
            try {
                ///response = request.execute(instagram).join();//Bot.instagram.sendRequest(request).join();
                req = instagram.sendRequest(request);
                response = req.join();
                status = response.getStatusCode();

            }
            catch (Exception e) {
                e.printStackTrace();
                TELEGRAM_API.notifyAdmins(e.getMessage());
                try {
                    req.get().getError_type();
                }
                catch (Exception ex) {
                    TELEGRAM_API.notifyAdmins(req);
                }

            }*/

            counter++;
            if (counter > 50) {
                System.out.println("TOO MUCH");
                TELEGRAM_API.notifyAdmins("Проблема с получением ответов от инстаграма. Я отключаюсь.");
                System.exit(-1);
            }
        }
        return response;
    }

    public static void randmonTask() {
        try {
            //разннобразить: рандомные действия при вызове
            //вызвать как TimerTask
            int random = new Random().nextInt(5);
            instagram.actions().account().currentUser().get().getUser().getFollower_count();
            switch (random) {
                case 1:
                    new FeedTimelineRequest().execute(instagram)
                            .thenAccept(response -> {
                                response.getFeed_items();
                            }).join();
                case 2:
                    new AccountsCurrentUserRequest().execute(instagram).join();
                case 3:
                    new UsersUsernameInfoRequest("doverchivost").execute(instagram).join();
                case 4:
                    new DiscoverTopicalExploreRequest().execute(instagram).join();
                case 5:
                    instagram.actions().users().findByUsername("instagram").join();
            }
        }
        catch (Exception e) {}
    }

    private static File convertURLintoFILE(String url, String pk, int version) {
        File file = null;
        if (version == 1) file = new File("media\\" + pk + ".jpg");
        else if (version == 2) file = new File("media\\" + pk + ".mp3");
        else {
            System.out.println("WRONG VERSION");
            System.exit(0);
        }
        downloadUsingStream(url, file.getAbsolutePath());
        return file;
    }

    private static void downloadUsingStream(String urlStr, String file) {
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try (BufferedInputStream bis = new BufferedInputStream(url.openStream());
             FileOutputStream fis = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                fis.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int randomSleep() {
        return 5000 + new Random().nextInt(15000);
    }
}
