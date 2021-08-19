package ir.mahdihmb.limoo_storage_bot;

import ir.limoo.driver.LimooDriver;
import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.entity.MessageFile;
import ir.limoo.driver.event.AddedToConversationEventListener;
import ir.limoo.driver.event.AddedToWorkspaceEventListener;
import ir.limoo.driver.event.MessageCreatedEventListener;
import ir.limoo.driver.exception.LimooException;
import ir.mahdihmb.limoo_storage_bot.core.ConfigService;
import ir.mahdihmb.limoo_storage_bot.core.HibernateSessionManager;
import ir.mahdihmb.limoo_storage_bot.core.MessageService;
import ir.mahdihmb.limoo_storage_bot.dao.BaseDAO;
import ir.mahdihmb.limoo_storage_bot.dao.UserDAO;
import ir.mahdihmb.limoo_storage_bot.dao.WorkspaceDAO;
import ir.mahdihmb.limoo_storage_bot.entity.MessageAssignment;
import ir.mahdihmb.limoo_storage_bot.entity.MessageAssignmentsProvider;
import ir.mahdihmb.limoo_storage_bot.entity.User;
import ir.mahdihmb.limoo_storage_bot.entity.Workspace;
import ir.mahdihmb.limoo_storage_bot.util.RequestUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LimooStorageBot {

    private static final transient Logger logger = LoggerFactory.getLogger(LimooStorageBot.class);

    private static final String PERSIAN_QUESTION_MARK = MessageService.get("questionMark");
    private static final String COMMA_MARK = ",";
    private static final String LINE_BREAK = "\n";
    private static final String LINE_BREAKS_REGEX = "[\r\n]";
    private static final String SPACE = " ";
    private static final String BACK_QUOTE = "`";
    private static final Pattern ILLEGAL_NAME_PATTERN = Pattern.compile("^[+*?" + PERSIAN_QUESTION_MARK + "\\-]");

    private static final String COMMAND_PREFIX = MessageService.get("commandPrefix");
    private static final String WORKSPACE_COMMAND_PREFIX = COMMAND_PREFIX + "#";
    private static final String ADD_PREFIX = "+ ";
    private static final String REMOVE_PREFIX = "- ";
    private static final String LIST_PREFIX = "*";
    private static final String UNIQUE_RES_SEARCH_PREFIX = "? ";
    private static final String UNIQUE_RES_SEARCH_PREFIX_PERSIAN = String.format("%s ", PERSIAN_QUESTION_MARK);
    private static final String LIST_RES_SEARCH_PREFIX = "?? ";
    private static final String LIST_RES_SEARCH_PREFIX_PERSIAN = String.format("%1$s%1$s ", PERSIAN_QUESTION_MARK);

    private static final int MAX_NAME_LEN = 200;
    private static final int TEXT_PREVIEW_LEN = 100;
    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000;

    private final LimooDriver limooDriver;
    private final String helpMsg;
    private Conversation reportConversation;
    private long lastTimeSentBugReport = 0;

    public LimooStorageBot(String limooUrl, String botUsername, String botPassword) throws LimooException {
        limooDriver = new LimooDriver(limooUrl, botUsername, botPassword);
        helpMsg = String.format(
                MessageService.get("help"),
                limooDriver.getBot().getDisplayName(),
                ConfigService.get("repo.address")
        );
        try {
            String reportWorkspaceKey = ConfigService.get("admin.reportWorkspaceKey");
            String reportConversationId = ConfigService.get("admin.reportConversationId");
            if (reportWorkspaceKey != null && !reportWorkspaceKey.isEmpty()
                    && reportConversationId != null && !reportConversationId.isEmpty()) {
                ir.limoo.driver.entity.Workspace reportWorkspace = limooDriver.getWorkspaceByKey(reportWorkspaceKey);
                if (reportWorkspace != null) {
                    reportConversation = reportWorkspace.getConversationById(reportConversationId);
                }
            }
        } catch (Throwable throwable) {
            logger.error("", throwable);
        }
    }

    @SuppressWarnings("unchecked")
    public void run() {
        limooDriver.addEventListener(new MessageCreatedEventListener() {
            @Override
            public void onNewMessage(Message message, Conversation conversation) {
                try {
                    String msgText = message.getText().trim();
                    if (!msgText.equals(COMMAND_PREFIX) && !msgText.startsWith(COMMAND_PREFIX + SPACE)
                            && !msgText.equals(WORKSPACE_COMMAND_PREFIX)
                            && !msgText.startsWith(WORKSPACE_COMMAND_PREFIX + SPACE)) {
                        return;
                    }

                    HibernateSessionManager.openSession();
                    User user = UserDAO.getInstance().getOrCreate(message.getUserId());
                    Workspace workspace = WorkspaceDAO.getInstance().getOrCreate(message.getWorkspace().getId());

                    if (msgText.equals(COMMAND_PREFIX) || msgText.equals(WORKSPACE_COMMAND_PREFIX)) {
                        handleHelp(message, conversation);
                        return;
                    }

                    String command;
                    MessageAssignmentsProvider<?> messageAssignmentsProvider;
                    BaseDAO dao;
                    String msgPrefix;
                    if (msgText.startsWith(WORKSPACE_COMMAND_PREFIX + SPACE)) {
                        command = msgText.substring((WORKSPACE_COMMAND_PREFIX + SPACE).length()).trim();
                        messageAssignmentsProvider = workspace;
                        dao = WorkspaceDAO.getInstance();
                        msgPrefix = MessageService.get("workspaceListIndicator") + SPACE;
                    } else {
                        command = msgText.substring((COMMAND_PREFIX + SPACE).length()).trim();
                        messageAssignmentsProvider = user;
                        dao = UserDAO.getInstance();
                        msgPrefix = "";
                    }

                    if (command.startsWith(ADD_PREFIX)) {
                        handleAdd(command, message, conversation, msgPrefix, messageAssignmentsProvider, dao);
                    } else if (command.startsWith(REMOVE_PREFIX)) {
                        handleRemove(command, message, msgPrefix, messageAssignmentsProvider, dao);
                    } else if (command.startsWith(LIST_PREFIX)) {
                        handleList(message, conversation, msgPrefix, messageAssignmentsProvider);
                    } else if (command.startsWith(UNIQUE_RES_SEARCH_PREFIX) || command.startsWith(UNIQUE_RES_SEARCH_PREFIX_PERSIAN)) {
                        handleUniqueResSearch(command, message, conversation, msgPrefix, messageAssignmentsProvider);
                    } else if (command.startsWith(LIST_RES_SEARCH_PREFIX) || command.startsWith(LIST_RES_SEARCH_PREFIX_PERSIAN)) {
                        handleListResSearch(command, message, conversation, msgPrefix, messageAssignmentsProvider);
                    } else {
                        handleGet(command, message, conversation, msgPrefix, messageAssignmentsProvider);
                    }
                } catch (Throwable throwable) {
                    logger.error("", throwable);
                    if (throwable instanceof HibernateException) {
                        try {
                            message.sendInThread(MessageService.get("reportTextForUser"));
                        } catch (LimooException e) {
                            logger.error("", e);
                        }

                        long now = System.currentTimeMillis();
                        if (reportConversation != null && now > lastTimeSentBugReport + ONE_HOUR_MILLIS) {
                            try {
                                String reportMsg = MessageService.get("reportTextForAdmin")
                                        + "\n```java\n"
                                        + throwable.getClass().getName()
                                        + ": "
                                        + throwable.getMessage()
                                        + "\n```";
                                reportConversation.send(reportMsg);
                            } catch (LimooException e) {
                                logger.error("", e);
                            }
                            lastTimeSentBugReport = now;
                        }
                    }
                } finally {
                    HibernateSessionManager.closeCurrentSession();
                    conversation.viewLog();
                }
            }
        });

        limooDriver.addEventListener(new AddedToConversationEventListener() {
            @Override
            public void onAddToConversation(Conversation conversation) {
                try {
                    conversation.send(helpMsg);
                } catch (LimooException e) {
                    logger.error("", e);
                }
            }
        });

        limooDriver.addEventListener(new AddedToWorkspaceEventListener() {
            @Override
            public void onAddToWorkspace(ir.limoo.driver.entity.Workspace workspace) {
                try {
                    workspace.getDefaultConversation().send(helpMsg);
                } catch (LimooException e) {
                    logger.error("", e);
                }
            }
        });
    }

    private void handleHelp(Message message, Conversation conversation) throws LimooException {
        if (message.getThreadRootId() == null)
            conversation.send(helpMsg);
        else
            message.sendInThread(helpMsg);
    }

    private <T> void handleAdd(String command, Message message, Conversation conversation, String msgPrefix,
                               MessageAssignmentsProvider<T> messageAssignmentsProvider,
                               BaseDAO<MessageAssignmentsProvider<T>> dao) throws Throwable {
        String content = command.substring(ADD_PREFIX.length()).trim();
        if (content.isEmpty()) {
            message.sendInThread(MessageService.get("badAddCommand"));
            return;
        }

        String name;
        String text = "";
        List<MessageFile> fileInfos = message.getCreatedFileInfos();
        String messageId = message.getId();

        if (content.contains(LINE_BREAK)) {
            int firstBreakIndex = content.indexOf(LINE_BREAK);
            name = content.substring(0, firstBreakIndex).trim();
            text = content.substring(firstBreakIndex + LINE_BREAK.length()).trim();
        } else {
            name = content;
        }

        String directReplyMessageId = message.getDirectReplyMessageId();
        if (text.isEmpty() && fileInfos.isEmpty() && (directReplyMessageId == null || directReplyMessageId.isEmpty())) {
            message.sendInThread(MessageService.get("badAddCommand"));
            return;
        }

        if (name.isEmpty()) {
            message.sendInThread(MessageService.get("noName"));
            return;
        } else if (name.length() > MAX_NAME_LEN) {
            message.sendInThread(String.format(MessageService.get("tooLongName"), MAX_NAME_LEN));
            return;
        } else if (ILLEGAL_NAME_PATTERN.matcher(name).matches()) {
            message.sendInThread(MessageService.get("illegalName"));
            return;
        }

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.containsKey(name)) {
            message.sendInThread(msgPrefix + MessageService.get("nameExists"));
            return;
        }

        if (text.isEmpty() && fileInfos.isEmpty()) {
            Message directReplyMessage = RequestUtils.getMessage(message.getWorkspace(), conversation.getId(), directReplyMessageId);
            if (directReplyMessage == null) {
                message.sendInThread(MessageService.get("noDirectReplyMessage"));
                return;
            }

            messageId = directReplyMessageId;
            text = directReplyMessage.getText();
            fileInfos = directReplyMessage.getCreatedFileInfos();
        }

        Message msg = new Message.Builder().text(text).fileInfos(fileInfos).build();
        msg.setId(messageId);
        messageAssignmentsProvider.putInMessageAssignmentsMap(name, new MessageAssignment<>(name, messageAssignmentsProvider, msg));
        dao.update(messageAssignmentsProvider);
        message.sendInThread(msgPrefix + MessageService.get("messageAdded"));
    }

    private <T> void handleGet(String name, Message message, Conversation conversation, String msgPrefix,
                               MessageAssignmentsProvider<T> messageAssignmentsProvider) throws LimooException {
        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();

        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        if (!messageAssignmentsMap.containsKey(name)) {
            message.sendInThread(msgPrefix + MessageService.get("noSuchMessage"));
            return;
        }

        Message msg = messageAssignmentsMap.get(name).getMessage();
        if (msg instanceof HibernateProxy)
            msg = (Message) Hibernate.unproxy(msg);

        Message.Builder messageBuilder = new Message.Builder()
                .text(msg.getText())
                .fileInfos(msg.getCreatedFileInfos());

        if (message.getThreadRootId() == null)
            conversation.send(messageBuilder);
        else
            message.sendInThread(messageBuilder);
    }

    private <T> void handleRemove(String command, Message message, String msgPrefix,
                                  MessageAssignmentsProvider<T> messageAssignmentsProvider,
                                  BaseDAO<MessageAssignmentsProvider<T>> dao) throws Throwable {
        String name = command.substring(REMOVE_PREFIX.length()).trim();
        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();

        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        if (!messageAssignmentsMap.containsKey(name)) {
            message.sendInThread(msgPrefix + MessageService.get("noSuchMessage"));
            return;
        }

        messageAssignmentsProvider.removeFromMessageAssignmentsMap(name);
        dao.update(messageAssignmentsProvider);
        message.sendInThread(msgPrefix + MessageService.get("messageRemoved"));
    }

    private <T> String generateMessagesListText(Map<String, MessageAssignment<T>> messageAssignmentsMap) {
        StringBuilder listText = new StringBuilder();
        for (String name : messageAssignmentsMap.keySet()) {
            Message msg = messageAssignmentsMap.get(name).getMessage();
            if (msg instanceof HibernateProxy)
                msg = (Message) Hibernate.unproxy(msg);

            String text = msg.getText();
            String textPreview = text.length() > TEXT_PREVIEW_LEN ? text.substring(0, TEXT_PREVIEW_LEN) : text;
            textPreview = textPreview.replaceAll(LINE_BREAKS_REGEX, SPACE);
            textPreview = textPreview.replaceAll(BACK_QUOTE, "");
            if (text.length() > textPreview.length())
                textPreview += "...";

            name = name.replaceAll(BACK_QUOTE, "");
            listText.append(LINE_BREAK).append("- ").append(String.format(MessageService.get("nameTemplate"), name))
                    .append(" - ").append(String.format(MessageService.get("textTemplate"), textPreview));

            List<MessageFile> fileInfos = msg.getCreatedFileInfos();
            if (!fileInfos.isEmpty()) {
                StringBuilder fileNamesBuilder = new StringBuilder();
                for (int i = 0; i < fileInfos.size(); i++) {
                    if (i > 0)
                        fileNamesBuilder.append(COMMA_MARK).append(SPACE);
                    fileNamesBuilder.append(fileInfos.get(i).getName());
                }
                String fileNames = fileNamesBuilder.toString().replaceAll(BACK_QUOTE, "");
                listText.append(" - ").append(String.format(MessageService.get("filesTemplate"), fileNames));
            }
        }
        return listText.toString();
    }

    private <T> void handleList(Message message, Conversation conversation, String msgPrefix,
                                MessageAssignmentsProvider<T> messageAssignmentsProvider) throws LimooException {
        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String sendingText = msgPrefix + MessageService.get("messagesList")
                + generateMessagesListText(messageAssignmentsMap);

        if (message.getThreadRootId() == null)
            conversation.send(sendingText);
        else
            message.sendInThread(sendingText);
    }

    private <T> void handleUniqueResSearch(String command, Message message, Conversation conversation, String msgPrefix,
                                           MessageAssignmentsProvider<T> messageAssignmentsProvider) throws LimooException {
        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String term = command.substring(UNIQUE_RES_SEARCH_PREFIX.length()).trim();
        String foundName = messageAssignmentsMap.keySet().stream()
                .filter(name -> name.toLowerCase().contains(term.toLowerCase()))
                .findAny().orElse(null);
        if (foundName == null) {
            message.sendInThread(msgPrefix + MessageService.get("noMatches"));
            return;
        }

        Message msg = messageAssignmentsMap.get(foundName).getMessage();
        if (msg instanceof HibernateProxy)
            msg = (Message) Hibernate.unproxy(msg);

        Message.Builder messageBuilder = new Message.Builder()
                .text(msg.getText())
                .fileInfos(msg.getCreatedFileInfos());

        if (message.getThreadRootId() == null)
            conversation.send(messageBuilder);
        else
            message.sendInThread(messageBuilder);
    }

    private <T> void handleListResSearch(String command, Message message, Conversation conversation, String msgPrefix,
                                         MessageAssignmentsProvider<T> messageAssignmentsProvider) throws LimooException {
        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String term = command.substring(LIST_RES_SEARCH_PREFIX.length()).trim();
        List<String> foundNames = messageAssignmentsMap.keySet().stream()
                .filter(name -> name.toLowerCase().contains(term.toLowerCase()))
                .collect(Collectors.toList());
        if (foundNames.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("noMatches"));
            return;
        }

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> filteredMessageAssignmentsMap = new HashMap<>();
        for (String foundName : foundNames) {
            filteredMessageAssignmentsMap.put(foundName, messageAssignmentsMap.get(foundName));
        }

        String sendingText = msgPrefix + MessageService.get("filteredMessagesList")
                + generateMessagesListText(filteredMessageAssignmentsMap);

        if (message.getThreadRootId() == null)
            conversation.send(sendingText);
        else
            message.sendInThread(sendingText);
    }
}
