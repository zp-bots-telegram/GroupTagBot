package pro.zackpollard.telegrambot.grouptagbot;

import pro.zackpollard.telegrambot.api.TelegramBot;
import pro.zackpollard.telegrambot.api.chat.ChatType;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.ParticipantJoinGroupChatEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.MessageEditReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.MessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;
import pro.zackpollard.telegrambot.grouptagbot.data.Group;

import java.util.Map;

/**
 * @author Zack Pollard
 */
public class GroupTagListener implements Listener {

    private final TelegramBot telegramBot;
    private final GroupTagBot instance;
    private final GroupTagManager manager;

    public GroupTagListener() {

        this.instance = GroupTagBot.getInstance();
        this.telegramBot = instance.getTelegramBot();
        this.manager = instance.getManager();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        User sender = event.getMessage().getSender();

        if(sender != null) {

            manager.getUsernameCache().updateUsername(sender.getId(), sender.getUsername());
        }

        ChatType chatType = event.getChat().getType();

        if(chatType.equals(ChatType.GROUP) || chatType.equals(ChatType.SUPERGROUP)) {

            long chatId = Long.parseLong(event.getChat().getId());
            Map<Long, Group> groups = manager.getGroupTags().getGroups();

            if(groups.get(chatId) == null) {
                Group group = new Group(chatId);
                groups.put(chatId, group);
            }
        }
    }

    @Override
    public void onMessageEditReceived(MessageEditReceivedEvent event) {

        User sender = event.getMessage().getSender();

        if(sender != null) {

            manager.getUsernameCache().updateUsername(sender.getId(), sender.getUsername());
        }
    }

    @Override
    public void onParticipantJoinGroupChat(ParticipantJoinGroupChatEvent event) {

        User joiner = event.getParticipant();

        if(joiner.getId() == telegramBot.getBotID()) {

            if(event.getChat().getType().equals(ChatType.GROUP) || event.getChat().getType().equals(ChatType.SUPERGROUP)) {

                manager.getGroupTags().getGroups().putIfAbsent(Long.valueOf(event.getChat().getId()), new Group(Long.parseLong(event.getChat().getId())));
            }
        } else {

            manager.getUsernameCache().updateUsername(event.getParticipant().getId(), event.getParticipant().getUsername());
        }
    }
}