import Constants.Constants;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Updater {

    protected static void storyUpdater() {
        try {
            String[] result = INST_API.checkForStoryUpdates();
            if (result != null) {
                long newStoryPk = Long.parseLong(result[0]);
                Constants.latestStoryPk = newStoryPk;
                rewritePkFile(Constants.latestStoryPk, Constants.storyPkFile);
                String msg = result[1];
                if (msg == Constants.successStory)
                    msg = "Недавно опубликованная стори пользователя уже добавлена в группу!";
                else
                    msg = "На проверке новых сториз:\n\n" + msg;
                try {
                    TELEGRAM_API.notifyAdmins(msg);
                } catch (Exception e) {
                    System.out.println(msg);
                }
            }
        }
        catch (ClientException | ApiException e) {
            TELEGRAM_API.notifyMainAdmin("Ошибка при попытке опубликовать новую стори в сообщество.");
            e.printStackTrace();
        }
    }

    protected static void postUpdater() {
        try {
            String[] result = INST_API.checkForPostUpdates();
            if (result != null) {
                long newPostPk = Long.parseLong(result[0]);
                Constants.latestPostPk = newPostPk;
                rewritePkFile(Constants.latestPostPk, Constants.postPkFile);
                String msg = result[1];
                if (msg == Constants.successPost)
                    msg = "Недавно опубликованный пост пользователя уже добавлен в группу!";
                else
                    msg = "На проверке новых постов:\n\n" + msg;
                try {
                    TELEGRAM_API.notifyAdmins(msg);
                } catch (Exception e) {
                    System.out.println(msg);
                }
            }
        }
        catch (ClientException | ApiException e) {
            TELEGRAM_API.notifyMainAdmin("Ошибка при попытке опубликовать новый пост в сообщество.");
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
