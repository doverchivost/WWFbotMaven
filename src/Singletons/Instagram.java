package Singletons;

import Constants.Constants;
import com.github.instagram4j.instagram4j.IGClient;
import com.github.instagram4j.instagram4j.utils.IGChallengeUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import okhttp3.OkHttpClient;


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.Callable;

public class Instagram {
    private static volatile IGClient instagram;

    public static IGClient getInstagram() {
        IGClient localInstagram = instagram;
        if (localInstagram == null) {
            synchronized (Instagram.class) {
                localInstagram = instagram;
                if (localInstagram == null) {
                    instagramLogin();
                    localInstagram = instagram;
                }
            }
        }
        return localInstagram;
    }

    public static void reLogin(String username, String password) {
        if (!username.isBlank())
            Constants.instagram_username = username;
        if (!password.isBlank())
            Constants.instagram_password = password;
        reLogin();
    }

    public static void reLogin() {
        synchronized (Instagram.class) {
            loginThroughPassword();
        }
    }

    private static final File clientFile = new File("inst\\client.ser");
    private static final File cookieFile = new File("inst\\cookie.ser");

    private static void loginThroughPassword() {
        Callable<String> inputCode = () -> {
            TelegramBot bot = Telegram.getTelegramBot();
            bot.execute(new SendMessage(Constants.admin_telegram_ids[0], "Please input code: "));
            while (!Constants.hasDigits) {
                Thread.sleep(5000);
            }
            Constants.hasDigits = false;
            return Constants.sixDigits;
        };

        IGClient.Builder.LoginHandler twoFactorHandler = (client, response) ->
                IGChallengeUtils.resolveTwoFactor(client, response, inputCode);

        IGClient.Builder.LoginHandler challengeHandler = (client, response) ->
                IGChallengeUtils.resolveChallenge(client, response, inputCode);

        try {
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("50.174.7.158", 80));
            //OkHttpClient httpClient = new OkHttpClient.Builder().proxy(proxy).build();
            instagram = IGClient.builder()
                    .username(Constants.instagram_username)
                    .password(Constants.instagram_password)
                    //.client(httpClient)
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

    private static void instagramLogin() {
        if (clientFile.exists() && cookieFile.exists()) {
            try {
                instagram = IGClient.deserialize(clientFile, cookieFile);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
            System.out.println("LOGIN THROUGH SERIALIZATION");
        }
        else {
            loginThroughPassword();
        }
    }
}
