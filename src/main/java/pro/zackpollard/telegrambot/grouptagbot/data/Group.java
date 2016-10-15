package pro.zackpollard.telegrambot.grouptagbot.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import lombok.Data;
import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.Chat;
import pro.zackpollard.telegrambot.api.chat.ChatMemberStatus;
import pro.zackpollard.telegrambot.api.chat.ChatType;
import pro.zackpollard.telegrambot.api.chat.GroupChat;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.content.type.MessageEntity;
import pro.zackpollard.telegrambot.api.chat.message.content.type.MessageEntityType;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.conversations.Conversation;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.MessageEditReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.MessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;
import pro.zackpollard.telegrambot.grouptagbot.GroupTagBot;
import pro.zackpollard.telegrambot.grouptagbot.prompts.InfoTagPrompt;
import pro.zackpollard.telegrambot.grouptagbot.prompts.ModifyTagsPrompt;
import pro.zackpollard.telegrambot.grouptagbot.prompts.GetTagPrompt;
import pro.zackpollard.telegrambot.grouptagbot.prompts.GetUserPrompt;

/**
 * @author Zack Pollard
 */
@Data
public class Group implements Listener {

    private long id;
    private final Map<String, Conversation> activeConversations;
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
        this.activeConversations = new HashMap<>();

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
                            createTag(event.getArgs()[0], event.getChat(), event.getMessage());
                        } else {
                            createConversation(event.getChat().getId(), conversationBuilderFor(event.getChat(), event.getMessage().getSender().getId())
                                    .prompts().first(ModifyTagsPrompt.CREATE_PROMPT).end()).setSilent(true);
                        }
                    }
                    return;
                }

                case "delete": {

                    if(hasPermission(event.getMessage().getSender(), chat, Role.FAKE_ADMIN)) {

                        if (event.getArgs().length == 1) {
                            deleteTag(event.getArgs()[0], event.getChat(), event.getMessage());
                        } else {
                            createConversation(event.getChat().getId(), conversationBuilderFor(event.getChat(), event.getMessage().getSender().getId())
                                    .prompts().first(ModifyTagsPrompt.DELETE_PROMPT).end()).setSilent(true);
                        }
                    }
                }

                case "remove":
                case "add": {

                    if(hasPermission(event.getMessage().getSender(), chat, Role.FAKE_ADMIN)) {

                        boolean commandAdd = event.getCommand().toLowerCase().equals("add");

                        if (event.getArgs().length >= 1) {
                            modifyUserTag(event.getArgs()[0], commandAdd, event.getContent(), event.getMessage(), event.getChat());
                        } else {
                            Conversation.ConversationBuilder convoBuilder = conversationBuilderFor(event.getChat(), event.getMessage().getSender().getId())
                                    .prompts()
                                        .first(GetTagPrompt.INSTANCE)
                                        .then(GetUserPrompt.INSTANCE)
                                    .end();

                            createConversation(event.getChat().getId(), convoBuilder).getContext().setSessionData("add", commandAdd);
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
                        info(event.getArgs()[0], event.getChat(), event.getMessage());
                    } else {

                        createConversation(event.getChat().getId(), conversationBuilderFor(event.getChat(), event.getMessage().getSender().getId())
                                .prompts().first(InfoTagPrompt.INSTANCE).end());
                    }
                }
            }
        }
    }

    public Conversation.ConversationBuilder conversationBuilderFor(Chat chat, long user) {
        return Conversation
                .builder(telegramBot)
                .forWhom(chat)
                .sessionData(new HashMap<String, Object>() {{
                    put("group", Group.this);
                }})
                .allowedUser(user)
                .repliesOnly(true);
    }

    public Conversation createConversation(String userId, Conversation.ConversationBuilder builder) {
        if (activeConversations.containsKey(userId)) {
            activeConversations.remove(userId).end();
        }

        Conversation conversation = builder.build();

        conversation.begin();
        activeConversations.put(userId, conversation);
        return conversation;
    }

    public boolean createTag(String tagText, Chat chat, Message message) {
        tagText = tagText.toLowerCase().trim();

        if (tagText.length() == 0) {
            chat.sendMessage(SendableTextMessage.plain("You must specify a tag longer than zero characters.").replyTo(message).build());
            return false;
        }

        if (this.getTags().containsKey(tagText)) {

            chat.sendMessage(SendableTextMessage.plain("A tag with this name already exists.").replyTo(message).build());
            return false;
        } else {

            this.getTags().put(tagText, new Tag(tagText));
            chat.sendMessage(SendableTextMessage.plain("This tag has been created, use /add (tagname) (username/userID) to add people.").replyTo(message).build());
            return true;
        }
    }

    public boolean deleteTag(String tagText, Chat chat, Message message) {
        tagText = tagText.toLowerCase().trim();

        if (tagText.length() == 0) {
            chat.sendMessage(SendableTextMessage.plain("You must specify a tag longer than zero characters.").replyTo(message).build());
            return false;
        }

        if (this.getTags().remove(tagText) != null) {

            chat.sendMessage(SendableTextMessage.plain("The tag '" + tagText + "' has been successfully removed.").replyTo(message).build());
            return true;
        } else {

            chat.sendMessage(SendableTextMessage.plain("A tag with name '" + tagText + "' doesn't exist.").replyTo(message).build());
            return false;
        }
    }
    
    public void modifyUserTag(String tagText, boolean commandAdd, TextContent content, Message message, Chat chat) {
        tagText = tagText.toLowerCase().trim();
        Tag tag = this.getTags().get(tagText);

        if (tag != null) {

            Map<String, Long> usernameMap = this.getUsernamesInChat();

            int usersModified = 0;

            for (MessageEntity entity : content.getEntities()) {

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

                            chat.sendMessage(SendableTextMessage.plain("The user '" + user.getFullName() + "' is " + (commandAdd ? "already in" : "not in") + " this tag so they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(message).build());
                        }
                        instance.getManager().getUsernameCache().updateUsername(user.getId(), user.getUsername());
                        ++usersModified;
                    } else {

                        chat.sendMessage(SendableTextMessage.plain("The user '" + user.getFullName() + "' doesn't have a username and so can't be pinged, they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(message).build());
                    }
                } else if (entity.getType().equals(MessageEntityType.MENTION)) {

                    String mention = content.getContent().substring(entity.getOffset() + 1, entity.getOffset() + entity.getLength()).toLowerCase();

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

                            chat.sendMessage(SendableTextMessage.plain("The user '" + mention + "' is " + (commandAdd ? "already in" : "not in") + " this tag so they will not be " + (commandAdd ? "added to" : "removed from") + " the tag").replyTo(message).build());
                        }
                        instance.getManager().getUsernameCache().updateUsername(userID, mention);
                        ++usersModified;
                    } else {

                        chat.sendMessage(SendableTextMessage.plain("The username + @" + mention + " is not known, they will not be " + (commandAdd ? "added to" : "removed from") + " the tag. The user may not have spoken since the bot was added, or may not be in this chat.").replyTo(message).build());
                    }
                }
            }

            chat.sendMessage(SendableTextMessage.plain(usersModified + " users were successfully " + (commandAdd ? "added to" : "removed from") + " the tag.").replyTo(message).build());
        } else {

            chat.sendMessage(SendableTextMessage.markdown("This tag has not been created, use `/create " + tagText + "` to create it.").replyTo(message).build());
        }
    }

    public void info(String tagText, Chat chat, Message msg) {
        tagText = tagText.toLowerCase().trim();
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

            chat.sendMessage(SendableTextMessage.plain(message).replyTo(msg).build());
        } else {

            chat.sendMessage(SendableTextMessage.markdown("This tag has not been created, use `/create " + tagText + "` to create it.").replyTo(msg).build());
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
