package pro.zackpollard.telegrambot.grouptagbot.data;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Zack Pollard
 */
@Data
public class Group implements Listener {

    private long id;
    private final Set<Long> userIDs;
    private final Map<String, Tag> tags;
    private boolean onlyAdmins;
    private boolean allowBroadcasts;

    private final transient Map<String, Tag> aliases;
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
        this.aliases = new HashMap<>();
        this.allowBroadcasts = true;

        telegramBot.getEventsManager().register(this);
    }

    @Override
    public void onTextMessageReceived(TextMessageReceivedEvent event) {

        User sender = event.getMessage().getSender();
        if(sender != null && (event.getChat().getType().equals(ChatType.GROUP) || event.getChat().getType().equals(ChatType.SUPERGROUP)) && Long.valueOf(event.getChat().getId()).equals(this.getId())) {

            if(GroupTagBot.DEBUG_MODE) {
                String userID = event.getMessage().getSender().getUsername() != null ? event.getMessage().getSender().getUsername() : String.valueOf(event.getMessage().getSender().getId());
                System.out.println("DEBUG: <" + event.getChat().getName() + "> - (" + userID + ") - " + event.getContent().getContent());
            }

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

                            sendTagMessage(this.getUserIDs(), event);
                            return;
                        } else {

                            event.getChat().sendMessage(SendableTextMessage.builder().message("You don't have permission to use the @everyone tag.").replyTo(event.getMessage()).build());
                            return;
                        }
                    }

                    Tag tag = tags.get(tagText);

                    if (tag != null) {

                        sendTagMessage(tag.getUsers(), event);
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

            System.out.println(event.getMessage().asJson().toString(4));

            this.getUserIDs().add(sender.getId());
        }
    }

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {

        if(event.getMessage().getSender() != null && (event.getChat().getType().equals(ChatType.GROUP) || event.getChat().getType().equals(ChatType.SUPERGROUP)) && Long.valueOf(event.getChat().getId()).equals(this.getId())) {

            if(GroupTagBot.DEBUG_MODE) {
                String userID = event.getMessage().getSender().getUsername() != null ? event.getMessage().getSender().getUsername() : String.valueOf(event.getMessage().getSender().getId());
                System.out.println("DEBUG: <" + event.getChat().getName() + "> - (" + userID + ") - " + event.getContent().getContent());
            }
            
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
                                                ++usersModified;
                                            } else {

                                                event.getChat().sendMessage(SendableTextMessage.builder().message("The user '" + user.getFullName() + "' is " + (commandAdd ? "already in" : "not in") + " this tag so they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(event.getMessage()).build());
                                            }
                                            instance.getManager().getUsernameCache().updateUsername(user.getId(), user.getUsername());
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
                                                ++usersModified;
                                            } else {

                                                event.getChat().sendMessage(SendableTextMessage.builder().message("The user '" + mention + "' is " + (commandAdd ? "already in" : "not in") + " this tag so they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(event.getMessage()).build());
                                            }

                                            instance.getManager().getUsernameCache().updateUsername(userID, mention);
                                        } else {

                                            event.getChat().sendMessage(SendableTextMessage.builder().message("The username + @" + mention + " is not known, they will not be " + (commandAdd ? "added to" : "removed from") + " the tag. The user may not have spoken since the bot was added, or may not be in this chat.").replyTo(event.getMessage()).build());
                                        }
                                    }
                                }

                                if(usersModified != 0) {

                                    event.getChat().sendMessage(SendableTextMessage.builder().message(usersModified + " users were successfully " + (commandAdd ? "added to" : "removed from") + " the tag.").replyTo(event.getMessage()).build());
                                }
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

                    StringBuilder message = new StringBuilder();

                    for(Tag tag : this.getTags().values()) {

                        message.append(tag.getTag()).append("\t - \t").append(tag.getUsers().size()).append(" Users\n");
                    }

                    event.getChat().sendMessage(SendableTextMessage.builder().message(message.toString()).replyTo(event.getMessage()).build());
                    break;
                }

                case "info": {

                    if(event.getArgs().length == 1) {

                        String tagText = event.getArgs()[0].toLowerCase();

                        Tag tag = this.getTags().get(tagText);

                        if(tag != null) {

                            StringBuilder message = new StringBuilder().append("Tag Name: ").append(tag.getTag()).append("\n");
                            message.append("Total Users: ").append(tag.getUsers().size()).append("\n");
                            message.append("User List: \n");

                            for(Long userID : tag.getUsers()) {

                                String username = instance.getManager().getUsernameCache().getUsernameCache().get(userID);

                                if(username != null) {

                                    message.append("  - ").append(username).append("\n");
                                }
                            }

                            event.getChat().sendMessage(SendableTextMessage.builder().message(message.toString()).replyTo(event.getMessage()).build());
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

                case "edit": {

                    if(hasPermission(event.getMessage().getSender(), chat, Role.FAKE_ADMIN)) {

                        if (event.getArgs().length == 2) {

                            String targetTag = event.getArgs()[0].toLowerCase();
                            String newName = event.getArgs()[1].toLowerCase();

                            Tag newTag = this.getTags().get(targetTag);

                            if (newTag != null) {

                                if (newName.length() != 0) {

                                    if (this.getTags().containsKey(newName)) {

                                        event.getChat().sendMessage(SendableTextMessage.builder().message("The new name you specified is already used.").replyTo(event.getMessage()).build());
                                    } else {

                                        this.getTags().put(newName, newTag);
                                        this.getTags().remove(targetTag);
                                        event.getChat().sendMessage(SendableTextMessage.builder().message("The tag has been renamed from @" + targetTag + " to @" + newName +".").replyTo(event.getMessage()).build());
                                    }
                                } else {

                                    event.getChat().sendMessage(SendableTextMessage.builder().message("You must specify a tag longer than zero characters.").replyTo(event.getMessage()).build());
                                }
                            } else {

                                event.getChat().sendMessage(SendableTextMessage.builder().replyTo(event.getMessage()).message("The new tag name you specified is already in use.").build());
                            }
                        } else {

                            event.getChat().sendMessage(SendableTextMessage.builder().replyTo(event.getMessage()).message("Incorrect command syntax, you probably meant /edit oldTagName newTagName").build());
                        }
                    }

                    break;
                }
                
                case "togglebroadcasts": {
                    
                    if(hasPermission(event.getMessage().getSender(), chat, Role.ADMIN)) {
                        
                        allowBroadcasts = !allowBroadcasts;
                        event.getChat().sendMessage(SendableTextMessage.builder().message("Broadcasts about bot updates and other information have been " + (allowBroadcasts ? "enabled" : "disabled") + ".").replyTo(event.getMessage()).build());
                    } else {
                        
                        event.getChat().sendMessage(SendableTextMessage.builder().message("You do not have permission to use this command, you must be an admin in the chat.").replyTo(event.getMessage()).build());
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
    
    private void sendTagMessage(Set<Long> userIDs, TextMessageReceivedEvent event) {
        
        User sender = event.getMessage().getSender();

        boolean messageDeleted = telegramBot.deleteMessage(event.getMessage());
        
        StringBuilder messageBuilder = new StringBuilder();
        
        if(messageDeleted) {
            messageBuilder.append(event.getContent().getContent()).append("\n\n");
        }

        boolean noUsers = true;
        
        for (long userID : userIDs) {

            if (sender.getId() != userID) {
                String username = instance.getManager().getUsernameCache().getUsernameCache().get(userID);
                if (username != null) {
                    noUsers = false;
                    messageBuilder.append("@").append(username).append(" ");
                }
            }
        }

        if (noUsers) {
            messageBuilder.append("There is nobody to tag!");
        }
        
        if(messageDeleted) {
            messageBuilder
                    .append("\n\n- Sent by ")
                    .append(sender.getFullName())
                    .append(" (")
                    .append(!sender.getUsername().equals("@") ? sender.getUsername() : sender.getId())
                    .append(")");
        }

        event.getChat().sendMessage(messageBuilder.toString());
    }

    private enum Role {
        NONE,
        FAKE_ADMIN,
        ADMIN
    }
}
