import Constants.Constants;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import Singletons.Instagram;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.models.feed.Reel;
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
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsActionRequest;
import com.github.instagram4j.instagram4j.requests.friendships.FriendshipsShowRequest;
import com.github.instagram4j.instagram4j.requests.media.MediaInfoRequest;
import com.github.instagram4j.instagram4j.requests.media.MediaSeenRequest;
import com.github.instagram4j.instagram4j.requests.users.UsersUsernameInfoRequest;
import com.github.instagram4j.instagram4j.responses.IGResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserResponse;
import com.github.instagram4j.instagram4j.responses.feed.FeedUserStoryResponse;
import com.github.instagram4j.instagram4j.responses.friendships.FriendshipsShowResponse;
import com.github.instagram4j.instagram4j.responses.media.MediaInfoResponse;
import com.github.instagram4j.instagram4j.responses.users.UserResponse;
import com.github.instagram4j.instagram4j.utils.IGUtils;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class INST_API {

    private static long main_account_pk = 0;
    private static IGClient instagram = Instagram.getInstagram();
    private static LinkedHashMap<String, List<ReelMedia>> stories;

    /**     START     **/
    public static void start() {
        System.out.println("Method: start - beginning");
        if (Constants.main_account_username.equals("munchinthepool")) {
            main_account_pk = 1819739099;
        } else {
            getUserPk(Constants.main_account_username);
        }

        stories = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<ReelMedia>> eldest) {
                return this.size() > 50;
            }
        };

        startWithoutTasks();

        System.out.println("Method: start - ending");
    }

    private static void startWithoutTasks() {
        Constants.post5latestPk.put(Constants.latestPostPk, new Date());
        Constants.story24HoursPk.put(Constants.latestStoryPk, new Date());
    }

    private static void startWithTasks() {
    /*    List<TimelineMedia> posts = getPostsByUserPk(main_account_pk);
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
            Constants.story24HoursPk.put(Constants.latestStoryPk, new Date());*/
    }

    /**     CHECKS     **/
    public static String[] checkForPostUpdates() throws ClientException, ApiException {
        System.out.println("Method: checkForPostUpdates");
        System.out.println("Method: checkForPostUpdates - getPostsByUserPk");
        List<TimelineMedia> posts = getPostsByUserPk(main_account_pk);
        if (posts == null) return null;

        ArrayList<InstagramPost> notUpdatedPosts = new ArrayList<>();
        int postsAmount = 3;
        if (posts.size() < 4) postsAmount = posts.size()-1;

        for (int i = postsAmount; i >= 0; i--) {
            TimelineMedia currentPost = posts.get(i);
            long currentPostPk = currentPost.getPk();
            if (!Constants.post5latestPk.containsKey(currentPostPk)) {
                InstagramPost newPost = createInstagramPost(currentPost);
                if (newPost.getUser() == null)
                    newPost = getUserPostByPk(currentPostPk);
                notUpdatedPosts.add(newPost);
                Constants.post5latestPk.put(newPost.getPk(), newPost.getDate());
            }
        }
        System.out.println("Method: checkForPostUpdates - ending");
        String[] answer = new String[2];
        if (notUpdatedPosts.size() >= 1) {
            for (InstagramPost update : notUpdatedPosts)
                answer[1] = TELEGRAM_API.postingPostNotification(update, "");
            answer[0] = notUpdatedPosts.get(notUpdatedPosts.size()-1).getPk() + "";
            return answer;
        }

        return null;
    }

    public static String[] checkForStoryUpdates() throws ClientException, ApiException {
        System.out.println("Method: checkForStoryUpdates");
        if (Constants.story24HoursPk.isEmpty())
            Constants.story24HoursPk.put(Constants.latestStoryPk, new Date());
        List<ReelMedia> stories = getStoriesByUserPk(main_account_pk);
        ArrayList<InstagramStory> notUpdatedStories = new ArrayList<>();
        if (stories == null) return null;

        sendRequest(new MediaSeenRequest(stories));
        for (int i = 0; i < stories.size(); i++) {
            long currentStoryPk = stories.get(i).getPk();
            if (!Constants.story24HoursPk.containsKey(currentStoryPk)) {
                InstagramStory currentStory = createInstagramStory(stories.get(i));
                if (currentStory.getUser() == null)
                    currentStory = getUserStoryByPk(currentStoryPk);
                Constants.story24HoursPk.put(currentStoryPk, currentStory.getDate());
                notUpdatedStories.add(currentStory);
            }
        }
        System.out.println("Method: checkForStoryUpdates - ending");
        String[] answer = new String[2];
        if (notUpdatedStories.size() >= 1) {
            answer[1] = TELEGRAM_API.postingStoriesNotification(notUpdatedStories.toArray(InstagramStory[]::new), "");
            answer[0] = notUpdatedStories.get(notUpdatedStories.size()-1).getPk() + "";
            return answer;
        }
        return null;
    }

    /**     USERS     **/
    private static long getUserPk(String username)  {
        System.out.println("Method: getUserPk");
        IGRequest request = new UsersUsernameInfoRequest(username);
        UserResponse response = (UserResponse) sendRequest(request);
        long userPk = response.getUser().getPk();
        return userPk;
    }

    /**     POSTS     **/
    public static InstagramPost getUserPostByLink(String link) {
        String postCode = link.split("/")[4];
        System.out.println("Method: getUserPostByLink");
        return getUserPostByPk(postCode);
    }

    private static InstagramPost getUserPostByPk (long pk) {
        IGRequest request =  new MediaInfoRequest(pk + "");
        MediaInfoResponse response = (MediaInfoResponse) sendRequest(request);
        TimelineMedia post = response.getItems().get(0);
        return createInstagramPost(post);
    }

    private static InstagramPost getUserPostByPk (String postCode) {
        IGRequest request =  new MediaInfoRequest(IGUtils.fromCode(postCode) + "");
        MediaInfoResponse response = (MediaInfoResponse) sendRequest(request);
        TimelineMedia post = response.getItems().get(0);
        return createInstagramPost(post);
    }

    private static List<TimelineMedia> getPostsByUserPk(long pk) {
        System.out.println("Method: getPostsByUserPk");
        IGRequest request = new FeedUserRequest(pk);
        FeedUserResponse response = (FeedUserResponse) sendRequest(request);
        if (response.getItems() != null && !response.getItems().isEmpty())
            return response.getItems();
        return null;
    }

    /**     STORIES     **/
    public static InstagramStory getUserStoryByLink(String link) {
        /*String storyCode = link.split("/")[5].split("\\?")[0];
        System.out.println("Method: getUserStoryByLink");
        return getUserStoryByPk(storyCode);*/
        String username = link.split("/")[4];
        long storyCode = Long.parseLong(link.split("/")[5].split("\\?")[0]);

        //todo bug fix
        if (stories.containsKey(username)) {
            for (ReelMedia reel : stories.get(username)) {
                if (reel.getPk() == storyCode) {
                    InstagramStory story = createInstagramStory(reel);
                    if (story != null) return story;
                    break;
                }
            }
        }

        long userPk = getUserPk(username);

        IGRequest request = new FeedUserStoryRequest(userPk);
        FeedUserStoryResponse response = (FeedUserStoryResponse) sendRequest(request);

        List<ReelMedia> storiesList = response.getReel().getItems();
        stories.put(username, storiesList);

        InstagramStory story;

        for (ReelMedia reel : storiesList) {
            if (reel.getPk() == storyCode) {
                story = createInstagramStory(reel);
                if (story == null)
                    return getUserStoryByLink(link);
                else
                    return story;
            }
        }
        return null;
    }

    private static InstagramStory getUserStoryByPk(String pk) {
        IGRequest request = new MediaInfoRequest(pk + "");
        MediaInfoResponse response = (MediaInfoResponse) sendRequest(request);
        TimelineMedia reel = response.getItems().get(0);
        //todo seen story
        //new MediaSeenRequest(response.getItems());
        return createInstagramStory(reel);
    }

    private static InstagramStory getUserStoryByPk(long pk) {
        return getUserStoryByPk(pk + "");
    }

    private static List<ReelMedia> getStoriesByUserPk(long pk) {
        System.out.println("Method: getStoriesByUserPk");
        IGRequest request = new FeedUserStoryRequest(pk);
        FeedUserStoryResponse response = (FeedUserStoryResponse) sendRequest(request);
        if (response.getReel() != null) {
            Reel reel = response.getReel();
            if (!response.getReel().getItems().isEmpty()) {
                List<ReelMedia> items = reel.getItems();
                if (reel.getSeen() == 0) {
                    sendRequest(new MediaSeenRequest(items));
                    return items;
                }
            }
        }
//        if (response.getReel() != null && !response.getReel().getItems().isEmpty())
//            return response.getReel().getItems();
        return null;
    }

    /**     REELS     **/
    public static InstagramReel getUserReelByLink(String link) {
        String reelCode = link.split("/")[4];
        IGRequest request = new MediaInfoRequest(IGUtils.fromCode(reelCode) + "");
        MediaInfoResponse response = (MediaInfoResponse) sendRequest(request);
        TimelineMedia reel = response.getItems().get(0);
        return createInstagramReel(reel);
    }

    /**     INSTAGRAM ITEMS     **/
    private static InstagramPost createInstagramPost(TimelineMedia media) {
        InstagramPost instPost = new InstagramPost();

        String username = media.getUser().getUsername();
        instPost.setUser(username);
        long date = media.getTaken_at();
        instPost.setDate(date);
        String postCode = media.getCode();
        instPost.setPk(media.getPk());

        if (username.equals(Constants.main_account_username)) {
            Constants.post5latestPk.put(media.getPk(), new Date());
        }

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
        String username = reel.getUser().getUsername();
        if (username == null) return null;
        story.setUser(username);
        int mediaType = Integer.parseInt(reel.getMedia_type());
        story.setVersion(mediaType);

        if (username.equals(Constants.main_account_username)) {
            Constants.story24HoursPk.put(pk, new Date());
        }

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

    private static InstagramStory createInstagramStory(TimelineMedia media) {
        System.out.println("Method: createInstagramStory - beginning");
        InstagramStory story = new InstagramStory();
        story.setDate(media.getTaken_at());
        long pk = media.getPk();
        story.setPk(pk);
        String username = media.getUser().getUsername();
        story.setUser(username);
        int mediaType = Integer.parseInt(media.getMedia_type());
        story.setVersion(mediaType);

        if (username.equals(Constants.main_account_username)) {
            Constants.story24HoursPk.put(pk, new Date());
        }

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

    private static InstagramReel createInstagramReel(TimelineMedia media) {
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

    /**     RANDOM TASKS     **/
    private static String[] randomAccounts = new String[] {
            "doverchivost", "terimlxx", "puppyradio", "paxxword97", "cifika_",
            "bizzionary", "eztag_", "maalib", "xocktar", "qqqtheqqq", "ann_darc", "khyosangjin",
            "hashblanccoa", "sogumm", "nucksal", "kiff_vinxen", "deantrbl", "drunkentigerjk", "hunjiya",
            "spotify", "nigo", "moresojuplease", "jungjukjae", "miyayeah", "a24", "netflixkr", "donmamsuki",
            "dmofxxkinark", "iamtouchthesky", "skeptagram", "free_miu", "layon_e", "cakeshopseoul",
            "jiwonstein", "lalalalisa_m", "feliciathegoat", "hypebeast", "yoon_ambush", "udtbro", "heizeheize",
            "berrics", "xodambi", "honjowolf", "jooyong", "dindinem", "anyovann", "jayho",
            "mashacreate.nails", "huskyekb", "tul_pakorn", "bb0un", "maxiiin_", "navalny"
    };

    public static void randomTask() {
        try {
            //разннобразить: рандомные действия при вызове
            //вызвать как TimerTask
            int random = new Random().nextInt(8);
            switch (random) {
                case 1:
                    new FeedTimelineRequest().execute(instagram)
                            .thenAccept(response -> {
                                response.getFeed_items();
                            }).join();
                    break;
                case 2:
                    new AccountsCurrentUserRequest().execute(instagram).join();
                    break;
                case 3:
                    new DiscoverTopicalExploreRequest().execute(instagram).join();
                    break;
                case 4:
                    instagram.actions().users().findByUsername("instagram").join();
                    break;
                case 5:
                    instagram.actions().account().currentUser().get().getUser().getFollower_count();
                    break;
                case 6:
                    new AccountsCurrentUserRequest().execute(instagram).join();
                    break;
                case 7:
                    int random1 = new Random().nextInt(5);
                    int random2 = new Random().nextInt(randomAccounts.length) - 1;
                    //IGRequest request = new UsersUsernameInfoRequest(randomAccounts[random2]);
                    long respUserPk = getUserPk(randomAccounts[random2]);
                    switch (random1) {
                        case 1:
                            getPostsByUserPk(respUserPk);
                            break;
                        case 2:
                            getStoriesByUserPk(respUserPk);
                            break;
                        case 3:
                            getStoriesByUserPk(respUserPk);
                            getPostsByUserPk(respUserPk);
                            break;
                        case 4:
/*                            IGRequest request = new UsersUsernameInfoRequest(ra);
                            UserResponse response = (UserResponse) sendRequest(request);
                            long userPk = response.getUser();
                            Friendship*/
                            boolean following = ((FriendshipsShowResponse) sendRequest((IGRequest) new FriendshipsShowRequest(respUserPk)))
                                    .getFriendship().isFollowing();
                            if (!following) {
                                IGRequest follow = new FriendshipsActionRequest(respUserPk, FriendshipsActionRequest.FriendshipsAction.CREATE);
                                sendRequest(follow);
                            }
                            break;
                    }
                    break;
            }
        }
        catch (Exception e) {}
    }

    /**     REQUESTS     **/
    private static IGResponse sendRequest(IGRequest<IGResponse> request) {
        IGResponse response = null;
        int status = 0;
        int counter = 0;
        while (status != 200) {
            try {
                Thread.sleep(randomSleep(), randomSleep());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //response = request.execute(instagram).join();//instagram.sendRequest(request).join();
            response = instagram.sendRequest(request).join();
            status = response.getStatusCode();

            counter++;
            if (counter > 50) {
                System.out.println("TOO MUCH");
                TELEGRAM_API.notifyAdmins("Проблема с получением ответов от инстаграма. Я отключаюсь.");
                System.exit(-1);
            }
        }
        return response;
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
        try (InputStream in = url.openStream()){
            Files.copy(in, Path.of(file), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*try (BufferedInputStream bis = new BufferedInputStream(url.openStream());
             FileOutputStream fis = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                fis.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    private static int randomSleep() {
        return 5000 + new Random().nextInt(15000);
    }
}
