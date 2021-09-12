package ir.mahdihmb.limoo_storage_bot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ir.limoo.driver.entity.Message;
import ir.limoo.driver.entity.User;
import ir.limoo.driver.entity.Workspace;
import ir.limoo.driver.exception.LimooException;
import ir.limoo.driver.util.JacksonUtils;
import ir.limoo.driver.util.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class RequestUtils {

    private static final transient Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    private static final String THREADS_ROOT_URI_TEMPLATE = "workspace/items/%s/thread/items/%s";
    private static final String GET_MESSAGE_URI_TEMPLATE = MessageUtils.MESSAGES_ROOT_URI_TEMPLATE + "/%s";
    private static final String DELETE_MESSAGE_URI_TEMPLATE = MessageUtils.MESSAGES_ROOT_URI_TEMPLATE + "/%s";
    private static final String REACT_URI_TEMPLATE = MessageUtils.MESSAGES_ROOT_URI_TEMPLATE + "/%s/reaction/items/%s";
    private static final String GET_USER_URI_TEMPLATE = "user/items/%s";
    private static final String GET_USERS_BY_IDS_URI_TEMPLATE = "user/ids";
    private static final String THREAD_VIEW_LOG_URI_TEMPLATE = THREADS_ROOT_URI_TEMPLATE + "/view_log";

    public static Message getMessage(Workspace workspace, String conversationId, String messageId) throws LimooException {
        String uri = String.format(GET_MESSAGE_URI_TEMPLATE, workspace.getId(), conversationId, messageId);
        JsonNode messageNode = workspace.getRequester().executeApiGet(uri, workspace.getWorker());
        try {
            return JacksonUtils.deserializeObject(messageNode, Message.class);
        } catch (IOException e) {
            logger.error("", e);
            return null;
        }
    }

    public static void deleteMessage(Workspace workspace, String conversationId, String messageId) throws LimooException {
        String uri = String.format(DELETE_MESSAGE_URI_TEMPLATE, workspace.getId(), conversationId, messageId);
        workspace.getRequester().executeApiDelete(uri, workspace.getWorker());
    }

    public static User getUser(Workspace workspace, String userId) throws LimooException {
        String uri = String.format(GET_USER_URI_TEMPLATE, userId);
        JsonNode userNode = workspace.getRequester().executeApiGet(uri, workspace.getWorker());
        try {
            return JacksonUtils.deserializeObject(userNode, User.class);
        } catch (IOException e) {
            logger.error("", e);
            return null;
        }
    }

    public static List<User> getUsersByIds(Workspace workspace, Set<String> userIds) throws LimooException {
        ArrayNode userIdsNode = JacksonUtils.getObjectMapper().createArrayNode();
        for (String userId : userIds) {
            userIdsNode.add(userId);
        }
        JsonNode usersNode = workspace.getRequester().executeApiPost(GET_USERS_BY_IDS_URI_TEMPLATE, userIdsNode, workspace.getWorker());
        return JacksonUtils.deserializeObjectToList(usersNode, User.class);
    }

    public static void viewLogThread(Workspace workspace, String threadRootId) throws LimooException {
        String uri = String.format(THREAD_VIEW_LOG_URI_TEMPLATE, workspace.getId(), threadRootId);
        ObjectNode body = JacksonUtils.createEmptyObjectNode();
        body.put("is_viewed", true);
        workspace.getRequester().executeApiPost(uri, body, workspace.getWorker());
    }

    public static void reactToMessage(Workspace workspace, String conversationId, String messageId, String reaction) throws LimooException {
        String uri = String.format(REACT_URI_TEMPLATE, workspace.getId(), conversationId, messageId, reaction);
        workspace.getRequester().executeApiPost(uri, JacksonUtils.createEmptyObjectNode(), workspace.getWorker());
    }
}
