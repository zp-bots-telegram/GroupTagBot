package pro.zackpollard.telegrambot.grouptagbot.prompts;

import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.conversations.ConversationContext;
import pro.zackpollard.telegrambot.api.conversations.prompt.TextPrompt;
import pro.zackpollard.telegrambot.grouptagbot.data.Group;

public class InfoTagPrompt extends TextPrompt {
    public static InfoTagPrompt INSTANCE = new InfoTagPrompt();

    @Override
    public boolean process(ConversationContext context, TextContent input) {
        Group group = (Group) context.sessionDataBy("group");

        group.info(input.getContent(), context.getFrom(), null);
        return false;
    }

    @Override
    public SendableMessage promptMessage(ConversationContext context) {
        return SendableTextMessage.plain("Please reply to this message with the tag you want to see the info. for").build();
    }
}
