package pro.zackpollard.telegrambot.grouptagbot.prompts;

import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.conversations.ConversationContext;
import pro.zackpollard.telegrambot.api.conversations.prompt.TextPrompt;
import pro.zackpollard.telegrambot.grouptagbot.data.Group;

public class GetUserPrompt extends TextPrompt {
    public static final GetUserPrompt INSTANCE = new GetUserPrompt();

    @Override
    public boolean process(ConversationContext context, TextContent input) {
        boolean modifier = (boolean) context.sessionDataBy("add");
        Group group = (Group) context.sessionDataBy("group");
        String tag = (String) context.sessionDataBy("tag");

        group.modifyUserTag(tag, modifier, input, null, context.getFrom());
        return false;
    }

    @Override
    public SendableMessage promptMessage(ConversationContext context) {
        boolean modifier = (boolean) context.sessionDataBy("add");
        return SendableTextMessage.plain("Please reply to this message with the user you wish to " + (modifier ? "add" : "remove"))
                .build();
    }
}
