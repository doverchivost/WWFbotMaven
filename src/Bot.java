import Constants.Constants;
import Singletons.Instagram;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Bot {

    //private final static Timer timerPosts = new Timer();
    //private final static Timer timerStories = new Timer();
    private final static TimerTask timerPosts = new TaskPostsCheck();
    private final static TimerTask timerStories = new TaskStoriesCheck();
    private final static TimerTask timerRandom = new TaskRandomAction();
    private final static Timer updatePosts = new Timer("TimerUpdatePosts");
    private final static Timer updateStories = new Timer("TimerUpdateStories");
    private final static Timer randomTimer = new Timer("RandomTimer");

    public static void main(String[] args) throws IOException {
        TELEGRAM_API.telegramAddListener();
        initializeVariables();
        INST_API.start();
        updatePosts.schedule(timerPosts, 15*60*1000);
        updateStories.schedule(timerStories, 10*60*1000);
        randomTimer.schedule(timerRandom, 60*60*1000);
        //new TaskPostsCheck().run();
        //new TaskStoriesCheck().run();
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

    static class TaskRandomAction extends TimerTask {
        @Override
        public void run() {
            int delay = ((30*60*1000) + new Random().nextInt(60*60*1000));
            try {
                INST_API.randmonTask();
            }
            catch (Exception e) {
                e.printStackTrace();
                TELEGRAM_API.notifyMainAdmin("На рандомном таске: " + e.getMessage() + "\n\n" + Constants.getStackTrace());
            }
            System.out.println("Next random task in: " + delay/1000 + " s.");
            //timerPosts.schedule(new TaskPostsCheck(), delay);
            randomTimer.schedule(new TaskPostsCheck(), delay);
        }
    }

    static class TaskPostsCheck extends TimerTask {
        @Override
        public void run() {
            //20-30 min
            //int delay = ((20*60*1000) + new Random().nextInt(10*60*1000));
            int delay = ((30*60*1000) + new Random().nextInt(60*60*1000));
            try {
                Updater.postUpdater();
            }
            catch (Exception e) {
                e.printStackTrace();
                //Instagram.reLogin();
                delay = 60 * 60 * 1000;
                String msg = "Ошибка в проверке постов. Посты будут проверены через час.\n\n" + e.getMessage();
                TELEGRAM_API.notifyMainAdmin(msg);
            }
            System.out.println("Next task for Posts in: " + delay/1000 + " s.");
            //timerPosts.schedule(new TaskPostsCheck(), delay);
            updatePosts.schedule(new TaskPostsCheck(), delay);
        }
    }

    static class TaskStoriesCheck extends TimerTask {
        @Override
        public void run() {
            //10-15 min
            //int delay = ((10*60*1000) + new Random().nextInt(5*60*1000));
            int delay = ((10*60*1000) + new Random().nextInt(30*60*1000));
            try {
                Updater.storyUpdater();
            }
            catch (Exception e) {
                e.printStackTrace();
                //Instagram.reLogin();
                delay = 60 * 60 * 1000;
                String msg = "Ошибка в проверке сториз. Сториз будут проверены через час.";
                TELEGRAM_API.notifyMainAdmin(msg);
            }
            System.out.println("Next task for Stories in: " + delay/1000 + " s.");
            //timerStories.schedule(new TaskStoriesCheck(), delay);
            updateStories.schedule(new TaskStoriesCheck(), delay);
        }
    }

    private static long readPk(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        BufferedReader postReader = new BufferedReader(new InputStreamReader(stream));
        String post = postReader.readLine();
        return Long.parseLong(post);
    }
}
