import Constants.Constants;
import InstagramItems.InstagramItem;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import Singletons.Telegram;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InputMedia;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.InputMediaVideo;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.File;
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
                for (long id : Constants.admin_telegram_ids)
                    if (id == chatId) {
                        is_admin = true;
                        break;
                    }

                //обрабатываем сообщения только от админов сообщества в ВК
                if (is_admin) {
                    String message = update.message().text().trim();
                    System.out.println("From " + chatId + ": " + message);

                    if (message.equals("/start")) answer = "Добро пожаловать, админ группы!";
                    else if (message.equals("/story")) {
                        try {
                            Updater.storyUpdater();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            answer = "В настоящий момент проверку сториз выполнить невозможно.";
                        }
                    }
                    else if (message.equals("/post")) {
                        try {
                            Updater.postUpdater();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            answer = "В настоящий момент проверку постов выполнить невозможно.";
                        }
                    }
                    else if (Pattern.matches("[0-9][0-9][0-9][0-9][0-9][0-9]", message)) {
                        Constants.sixDigits = message;
                        Constants.hasDigits = true;
                    }
                    else {
                        answer = TELEGRAM_API.responseTelegram(message);
                        //if (answer.length() <= 1) {
                        if (answer != Constants.successStory && answer != Constants.successPost
                        && answer != Constants.successReel) {
                            answer += "\n\nК сожалению, сейчас я не могу обработать ваш запрос.\n" +
                                    "Пожалуйста, попытайтесь позже.\n\n" +
                                    Constants.getStackTrace();
                        }
                    }
                } else {
                    answer = "Sorry, you are not allowed to use me. Your id: " + chatId;
                }
                bot.execute(new SendMessage(chatId, answer));
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public static void notifyAdmins(String msg) {
        //for (long admin_id : Constants.admin_telegram_ids)
        //    bot.execute(new SendMessage(admin_id, msg));
        bot.execute(new SendMessage(Constants.admin_telegram_ids[0], msg));
    }

    private static String responseTelegram(String message) {
        String answer = "";

        if (message.length() < 30)
            answer = "Ты отправил что-то не то. Попробуй еще раз.";
        else {
            if (message.contains("/stories/")) {
                try {
                    InstagramStory story = INST_API.getUserStoryByLink(message);
                    answer = postingStoryNotification(story);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (message.contains("/p/") || message.contains("/tv/")) {
                try {
                    InstagramPost post = INST_API.getUserPostByLink(message);
                    answer = postingPostNotification(post);
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

    private static void sendMessageToChannel(InstagramItem item) {
        String className = item.getClass().getSimpleName();
        String user = item.getUser();
        String date = Constants.dateFormat(item.getDate());

        switch (className) {
            case "InstagramPost" -> {
                System.out.println("Posting Instagram Post to Channel");
                InstagramPost instagramPost = ((InstagramPost) item);
                String caption = instagramPost.getCaption();
                String translated = instagramPost.getTranslatedCaption();
                int[] versions = instagramPost.getVersions();
                int count = instagramPost.getMediaCount();
                String msg = "Пост " + user + "\n\n"
                        + caption + "\n\n" +
                        "Перевод: " + "\n\n" + translated +
                        "\n\n" + date;
                InputMedia[] media = new InputMedia[count];
                for (int i = 0; i < count; i++) {
                    if (versions[i] == 1)
                        media[i] = new InputMediaPhoto(instagramPost.getMedia()[i]);
                    else if (versions[i] == 2)
                        media[i] = new InputMediaVideo(instagramPost.getMedia()[i]);
                }
                bot.execute(new SendMediaGroup(Constants.channelId, media));
                bot.execute(new SendMessage(Constants.channelId, msg));
            }
            case "InstagramStory" -> {
                System.out.println("Posting Instagram Story to Channel");
                InstagramStory instagramStory = ((InstagramStory) item);
                int version = instagramStory.getVersion();
                String msg = "Стори " + user + "\n\n" + date;
                if (version == 1)
                    bot.execute(new SendPhoto(Constants.channelId, instagramStory.getMedia()).caption(msg));
                else if (version == 2) {
                    bot.execute(new SendVideo(Constants.channelId, instagramStory.getMedia()).caption(msg));
                }
            }
            case "InstagramReel" -> {
                System.out.println("Posting Instagram Reel to Channel");
                InstagramReel instagramReel = ((InstagramReel) item);
                String caption = instagramReel.getCaption();
                String translated = instagramReel.getTranslatedCaption();
                String msg = "Рил " + user + "\n\n"
                        + caption + "\n\n" +
                        "Перевод: " + "\n\n" + translated +
                        "\n\n" + date;
                bot.execute(new SendVideo(Constants.channelId, instagramReel.getVideo()).caption(msg));
            }
        }
    }

    public static String postingPostNotification (InstagramPost post)  {
        String answer;
        if (post.getUser() != null) {
            //публикуем пост в канал и вк и удаляем файлы
            try {
                sendMessageToChannel(post);
                VK_API.postPost(post);
                answer = Constants.successPost;
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
                answer = "Не удалось опубликовать пост в вк";
                answer += Constants.getStackTrace();
            } catch (Exception e) {
                answer = "Что-то пошло не так";
                answer += Constants.getStackTrace();
            }
            finally {
                for (File f : post.getMedia())
                    f.delete();
            }
        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += Constants.getStackTrace();
        }
        return answer;
    }

    public static String postingStoryNotification (InstagramStory story) {
        String answer;
        if (story.getUser() != null) {
            //публикуем стори в канал и вк и уадляем файлы
            try {
                sendMessageToChannel(story);
                VK_API.postStory(story);
                answer = Constants.successStory;
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
                answer = "Не удалось опубликовать стори в вк";
                answer += Constants.getStackTrace();
            } catch (Exception e) {
                answer = "Что-то пошло не так";
                answer += Constants.getStackTrace();
            } finally {
                story.getMedia().delete();
            }

        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += Constants.getStackTrace();
        }
        return answer;
    }

    public static String postingReelNotification (InstagramReel reel) {
        String answer;
        if (reel.getUser() != null) {
            //публикуем рил в канал и вк и уадляем файлы
            try {
                sendMessageToChannel(reel);
                VK_API.postReel(reel);
                answer = Constants.successReel;
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
                answer = "Не удалось опубликовать рил";
                answer += Constants.getStackTrace();
            } catch (Exception e) {
                answer = "Что-то пошло не так";
                answer += Constants.getStackTrace();
            } finally {
                reel.getVideo().delete();
            }
        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += Constants.getStackTrace();
        }
        return answer;
    }
}