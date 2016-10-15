package pro.zackpollard.telegrambot.grouptagbot.prompts;

import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.conversations.ConversationContext;
import pro.zackpollard.telegrambot.api.conversations.prompt.TextValidatingPrompt;
import pro.zackpollard.telegrambot.grouptagbot.data.Group;

public class GetTagPrompt extends TextValidatingPrompt {
    public static final GetTagPrompt INSTANCE = new GetTagPrompt();

    @Override
    protected boolean validate(ConversationContext context, TextContent input) {
        Group group = (Group) context.sessionDataBy("group");

        return group.getTags().containsKey(input.getContent());
    }

    @Override
    protected boolean accept(ConversationContext context, TextContent input) {
        context.setSessionData("tag", input.getContent());
        return false;
    }

    @Override
    public SendableMessage promptMessage(ConversationContext context) {
        return SendableTextMessage.plain("Please reply to this message with the tag you want to modify").build();
    }

    @Override
    protected SendableMessage invalidationMessage(ConversationContext context, TextContent input) {
        return SendableTextMessage.plain("Tag by the name of '" + input.getContent() + "' was not found! Try again").build();
    }
}
