package ir.mahdihmb.limoo_storage_bot.handler;

import ir.limoo.driver.LimooDriver;
import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.entity.MessageFile;
import ir.limoo.driver.exception.LimooException;
import ir.limoo.driver.util.MessageUtils;
import ir.mahdihmb.limoo_storage_bot.core.HibernateSessionManager;
import ir.mahdihmb.limoo_storage_bot.core.MessageService;
import ir.mahdihmb.limoo_storage_bot.dao.BaseDAO;
import ir.mahdihmb.limoo_storage_bot.dao.FeedbackDAO;
import ir.mahdihmb.limoo_storage_bot.dao.UserDAO;
import ir.mahdihmb.limoo_storage_bot.dao.WorkspaceDAO;
import ir.mahdihmb.limoo_storage_bot.entity.*;
import ir.mahdihmb.limoo_storage_bot.exception.BotException;
import ir.mahdihmb.limoo_storage_bot.util.RequestUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ir.mahdihmb.limoo_storage_bot.util.Constants.*;
import static ir.mahdihmb.limoo_storage_bot.util.GeneralUtils.*;

public class UserCommandHandler extends Thread {

    private static final transient Logger logger = LoggerFactory.getLogger(UserCommandHandler.class);

    private final String limooUrl;
    private final LimooDriver limooDriver;
    private final Message message;
    private final Conversation conversation;
    private final Conversation reportConversation;

    private static long lastTimeSentBugReport = 0;
    private boolean isWorkspaceCommand = false;
    private String msgPrefix = "";

    public UserCommandHandler(String limooUrl, LimooDriver limooDriver, Message message, Conversation conversation,
                              Conversation reportConversation) {
        this.limooUrl = limooUrl;
        this.limooDriver = limooDriver;
        this.message = message;
        this.conversation = conversation;
        this.reportConversation = reportConversation;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void run() {
        try {
            HibernateSessionManager.openSession();
            User user = UserDAO.getInstance().getOrCreate(message.getUserId());
            Workspace workspace = WorkspaceDAO.getInstance().getOrCreate(message.getWorkspace().getId());

            String msgText = message.getText().trim();
            String command;
            MessageAssignmentsProvider<?> messageAssignmentsProvider;
            BaseDAO dao;
            if (msgText.startsWith(WORKSPACE_COMMAND_PREFIX + SPACE)) {
                command = trimSpaces(msgText.substring((WORKSPACE_COMMAND_PREFIX + SPACE).length()));
                messageAssignmentsProvider = workspace;
                dao = WorkspaceDAO.getInstance();
                isWorkspaceCommand = true;
                msgPrefix = MessageService.get("workspaceListIndicator") + SPACE;
            } else {
                command = trimSpaces(msgText.substring((COMMAND_PREFIX + SPACE).length()));
                messageAssignmentsProvider = user;
                dao = UserDAO.getInstance();
            }

            if (command.startsWith(ADD_PREFIX)) {
                handleAdd(command, messageAssignmentsProvider, dao);
            } else if (command.startsWith(GET_PREFIX)) {
                handleGet(command, messageAssignmentsProvider);
            } else if (command.startsWith(REMOVE_PREFIX)) {
                handleRemove(command, messageAssignmentsProvider, dao);
            } else if (command.startsWith(FEEDBACK_PREFIX)) {
                handleFeedback(command);
            } else if (command.startsWith(LIST_PREFIX)) {
                handleList(command, messageAssignmentsProvider);
            } else if (command.startsWith(SEARCH_PREFIX) || command.startsWith(SEARCH_PREFIX_PERSIAN)) {
                handleSearch(command, messageAssignmentsProvider);
            } else {
                handleGetByKeywords(command, messageAssignmentsProvider);
            }
        } catch (Throwable throwable) {
            handleException(throwable);
        } finally {
            HibernateSessionManager.closeCurrentSession();
        }
    }

    private void handleException(Throwable throwable) {
        if (throwable instanceof BotException) {
            try {
                message.sendInThread(throwable.getMessage());
            } catch (LimooException e) {
                logger.error("", e);
            }
        } else {
            logger.error("", throwable);
            if (throwable instanceof HibernateException) {
                try {
                    message.sendInThread(MessageService.get("bugReportTextForUser"));
                } catch (LimooException e) {
                    logger.error("", e);
                }

                long now = System.currentTimeMillis();
                if (reportConversation != null && now > lastTimeSentBugReport + ONE_HOUR_MILLIS) {
                    try {
                        String reportMsg = MessageService.get("bugReportTextForAdminDetailed") + LINE_BREAK
                                + "```java" + LINE_BREAK
                                + getMessageOfThrowable(throwable) + LINE_BREAK
                                + "```";
                        reportConversation.send(reportMsg);
                    } catch (LimooException e) {
                        logger.error("", e);
                    }
                    lastTimeSentBugReport = now;
                }
            }
        }
    }

    private <T> void handleAdd(String command, MessageAssignmentsProvider<T> messageAssignmentsProvider,
                               BaseDAO<MessageAssignmentsProvider<T>> dao) throws BotException, LimooException {
        String notTrimmedName = command.substring(ADD_PREFIX.length());
        String name = notTrimmedName.trim();
        if (name.contains(LINE_BREAK))
            name = name.substring(0, name.indexOf(LINE_BREAK)).trim();

        if (name.isEmpty())
            throw BotException.createWithI18n("noName");
        if (!notTrimmedName.startsWith(SPACE))
            throw BotException.createWithI18n("badCommand");

        String threadRootId = message.getThreadRootId();
        if (empty(threadRootId))
            throw BotException.createWithI18n("addCommandInConversation");

        String directReplyMessageId = message.getDirectReplyMessageId();
        if (empty(directReplyMessageId))
            throw BotException.createWithI18n("noReplyInThread");

        if (name.length() > MAX_NAME_LEN)
            throw BotException.create(String.format(MessageService.get("tooLongName"), MAX_NAME_LEN));
        if (ILLEGAL_NAME_PATTERN.matcher(name).matches())
            throw BotException.createWithI18n("illegalName");

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.containsKey(name))
            throw BotException.createWithI18n("nameExists", msgPrefix);

        Message directReplyMessage = RequestUtils.getMessage(message.getWorkspace(), message.getConversationId(), directReplyMessageId);
        if (directReplyMessage == null)
            throw BotException.createWithI18n("noDirectReplyMessage");

        Message msg = new Message();
        msg.setId(directReplyMessageId);
        msg.setText(directReplyMessage.getText());
        msg.setFileInfos(directReplyMessage.getCreatedFileInfos());
        msg.setWorkspaceKey(message.getWorkspace().getKey());
        msg.setConversationId(message.getConversationId());
        msg.setThreadRootId(threadRootId.equals(directReplyMessageId) ? null : threadRootId);
        messageAssignmentsProvider.putInMessageAssignmentsMap(name, new MessageAssignment<>(name, messageAssignmentsProvider, msg));
        dao.update(messageAssignmentsProvider);
        message.sendInThread(successText(msgPrefix + MessageService.get("messageAdded")));
    }

