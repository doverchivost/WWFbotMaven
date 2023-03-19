import Constants.Constants;
import InstagramItems.InstagramPost;
import InstagramItems.InstagramReel;
import InstagramItems.InstagramStory;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

public class ConsoleHandler {
    public static void run() throws IOException {
        //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String[] messages = new String[]{
                "https://instagram.com/stories/yooyongwook/3061969595236887427?igshid=YmMyMTA2M2Y="
        };
        //while (true) {
        for (String message : messages) {
            //String message = in.readLine().trim();
            System.out.println(handler(message));

        }
    }

    private static String handler(String message) {
        if (message.equals("/story")) {
            try {
                Updater.storyUpdater();
                return "Сториз были проверены";
            }
            catch (Exception e) {
                e.printStackTrace();
                return "В настоящий момент проверку сториз выполнить невозможно.";
            }
        } else if (message.equals("/post")) {
            try {
                Updater.postUpdater();
                return "Посты были проверены";
            }
            catch (Exception e) {
                e.printStackTrace();
                return "В настоящий момент проверку постов выполнить невозможно.";
            }
        } else if (message.contains("instagram.com/") || message.contains("youtu.be") || message.contains("youtube.com")) {
            String answer = response(message);
            if (answer != Constants.successStory && answer != Constants.successPost
                    && answer != Constants.successReel && answer != Constants.successYoutube) {
                answer += "\n\nК сожалению, сейчас я не могу обработать ваш запрос.\n" +
                        "Пожалуйста, попытайтесь позже.\n\n";
            }
            return answer;
        }
        return "";
    }

    private static String response(String message) {
        String answer = "";
        String flag = "";
        String possibleFlag = message.split("-")[message.split("-").length - 1].toLowerCase();
        if (possibleFlag.equals(("v"))) {
            flag = Constants.flagVk;
            System.out.println("Posting to VK only");
        }
        else if (possibleFlag.equals("t")) {
            flag = Constants.flagTg;
            System.out.println("Posting to TELEGRAM only");
        }

        if (message.contains("youtu.be") || message.contains("youtube.com")) {
            String[] msg = message.split("\n");
            String youtubeUrl = "";
            String messagePost = "";
            for (String m : msg) {
                if (m.contains("youtu.be") || m.contains("youtube.com"))
                    youtubeUrl = m.trim();
                else
                if (!m.isBlank())
                    messagePost = m.trim();
            }
            answer = postingVideoFromYoutube(youtubeUrl, messagePost, flag);
        } else if (message.length() < 30)
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
                            story.setLink(links[i]);
                            storiesToPost[i] = story;
                        }
                        answer = postingStoriesNotification(storiesToPost, flag);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (message.contains("/p/") || message.contains("/tv/")) {
                try {
                    String originalLink = message.split(" ")[0];
                    InstagramPost post = INST_API.getUserPostByLink(message);
                    post.setLink(originalLink);
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
                        answer = postingPostNotification(post, indexes, flag);
                    } else {
                        answer = postingPostNotification(post, flag);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (message.contains("/reel/")) {
                try {
                    InstagramReel reel = INST_API.getUserReelByLink(message);
                    reel.setLink(message);
                    answer = postingReelNotification(reel, flag);
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

    public static String postingVideoFromYoutube(String youtubeUrl, String message, String flag) {
        StringJoiner joiner = new StringJoiner("\n\n");
        boolean successVK = false;
        boolean successTG = false;

        if (flag.isEmpty() || flag == Constants.flagVk) {
            try {
                VK_API.postVideoFromYouTube(youtubeUrl, message);
                successVK = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать видео в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            }
        }

        if (flag.isEmpty() || flag == Constants.flagTg) {
            try {
                ChannelPosting.sendVideoLink(youtubeUrl, message);
                successTG = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать пост в тг\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            }
        }

        if ((flag.isEmpty() && successTG && successVK) ||
                (!flag.isEmpty() && (successTG || successVK))) return Constants.successYoutube;
        return joiner.toString();
    }

    public static String postingPostNotification (InstagramPost post, String flag)  {
        if (post.getUser() == null) return "Null Pointer Exception >_<" + Constants.getStackTrace();

        StringJoiner joiner = new StringJoiner("\n\n");
        boolean successVK = false;
        boolean successTG = false;
        String vkLink = null;

        if (flag.isEmpty() || flag == Constants.flagVk) {
            try {
                vkLink = VK_API.postPost(post);
                successVK = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать пост в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            }
        }

        if (flag.isEmpty() || flag == Constants.flagTg) {
            try {
                ChannelPosting.sendMessageToChannel(post);
                successTG = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать пост в тг\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
                if (vkLink != null)
                    ChannelPosting.sendLinks(vkLink, post.getLink());
            } finally {
                for (File f : post.getMedia())
                    f.delete();
            }
        }

        if ((flag.isEmpty() && successTG && successVK) ||
                (!flag.isEmpty() && (successTG || successVK))) return Constants.successPost;
        return joiner.toString();
    }

    public static String postingPostNotification (InstagramPost post, Set<Integer> indexes, String flag)  {
        if (post.getUser() == null) return "Null Pointer Exception >_<" + Constants.getStackTrace();

        for (int index : indexes)
            if (index > post.getMediaCount())
                return "Заданы неверные индексы для публикации медиа из поста";

        StringJoiner joiner = new StringJoiner("\n\n");
        boolean successVK = false;
        boolean successTG = false;
        String vkLink = null;

        if (flag.isEmpty() || flag == Constants.flagVk) {
            try {
                vkLink = VK_API.postPost(post, indexes);
                successVK = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать пост в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
                if (vkLink != null)
                    ChannelPosting.sendLinks(vkLink, post.getLink());
            }
        }

        if (flag.isEmpty() || flag == Constants.flagTg) {
            try {
                ChannelPosting.sendMessageToChannel(post, indexes);
                successTG = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать пост в тг\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            } finally {
                for (File f : post.getMedia())
                    f.delete();
            }
        }

        if ((flag.isEmpty() && successTG && successVK) ||
                (!flag.isEmpty() && (successTG || successVK))) return Constants.successPost;
        return joiner.toString();
    }

    public static String postingStoriesNotification (InstagramStory[] stories, String flag) {
        for (InstagramStory story : stories) {
            if (story.getUser() == null)
                return "Null Pointer Exception >_<" + Constants.getStackTrace();
        }

        StringJoiner joiner = new StringJoiner("\n\n");
        boolean successVK = false;
        boolean successTG = false;

        if (flag.isEmpty() || flag == Constants.flagVk) {
            try {
                VK_API.postStory(stories);
                successVK = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать стори в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            }
        }

        if (flag.isEmpty() || flag == Constants.flagTg) {
            try {
                ChannelPosting.sendMessageToChannel(stories);
                successTG = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать стори в тг\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            } finally {
                for (InstagramStory story : stories)
                    story.getMedia().delete();
            }
        }

        if ((flag.isEmpty() && successTG && successVK) ||
                (!flag.isEmpty() && (successTG || successVK))) return Constants.successStory;
        return joiner.toString();
    }

    public static String postingReelNotification (InstagramReel reel, String flag) {
        if (reel.getUser() == null)
            return "Null Pointer Exception >_<" + Constants.getStackTrace();

        StringJoiner joiner = new StringJoiner("\n\n");
        boolean successVK = false;
        boolean successTG = false;

        if (flag.isEmpty() || flag == Constants.flagVk) {
            try {
                VK_API.postReel(reel);
                successVK = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать рил в вк\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            }
        }

        if (flag.isEmpty() || flag == Constants.flagTg) {
            try {
                ChannelPosting.sendMessageToChannel(reel);
                successTG = true;
            } catch (Exception e) {
                joiner.add("Не удалось опубликовать рил в тг\n\n" + e.getMessage() + "\n\n" + Constants.getStackTrace());
            } finally {
                reel.getVideo().delete();
            }
        }

        if ((flag.isEmpty() && successTG && successVK) ||
                (!flag.isEmpty() && (successTG || successVK))) return Constants.successReel;
        return joiner.toString();
    }
}
