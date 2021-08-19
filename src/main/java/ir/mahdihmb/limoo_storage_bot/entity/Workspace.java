package ir.mahdihmb.limoo_storage_bot.entity;

import java.util.HashMap;
import java.util.Map;

public class Workspace {

    private String id;
    private Map<String, WorkspaceMessage> workspaceMessageAssignmentsMap;

    public Workspace() {
    }

    public Workspace(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, WorkspaceMessage> getWorkspaceMessageAssignmentsMap() {
        return workspaceMessageAssignmentsMap;
    }

    public void setWorkspaceMessageAssignmentsMap(Map<String, WorkspaceMessage> workspaceMessageAssignmentsMap) {
        this.workspaceMessageAssignmentsMap = workspaceMessageAssignmentsMap;
    }

    public Map<String, WorkspaceMessage> getCreatedWorkspaceMessageAssignmentsMap() {
        if (workspaceMessageAssignmentsMap == null)
            workspaceMessageAssignmentsMap = new HashMap<>();
        return workspaceMessageAssignmentsMap;
    }

    public void putInWorkspaceMessageAssignmentsMap(String name, WorkspaceMessage workspaceMessage) {
        getCreatedWorkspaceMessageAssignmentsMap().put(name, workspaceMessage);
    }

    public void removeFromWorkspaceMessageAssignmentsMap(String name) {
        if (workspaceMessageAssignmentsMap != null)
            workspaceMessageAssignmentsMap.remove(name);
    }
}
