package ir.mahdihmb.limoo_storage_bot.entity;

import java.util.HashMap;
import java.util.Map;

public class User {

    private String id;
    private Map<String, UserMessage> userMessagesAssignments;

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

    public Map<String, UserMessage> getUserMessagesAssignments() {
        return userMessagesAssignments;
    }

    public void setUserMessagesAssignments(Map<String, UserMessage> userMessagesAssignments) {
        this.userMessagesAssignments = userMessagesAssignments;
    }

    public Map<String, UserMessage> getCreatedUserMessagesAssignments() {
        if (userMessagesAssignments == null)
            userMessagesAssignments = new HashMap<>();
        return userMessagesAssignments;
    }

    public void putInUserMessagesAssignments(String name, UserMessage userMessage) {
        getCreatedUserMessagesAssignments().put(name, userMessage);
    }

    public void removeFromUserMessagesAssignments(String name) {
        if (userMessagesAssignments != null)
            userMessagesAssignments.remove(name);
    }
}
