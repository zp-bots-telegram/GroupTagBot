package pro.zackpollard.telegrambot.grouptagbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.grouptagbot.data.GroupTags;
import pro.zackpollard.telegrambot.grouptagbot.data.UsernameCache;
import pro.zackpollard.telegrambot.grouptagbot.utils.Utils;

import java.io.*;

/**
 * @author Zack Pollard
 */
@Data
public class GroupTagManager {

    private final static File tagLocation = new File("./datastore.json");
    private final static File usernameCacheLocation = new File("./userstore.json");

    private final TelegramBot telegramBot;
    private final GroupTagBot instance;

    private GroupTags groupTags;
    private UsernameCache usernameCache;

    public GroupTagManager() {

        this.instance = GroupTagBot.getInstance();
        this.telegramBot = instance.getTelegramBot();

        this.initData();

        System.out.println(usernameCache == null);
    }

    private void initData() {

        if (tagLocation.exists()) {

            groupTags = loadTags();
        } else {

            try {
                tagLocation.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            groupTags = new GroupTags();

            this.saveTags();
        }

        if(usernameCacheLocation.exists()) {

            usernameCache = loadUsernames();
        } else {

            try {
                usernameCacheLocation.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            usernameCache = new UsernameCache();

            this.saveUsernameCache();
        }

        if (groupTags == null || usernameCache == null) {

            System.err.println("The save file could not be loaded. Either fix the save file or delete it and restart the bot.");
            System.exit(1);
        }
    }

    private GroupTags loadTags() {

        GroupTags loadedSaveFile;

        try (Reader reader = new InputStreamReader(new FileInputStream(tagLocation), "UTF-8")) {

            Gson gson = new GsonBuilder().create();
            loadedSaveFile = gson.fromJson(reader, GroupTags.class);

            return loadedSaveFile;
        } catch (IOException e) {

            e.printStackTrace();
        }

        return null;
    }

    private UsernameCache loadUsernames() {

        UsernameCache loadedSaveFile;

        try (Reader reader = new InputStreamReader(new FileInputStream(usernameCacheLocation), "UTF-8")) {

            Gson gson = new GsonBuilder().create();
            loadedSaveFile = gson.fromJson(reader, UsernameCache.class);

            return loadedSaveFile;
        } catch (IOException e) {

            e.printStackTrace();
        }

        return null;
    }

    public boolean saveTags() {

        String json = GroupTagBot.getGson().toJson(groupTags);

        return Utils.writeFile(tagLocation, json);
    }

    public boolean saveUsernameCache() {

        String json = GroupTagBot.getGson().toJson(usernameCache);

        return Utils.writeFile(usernameCacheLocation, json);
    }
}
