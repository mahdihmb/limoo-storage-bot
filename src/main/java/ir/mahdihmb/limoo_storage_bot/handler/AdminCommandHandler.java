package ir.mahdihmb.limoo_storage_bot.handler;

import ir.limoo.driver.LimooDriver;
import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.entity.MessageFile;
import ir.limoo.driver.entity.Workspace;
import ir.limoo.driver.exception.LimooException;
import ir.limoo.driver.util.MessageUtils;
import ir.mahdihmb.limoo_storage_bot.core.CoreManager;
import ir.mahdihmb.limoo_storage_bot.core.HibernateSessionManager;
import ir.mahdihmb.limoo_storage_bot.core.MessageService;
import ir.mahdihmb.limoo_storage_bot.dao.FeedbackDAO;
import ir.mahdihmb.limoo_storage_bot.dao.UserDAO;
import ir.mahdihmb.limoo_storage_bot.dao.WorkspaceDAO;
import ir.mahdihmb.limoo_storage_bot.entity.Feedback;
import ir.mahdihmb.limoo_storage_bot.entity.User;
import ir.mahdihmb.limoo_storage_bot.util.RequestUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ir.mahdihmb.limoo_storage_bot.util.Constants.*;
import static ir.mahdihmb.limoo_storage_bot.util.GeneralUtils.*;

