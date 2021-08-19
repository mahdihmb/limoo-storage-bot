package ir.mahdihmb.limoo_storage_bot.entity;

import ir.limoo.driver.entity.Message;

public class MessageAssignment<T> {

    private long id;
    private String name;
    private T entity;
    private Message message;

    public MessageAssignment() {
    }

    public MessageAssignment(String name, T entity, Message message) {
        this.name = name;
        this.entity = entity;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public static class UserMessage extends MessageAssignment<User> {
    }

    public static class WorkspaceMessage extends MessageAssignment<Workspace> {
    }
}
