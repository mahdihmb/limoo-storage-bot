package ir.mahdihmb.limoo_storage_bot.entity;

import ir.limoo.driver.entity.Message;

public class UserMessage {

    private long id;
    private String name;
    private User user;
    private Message message;

    public UserMessage() {
    }

    public UserMessage(String name, User user, Message message) {
        this.name = name;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
