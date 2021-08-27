package ir.mahdihmb.limoo_storage_bot.util;

import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.exception.LimooException;

public class GeneralUtils {

    public static String trimSpaces(String text) {
        return text.replaceFirst("^[ \\t]+", "").replaceFirst("[ \\t]+$", "");
    }

    public static String getMessageOfThrowable(Throwable throwable) {
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    public static void sendInThreadOrConversation(Message message, Conversation conversation, String sendingText) throws LimooException {
        if (message.getThreadRootId() == null)
            conversation.send(sendingText);
        else
            message.sendInThread(sendingText);
    }

    public static void sendInThreadOrConversation(Message message, Conversation conversation, Message.Builder messageBuilder) throws LimooException {
        if (message.getThreadRootId() == null)
            conversation.send(messageBuilder);
        else
            message.sendInThread(messageBuilder);
    }
}
