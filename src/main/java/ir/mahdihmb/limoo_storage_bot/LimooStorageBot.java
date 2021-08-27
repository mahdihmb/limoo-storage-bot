package ir.mahdihmb.limoo_storage_bot;

import ir.limoo.driver.LimooDriver;
import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.event.AddedToConversationEventListener;
import ir.limoo.driver.event.AddedToWorkspaceEventListener;
import ir.limoo.driver.event.MessageCreatedEventListener;
import ir.limoo.driver.exception.LimooException;
import ir.mahdihmb.limoo_storage_bot.core.ConfigService;
import ir.mahdihmb.limoo_storage_bot.core.HibernateSessionManager;
import ir.mahdihmb.limoo_storage_bot.core.MessageService;
import ir.mahdihmb.limoo_storage_bot.dao.UserDAO;
import ir.mahdihmb.limoo_storage_bot.dao.WorkspaceDAO;
import ir.mahdihmb.limoo_storage_bot.entity.User;
import ir.mahdihmb.limoo_storage_bot.entity.Workspace;
import ir.mahdihmb.limoo_storage_bot.handler.AdminCommandHandler;
import ir.mahdihmb.limoo_storage_bot.handler.UserCommandHandler;
import ir.mahdihmb.limoo_storage_bot.util.RequestUtils;
import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ir.mahdihmb.limoo_storage_bot.util.Constants.*;
import static ir.mahdihmb.limoo_storage_bot.util.GeneralUtils.*;

public class LimooStorageBot {

    private static final transient Logger logger = LoggerFactory.getLogger(LimooStorageBot.class);

    private final LimooDriver limooDriver;
    private final String helpMsg;
    private final AdminCommandHandler adminCommandHandler;
    private final UserCommandHandler userCommandsHandler;
    private Conversation reportConversation;
    private long lastTimeSentBugReport = 0;
    private String adminUserId;

    public LimooStorageBot(String limooUrl, String botUsername, String botPassword) throws LimooException {
        limooDriver = new LimooDriver(limooUrl, botUsername, botPassword);
        helpMsg = String.format(
                MessageService.get("help"),
                limooDriver.getBot().getDisplayName(),
                ConfigService.get("repo.address")
        );

        adminCommandHandler = new AdminCommandHandler(limooDriver, helpMsg);

        try {
            String reportWorkspaceKey = ConfigService.get("admin.reportWorkspaceKey");
            String reportConversationId = ConfigService.get("admin.reportConversationId");
            if (notEmpty(reportWorkspaceKey) && notEmpty(reportConversationId)) {
                ir.limoo.driver.entity.Workspace reportWorkspace = limooDriver.getWorkspaceByKey(reportWorkspaceKey);
                if (reportWorkspace != null) {
                    reportConversation = reportWorkspace.getConversationById(reportConversationId);
                }
            }
        } catch (Throwable throwable) {
            logger.error("", throwable);
        }

        userCommandsHandler = new UserCommandHandler(limooUrl, reportConversation);

        try {
            String userId = ConfigService.get("admin.userId");
            if (!userId.isEmpty())
                adminUserId = userId;
        } catch (Throwable throwable) {
            logger.error("", throwable);
        }
    }

    public void run() {
        limooDriver.addEventListener(new MessageCreatedEventListener() {
            @Override
            public void onNewMessage(Message message, Conversation conversation) {
                try {
                    String msgText = trimSpaces(message.getText());

                    if (adminUserId != null && adminUserId.equals(message.getUserId()) && msgText.startsWith(ADMIN_COMMAND_PREFIX)) {
                        adminCommandHandler.handle(message, conversation);
                        return;
                    }

                    if (msgText.equals("@" + limooDriver.getBot().getUsername())) {
                        if (message.getThreadRootId() == null) {
                            handleHelp(message, conversation);
                            return;
                        } else {
                            RequestUtils.followThread(message.getWorkspace(), message.getThreadRootId());
                            RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), LIKE_REACTION);

                            String directReplyMessageId = message.getDirectReplyMessageId();
                            if (directReplyMessageId == null || directReplyMessageId.isEmpty()) {
                                return;
                            }

                            Message directReplyMessage = RequestUtils.getMessage(message.getWorkspace(), conversation.getId(), directReplyMessageId);
                            if (directReplyMessage == null) {
                                sendErrorMsgInThread(message, MessageService.get("noDirectReplyMessage"));
                                return;
                            }

                            directReplyMessage.setWorkspace(message.getWorkspace());
                            message = directReplyMessage;
                            msgText = trimSpaces(message.getText());
                        }
                    }

                    if (msgText.equals(COMMAND_PREFIX) || msgText.equals(WORKSPACE_COMMAND_PREFIX)) {
                        handleHelp(message, conversation);
                        return;
                    }

                    if (!msgText.startsWith(COMMAND_PREFIX + SPACE)
                            && !msgText.startsWith(WORKSPACE_COMMAND_PREFIX + SPACE)) {
                        return;
                    }

                    HibernateSessionManager.openSession();
                    User user = UserDAO.getInstance().getOrCreate(message.getUserId());
                    Workspace workspace = WorkspaceDAO.getInstance().getOrCreate(message.getWorkspace().getId());

                    userCommandsHandler.handle(message, conversation, user, workspace);
                } catch (Throwable throwable) {
                    logger.error("", throwable);
                    handleException(message, throwable);
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

    private void handleException(Message message, Throwable throwable) {
        if (throwable instanceof HibernateException) {
            try {
                message.sendInThread(MessageService.get("bugReportTextForUser"));
            } catch (LimooException e) {
                logger.error("", e);
            }

            long now = System.currentTimeMillis();
            if (reportConversation != null && now > lastTimeSentBugReport + ONE_HOUR_MILLIS) {
                try {
                    String reportMsg = MessageService.get("bugReportTextForAdmin") + LINE_BREAK
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
