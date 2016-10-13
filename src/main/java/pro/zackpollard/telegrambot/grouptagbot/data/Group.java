package pro.zackpollard.telegrambot.grouptagbot.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Data;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.ChatMemberStatus;
import pro.zackpollard.telegrambot.api.chat.ChatType;
import pro.zackpollard.telegrambot.api.chat.GroupChat;
import pro.zackpollard.telegrambot.api.chat.message.content.type.MessageEntity;
import pro.zackpollard.telegrambot.api.chat.message.content.type.MessageEntityType;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.MessageEditReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.MessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;
import pro.zackpollard.telegrambot.grouptagbot.GroupTagBot;

/**
 * @author Zack Pollard
 */
@Data
public class Group implements Listener {

    private long id;
    private final Set<Long> userIDs;
    private final Map<String, Tag> tags;
    private boolean onlyAdmins;

    private final transient TelegramBot telegramBot;
    private final transient GroupTagBot instance;

    public Group(long id) {

        this();
        this.id = id;
    }

    public Group() {

        instance = GroupTagBot.getInstance();
        telegramBot = instance.getTelegramBot();

        this.userIDs = new TreeSet<>();
        this.tags = new HashMap<>();

        telegramBot.getEventsManager().register(this);
    }

    @Override
    public void onTextMessageReceived(TextMessageReceivedEvent event) {

        User sender = event.getMessage().getSender();
        if(sender != null && (event.getChat().getType().equals(ChatType.GROUP) || event.getChat().getType().equals(ChatType.SUPERGROUP)) && Long.valueOf(event.getChat().getId()).equals(this.getId())) {

            GroupChat chat = (GroupChat) event.getChat();

            String text = event.getContent().getContent();

            if (text.startsWith("@")) {

                String tagText = text.substring(1);

                if (text.contains(" ")) {

                    tagText = text.substring(1, text.indexOf(' ')).toLowerCase();
                }

                if (tagText.length() != 0) {

                    if (tagText.equals("everyone")) {

                        if(hasPermission(event.getMessage().getSender(), chat, Role.ADMIN)) {

                            String message = "";

                            for (long userID : this.getUserIDs()) {

                                if (sender.getId() != userID) {
                                    String username = instance.getManager().getUsernameCache().getUsernameCache().get(userID);
                                    if (username != null) {
                                        message += "@" + username + " ";
                                    }
                                }
                            }

                            if (message.isEmpty()) {
                                message = "There is nobody to tag!";
                            }

                            event.getChat().sendMessage(message);
                            return;
                        } else {

                            event.getChat().sendMessage(SendableTextMessage.builder().message("You must only specify one tag.").replyTo(event.getMessage()).build());
                            return;
                        }
                    }

                    Tag tag = tags.get(tagText);

                    if (tag != null) {

                        String message = "";

                        for (long userID : tag.getUsers()) {

                            if (sender.getId() != userID) {
                                String username = instance.getManager().getUsernameCache().getUsernameCache().get(userID);
                                if (username != null) {
                                    message += "@" + username + " ";
                                }
                            }
                        }

                        if (message.isEmpty()) {
                            message = "There is nobody to tag!";
                        }

                        event.getChat().sendMessage(message);
                    }
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        User sender = event.getMessage().getSender();

        if(sender != null && (event.getChat().getType().equals(ChatType.GROUP) || event.getChat().getType().equals(ChatType.SUPERGROUP)) && Long.valueOf(event.getChat().getId()).equals(this.getId())) {

            this.getUserIDs().add(sender.getId());
        }
    }

    @Override
    public void onMessageEditReceived(MessageEditReceivedEvent event) {

        User sender = event.getMessage().getSender();

        if(sender != null && (event.getChat().getType().equals(ChatType.GROUP) || event.getChat().getType().equals(ChatType.SUPERGROUP)) && Long.valueOf(event.getChat().getId()).equals(this.getId())) {

            this.getUserIDs().add(sender.getId());
        }
    }

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {

        if(event.getMessage().getSender() != null && (event.getChat().getType().equals(ChatType.GROUP) || event.getChat().getType().equals(ChatType.SUPERGROUP)) && Long.valueOf(event.getChat().getId()).equals(this.getId())) {

            GroupChat chat = (GroupChat) event.getChat();

            switch (event.getCommand().toLowerCase()) {

                case "create": {

                    if(hasPermission(event.getMessage().getSender(), chat, Role.FAKE_ADMIN)) {

                        if (event.getArgs().length == 1) {

                            String tagText = event.getArgs()[0].toLowerCase();

                            if (tagText.length() != 0) {

                                if (this.getTags().containsKey(tagText)) {

                                    event.getChat().sendMessage(SendableTextMessage.builder().message("A tag with this name already exists.").replyTo(event.getMessage()).build());
                                    return;
                                } else {

                                    this.getTags().put(tagText, new Tag(tagText));
                                    event.getChat().sendMessage(SendableTextMessage.builder().message("This tag has been created, use /add (tagname) (username/userID) to add people.").replyTo(event.getMessage()).build());
                                    return;
                                }
                            } else {

                                event.getChat().sendMessage(SendableTextMessage.builder().message("You must specify a tag longer than zero characters.").replyTo(event.getMessage()).build());
                                return;
                            }
                        } else {

                            event.getChat().sendMessage(SendableTextMessage.builder().message("You must only specify one tag.").replyTo(event.getMessage()).build());
                            return;
                        }
                    }
                }

                case "delete": {

                    if(hasPermission(event.getMessage().getSender(), chat, Role.FAKE_ADMIN)) {

                        if (event.getArgs().length == 1) {

                            String tagText = event.getArgs()[0].toLowerCase();

                            if (tagText.length() != 0) {

                                if (this.getTags().remove(tagText) != null) {

                                    event.getChat().sendMessage(SendableTextMessage.builder().message("The tag '" + tagText + "' has been successfully removed.").replyTo(event.getMessage()).build());
                                    return;
                                } else {

                                    event.getChat().sendMessage(SendableTextMessage.builder().message("A tag with name '" + tagText + "' doesn't exist.").replyTo(event.getMessage()).build());
                                    return;
                                }
                            } else {

                                event.getChat().sendMessage(SendableTextMessage.builder().message("You must specify a tag longer than zero characters.").replyTo(event.getMessage()).build());
                                return;
                            }
                        } else {

                            event.getChat().sendMessage(SendableTextMessage.builder().message("You must only specify one tag.").replyTo(event.getMessage()).build());
                            return;
                        }
                    }
                }

                case "remove":
                case "add": {

                    if(hasPermission(event.getMessage().getSender(), chat, Role.FAKE_ADMIN)) {

                        boolean commandAdd = event.getCommand().toLowerCase().equals("add");

                        if (event.getArgs().length >= 1) {

                            String tagText = event.getArgs()[0].toLowerCase();

                            Tag tag = this.getTags().get(tagText);

                            if (tag != null) {

                                Map<String, Long> usernameMap = this.getUsernamesInChat();

                                int usersModified = 0;

                                for (MessageEntity entity : event.getContent().getEntities()) {

                                    if (entity.getType().equals(MessageEntityType.TEXT_MENTION)) {

                                        User user = entity.getUser();

                                        if (user.getUsername() != null) {

                                            if (tag.getUsers().contains(user.getId()) != commandAdd) {

                                                if (commandAdd) {

                                                    tag.getUsers().add(user.getId());
                                                } else {

                                                    tag.getUsers().remove(user.getId());
                                                }
                                            } else {

                                                event.getChat().sendMessage(SendableTextMessage.builder().message("The user '" + user.getFullName() + "' is " + (commandAdd ? "already in" : "not in") + " this tag so they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(event.getMessage()).build());
                                            }
                                            instance.getManager().getUsernameCache().updateUsername(user.getId(), user.getUsername());
                                            ++usersModified;
                                        } else {

                                            event.getChat().sendMessage(SendableTextMessage.builder().message("The user '" + user.getFullName() + "' doesn't have a username and so can't be pinged, they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(event.getMessage()).build());
                                        }
                                    } else if (entity.getType().equals(MessageEntityType.MENTION)) {

                                        String mention = event.getContent().getContent().substring(entity.getOffset() + 1, entity.getOffset() + entity.getLength()).toLowerCase();

                                        System.out.println(mention);

                                        Long userID = usernameMap.get(mention);

                                        if (userID != null) {

                                            if (tag.getUsers().contains(userID) != commandAdd) {

                                                if (commandAdd) {

                                                    tag.getUsers().add(userID);
                                                } else {

                                                    tag.getUsers().remove(userID);
                                                }
                                            } else {

                                                event.getChat().sendMessage(SendableTextMessage.builder().message("The user '" + mention + "' is " + (commandAdd ? "already in" : "not in") + " this tag so they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(event.getMessage()).build());
                                            }
                                            instance.getManager().getUsernameCache().updateUsername(userID, mention);
                                            ++usersModified;
                                        } else {

                                            event.getChat().sendMessage(SendableTextMessage.builder().message("The username + @" + mention + " is not known, they will not be " + (commandAdd ? "added to" : "removed from") + " the tag. The user may not have spoken since the bot was added, or may not be in this chat.").replyTo(event.getMessage()).build());
                                        }
                                    }
                                }

                                event.getChat().sendMessage(SendableTextMessage.builder().message(usersModified + " users were successfully added to the tag.").replyTo(event.getMessage()).build());
                            } else {

                                event.getChat().sendMessage(SendableTextMessage.builder().message("This tag has not been created, use `/create " + tagText + "` to create it.").parseMode(ParseMode.MARKDOWN).replyTo(event.getMessage()).build());
                                return;
                            }
                        } else {

                            event.getChat().sendMessage(SendableTextMessage.builder().message("You must specify two arguments, the tag you want to add to and then the user you are adding by mentioning them").replyTo(event.getMessage()).build());
                            return;
                        }

                        break;
                    }
                }

                case "tags": {

                    String message = "";

                    for(Tag tag : this.getTags().values()) {

                        message += tag.getTag() + "\t - \t" + tag.getUsers().size() + " Users\n";
                    }

                    event.getChat().sendMessage(SendableTextMessage.builder().message(message).replyTo(event.getMessage()).build());
                    break;
                }

                case "info": {

                    if(event.getArgs().length == 1) {

                        String tagText = event.getArgs()[0].toLowerCase();

                        Tag tag = this.getTags().get(tagText);

                        if(tag != null) {

                            String message = "Tag Name: " + tag.getTag() + "\n";
                            message += "Total Users: " + tag.getUsers().size() + "\n";
                            message += "User List: \n";

                            for(Long userID : tag.getUsers()) {

                                String username = instance.getManager().getUsernameCache().getUsernameCache().get(userID);

                                if(username != null) {

                                    message += "  - " + username + "\n";
                                }
                            }

                            event.getChat().sendMessage(SendableTextMessage.builder().message(message).replyTo(event.getMessage()).build());
                            return;
                        } else {

                            event.getChat().sendMessage(SendableTextMessage.builder().message("This tag has not been created, use `/create " + tagText + "` to create it.").parseMode(ParseMode.MARKDOWN).replyTo(event.getMessage()).build());
                            return;
                        }
                    } else {

                        event.getChat().sendMessage(SendableTextMessage.builder().message("You must specify one argument which is the tag you want to get info about.").replyTo(event.getMessage()).build());
                        return;
                    }
                }
            }
        }
    }

    public boolean hasPermission(User user, GroupChat chat, Role requiredRole) {

        switch(requiredRole) {

            case NONE:

                return true;
            case FAKE_ADMIN:

                return !this.isOnlyAdmins();
            case ADMIN:

                ChatMemberStatus status = chat.getChatMember(user).getStatus();

                return (status.equals(ChatMemberStatus.ADMINISTRATOR) || status.equals(ChatMemberStatus.CREATOR));
            default:

                return false;
        }
    }

    public Map<String, Long> getUsernamesInChat() {

        Map<String, Long> userMap = new HashMap<>();

        for(Long userID : this.getUserIDs()) {

            String username = instance.getManager().getUsernameCache().getUsernameCache().get(userID);

            if(username != null) {

                userMap.put(username, userID);
            }
        }

        return userMap;
    }

    private enum Role {

        NONE,
        FAKE_ADMIN,
        ADMIN
    }
}
