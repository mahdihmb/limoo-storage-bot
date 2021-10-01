package ir.mahdihmb.limoo_storage_bot;

import ir.limoo.driver.LimooDriver;
import ir.limoo.driver.entity.Conversation;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.event.AddedToConversationEventListener;
import ir.limoo.driver.event.AddedToWorkspaceEventListener;
import ir.limoo.driver.event.MessageCreatedEventListener;
import ir.limoo.driver.exception.LimooException;
import ir.mahdihmb.limoo_storage_bot.core.ConfigService;
import ir.mahdihmb.limoo_storage_bot.core.MessageService;
import ir.mahdihmb.limoo_storage_bot.handler.AdminCommandHandler;
import ir.mahdihmb.limoo_storage_bot.handler.UserCommandHandler;
import ir.mahdihmb.limoo_storage_bot.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ir.mahdihmb.limoo_storage_bot.util.Constants.*;
import static ir.mahdihmb.limoo_storage_bot.util.GeneralUtils.*;

public class LimooStorageBot {

    private static final transient Logger logger = LoggerFactory.getLogger(LimooStorageBot.class);

    private final String limooUrl;
    private final LimooDriver limooDriver;
    private final String botMention;
    private Conversation reportConversation;
    private String adminUserId;
    private String restartPostgresScriptFile;

    public LimooStorageBot(String limooUrl, String botUsername, String botPassword) throws LimooException {
        this.limooUrl = limooUrl;
        this.limooDriver = new LimooDriver(limooUrl, botUsername, botPassword);
        this.botMention = "@" + limooDriver.getBot().getUsername();

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

        try {
            String userId = ConfigService.get("admin.userId");
            if (!userId.isEmpty())
                this.adminUserId = userId;
        } catch (Throwable throwable) {
            logger.error("", throwable);
        }

        try {
            String scriptFile = ConfigService.get("admin.restartPostgresScriptFile");
            if (!scriptFile.isEmpty())
                restartPostgresScriptFile = scriptFile;
        } catch (Throwable throwable) {
            logger.error("", throwable);
        }
    }

    public void run() {
        limooDriver.addEventListener(new MessageCreatedEventListener() {
            @Override
            public void onNewMessage(Message message, Conversation conversation) {
                String threadRootId = message.getThreadRootId();
                String msgText = message.getText().trim();
                try {
                    if (adminUserId != null && adminUserId.equals(message.getUserId()) && msgText.startsWith(ADMIN_COMMAND_PREFIX)) {
                        new AdminCommandHandler(limooDriver, message, conversation, restartPostgresScriptFile).start();
                        return;
                    }

                    if (msgText.equals(botMention)) {
                        if (empty(threadRootId)) {
                            conversation.send(MessageService.get("help.introduction"));
                            return;
                        } else {
                            RequestUtils.reactToMessage(message.getWorkspace(), conversation.getId(), message.getId(), LIKE_REACTION);

                            String directReplyMessageId = message.getDirectReplyMessageId();
                            if (empty(directReplyMessageId)) {
                                return;
                            }

                            Message directReplyMessage = RequestUtils.getMessage(message.getWorkspace(), conversation.getId(), directReplyMessageId);
                            if (directReplyMessage == null) {
                                message.sendInThread(errorText(MessageService.get("error.noDirectReplyMessage")));
                                return;
                            }

                            directReplyMessage.setWorkspace(message.getWorkspace());
                            message = directReplyMessage;
                            msgText = message.getText().trim();
                        }
                    } else if (msgText.startsWith(botMention)) {
                        message.setText(msgText.substring(botMention.length()));
                        msgText = message.getText().trim();
                    }

                    if (msgText.startsWith(COMMAND_PREFIX) || msgText.startsWith(WORKSPACE_COMMAND_PREFIX)) {
                        new UserCommandHandler(limooUrl, limooDriver, message, conversation, reportConversation).start();
                    }
                } catch (Throwable throwable) {
                    logger.error("", throwable);
                } finally {
                    if (empty(threadRootId)) {
                        conversation.viewLog();
                    } else {
                        try {
                            RequestUtils.viewLogThread(message.getWorkspace(), threadRootId);
                        } catch (LimooException e) {
                            logger.info("Can't send viewLog for a thread: ", e);
                        }
                    }
                }
            }
        });

        limooDriver.addEventListener(new AddedToConversationEventListener() {
            @Override
            public void onAddToConversation(Conversation conversation) {
                try {
                    conversation.send(MessageService.get("help.introduction"));
                } catch (LimooException e) {
                    logger.error("", e);
                }
            }
        });

        limooDriver.addEventListener(new AddedToWorkspaceEventListener() {
            @Override
            public void onAddToWorkspace(ir.limoo.driver.entity.Workspace workspace) {
                try {
                    workspace.getDefaultConversation().send(MessageService.get("help.introduction"));
                } catch (LimooException e) {
                    logger.error("", e);
                }
            }
        });
    }
}
