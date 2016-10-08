package pro.zackpollard.telegrambot.grouptagbot;

import com.google.gson.Gson;
import lombok.Getter;
import pro.zackpollard.telegrambot.api.TelegramBot;

import java.util.Scanner;

/**
 * @author Zack Pollard
 */
public class GroupTagBot {

    private static String API_KEY;

    @Getter
    private final TelegramBot telegramBot;
    @Getter
    private GroupTagListener listener;
    @Getter
    private GroupTagManager manager;
    private static GroupTagBot instance;

    private final static Gson gson = new Gson();

    public static void main(String[] args) {

        //This simply takes the bots API key from the first command line argument sent to the bot.
        //You do not have to retrieve the API key in this way.
        API_KEY = args[0];
        new GroupTagBot();
    }

    public GroupTagBot() {

        instance = this;

        //This returns a logged in TelegramBot instance or null if the API key was invalid.
        telegramBot = TelegramBot.login(API_KEY);
        //This registers the SpoilerListener Listener to this bot.

        manager = new GroupTagManager();

        telegramBot.getEventsManager().register(listener = new GroupTagListener());
        //This method starts the retrieval of updates.
        //The boolean it accepts is to specify whether to retrieve messages
        //which were sent before the bot was started but after the bot was last turned off.
        telegramBot.startUpdates(false);

        Scanner scanner = new Scanner(System.in);

        boolean running = true;

        while(running) {

            System.out.print("root@GroupTagBot$ ");
            String input = scanner.nextLine();

            switch(input) {

                case "exit":
                case "shutdown": {
                    System.out.println("Saving and exiting safely...");
                    manager.saveUsernameCache();
                    System.out.println("Saving username cache...");
                    manager.saveTags();
                    System.out.println("Saving tags...");
                    running = false;
                    break;
                }
            }
        }

        scanner.close();
        System.exit(0);
    }

    public static Gson getGson() {

        return gson;
    }

    public static GroupTagBot getInstance() {
        return instance;
    }
}