public class AdminCommandHandler extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(AdminCommandHandler.class);

    private final LimooDriver limooDriver;
    private final Message message;
    private final Conversation conversation;
    private final String restartPostgresScriptFile;

    public AdminCommandHandler(LimooDriver limooDriver, Message message, Conversation conversation,
                               String restartPostgresScriptFile) {
        this.limooDriver = limooDriver;
        this.message = message;
        this.conversation = conversation;
        this.restartPostgresScriptFile = restartPostgresScriptFile;
    }

    @Override
    public void run() {
        String command = message.getText().trim().substring((ADMIN_COMMAND_PREFIX).length()).trim();
        try {
            if (command.isEmpty()) {
                handleHelp();
            } else if (command.equals(ADMIN_SEND_INTRODUCTION_IN_LOBBY_COMMAND)) {
                handleSendIntroductionInLobby();
            } else if (command.equals(ADMIN_RESTART_POSTGRESQL_COMMAND)) {
                handleRestartPostgresql();
            } else if (command.equals(ADMIN_REPORT_COMMAND)) {
                handleReport();
            } else if (command.equals(ADMIN_DELETE_BOT_MESSAGE_COMMAND_PREFIX)) {
                handleDeleteBotMessage();
            } else if (command.startsWith(ADMIN_SEND_UPDATE_IN_LOBBY_COMMAND_PREFIX)) {
                handleSendUpdateInLobby(command);
            } else if (command.startsWith(ADMIN_RESPONSE_TO_FEEDBACK_COMMAND_PREFIX)) {
                handleResponseToFeedback(command);
            }
        } catch (Throwable throwable) {
            handleException(throwable);
        }
    }

    private void handleException(Throwable throwable) {
        logger.error("", throwable);
        if (throwable instanceof HibernateException) {
            try {
                message.sendInThread(MessageService.get("bugReportTextForAdmin"));
            } catch (LimooException e) {
                logger.error("", e);
            }
        }
    }

    private void handleHelp() throws LimooException {
        conversation.send(MessageService.get("adminCommand.help"));
    }

    private void handleSendIntroductionInLobby() throws LimooException {
        for (Workspace workspace : limooDriver.getWorkspaces()) {
            workspace.getDefaultConversation().send(MessageService.get("help.introduction"));
        }
        RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), LIKE_REACTION);
    }

    private void sendMessageAndReact(String msg, String reaction) throws LimooException {
        RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), reaction);
        message.sendInThread(msg);
    }

    private void handleRestartPostgresql() throws LimooException {
        if (restartPostgresScriptFile == null) {
            message.sendInThread(MessageService.get("noScriptSpecified"));
            return;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(restartPostgresScriptFile);
            Process process = processBuilder.start();

            try {
                InputStream inputStream;
                String reaction;
                if (process.waitFor() == 0) {
                    CoreManager.reInitDatabaseInRuntime();
                    inputStream = process.getInputStream();
                    reaction = LIKE_REACTION;
                } else {
                    inputStream = process.getErrorStream();
                    reaction = DISLIKE_REACTION;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(LINE_BREAK);
                }
                sendMessageAndReact("```" + LINE_BREAK + output + LINE_BREAK + "```", reaction);
            } catch (InterruptedException e) {
                sendMessageAndReact("```" + LINE_BREAK + getMessageOfThrowable(e) + LINE_BREAK + "```", DISLIKE_REACTION);
            }
        } catch (IOException e) {
            sendMessageAndReact("```" + LINE_BREAK + getMessageOfThrowable(e) + LINE_BREAK + "```", DISLIKE_REACTION);
        }
    }

    private void handleReport() throws LimooException {
        try (Session ignored = HibernateSessionManager.openSession()) {
            StringBuilder report = new StringBuilder();

            Map<Serializable, Integer> workspaceToMessageSize = new HashMap<>();
            for (ir.mahdihmb.limoo_storage_bot.entity.Workspace workspace : WorkspaceDAO.getInstance().list()) {
                workspaceToMessageSize.put(workspace.getId(), workspace.getCreatedMessageAssignmentsMap().size());
            }
            report.append(MessageService.get("workspaces")).append(LINE_BREAK);
            for (Workspace workspace : limooDriver.getWorkspaces()) {
                report.append("- ").append(workspace.getDisplayName())
                        .append(SPACE).append("(").append(workspaceToMessageSize.get(workspace.getId())).append(")")
                        .append(LINE_BREAK);
            }

            Map<String, Integer> userToMessageSize = new HashMap<>();
            for (User user : UserDAO.getInstance().list()) {
                userToMessageSize.put((String) user.getId(), user.getCreatedMessageAssignmentsMap().size());
            }
            report.append(MessageService.get("users")).append(LINE_BREAK);
            for (ir.limoo.driver.entity.User user : RequestUtils.getUsersByIds(message.getWorkspace(), userToMessageSize.keySet())) {
                report.append("- ").append(user.getDisplayName())
                        .append(SPACE).append("(").append(userToMessageSize.get(user.getId())).append(")")
                        .append(LINE_BREAK);
            }

            message.sendInThread(report.toString());
        }
    }

    private void handleSendUpdateInLobby(String command) throws LimooException {
        String text = command.substring(ADMIN_SEND_UPDATE_IN_LOBBY_COMMAND_PREFIX.length()).trim();
        List<MessageFile> fileInfos = message.getCreatedFileInfos();
        if (text.isEmpty() && fileInfos.isEmpty()) {
            RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), DISLIKE_REACTION);
            return;
        }

        Message.Builder messageBuilder = new Message.Builder().text(text).fileInfos(fileInfos);
        for (Workspace workspace : limooDriver.getWorkspaces()) {
            workspace.getDefaultConversation().send(messageBuilder);
        }
        RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), LIKE_REACTION);
    }

    private void handleResponseToFeedback(String command) throws LimooException {
        try (Session ignored = HibernateSessionManager.openSession()) {
            String response = command.substring(ADMIN_RESPONSE_TO_FEEDBACK_COMMAND_PREFIX.length()).trim();
            List<MessageFile> fileInfos = message.getCreatedFileInfos();
            if (empty(message.getThreadRootId()) || response.isEmpty() && fileInfos.isEmpty()) {
                RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), DISLIKE_REACTION);
                return;
            }

            Feedback feedback = FeedbackDAO.getInstance().getByAdminThreadRootId(message.getThreadRootId());
            if (feedback == null) {
                message.sendInThread(MessageService.get("noFeedbackInDb"));
                return;
            }

            String fullResponseToUser = MessageService.get("adminResponse") + LINE_BREAK + response;
            Message.Builder messageBuilder = new Message.Builder().text(fullResponseToUser).fileInfos(fileInfos)
                    .threadRootId(feedback.getUserThreadRootId());
            MessageUtils.sendMessage(
                    messageBuilder,
                    limooDriver.getWorkspaceById(feedback.getUserWorkspaceId()),
                    feedback.getUserConversationId()
            );
            RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), LIKE_REACTION);
        }
    }

    private void handleDeleteBotMessage() throws LimooException {
        String directReplyMessageId = message.getDirectReplyMessageId();
        if (notEmpty(directReplyMessageId)) {
            RequestUtils.deleteMessage(message.getWorkspace(), message.getConversationId(), directReplyMessageId);
        }
    }
}
