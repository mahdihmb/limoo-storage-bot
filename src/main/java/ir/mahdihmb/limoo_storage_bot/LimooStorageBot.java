package ir.mahdihmb.limoo_storage_bot;

import ir.limoo.driver.LimooDriver;
import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.entity.MessageFile;
import ir.limoo.driver.event.AddToConversationEventListener;
import ir.limoo.driver.event.MessageCreatedEventListener;
import ir.limoo.driver.exception.LimooException;
import ir.mahdihmb.limoo_storage_bot.core.ConfigService;
import ir.mahdihmb.limoo_storage_bot.core.HibernateSessionManager;
import ir.mahdihmb.limoo_storage_bot.core.MessageService;
import ir.mahdihmb.limoo_storage_bot.dao.UserDAO;
import ir.mahdihmb.limoo_storage_bot.entity.User;
import ir.mahdihmb.limoo_storage_bot.entity.UserMessage;
import ir.mahdihmb.limoo_storage_bot.util.RequestUtils;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LimooStorageBot {

    private static final transient Logger logger = LoggerFactory.getLogger(LimooStorageBot.class);

    private static final String PERSIAN_QUESTION_MARK = MessageService.get("questionMark");
    private static final String PERSIAN_COMMA_MARK = MessageService.get("commaMark");
    private static final String LINE_BREAK = "\n";
    private static final String LINE_BREAKS_REGEX = "[\r\n]";
    private static final String SPACE = " ";
    private static final String BACK_QUOTE = "`";
    private static final String COMMAND_PREFIX = MessageService.get("commandPrefix");
    private static final String HELP_PREFIX = "!";
    private static final String ADD_PREFIX = "+ ";
    private static final String REMOVE_PREFIX = "- ";
    private static final String LIST_PREFIX = "*";
    private static final String UNIQUE_RES_SEARCH_PREFIX = "? ";
    private static final String UNIQUE_RES_SEARCH_PREFIX_PERSIAN = String.format("%s ", PERSIAN_QUESTION_MARK);
    private static final String LIST_RES_SEARCH_PREFIX = "?? ";
    private static final String LIST_RES_SEARCH_PREFIX_PERSIAN = String.format("%1$s%1$s ", PERSIAN_QUESTION_MARK);
    private static final int MAX_NAME_LEN = 200;
    private static final int TEXT_PREVIEW_LEN = 100;

    private final LimooDriver limooDriver;
    private final String helpMsg;

    public LimooStorageBot(String limooUrl, String botUsername, String botPassword) throws LimooException {
        limooDriver = new LimooDriver(limooUrl, botUsername, botPassword);
        helpMsg = String.format(
                MessageService.get("help"),
                limooDriver.getBot().getDisplayName(),
                ConfigService.get("repo.address")
        );
    }

    public void run() {
        limooDriver.addEventListener(new MessageCreatedEventListener() {
            @Override
            public void onNewMessage(Message message, Conversation conversation) {
                try {
                    String msgText = message.getText().trim();
                    if (!msgText.equals(COMMAND_PREFIX) && !msgText.startsWith(COMMAND_PREFIX + SPACE)) {
                        return;
                    }

                    HibernateSessionManager.openSession();
                    User user = UserDAO.getInstance().getOrCreate(message.getUserId());

                    if (msgText.equals(COMMAND_PREFIX)) {
                        handleHelp(message, conversation);
                        return;
                    }

                    String command = msgText.substring((COMMAND_PREFIX + SPACE).length()).trim();
                    if (command.isEmpty()) {
                        handleHelp(message, conversation);
                        return;
                    }

                    if (command.startsWith(HELP_PREFIX)) {
                        handleHelp(message, conversation);
                    } else if (command.startsWith(ADD_PREFIX)) {
                        handleAdd(command, message, conversation, user);
                    } else if (command.startsWith(REMOVE_PREFIX)) {
                        handleRemove(command, message, user);
                    } else if (command.startsWith(LIST_PREFIX)) {
                        handleList(message, conversation, user);
                    } else if (command.startsWith(UNIQUE_RES_SEARCH_PREFIX) || command.startsWith(UNIQUE_RES_SEARCH_PREFIX_PERSIAN)) {
                        handleUniqueResSearch(command, message, conversation, user);
                    } else if (command.startsWith(LIST_RES_SEARCH_PREFIX) || command.startsWith(LIST_RES_SEARCH_PREFIX_PERSIAN)) {
                        handleListResSearch(command, message, conversation, user);
                    } else {
                        handleGet(command, message, conversation, user);
                    }
                } catch (LimooException e) {
                    logger.error("", e);
                } finally {
                    HibernateSessionManager.closeCurrentSession();
                    conversation.viewLog();
                }
            }
        });

        limooDriver.addEventListener(new AddToConversationEventListener() {
            @Override
            public void onAddToConversation(Conversation conversation) {
                try {
                    conversation.send(helpMsg);
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

    private void handleAdd(String command, Message message, Conversation conversation, User user) throws LimooException {
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
        }

        final Map<String, UserMessage> userMessagesAssignments = user.getCreatedUserMessagesAssignments();
        if (userMessagesAssignments.containsKey(name)) {
            message.sendInThread(MessageService.get("nameExists"));
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
        user.putInUserMessagesAssignments(name, new UserMessage(name, user, msg));
        UserDAO.getInstance().update(user);
        message.sendInThread(MessageService.get("messageAdded"));
    }

    private void handleGet(String name, Message message, Conversation conversation, User user) throws LimooException {
        final Map<String, UserMessage> userMessagesAssignments = user.getCreatedUserMessagesAssignments();
        if (!userMessagesAssignments.containsKey(name)) {
            message.sendInThread(MessageService.get("noSuchMessage"));
            return;
        }

        Message msg = userMessagesAssignments.get(name).getMessage();
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

    private void handleRemove(String command, Message message, User user) throws LimooException {
        String name = command.substring(REMOVE_PREFIX.length()).trim();
        final Map<String, UserMessage> userMessagesAssignments = user.getCreatedUserMessagesAssignments();
        if (!userMessagesAssignments.containsKey(name)) {
            message.sendInThread(MessageService.get("noSuchMessage"));
            return;
        }

        user.removeFromUserMessagesAssignments(name);
        UserDAO.getInstance().update(user);
        message.sendInThread(MessageService.get("messageRemoved"));
    }

    private String generateMessagesListText(Map<String, UserMessage> userMessagesAssignments) {
        StringBuilder listText = new StringBuilder(MessageService.get("messagesList"));
        for (String name : userMessagesAssignments.keySet()) {
            Message msg = userMessagesAssignments.get(name).getMessage();
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
                        fileNamesBuilder.append(PERSIAN_COMMA_MARK).append(SPACE);
                    fileNamesBuilder.append(fileInfos.get(i).getName());
                }
                String fileNames = fileNamesBuilder.toString().replaceAll(BACK_QUOTE, "");
                listText.append(" - ").append(String.format(MessageService.get("filesTemplate"), fileNames));
            }
        }
        return listText.toString();
    }

    private void handleList(Message message, Conversation conversation, User user) throws LimooException {
        final Map<String, UserMessage> userMessagesAssignments = user.getCreatedUserMessagesAssignments();
        if (userMessagesAssignments.isEmpty()) {
            message.sendInThread(MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String sendingText = generateMessagesListText(userMessagesAssignments);

        if (message.getThreadRootId() == null)
            conversation.send(sendingText);
        else
            message.sendInThread(sendingText);
    }

    private void handleUniqueResSearch(String command, Message message, Conversation conversation, User user) throws LimooException {
        final Map<String, UserMessage> userMessagesAssignments = user.getCreatedUserMessagesAssignments();
        if (userMessagesAssignments.isEmpty()) {
            message.sendInThread(MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String term = command.substring(UNIQUE_RES_SEARCH_PREFIX.length()).trim();
        String foundName = userMessagesAssignments.keySet().stream()
                .filter(name -> name.toLowerCase().contains(term.toLowerCase()))
                .findAny().orElse(null);
        if (foundName == null) {
            message.sendInThread(MessageService.get("noMatches"));
            return;
        }

        Message msg = userMessagesAssignments.get(foundName).getMessage();
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

    private void handleListResSearch(String command, Message message, Conversation conversation, User user) throws LimooException {
        final Map<String, UserMessage> userMessagesAssignments = user.getCreatedUserMessagesAssignments();
        if (userMessagesAssignments.isEmpty()) {
            message.sendInThread(MessageService.get("dontHaveAnyMessages"));
            return;
        }

        String term = command.substring(UNIQUE_RES_SEARCH_PREFIX.length()).trim();
        List<String> foundNames = userMessagesAssignments.keySet().stream()
                .filter(name -> name.toLowerCase().contains(term.toLowerCase()))
                .collect(Collectors.toList());
        if (foundNames.isEmpty()) {
            message.sendInThread(MessageService.get("noMatches"));
            return;
        }

        final Map<String, UserMessage> filteredUserMessagesAssignments = new HashMap<>();
        for (String foundName : foundNames) {
            filteredUserMessagesAssignments.put(foundName, userMessagesAssignments.get(foundName));
        }

        String sendingText = generateMessagesListText(filteredUserMessagesAssignments);

        if (message.getThreadRootId() == null)
            conversation.send(sendingText);
        else
            message.sendInThread(sendingText);
    }
}
