package ir.mahdihmb.limoo_storage_bot.entity;

import java.io.Serializable;

public class Feedback implements IdProvider {

    private Serializable id;
    private String userId;
    private String userWorkspaceId;
    private String userConversationId;
    private String userThreadRootId;
    private String adminWorkspaceId;
    private String adminConversationId;
    private String adminThreadRootId;

    public Feedback() {
    }

    public Feedback(String userId, String userWorkspaceId, String userConversationId, String userThreadRootId,
                    String adminWorkspaceId, String adminConversationId, String adminThreadRootId) {
        this.userId = userId;
        this.userWorkspaceId = userWorkspaceId;
        this.userConversationId = userConversationId;
        this.userThreadRootId = userThreadRootId;
        this.adminWorkspaceId = adminWorkspaceId;
        this.adminConversationId = adminConversationId;
        this.adminThreadRootId = adminThreadRootId;
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    @Override
    public void setId(Serializable id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserWorkspaceId() {
        return userWorkspaceId;
    }

    public void setUserWorkspaceId(String userWorkspaceId) {
        this.userWorkspaceId = userWorkspaceId;
    }

    public String getUserConversationId() {
        return userConversationId;
    }

    public void setUserConversationId(String userConversationId) {
        this.userConversationId = userConversationId;
    }

    public String getUserThreadRootId() {
        return userThreadRootId;
    }

    public void setUserThreadRootId(String userThreadRootId) {
        this.userThreadRootId = userThreadRootId;
    }

    public String getAdminWorkspaceId() {
        return adminWorkspaceId;
    }

    public void setAdminWorkspaceId(String adminWorkspaceId) {
        this.adminWorkspaceId = adminWorkspaceId;
    }

    public String getAdminConversationId() {
        return adminConversationId;
    }

    public void setAdminConversationId(String adminConversationId) {
        this.adminConversationId = adminConversationId;
    }

    public String getAdminThreadRootId() {
        return adminThreadRootId;
    }

    public void setAdminThreadRootId(String adminThreadRootId) {
        this.adminThreadRootId = adminThreadRootId;
    }
}
