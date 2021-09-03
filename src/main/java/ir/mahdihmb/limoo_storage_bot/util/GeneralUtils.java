package ir.mahdihmb.limoo_storage_bot.util;

import ir.limoo.driver.entity.Message;

import java.util.stream.Stream;

import static ir.mahdihmb.limoo_storage_bot.util.Constants.*;

public class GeneralUtils {

    public static String trimSpaces(String text) {
        return text.replaceFirst("^[ \\t]+", "").replaceFirst("[ \\t]+$", "");
    }

    public static boolean notEmpty(String text) {
        return text != null && !text.isEmpty();
    }

    public static String successText(String text) {
        return CHECK_MARK_EMOJI + SPACE + text;
    }

    public static String errorText(String text) {
        return EXCLAMATION_EMOJI + SPACE + text;
    }

    public static String italic(String text) {
        return "*" + text + "*";
    }

    public static String bold(String text) {
        return "**" + text + "**";
    }

    public static String italicBold(String text) {
        return "***" + text + "***";
    }

    public static String concatUris(String first, String second) {
        return first + (first.endsWith("/") || second.startsWith("/") ? "" : "/") + second;
    }

    public static String getMessageOfThrowable(Throwable throwable) {
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    public static String generateDirectLink(Message msg, String limooUrl) {
        if (notEmpty(msg.getWorkspaceKey()) && notEmpty(msg.getConversationId()) && notEmpty(msg.getId())) {
            String directLinkUri;
            if (notEmpty(msg.getThreadRootId())) {
                directLinkUri = String.format(THREAD_DIRECT_LINK_URI_TEMPLATE,
                        msg.getWorkspaceKey(), msg.getConversationId(), msg.getThreadRootId(), msg.getId());
            } else {
                directLinkUri = String.format(DIRECT_LINK_URI_TEMPLATE,
                        msg.getWorkspaceKey(), msg.getConversationId(), msg.getId());
            }
            return String.format(MARKDOWN_LINK_TEMPLATE, LINK_EMOJI, concatUris(limooUrl, directLinkUri));
        }
        return null;
    }

    public static Stream<String> filterKeywords(Stream<String> stream, String query) {
        String[] keywords = query.split(" +");
        return stream.filter(name -> {
            for (String keyword : keywords) {
                if (!name.toLowerCase().contains(keyword.toLowerCase()))
                    return false;
            }
            return true;
        });
    }
}