    private <T> void handleGet(String command, MessageAssignmentsProvider<T> messageAssignmentsProvider)
            throws BotException, LimooException {
        String notTrimmedName = command.substring(GET_PREFIX.length());
        String name = notTrimmedName.trim();
        if (name.contains(LINE_BREAK))
            name = name.substring(0, name.indexOf(LINE_BREAK)).trim();

        if (name.isEmpty())
            throw BotException.createWithI18n("noName");
        if (!notTrimmedName.startsWith(SPACE))
            throw BotException.createWithI18n("badCommand");

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();

        if (messageAssignmentsMap.isEmpty())
            throw BotException.createWithI18n("dontHaveAnyMessages", msgPrefix);
        if (!messageAssignmentsMap.containsKey(name))
            throw BotException.createWithI18n("noSuchMessage", msgPrefix);

        sendSingleResult(messageAssignmentsMap.get(name).getMessage(), name);
    }

    private <T> void handleGetByKeywords(String command, MessageAssignmentsProvider<T> messageAssignmentsProvider)
            throws BotException, LimooException {
        String query = command;
        if (query.contains(LINE_BREAK))
            query = query.substring(0, query.indexOf(LINE_BREAK)).trim();

        if (query.isEmpty())
            throw BotException.createWithI18n("noQuery");

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty())
            throw BotException.createWithI18n("dontHaveAnyMessages", msgPrefix);

        String foundName = filterKeywords(messageAssignmentsMap.keySet().stream(), query)
                .findAny().orElse(null);
        if (foundName == null)
            throw BotException.createWithI18n("noMatches", msgPrefix);

