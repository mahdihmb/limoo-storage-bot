package ir.mahdihmb.limoo_storage_bot.entity;

import java.util.HashMap;
import java.util.Map;

public class User {

    private String id;
    private Map<String, UserMessage> userMessageAssignmentsMap;

    public User() {
    }

    public User(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, UserMessage> getUserMessageAssignmentsMap() {
        return userMessageAssignmentsMap;
    }

    public void setUserMessageAssignmentsMap(Map<String, UserMessage> userMessageAssignmentsMap) {
        this.userMessageAssignmentsMap = userMessageAssignmentsMap;
    }

    public Map<String, UserMessage> getCreatedUserMessageAssignmentsMap() {
        if (userMessageAssignmentsMap == null)
            userMessageAssignmentsMap = new HashMap<>();
        return userMessageAssignmentsMap;
    }

    public void putInUserMessageAssignmentsMap(String name, UserMessage userMessage) {
        getCreatedUserMessageAssignmentsMap().put(name, userMessage);
    }

    public void removeFromUserMessageAssignmentsMap(String name) {
        if (userMessageAssignmentsMap != null)
            userMessageAssignmentsMap.remove(name);
    }
}
