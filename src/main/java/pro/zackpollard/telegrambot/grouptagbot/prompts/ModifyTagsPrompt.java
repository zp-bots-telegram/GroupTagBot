package pro.zackpollard.telegrambot.grouptagbot.prompts;

import pro.zackpollard.telegrambot.api.chat.message.content.TextContent;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.conversations.ConversationContext;
import pro.zackpollard.telegrambot.api.conversations.prompt.TextPrompt;
import pro.zackpollard.telegrambot.grouptagbot.data.Group;

public class ModifyTagsPrompt extends TextPrompt {
    public static final ModifyTagsPrompt CREATE_PROMPT = new ModifyTagsPrompt(true);
    public static final ModifyTagsPrompt DELETE_PROMPT = new ModifyTagsPrompt(false);
    private final boolean create;

    private ModifyTagsPrompt(boolean create) {
        this.create = create;
    }

    @Override
    public boolean process(ConversationContext conversationContext, TextContent textContent) {
        Group group = (Group) conversationContext.sessionDataBy("group");

        if (create) {
            return !group.createTag(textContent.getContent(), conversationContext.getFrom(), null);
        } else {
            return !group.deleteTag(textContent.getContent(), conversationContext.getFrom(), null);
        }
    }

    @Override
    public SendableMessage promptMessage(ConversationContext conversationContext) {
        return SendableTextMessage.plain("Please reply to this message with the tag you wish to "
                + (create ? "create" : "delete")).build();
    }
}
