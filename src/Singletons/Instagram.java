package Singletons;

import Constants.Constants;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.utils.IGChallengeUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class Instagram {
    private static volatile IGClient instagram;

    public static IGClient getInstagram() {
        IGClient localInstagram = instagram;
        if (localInstagram == null) {
            synchronized (Instagram.class) {
                localInstagram = instagram;
                if (localInstagram == null) {
                    instagramLogin(Constants.instagram_username, Constants.instagram_password);
                    localInstagram = instagram;
                }
            }
        }
        return localInstagram;
    }

    private static void instagramLogin(String instagramUsername, String instagramPassword) {
        Callable<String> inputCode = () -> {
            TelegramBot bot = Telegram.getTelegramBot();
            bot.execute(new SendMessage(Constants.admin_telegram_ids[0], "Please input code: "));
            while (!Constants.hasDigits) {
                Thread.sleep(5000);
            }
            Constants.hasDigits = false;
            return Constants.sixDigits;
        };

        File clientFile = new File("inst\\client.ser");
        File cookieFile = new File("inst\\cookie.ser");

        if (clientFile.exists() && cookieFile.exists()) {
            try {
                instagram = IGClient.deserialize(clientFile, cookieFile);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
            System.out.println("LOGIN THROUGH SERIALIZATION");
        }
        else {
            IGClient.Builder.LoginHandler twoFactorHandler = (client, response) ->
                    IGChallengeUtils.resolveTwoFactor(client, response, inputCode);

            IGClient.Builder.LoginHandler challengeHandler = (client, response) ->
                    IGChallengeUtils.resolveChallenge(client, response, inputCode);

            try {
                instagram = IGClient.builder()
                        .username(instagramUsername)
                        .password(instagramPassword)
                        .onTwoFactor(twoFactorHandler)
                        .onChallenge(challengeHandler)
                        .login();

                instagram.serialize(clientFile, cookieFile);
                System.out.println("LOGIN THROUGH PASSWORD");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
