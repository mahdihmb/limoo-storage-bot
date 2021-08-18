package ir.mahdihmb.limoo_storage_bot.util;

import com.fasterxml.jackson.databind.JsonNode;
import ir.limoo.driver.connection.LimooRequester;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.entity.Workspace;
import ir.limoo.driver.exception.LimooException;
import ir.limoo.driver.util.JacksonUtils;
import ir.limoo.driver.util.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RequestUtils {

    private static final transient Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    private static final String GET_MESSAGE_URI_TEMPLATE = MessageUtils.MESSAGES_ROOT_URI_TEMPLATE + "/%s";

    public static Message getMessage(Workspace workspace, String conversationId, String messageId) throws LimooException {
        String uri = String.format(GET_MESSAGE_URI_TEMPLATE, workspace.getId(), conversationId, messageId);
        JsonNode messageNode = LimooRequester.getInstance().executeApiGet(uri, workspace.getWorker());
        try {
            return JacksonUtils.deserializeObject(messageNode, Message.class);
        } catch (IOException e) {
            logger.error("", e);
            return null;
        }
    }
}
