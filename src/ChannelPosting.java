import Constants.Constants;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import Singletons.Telegram;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InputMedia;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.InputMediaVideo;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.request.SendVideo;

import java.util.Set;

public class ChannelPosting {

    private static final TelegramBot bot = Telegram.getTelegramBot();

    private static void sendMessageToChannel(InstagramStory item) {
        System.out.println("Posting Instagram Story to Channel");
        String user = item.getUser();
        String date = Constants.dateFormat(item.getDate());
        int version = item.getVersion();
        String msg = "Стори " + user + "\n\n" + date;
        if (version == 1)
            bot.execute(new SendPhoto(Constants.channelId, item.getMedia()).caption(msg));
        else if (version == 2) {
            bot.execute(new SendVideo(Constants.channelId, item.getMedia()).caption(msg));
        }
    }

    public static void sendMessageToChannel(InstagramStory[] items) {
        System.out.println("Posting Instagram Stories to Channel");
        if (items.length == 1) sendMessageToChannel(items[0]);
        else {
            String caption = "Сториз \n";
            InputMedia[] media = new InputMedia[items.length];
            for (int i = 0; i < items.length; i++) {
                InstagramStory item = items[i];
                String user = item.getUser();
                String date = Constants.dateFormat(item.getDate());
                caption += user + " (" + date + ")\n";

                if (item.getVersion() == 1)
                    media[i] = new InputMediaPhoto(item.getMedia());
                else if (item.getVersion() == 2)
                    media [i] = new InputMediaVideo(item.getMedia());
                else
                    System.out.println("SOMETHING WRONG");
            }
            bot.execute(new SendMediaGroup(Constants.channelId, media));
            bot.execute(new SendMessage(Constants.channelId, caption));
        }
    }

    public static void sendMessageToChannel(InstagramPost item) {
        System.out.println("Posting Instagram Post to Channel");
        String user = item.getUser();
        String date = Constants.dateFormat(item.getDate());
        String caption = item.getCaption();
        String translated = item.getTranslatedCaption();
        int[] versions = item.getVersions();
        int count = item.getMediaCount();
        String msg = "Пост " + user + "\n\n"
                + caption + "\n\n" +
                "Перевод: " + "\n\n" + translated +
                "\n\n" + date;
        InputMedia[] media = new InputMedia[count];
        for (int i = 0; i < count; i++) {
            if (versions[i] == 1)
                media[i] = new InputMediaPhoto(item.getMedia()[i]);
            else if (versions[i] == 2)
                media[i] = new InputMediaVideo(item.getMedia()[i]);
            else
                System.out.println("SOMETHING WRONG");
        }
        bot.execute(new SendMediaGroup(Constants.channelId, media));
        bot.execute(new SendMessage(Constants.channelId, msg));
    }

    public static void sendMessageToChannel(InstagramPost item, Set<Integer> indexes) {
        System.out.println("Posting Instagram Post partly to Channel");
        String user = item.getUser();
        String date = Constants.dateFormat(item.getDate());
        String caption = item.getCaption();
        String translated = item.getTranslatedCaption();
        String msg = "Медиа из поста " + user + "\n\n"
                + caption + "\n\n" +
                "Перевод: " + "\n\n" + translated +
                "\n\n" + date;
        int[] versions = item.getVersions();
        InputMedia[] media = new InputMedia[indexes.size()];

        int i = 0;
        for (int index : indexes) {
            index--;
            if (versions[index] == 1)
                media[i] = new InputMediaPhoto(item.getMedia()[index]);
            else if (versions[index] == 2)
                media[i] = new InputMediaVideo(item.getMedia()[index]);
            else
                System.out.println("SOMETHING WRONG");
            i++;
        }
        bot.execute(new SendMediaGroup(Constants.channelId, media));
        bot.execute(new SendMessage(Constants.channelId, msg));
    }

    public static void sendMessageToChannel(InstagramReel item) {
        System.out.println("Posting Instagram Reel to Channel");
        String user = item.getUser();
        String date = Constants.dateFormat(item.getDate());
        String caption = item.getCaption();
        String translated = item.getTranslatedCaption();
        String msg = "Рил " + user + "\n\n"
                + caption + "\n\n" +
                "Перевод: " + "\n\n" + translated +
                "\n\n" + date;
        bot.execute(new SendVideo(Constants.channelId, item.getVideo()).caption(msg));
    }
}