        sendSingleResult(messageAssignmentsMap.get(foundName).getMessage(), foundName);
    }

    private <T> void handleRemove(String command, MessageAssignmentsProvider<T> messageAssignmentsProvider,
                                  BaseDAO<MessageAssignmentsProvider<T>> dao) throws BotException, LimooException {
        String notTrimmedName = command.substring(REMOVE_PREFIX.length());
        String name = notTrimmedName.trim();
        if (name.contains(LINE_BREAK))
            name = name.substring(0, name.indexOf(LINE_BREAK)).trim();

        if (name.isEmpty())
            throw BotException.createWithI18n("noName");
        if (!notTrimmedName.startsWith(SPACE))
            throw BotException.createWithI18n("badCommand");

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();

        if (messageAssignmentsMap.isEmpty())
            throw BotException.createWithI18n("dontHaveAnyMessages", msgPrefix);
        if (!messageAssignmentsMap.containsKey(name))
            throw BotException.createWithI18n("noSuchMessage", msgPrefix);

        messageAssignmentsProvider.removeFromMessageAssignmentsMap(name);
        dao.update(messageAssignmentsProvider);
        message.sendInThread(successText(msgPrefix + MessageService.get("messageRemoved")));
    }

    private void handleFeedback(String command) throws BotException, LimooException {
        String feedbackText = command.substring(FEEDBACK_PREFIX.length()).trim();
        List<MessageFile> fileInfos = message.getCreatedFileInfos();
        if (feedbackText.isEmpty() && fileInfos.isEmpty())
            throw BotException.createWithI18n("badCommand");
        if (reportConversation == null)
            throw BotException.createWithI18n("feedbackNotSupported");

        ir.limoo.driver.entity.User user = RequestUtils.getUser(message.getWorkspace(), message.getUserId());
        String userDisplayName = user != null ? user.getDisplayName() : MessageService.get("unknownUser");
        String reportMsg = String.format(MessageService.get("feedbackReportTextForAdmin"), userDisplayName) + LINE_BREAK + feedbackText;
        Message.Builder messageBuilder = new Message.Builder().text(reportMsg).fileInfos(fileInfos);

        if (message.getThreadRootId() != null) {
            Feedback rootFeedback = FeedbackDAO.getInstance().getByUserThreadRootId(message.getThreadRootId());
            if (rootFeedback != null) {
                messageBuilder.threadRootId(rootFeedback.getAdminThreadRootId());
                MessageUtils.sendMessage(
                        messageBuilder,
                        limooDriver.getWorkspaceById(rootFeedback.getAdminWorkspaceId()),
                        rootFeedback.getAdminConversationId()
                );
                RequestUtils.reactToMessage(message.getWorkspace(), message.getConversationId(), message.getId(), SEEN_REACTION);
                return;
            }
        }

        Message sentMdgForAdmin = reportConversation.send(messageBuilder);
        RequestUtils.reactToMessage(message.getWorkspace(), message.getConversationId(), message.getId(), SEEN_REACTION);
        message.sendInThread(MessageService.get("feedbackSent"));

        String threadId = empty(message.getThreadRootId()) ? message.getId() : message.getThreadRootId();
        Feedback feedback = new Feedback(
                message.getUserId(), message.getWorkspace().getId(), message.getConversationId(), threadId,
                reportConversation.getWorkspace().getId(), reportConversation.getId(), sentMdgForAdmin.getId()
        );
        FeedbackDAO.getInstance().add(feedback);
    }

    private <T> void handleList(String command, MessageAssignmentsProvider<T> messageAssignmentsProvider)
            throws BotException, LimooException {
        String content = command.substring(LIST_PREFIX.length()).trim();
        if (content.contains(LINE_BREAK))
            content = content.substring(0, content.indexOf(LINE_BREAK)).trim();

        if (!content.isEmpty())
            throw BotException.createWithI18n("badCommand");

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty())
            throw BotException.createWithI18n("dontHaveAnyMessages", msgPrefix);

        sendListResult(messageAssignmentsMap, "messagesList", "messagesListRest");
    }

    private <T> void handleSearch(String command, MessageAssignmentsProvider<T> messageAssignmentsProvider)
            throws BotException, LimooException {
        String notTrimmedQuery = command.substring(SEARCH_PREFIX.length());
        String query = notTrimmedQuery.trim();
        if (query.contains(LINE_BREAK))
            query = query.substring(0, query.indexOf(LINE_BREAK)).trim();

        if (query.isEmpty())
            throw BotException.createWithI18n("noQuery");
        if (!notTrimmedQuery.startsWith(SPACE))
            throw BotException.createWithI18n("badCommand");

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty())
            throw BotException.createWithI18n("dontHaveAnyMessages", msgPrefix);

        List<String> foundNames = filterKeywords(messageAssignmentsMap.keySet().stream(), query)
                .collect(Collectors.toList());
        if (foundNames.isEmpty())
            throw BotException.createWithI18n("noMatches", msgPrefix);

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> filteredMessageAssignmentsMap = new HashMap<>();
        for (String foundName : foundNames) {
            filteredMessageAssignmentsMap.put(foundName, messageAssignmentsMap.get(foundName));
        }

        sendListResult(filteredMessageAssignmentsMap, "filteredMessagesList", "filteredMessagesListRest");
    }

    private void sendSingleResult(Message msg, String name) throws LimooException {
        if (msg instanceof HibernateProxy)
            msg = (Message) Hibernate.unproxy(msg);

        StringBuilder textBuilder = new StringBuilder(RTL_CONTROL_CHAR);
        String directLink = generateDirectLink(msg, limooUrl);
        if (directLink != null)
            textBuilder.append(directLink).append(SPACE);
        textBuilder.append(italicBold(name)).append(COLON).append(LINE_BREAK).append(msg.getText());

        Message.Builder messageBuilder = new Message.Builder()
                .text(textBuilder.toString())
                .fileInfos(msg.getCreatedFileInfos());
        sendInThreadOrConversation(messageBuilder);
    }

    private <T> void sendListResult(Map<String, MessageAssignment<T>> messageAssignmentsMap,
                                    String messagesListKey, String messagesListRestKey) throws LimooException {
        List<String> batches = new ArrayList<>();
        StringBuilder singleTextBuilder = new StringBuilder();
        StringBuilder listTextBuilder = new StringBuilder();
        for (String name : messageAssignmentsMap.keySet()) {
            Message msg = messageAssignmentsMap.get(name).getMessage();
            if (msg instanceof HibernateProxy)
                msg = (Message) Hibernate.unproxy(msg);

            String directLink = generateDirectLink(msg, limooUrl);
            if (directLink == null)
                directLink = LINK_EMOJI;
            String getLinkTemplateKey = isWorkspaceCommand ? "getLinkTemplateForWorkspace" : "getLinkTemplateForUser";
            singleTextBuilder.append(LINE_BREAK)
                    .append(RTL_CONTROL_CHAR).append(directLink).append(SPACE)
                    .append(String.format(MessageService.get(getLinkTemplateKey), name));

            String text = msg.getText();
            String textPreview = text.length() > TEXT_PREVIEW_LEN ? text.substring(0, TEXT_PREVIEW_LEN) : text;
            textPreview = textPreview.replaceAll(LINE_BREAKS_REGEX, SPACE);
            textPreview = textPreview.replaceAll(BACK_QUOTE, "");
            if (text.length() > textPreview.length())
                textPreview += "...";

            if (!textPreview.isEmpty())
                singleTextBuilder.append(" - ").append(String.format(MessageService.get("textTemplate"), textPreview));

            int filesCount = msg.getCreatedFileInfos().size();
            if (filesCount > 0)
                singleTextBuilder.append(" - ").append(String.format(MessageService.get("filesTemplate"), filesCount));

            if (singleTextBuilder.length() + listTextBuilder.length() > MAX_MESSAGE_LEN) {
                batches.add(listTextBuilder.toString());
                listTextBuilder = new StringBuilder();
            }

            listTextBuilder.append(singleTextBuilder);
            singleTextBuilder = new StringBuilder();
        }
        if (listTextBuilder.length() > 0) {
            batches.add(listTextBuilder.toString());
        }

        for (int i = 0; i < batches.size(); i++) {
            String msgHeader = MessageService.get(i == 0 ? messagesListKey : messagesListRestKey);
            String sendingText = msgPrefix + msgHeader + batches.get(i);
            sendInThreadOrConversation(sendingText);
        }
    }

    private void sendInThreadOrConversation(String sendingText) throws LimooException {
        if (empty(message.getThreadRootId()))
            conversation.send(sendingText);
        else
            message.sendInThread(sendingText);
    }

    private void sendInThreadOrConversation(Message.Builder messageBuilder) throws LimooException {
        if (empty(message.getThreadRootId()))
            conversation.send(messageBuilder);
        else
            message.sendInThread(messageBuilder);
    }
}
