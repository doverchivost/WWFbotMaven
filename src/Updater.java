import Constants.Constants;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramStory;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Updater {

    protected static void storyUpdater() {
        try {
            InstagramStory newStory = INST_API.checkForStoryUpdates();
            if (newStory != null) {
                Constants.latestStoryPk = newStory.getPk();
                rewritePkFile(Constants.latestStoryPk, Constants.storyPkFile);
                String msg = TELEGRAM_API.postingStoryNotification(newStory);
                if (msg.equals(Constants.successStory))
                    msg = "Недавно опубликованная стори пользователя уже добавлена в группу!";
                else
                    msg = "На проверке новых сториз:\n\n" + msg;
                TELEGRAM_API.notifyAdmins(msg);
            }
        }
        catch (ClientException | ApiException e) {
            TELEGRAM_API.notifyAdmins("Ошибка при проерке на наличие новых сториз");
            e.printStackTrace();
        }
    }

    protected static void postUpdater() {
        try {
            InstagramPost newPost = INST_API.checkForPostUpdates();
            if (newPost != null) {
                Constants.latestPostPk = newPost.getPk();
                rewritePkFile(Constants.latestPostPk, Constants.postPkFile);
                String msg = TELEGRAM_API.postingPostNotification(newPost);
                if (msg.equals(Constants.successPost))
                    msg = "Недавно опубликованный пост пользователя уже добавлен в группу!";
                else
                    msg = "На проверке новых постов:\n\n" + msg;
                TELEGRAM_API.notifyAdmins(msg);
            }
        }
        catch (ClientException | ApiException e) {
            TELEGRAM_API.notifyAdmins("Ошибка при проерке на наличие новых постов");
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
}
