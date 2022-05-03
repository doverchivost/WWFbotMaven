import InstagramItems.InstagramPost;
import InstagramItems.InstagramStory;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.utils.IGChallengeUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Bot {

    static IGClient instagram;
    static VkApiClient vk;
    static GroupActor groupActor;
    static UserActor userActor;
    private static TelegramBot bot;

    static long latestPostPk;
    static long latestStoryPk;
    static PassiveExpiringMap<Long, Date> story24HoursPk;
    static LinkedHashMap<Long, Date> post5latestPk;

    static Timer timerPosts = new Timer();
    static Timer timerStories = new Timer();

    static String sixDigits = "";
    static boolean hasDigits = false;

    static File storyPkFile = new File("pk\\story.pk");
    static File postPkFile = new File("pk\\post.pk");

    public static void main(String[] args) throws IOException {
        //логинимся в В
        vkLogin(Constants.vk_group_id, Constants.vk_group_token,
                Constants.vk_admin_id, Constants.vk_admin_token);
        //подключаем бота
        telegramLogin(Constants.telegram_bot_token);
        //логинимся в иснтаграм
        instagramLogin(Constants.instagram_username, Constants.instagram_password);
        //инициализируем переменные
        prepareVariables();
        //запусакм таск, который через рандомное время проверяет main_account на наличие новых постов и стори
        new TaskPostsCheck().run();
        new TaskStoriesCheck().run();
        //new TaskReelsCheck().run();
    }

    private static void instagramLogin(String instagramUsername, String instagramPassword) throws IOException {
        Callable<String> inputCode = () -> {
            bot.execute(new SendMessage(Constants.admin_telegram_ids[0], "Please input code: "));
            while (!hasDigits) {
                //wait in loop
            }
            hasDigits = false;
            return sixDigits;
        };

        File clientFile = new File("inst\\client.ser");
        File cookieFile = new File("inst\\cookie.ser");

        if (clientFile.exists() && cookieFile.exists()) {
            try {
                instagram = IGClient.deserialize(clientFile, cookieFile);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("LOGIN THROUGH SERIALIZATION");
        }
        else {
            IGClient.Builder.LoginHandler twoFactorHandler = (client, response) ->
                    IGChallengeUtils.resolveTwoFactor(client, response, inputCode);

            IGClient.Builder.LoginHandler challengeHandler = (client, response) ->
                    IGChallengeUtils.resolveChallenge(client, response, inputCode);

            instagram = IGClient.builder()
                    .username(instagramUsername)
                    .password(instagramPassword)
                    .onTwoFactor(twoFactorHandler)
                    .onChallenge(challengeHandler)
                    .login();

            instagram.serialize(clientFile, cookieFile);
            System.out.println("LOGIN THROUGH PASSWORD");
        }
    }

    private static void vkLogin(int groupID, String groupToken, int userID, String userToken) {
        TransportClient transportClient = new HttpTransportClient();
        vk = new VkApiClient(transportClient);
        groupActor = new GroupActor(groupID, groupToken);
        userActor = new UserActor(userID, userToken);
    }

    private static void telegramLogin(String telegramToken) {
        bot = new TelegramBot(telegramToken);

        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                String answer = "";
                long chatId = update.message().chat().id();

                boolean is_admin = false;
                for (long id : Constants.admin_telegram_ids) {
                    if (id == chatId) {
                        is_admin = true;
                        break;
                    }
                }

                //обрабатываем сообщения только от админов сообщества в ВК
                if (is_admin) {
                    String message = update.message().text().trim();
                    if (message.equals("/story")) storyUpdater();
                    else if (message.equals("/post")) postUpdater();
                    else if (Pattern.matches("[0-9][0-9][0-9][0-9][0-9][0-9]", message)) {
                        sixDigits = message;
                        hasDigits = true;
                    }
                    else {
                        System.out.println("From " + chatId + ": " + message);
                        answer = TELEGRAM_API.responseTelegram(message);
                    }
                } else {
                    answer = "Sorry, you are not allowed to use me. Your id: " + chatId;
                }
                bot.execute(new SendMessage(chatId, answer));
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private static void prepareVariables() throws IOException {
        File mediaDir = new File("media");
        if (!mediaDir.exists())
            mediaDir.mkdir();

        File instDir = new File("inst");
        if (!instDir.exists())
            instDir.mkdir();

        File pkDir = new File("pk");
        if (!pkDir.exists())
            pkDir.mkdir();
        else {
            if (storyPkFile.exists())
                latestStoryPk = readPk(storyPkFile);
            else
                latestStoryPk = 0;

            if (postPkFile.exists())
                latestPostPk = readPk(postPkFile);
            else
                latestPostPk = 0;
        }

        //создаем словарь, в котором pk от стори хранятся 24 часа
        PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<Long, Date>
                expirationPolicy = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
                        24, TimeUnit.HOURS);
        story24HoursPk = new PassiveExpiringMap<>(expirationPolicy, new HashMap<>());

        //создаем словарь, в котором хранятся последние 5 pk постов
        post5latestPk = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Date> eldest) {
                return this.size() > 10;
            }
        };

        INST_API.start();
    }

    static class TaskPostsCheck extends TimerTask {
        @Override
        public void run() {
            int delay = (180 + new Random().nextInt(420)) * 1000;
            //for test int delay = (10 + new Random().nextInt(50)) * 1000;
            timerPosts.schedule(new TaskPostsCheck(), delay);
            postUpdater();
        }
    }

    static class TaskStoriesCheck extends TimerTask {
        @Override
        public void run() {
            int delay = (100 + new Random().nextInt(400)) * 1000;
            //for test int delay = (10 + new Random().nextInt(50)) * 1000;
            timerStories.schedule(new TaskStoriesCheck(), delay);
            storyUpdater();
        }
    }

    public static void notifyAdmins(String msg) {
        for (long admin_id : Constants.admin_telegram_ids)
            bot.execute(new SendMessage(admin_id, msg));
    }

    protected static void storyUpdater() {
        try {
            InstagramStory newStory = INST_API.checkForStoryUpdates();
            if (newStory != null) {
                latestStoryPk = newStory.getPk();
                rewritePkFile(latestStoryPk, storyPkFile);
                String msg = TELEGRAM_API.postingStoryNotification(newStory);
                if (msg.equals("story posted on vk"))
                    msg = "Недавно опубликованная стори пользователя уже добавлена в группу!";
                else
                    msg = "На проверке новых сториз:\n\n" + msg;
                for (long admin_id : Constants.admin_telegram_ids)
                    bot.execute(new SendMessage(admin_id, msg));
            }
        }
        catch (ClientException | ApiException e) {
            e.printStackTrace();
        }
    }

    protected static void postUpdater() {
        try {
            InstagramPost newPost = INST_API.checkForPostUpdates();
            if (newPost != null) {
                latestPostPk = newPost.getPk();
                rewritePkFile(latestPostPk, postPkFile);
                String msg = TELEGRAM_API.postingPostNotification(newPost);
                if (msg.equals("post posted on vk"))
                    msg = "Недавно опубликованный пост пользователя уже добавлен в группу!";
                else
                    msg = "На проверке новых постов:\n\n" + msg;
                for (long admin_id : Constants.admin_telegram_ids)
                    bot.execute(new SendMessage(admin_id, msg));
            }
        }
        catch (ClientException | ApiException e) {
            e.printStackTrace();
        }
    }

    private static void rewritePkFile(long newPk, File file) {
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream stream = new FileOutputStream(file, false)) {
            stream.write(String.valueOf(newPk).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long readPk(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        BufferedReader postReader = new BufferedReader(new InputStreamReader(stream));
        String post = postReader.readLine();
        return Long.parseLong(post);
    }
}
