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
                    if (message.equals("/story")) Updater.storyUpdater();
                    else if (message.equals("/post")) Updater.postUpdater();
                    else if (Pattern.matches("[0-9][0-9][0-9][0-9][0-9][0-9]", message)) {
                        Constants.sixDigits = message;
                        Constants.hasDigits = true;
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

    public static void notifyAdmins(String msg) {
        for (long admin_id : Constants.admin_telegram_ids)
            bot.execute(new SendMessage(admin_id, msg));
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
                //answer = "Прости, я пока не умею с ними работать :(";
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
        String date = VK_API.dateFormat(item.getDate());

        if (className.equals("InstagramPost")) {
            InstagramPost instagramPost = ((InstagramPost) item);
            String caption = instagramPost.getCaption();
            String translated = Translator.translateTextToRussian(caption);
            int[] versions = instagramPost.getVersions();
            int count = instagramPost.getMediaCount();
            String msg = "Пост " + user + "\n\n"
                    + caption + "\n\n" +
                    "Перевод: " + "\n\n" + translated +
                    "\n\n" + date;
            if (count == 1) {
                if (versions[0] == 1)
                    bot.execute(new SendPhoto(Constants.channelId, instagramPost.getMedia()[0]).caption(msg));
                else if (versions[0] == 2)
                    bot.execute(new SendVideo(Constants.channelId, instagramPost.getMedia()[0]).caption(msg));
            }
            else {
                InputMedia[] media = new InputMedia[count];
                for (int i = 0; i < count; i ++) {
                    if (versions[i] == 1)
                        media[i] = new InputMediaPhoto(instagramPost.getMedia()[i]);
                    else if (versions[i] == 2)
                        media[i] = new InputMediaVideo(instagramPost.getMedia()[i]);
                }
                bot.execute(new SendMediaGroup(Constants.channelId, media));
                bot.execute(new SendMessage(Constants.channelId, msg));
            }
        }
        else if (className.equals("InstagramStory")) {
            InstagramStory instagramStory = ((InstagramStory) item);
            int version = instagramStory.getVersion();
            String msg = "Стори " + user + "\n\n" + date;
            if (version == 1)
                bot.execute(new SendPhoto(Constants.channelId, instagramStory.getMedia()).caption(msg));
            else if (version == 2) {
                bot.execute(new SendVideo(Constants.channelId, instagramStory.getMedia()).caption(msg));
            }
        }
        else if (className.equals("InstagramReel")) {
            InstagramReel instagramReel = ((InstagramReel) item);
            String caption = instagramReel.getCaption();
            String translated = Translator.translateTextToRussian(caption);
            String msg = "Рил " + user + "\n\n"
                    + caption + "\n\n" +
                    "Перевод: " + "\n\n" + translated +
                    "\n\n" + date;
            bot.execute(new SendVideo(Constants.channelId, instagramReel.getVideo()).caption(msg));
        }
    }

    public static String postingPostNotification (InstagramPost post)  {
        String answer;
        if (post.getUser() != null) {
            //пуюликуем пост в вк и удаляем файлы
            try {
                sendMessageToChannel(post);
                VK_API.postPost(post);
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
            }
            finally {
                for (File f : post.getMedia())
                    f.delete();
            }
            answer = "post posted on vk and channel";
        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += getCallingMethodName();
        }
        return answer;
    }

    public static String postingStoryNotification (InstagramStory story) {
        String answer;
        if (story.getUser() != null) {
            //публикуем стори в вк и уадляем файлы
            try {
                sendMessageToChannel(story);
                VK_API.postStory(story);
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
            } finally {
                story.getMedia().delete();
            }
            answer = "story posted on vk and channel";
        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += getCallingMethodName();
        }
        return answer;
    }

    public static String postingReelNotification (InstagramReel reel) {
        String answer;
        if (reel.getUser() != null) {
            //публикуем рил в вк и уадляем файлы
            try {
                sendMessageToChannel(reel);
                VK_API.postReel(reel);
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
            } finally {
                reel.getVideo().delete();
            }
            answer = "reel posted on vk and channel";
        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += getCallingMethodName();
        }
        return answer;
    }

    private static String getCallingMethodName() {
        StackTraceElement callingFrame = Thread.currentThread().getStackTrace()[10];
        return callingFrame.getMethodName();
    }
}