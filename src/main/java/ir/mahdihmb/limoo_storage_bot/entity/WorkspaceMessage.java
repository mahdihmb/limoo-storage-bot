package ir.mahdihmb.limoo_storage_bot.entity;

import ir.limoo.driver.entity.Message;

public class WorkspaceMessage {

    private long id;
    private String name;
    private Workspace workspace;
    private Message message;

    public WorkspaceMessage() {
    }

    public WorkspaceMessage(String name, Workspace workspace, Message message) {
        this.name = name;
        this.workspace = workspace;
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

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
