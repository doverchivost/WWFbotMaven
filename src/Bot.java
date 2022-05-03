import Constants.Constants;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Bot {

    private final static Timer timerPosts = new Timer();
    private final static Timer timerStories = new Timer();

    public static void main(String[] args) throws IOException {
        TELEGRAM_API.telegramAddListener();
        initializeVariables();
        INST_API.start();
        new TaskPostsCheck().run();
        new TaskStoriesCheck().run();
    }

    private static void initializeVariables() throws IOException {
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
            if (Constants.storyPkFile.exists())
                Constants.latestStoryPk = readPk(Constants.storyPkFile);
            else
                Constants.latestStoryPk = 0;

            if (Constants.postPkFile.exists())
                Constants.latestPostPk = readPk(Constants.postPkFile);
            else
                Constants.latestPostPk = 0;
        }

        //создаем словарь, в котором pk от стори хранятся 24 часа
        PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<Long, Date>
                expirationPolicy = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
                        24, TimeUnit.HOURS);
        Constants.story24HoursPk = new PassiveExpiringMap<>(expirationPolicy, new HashMap<>());

        //создаем словарь, в котором хранятся последние 5 pk постов
        Constants.post5latestPk = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Date> eldest) {
                return this.size() > 10;
            }
        };
    }

    static class TaskPostsCheck extends TimerTask {
        @Override
        public void run() {
            int delay = (180 + new Random().nextInt(420)) * 1000;
            //for test int delay = (10 + new Random().nextInt(50)) * 1000;
            timerPosts.schedule(new TaskPostsCheck(), delay);
            Updater.postUpdater();
        }
    }

    static class TaskStoriesCheck extends TimerTask {
        @Override
        public void run() {
            int delay = (100 + new Random().nextInt(400)) * 1000;
            //for test int delay = (10 + new Random().nextInt(50)) * 1000;
            timerStories.schedule(new TaskStoriesCheck(), delay);
            Updater.storyUpdater();
        }
    }

    private static long readPk(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        BufferedReader postReader = new BufferedReader(new InputStreamReader(stream));
        String post = postReader.readLine();
        return Long.parseLong(post);
    }
}
