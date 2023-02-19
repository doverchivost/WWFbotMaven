package Singletons;

import Constants.Constants;
import com.pengrad.telegrambot.TelegramBot;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class Telegram {
    private static volatile TelegramBot telegramBot;

    public static TelegramBot getTelegramBot() {
        TelegramBot localBot = telegramBot;
        if (localBot == null) {
            synchronized (Telegram.class) {
                localBot = telegramBot;
                if (localBot == null) {
                    //telegramBot = localBot = new TelegramBot(Constants.telegram_bot_token);
                    telegramBot = localBot = new TelegramBot.Builder(Constants.telegram_bot_token)
                            .okHttpClient(new OkHttpClient.Builder()
                                    .readTimeout(60, TimeUnit.SECONDS)
                                    .connectTimeout(60, TimeUnit.SECONDS)
                                    .writeTimeout(60, TimeUnit.SECONDS)
                                    .build())
                            .build();
                }
            }
        }
        return localBot;
    }
}
