import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.File;

public class TELEGRAM_API {

    public static String responseTelegram(String message) {
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

    public static String postingPostNotification (InstagramPost post)  {
        String answer = "";
        if (post.getUser() != null) {
            //пуюликуем пост в вк и удаляем файлы
            try {
                VK_API.postPost(post);
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
            }
            finally {
                for (File f : post.getMedia())
                    f.delete();
            }
            answer = "post posted on vk";
        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += getCallingMethodName();
        }
        return answer;
    }

    public static String postingStoryNotification (InstagramStory story) {
        String answer = "";
        if (story.getUser() != null) {
            //публикуем стори в вк и уадляем файлы
            try {
                VK_API.postStory(story);
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
            } finally {
                story.getMedia().delete();
            }
            answer = "story posted on vk";
        }
        else {
            answer = "Null Pointer Exception >_<";
            answer += getCallingMethodName();
        }
        return answer;
    }

    public static String postingReelNotification (InstagramReel reel) {
        String answer = "";
        if (reel.getUser() != null) {
            //публикуем рил в вк и уадляем файлы
            try {
                VK_API.postReel(reel);
            } catch (ClientException | ApiException e) {
                e.printStackTrace();
            } finally {
                //reel.getMedia().delete();
                reel.getVideo().delete();
            }
            answer = "reel posted on vk";
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