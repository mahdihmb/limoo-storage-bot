package ir.mahdihmb.limoo_storage_bot.handler;

import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.entity.MessageFile;
import ir.limoo.driver.exception.LimooException;
import ir.mahdihmb.limoo_storage_bot.core.MessageService;
import ir.mahdihmb.limoo_storage_bot.dao.BaseDAO;
import ir.mahdihmb.limoo_storage_bot.dao.UserDAO;
import ir.mahdihmb.limoo_storage_bot.dao.WorkspaceDAO;
import ir.mahdihmb.limoo_storage_bot.entity.MessageAssignment;
import ir.mahdihmb.limoo_storage_bot.entity.MessageAssignmentsProvider;
import ir.mahdihmb.limoo_storage_bot.entity.User;
import ir.mahdihmb.limoo_storage_bot.entity.Workspace;
import ir.mahdihmb.limoo_storage_bot.util.GeneralUtils;
import ir.mahdihmb.limoo_storage_bot.util.RequestUtils;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ir.mahdihmb.limoo_storage_bot.util.Constants.*;

public class UserCommandHandler {

    private Conversation reportConversation;

    public UserCommandHandler(Conversation reportConversation) {
        this.reportConversation = reportConversation;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void handle(Message message, Conversation conversation, User user, Workspace workspace) throws Throwable {
        String msgText = GeneralUtils.trimSpaces(message.getText());
        String command;
        MessageAssignmentsProvider<?> messageAssignmentsProvider;
        BaseDAO dao;
        String msgPrefix = "";
        boolean isWorkspaceCommand = false;
        if (msgText.startsWith(WORKSPACE_COMMAND_PREFIX + SPACE)) {
            command = GeneralUtils.trimSpaces(msgText.substring((WORKSPACE_COMMAND_PREFIX + SPACE).length()));
            messageAssignmentsProvider = workspace;
            dao = WorkspaceDAO.getInstance();
            msgPrefix = MessageService.get("workspaceListIndicator") + SPACE;
            isWorkspaceCommand = true;
        } else {
            command = GeneralUtils.trimSpaces(msgText.substring((COMMAND_PREFIX + SPACE).length()));
            messageAssignmentsProvider = user;
            dao = UserDAO.getInstance();
        }

        if (command.startsWith(ADD_PREFIX)) {
            handleAdd(command, message, conversation, msgPrefix, messageAssignmentsProvider, dao);
        } else if (command.startsWith(REMOVE_PREFIX)) {
            handleRemove(command, message, msgPrefix, messageAssignmentsProvider, dao);
        } else if (command.startsWith(FEEDBACK_PREFIX)) {
            handleFeedback(command, message);
        } else if (command.startsWith(LIST_PREFIX)) {
            handleList(message, conversation, msgPrefix, messageAssignmentsProvider, isWorkspaceCommand);
        } else if (command.startsWith(LIST_RES_SEARCH_PREFIX) || command.startsWith(LIST_RES_SEARCH_PREFIX_PERSIAN)) {
            handleListResSearch(command, message, conversation, msgPrefix, messageAssignmentsProvider, isWorkspaceCommand);
        } else if (command.startsWith(UNIQUE_RES_SEARCH_PREFIX) || command.startsWith(UNIQUE_RES_SEARCH_PREFIX_PERSIAN)) {
            handleUniqueResSearch(command, message, conversation, msgPrefix, messageAssignmentsProvider);
        } else if (!ILLEGAL_NAME_PATTERN.matcher(command).matches()) {
            handleGet(command, message, conversation, msgPrefix, messageAssignmentsProvider);
        }
    }

    private <T> void handleAdd(String command, Message message, Conversation conversation, String msgPrefix,
                               MessageAssignmentsProvider<T> messageAssignmentsProvider,
                               BaseDAO<MessageAssignmentsProvider<T>> dao) throws Throwable {
        String temp = command.substring(ADD_PREFIX.length());
        String content = GeneralUtils.trimSpaces(temp);
        if (content.isEmpty() || !temp.startsWith(SPACE)) {
            message.sendInThread(MessageService.get("badCommand"));
            return;
        }

        String name;
        String text = "";
        List<MessageFile> fileInfos = message.getCreatedFileInfos();
        String messageId = message.getId();

        if (content.contains(LINE_BREAK)) {
            int firstBreakIndex = content.indexOf(LINE_BREAK);
            name = GeneralUtils.trimSpaces(content.substring(0, firstBreakIndex));
            text = GeneralUtils.trimSpaces(content.substring(firstBreakIndex + LINE_BREAK.length()));
        } else {
            name = content;
        }

        String directReplyMessageId = message.getDirectReplyMessageId();
        String threadRootId = message.getThreadRootId();
        if (text.isEmpty() && fileInfos.isEmpty() && (directReplyMessageId == null || directReplyMessageId.isEmpty())) {
            if (threadRootId == null)
                message.sendInThread(MessageService.get("emptyBodyAddCommandInConversation"));
            else
                message.sendInThread(MessageService.get("emptyBodyAddCommandInThread"));
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

        Message msg = new Message();
        msg.setId(messageId);
        msg.setText(text);
        msg.setFileInfos(fileInfos);
        msg.setWorkspaceId(message.getWorkspace().getId());
        msg.setConversationId(message.getConversationId());
        msg.setThreadRootId(threadRootId == null || threadRootId.equals(messageId) ? null : threadRootId);
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

        GeneralUtils.sendInThreadOrConversation(message, conversation, messageBuilder);
    }

    private <T> void handleRemove(String command, Message message, String msgPrefix,
                                  MessageAssignmentsProvider<T> messageAssignmentsProvider,
                                  BaseDAO<MessageAssignmentsProvider<T>> dao) throws Throwable {
        String notTrimmedName = command.substring(REMOVE_PREFIX.length());
        String name = GeneralUtils.trimSpaces(notTrimmedName);
        if (name.isEmpty()) {
            message.sendInThread(MessageService.get("noName"));
            return;
        }
        if (!notTrimmedName.startsWith(SPACE)) {
            message.sendInThread(MessageService.get("badCommand"));
            return;
        }

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

    private void handleFeedback(String command, Message message) throws Throwable {
        String feedbackText = GeneralUtils.trimSpaces(command.substring(FEEDBACK_PREFIX.length()));
        List<MessageFile> fileInfos = message.getCreatedFileInfos();
        if (feedbackText.isEmpty() && fileInfos.isEmpty())
            return;

        if (reportConversation == null) {
            message.sendInThread(MessageService.get("feedbackNotSupported"));
            return;
        }

        ir.limoo.driver.entity.User user = RequestUtils.getUser(message.getWorkspace(), message.getUserId());
        String userDisplayName = user != null ? user.getDisplayName() : MessageService.get("unknownUser");
        String reportMsg = String.format(MessageService.get("feedbackReportTextForAdmin"), userDisplayName)
                + LINE_BREAK + feedbackText;
        Message.Builder messageBuilder = new Message.Builder()
                .text(reportMsg)
                .fileInfos(fileInfos);
        reportConversation.send(messageBuilder);
        message.sendInThread(MessageService.get("feedbackSent"));
    }

    private <T> List<String> generateMessagesListBatches(Map<String, MessageAssignment<T>> messageAssignmentsMap,
                                                         boolean isWorkspaceCommand) {
        List<String> batches = new ArrayList<>();
        StringBuilder listTextBuilder = new StringBuilder();
        int counter = 0;
        for (String name : messageAssignmentsMap.keySet()) {
            String getLinkTemplateKey = isWorkspaceCommand ? "getLinkTemplateForWorkspace" : "getLinkTemplateForUser";
            listTextBuilder.append(LINE_BREAK)
                    .append("- ").append(String.format(MessageService.get(getLinkTemplateKey), name));

            Message msg = messageAssignmentsMap.get(name).getMessage();
            if (msg instanceof HibernateProxy)
                msg = (Message) Hibernate.unproxy(msg);

            String text = msg.getText();
            String textPreview = text.length() > TEXT_PREVIEW_LEN ? text.substring(0, TEXT_PREVIEW_LEN) : text;
            textPreview = textPreview.replaceAll(LINE_BREAKS_REGEX, SPACE);
            textPreview = textPreview.replaceAll(BACK_QUOTE, "");
            if (text.length() > textPreview.length())
                textPreview += "...";

            if (!textPreview.isEmpty())
                listTextBuilder.append(" - ").append(String.format(MessageService.get("textTemplate"), textPreview));

            int filesCount = msg.getCreatedFileInfos().size();
            if (filesCount > 0)
                listTextBuilder.append(" - ").append(String.format(MessageService.get("filesTemplate"), filesCount));

            counter += 1;
            if (counter % MESSAGES_LIST_BATCH_SIZE == 0) {
                batches.add(listTextBuilder.toString());
                listTextBuilder = new StringBuilder();
            }
        }
        if (!listTextBuilder.toString().isEmpty()) {
            batches.add(listTextBuilder.toString());
        }
        return batches;
    }

    private <T> void handleList(Message message, Conversation conversation, String msgPrefix,
                                MessageAssignmentsProvider<T> messageAssignmentsProvider,
                                boolean isWorkspaceCommand) throws LimooException {
        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        List<String> messagesListBatches = generateMessagesListBatches(messageAssignmentsMap, isWorkspaceCommand);
        for (int i = 0; i < messagesListBatches.size(); i++) {
            String messagesListKey = i == 0 ? "messagesList" : "messagesListRest";
            String sendingText = msgPrefix + String.format(MessageService.get(messagesListKey), messagesListBatches.get(i));
            GeneralUtils.sendInThreadOrConversation(message, conversation, sendingText);
        }
    }

    private <T> void handleUniqueResSearch(String command, Message message, Conversation conversation, String msgPrefix,
                                           MessageAssignmentsProvider<T> messageAssignmentsProvider) throws LimooException {
        String notTrimmedQuery = command.substring(UNIQUE_RES_SEARCH_PREFIX.length());
        String query = GeneralUtils.trimSpaces(notTrimmedQuery);
        if (query.isEmpty()) {
            message.sendInThread(MessageService.get("noQuery"));
            return;
        }
        if (!notTrimmedQuery.startsWith(SPACE)) {
            message.sendInThread(MessageService.get("badCommand"));
            return;
        }

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String[] keywords = query.split(" +");
        String foundName = messageAssignmentsMap.keySet().stream()
                .filter(name -> {
                    for (String keyword : keywords) {
                        if (!name.toLowerCase().contains(keyword.toLowerCase()))
                            return false;
                    }
                    return true;
                })
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

        GeneralUtils.sendInThreadOrConversation(message, conversation, messageBuilder);
    }

    private <T> void handleListResSearch(String command, Message message, Conversation conversation, String msgPrefix,
                                         MessageAssignmentsProvider<T> messageAssignmentsProvider,
                                         boolean isWorkspaceCommand) throws LimooException {
        String notTrimmedQuery = command.substring(LIST_RES_SEARCH_PREFIX.length());
        String query = GeneralUtils.trimSpaces(notTrimmedQuery);
        if (query.isEmpty()) {
            message.sendInThread(MessageService.get("noQuery"));
            return;
        }
        if (!notTrimmedQuery.startsWith(SPACE)) {
            message.sendInThread(MessageService.get("badCommand"));
            return;
        }

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap
                = messageAssignmentsProvider.getCreatedMessageAssignmentsMap();
        if (messageAssignmentsMap.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String[] keywords = query.split(" +");
        List<String> foundNames = messageAssignmentsMap.keySet().stream()
                .filter(name -> {
                    for (String keyword : keywords) {
                        if (!name.toLowerCase().contains(keyword.toLowerCase()))
                            return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        if (foundNames.isEmpty()) {
            message.sendInThread(msgPrefix + MessageService.get("noMatches"));
            return;
        }

        Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> filteredMessageAssignmentsMap = new HashMap<>();
        for (String foundName : foundNames) {
            filteredMessageAssignmentsMap.put(foundName, messageAssignmentsMap.get(foundName));
        }

        List<String> messagesListBatches = generateMessagesListBatches(filteredMessageAssignmentsMap, isWorkspaceCommand);
        for (int i = 0; i < messagesListBatches.size(); i++) {
            String messagesListKey = i == 0 ? "filteredMessagesList" : "filteredMessagesListRest";
            String sendingText = msgPrefix + String.format(MessageService.get(messagesListKey), messagesListBatches.get(i));
            GeneralUtils.sendInThreadOrConversation(message, conversation, sendingText);
        }
    }
}
