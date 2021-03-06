package ir.mahdihmb.limoo_storage_bot.util;

import ir.mahdihmb.limoo_storage_bot.core.MessageService;

import java.util.regex.Pattern;

public class Constants {

    public static final String PERSIAN_QUESTION_MARK = MessageService.get("persianQuestionMark");
    public static final String PERSIAN_PERCENTAGE_MARK = MessageService.get("persianPercentageMark");
    public static final String LINE_BREAK = "\n";
    public static final String LINE_BREAKS_REGEX = "[\r\n]";
    public static final String SPACE = " ";
    public static final String COLON = ":";
    public static final String BACK_QUOTE = "`";
    public static final String RTL_CONTROL_CHAR = "\u200F";

    public static final Pattern ILLEGAL_NAME_PATTERN = Pattern.compile("^[+.*?" + PERSIAN_QUESTION_MARK + PERSIAN_PERCENTAGE_MARK + "\\-!%=]");

    public static final String COMMAND_PREFIX = MessageService.get("commandPrefix");
    public static final String WORKSPACE_COMMAND_PREFIX = COMMAND_PREFIX + "#";
    public static final String ADD_PREFIX = "+";
    public static final String GET_PREFIX = ".";
    public static final String LIST_PREFIX = "*";
    public static final String SEARCH_PREFIX = "?";
    public static final String SEARCH_PREFIX_PERSIAN = PERSIAN_QUESTION_MARK;
    public static final String REMOVE_PREFIX = "-";
    public static final String FEEDBACK_PREFIX = "!";

    public static final String ADMIN_COMMAND_PREFIX = MessageService.get("adminCommandPrefix");
    public static final String ADMIN_RESTART_POSTGRESQL_COMMAND = MessageService.get("adminCommand.restartPostgresql");
    public static final String ADMIN_REPORT_COMMAND = MessageService.get("adminCommand.report");
    public static final String ADMIN_SEND_INTRODUCTION_IN_LOBBY_COMMAND = MessageService.get("adminCommand.sendIntroductionInLobby");
    public static final String ADMIN_SEND_UPDATE_IN_LOBBY_COMMAND_PREFIX = MessageService.get("adminCommand.sendUpdateInLobbyPrefix");
    public static final String ADMIN_RESPONSE_TO_FEEDBACK_COMMAND_PREFIX = MessageService.get("adminCommand.responseToFeedbackPrefix");
    public static final String ADMIN_DELETE_BOT_MESSAGE_COMMAND_PREFIX = MessageService.get("adminCommand.deleteBotMessagePrefix");

    public static final int MAX_NAME_LEN = 200;
    public static final int TEXT_PREVIEW_LEN = 70;
    public static final long ONE_HOUR_MILLIS = 60 * 60 * 1000;
    public static final int MAX_MESSAGE_LEN = 7500;

    public static final String LIKE_REACTION = "+1";
    public static final String DISLIKE_REACTION = "-1";
    public static final String SEEN_REACTION = "heavy_check_mark";
    public static final String EXCLAMATION_EMOJI = ":exclamation:";
    public static final String CHECK_MARK_EMOJI = ":white_check_mark:";
    public static final String LINK_EMOJI = ":link:";

    public static final String MARKDOWN_LINK_TEMPLATE = "[%s](%s)";
    public static final String DIRECT_LINK_URI_TEMPLATE = "workspace/%s/conversation/%s/message/%s";
    public static final String THREAD_DIRECT_LINK_URI_TEMPLATE = "workspace/%s/conversation/%s/thread/%s/message/%s";
}
