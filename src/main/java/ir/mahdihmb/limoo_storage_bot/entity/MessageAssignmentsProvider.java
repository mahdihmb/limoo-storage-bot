package ir.mahdihmb.limoo_storage_bot.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class MessageAssignmentsProvider<T> implements IdProvider {

    private Serializable id;
    private Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap;

    public Serializable getId() {
        return id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }

    public Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> getMessageAssignmentsMap() {
        return messageAssignmentsMap;
    }

    public void setMessageAssignmentsMap(Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> messageAssignmentsMap) {
        this.messageAssignmentsMap = messageAssignmentsMap;
    }

    public Map<String, MessageAssignment<MessageAssignmentsProvider<T>>> getCreatedMessageAssignmentsMap() {
        if (messageAssignmentsMap == null)
            messageAssignmentsMap = new HashMap<>();
        return messageAssignmentsMap;
    }

    public void putInMessageAssignmentsMap(String name, MessageAssignment<MessageAssignmentsProvider<T>> messageAssignment) {
        getCreatedMessageAssignmentsMap().put(name, messageAssignment);
    }

    public void removeFromMessageAssignmentsMap(String name) {
        if (messageAssignmentsMap != null)
            messageAssignmentsMap.remove(name);
    }
}
