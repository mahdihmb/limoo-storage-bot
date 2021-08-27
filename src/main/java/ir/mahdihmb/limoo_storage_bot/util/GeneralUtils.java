package ir.mahdihmb.limoo_storage_bot.util;

import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.exception.LimooException;

import static ir.mahdihmb.limoo_storage_bot.util.Constants.*;

public class GeneralUtils {

    public static String trimSpaces(String text) {
        return text.replaceFirst("^[ \\t]+", "").replaceFirst("[ \\t]+$", "");
    }

    public static boolean notEmpty(String text) {
        return text != null && !text.isEmpty();
    }

    public static void sendErrorMsgInThread(Message message, String text) throws LimooException {
        message.sendInThread(EXCLAMATION_EMOJI + SPACE + text);
    }

    public static void sendSuccessMsgInThread(Message message, String text) throws LimooException {
        message.sendInThread(CHECK_MARK_EMOJI + SPACE + text);
    }

    public static String concatUris(String first, String second) {
        return first + (first.endsWith("/") || second.startsWith("/") ? "" : "/") + second;
    }

    public static String getMessageOfThrowable(Throwable throwable) {
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    public static ir.limoo.driver.entity.Message sendInThreadOrConversation(Message message, Conversation conversation,
                                                                            String sendingText) throws LimooException {
        if (message.getThreadRootId() == null)
            return conversation.send(sendingText);
        else
            return message.sendInThread(sendingText);
    }

    public static ir.limoo.driver.entity.Message sendInThreadOrConversation(Message message, Conversation conversation,
                                                                            Message.Builder messageBuilder) throws LimooException {
        if (message.getThreadRootId() == null)
            return conversation.send(messageBuilder);
        else
            return message.sendInThread(messageBuilder);
    }
}
