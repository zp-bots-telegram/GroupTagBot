package pro.zackpollard.telegrambot.grouptagbot;

import com.google.gson.Gson;
import lombok.Getter;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.ChatMemberStatus;
import pro.zackpollard.telegrambot.api.chat.GroupChat;
import pro.zackpollard.telegrambot.grouptagbot.data.Group;
import pro.zackpollard.telegrambot.grouptagbot.data.Tag;

import java.util.*;

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
    
    public static boolean DEBUG_MODE = false;

    public static void main(String[] args) {

        //This simply takes the bots API key from the first command line argument sent to the bot.
        //You do not have to retrieve the API key in this way.
        API_KEY = System.getenv("BOT_API_KEY");
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

            System.out.print("\nroot@GroupTagBot$ ");
            String input = scanner.nextLine().trim();
            int indexOfSpace = input.indexOf(' ');
            String command = indexOfSpace != -1 ? input.substring(0, indexOfSpace) : input;
            String fullArgs = indexOfSpace != -1 ? input.substring(indexOfSpace + 1) : "";
            String[] argsArray = fullArgs.split(" ");

            switch(command) {

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
                case "stats": {

                    System.out.println("Total cached users: " + manager.getUsernameCache().getUsernameCache().size());
                    System.out.println("Total groups: " + manager.getGroupTags().getGroups().size());
                    break;
                }
                case "cleanup": {

                    int emptyUsernames = 0;
                    int usersRemoved = 0;

                    Set<Long> toCleanup = new HashSet<>();

                    for(Map.Entry<Long, String> entry : manager.getUsernameCache().getUsernameCache().entrySet()) {

                        if(entry.getValue().equals("") || entry.getValue() == null) {

                            ++emptyUsernames;
                            toCleanup.add(entry.getKey());
                        }
                    }

                    for(Group group : manager.getGroupTags().getGroups().values()) {

                        GroupChat chat = (GroupChat) telegramBot.getChat(group.getId());

                        for(Tag tag : group.getTags().values()) {

                            Iterator<Long> users = tag.getUsers().iterator();

                            while(users.hasNext()) {

                                long user = users.next();

                                ChatMemberStatus chatMemberStatus;

                                if(!group.getUserIDs().contains(user)) {

                                    chatMemberStatus = ChatMemberStatus.LEFT;
                                } else {

                                    chatMemberStatus = chat.getChatMember(user).getStatus();
                                }

                                switch(chatMemberStatus) {

                                    case KICKED:
                                    case LEFT: {

                                        tag.getUsers().remove(user);
                                        if(group.getUserIDs().contains(user)) {

                                            ++usersRemoved;
                                            group.getUserIDs().remove(user);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    for(Long id : toCleanup) {

                        manager.getUsernameCache().getUsernameCache().remove(id);
                    }

                    System.out.println("Usernames cleaned up: " + emptyUsernames);
                    System.out.println("Removed/Left Users cleaned up: " + usersRemoved);
                    break;
                }
                case "toggledebug": {
                    DEBUG_MODE = !DEBUG_MODE;
                    System.out.println("Debug mode was " + (DEBUG_MODE ? "enabled" : "disabled") + ".");
                    break;
                }
                case "testbroadcast":
                case "broadcast": {

                    int sent = 0;
                    int enabledGroups = 0;
                    int disabledGroups = 0;
                    
                    for(Group group : manager.getGroupTags().getGroups().values()) {
                        group.setAllowBroadcasts(true);
                        if(group.isAllowBroadcasts()) {
                            ++enabledGroups;
                        } else {
                            ++disabledGroups;
                        }
                    }
                    
                    System.out.println("Groups with broadcasts disabled: " + disabledGroups + "/" + enabledGroups);
                    
                    for(Group group : new ArrayList<>(manager.getGroupTags().getGroups().values())) {
                        
                        if(group.isAllowBroadcasts()) {
                            
                            Chat chat = (GroupChat) telegramBot.getChat(group.getId());
                            if(command.equals("testbroadcast")) {
                                chat = telegramBot.getChat(87425504L);
                            }
                            chat.sendMessage(fullArgs.replace("\\n", "\n"));
                            ++sent;
                        }
                        
                        //[#                   ] 1%\r
                        System.out.print("\r[");
                        double percentDone = ((double) sent / (double) enabledGroups) * 100;

                        for (int i = 0; i < 100; i += 5) {

                            System.out.print((i < percentDone) ? "#" : " ");
                        }

                        System.out.print("] " + (int) percentDone + "%");

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    
                    break;
                }
                default: {

                    int spaceChar = input.indexOf(" ");
                    System.out.println("-console: " + (spaceChar != -1 ? input.substring(0, spaceChar) : input) + ": command not found");
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