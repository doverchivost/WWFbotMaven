import Constants.Constants;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import Singletons.Telegram;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class TELEGRAM_API {
    private static final TelegramBot bot = Telegram.getTelegramBot();

    public static void telegramAddListener() {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() == null) continue;
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
                    System.out.println("From " + chatId + ": " + message);
                    answer = adminMessageHandler(message);
                } else {
                    answer = "Sorry, you are not allowed to use me. Your id: " + chatId;
                }
                bot.execute(new SendMessage(chatId, answer));
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private static String adminMessageHandler (String message) {
        if (message.equals("/start")) return "Добро пожаловать, админ группы!";
        if (message.equals("/end") || message.equals("/stop")) {
            scheduleShutDown();
            return "Приложение отключится через минуту";
        }
        if (message.equals("/story")) {
            try {
                Updater.storyUpdater();
                return "Сториз были проверены";
            }
            catch (Exception e) {
                e.printStackTrace();
                return "В настоящий момент проверку сториз выполнить невозможно.";
            }
        }
        if (message.equals("/post")) {
            try {
                Updater.postUpdater();
                return "Посты были проверены";
            }
            catch (Exception e) {
                e.printStackTrace();
                return "В настоящий момент проверку постов выполнить невозможно.";
            }
        }
        if (Pattern.matches("[0-9][0-9][0-9][0-9][0-9][0-9]", message)) {
            Constants.sixDigits = message;
            Constants.hasDigits = true;
            return "Шестизначный код обработан";
        }
        if (message.contains("instagram.com/")) {
            String answer = TELEGRAM_API.responseTelegram(message);
            if (answer != Constants.successStory && answer != Constants.successPost
                    && answer != Constants.successReel) {
                answer += "\n\nК сожалению, сейчас я не могу обработать ваш запрос.\n" +
                        "Пожалуйста, попытайтесь позже.\n\n" +
                        Constants.getStackTrace();
            }
            return answer;
        }
        return "Ты отправил что-то не то";
    }

    private static void scheduleShutDown() {
        Timer shutdown = new Timer("ShutDownTheApp");
        shutdown.schedule(new TimerTask() {
            @Override
            public void run() {
                String command = "heroku ps:scale worker=1 --app " + Constants.herokuAppName;
                try {
                    //change to linux
                    ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
                    //"cmd.exe", "/c", command);
                    Process p = builder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 60*1000);
    }

    public static void notifyAdmins(String msg) {
        for (long admin_id : Constants.admin_telegram_ids)
            bot.execute(new SendMessage(admin_id, msg));
    }

    public static void notifyMainAdmin(String msg) {
        bot.execute(new SendMessage(Constants.admin_telegram_ids[0], msg));
    }

    private static String responseTelegram(String message) {
        String answer = "";

        if (message.length() < 30)
            answer = "Ты отправил что-то не то. Попробуй еще раз.";
        else {
            if (message.contains("/stories/")) {
                if (message.contains("/p/") || message.contains("/tv/") || message.contains("/reel/")) {
                    answer = "Нельзя отправлять разные виды постов в одном сообщении!";
                } else {
                    String[] links = Arrays.stream(message.split("\n"))
                            .filter(e -> e.contains("instagram.com")).toArray(String[]::new);
                    InstagramStory[] storiesToPost = new InstagramStory[links.length];
                    try {
                        for (int i = 0; i < links.length; i++) {
                            InstagramStory story = INST_API.getUserStoryByLink(links[i]);
                            storiesToPost[i] = story;
                        }
                        answer = postingStoriesNotification(storiesToPost);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (message.contains("/p/") || message.contains("/tv/")) {
                try {
                    InstagramPost post = INST_API.getUserPostByLink(message);
                    if (message.split(" ").length > 1) {
                        int i = message.indexOf(" ") + 1;
                        String[] inds = message.substring(i).split(",");
                        Set<Integer> indexes = new LinkedHashSet<>();
                        for (String ind : inds) {
                            try {
                                indexes.add(Integer.parseInt(ind.trim()));
                            }
                            catch (NumberFormatException e) {
                                return "Неверно указан индекс!";
                            }
                        }
                        answer = postingPostNotification(post, indexes);
                    } else {
                        answer = postingPostNotification(post);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (message.contains("/reel/")) {
                try {
                    InstagramReel reel = INST_API.getUserReelByLink(message);
                    answer = postingReelNotification(reel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                answer = "Не могу разобрать ссылку. Попробуй снова!";
            }
        }

        return answer;
    }

    public static String postingPostNotification (InstagramPost post)  {
        if (post.getUser() == null) return "Null Pointer Exception >_<" + Constants.getStackTrace();

        String answer;
        try {
            ChannelPosting.sendMessageToChannel(post);
            VK_API.postPost(post);
            answer = Constants.successPost;
        } catch (ClientException | ApiException e) {
            answer = "Не удалось опубликовать пост в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();
        } catch (Exception e) {
            answer = "Что-то пошло не так\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();
        } finally {
            for (File f : post.getMedia())
                f.delete();
        }
        return answer;
    }

    public static String postingPostNotification (InstagramPost post, Set<Integer> indexes)  {
        if (post.getUser() == null) return "Null Pointer Exception >_<" + Constants.getStackTrace();

        for (int index : indexes)
            if (index > post.getMediaCount())
                return "Заданы неверные индексы для публикации медиа из поста";

        String answer;
        try {
            ChannelPosting.sendMessageToChannel(post, indexes);
            VK_API.postPost(post, indexes);
            answer = Constants.successPost;
        } catch (ClientException | ApiException e) {
            answer = "Не удалось опубликовать пост в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();
        } catch (Exception e) {
            answer = "Что-то пошло не так\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();
        } finally {
            for (File f : post.getMedia())
                f.delete();
        }
        return answer;
    }

    public static String postingStoriesNotification (InstagramStory[] stories) {
        for (InstagramStory story : stories) {
            if (story.getUser() == null)
                return "Null Pointer Exception >_<" + Constants.getStackTrace();
        }
        String answer;
        try {
            ChannelPosting.sendMessageToChannel(stories);
            VK_API.postStory(stories);
            answer = Constants.successStory;
        } catch (ClientException | ApiException e) {
            answer = "Не удалось опубликовать стори в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();
        } catch (Exception e) {
            answer = "Что-то пошло не так\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();
        } finally {
            for (InstagramStory story : stories)
                story.getMedia().delete();
        }
        return answer;
    }

    public static String postingReelNotification (InstagramReel reel) {
        if (reel.getUser() == null)
            return "Null Pointer Exception >_<" + Constants.getStackTrace();

        String answer;
        try {
            ChannelPosting.sendMessageToChannel(reel);
            VK_API.postReel(reel);
            answer = Constants.successReel;
        } catch (ClientException | ApiException e) {
            answer = "Не удалось опубликовать рил в вк \n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();;
        } catch (Exception e) {
            answer = "Что-то пошло не так\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace();
        } finally {
            reel.getVideo().delete();
        }
        return answer;
    }
}