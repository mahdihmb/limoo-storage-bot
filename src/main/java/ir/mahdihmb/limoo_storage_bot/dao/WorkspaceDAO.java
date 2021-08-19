package ir.mahdihmb.limoo_storage_bot.dao;

import ir.mahdihmb.limoo_storage_bot.entity.Workspace;

public class WorkspaceDAO extends BaseDAO<Workspace> {

    private static final Class<Workspace> persistentClass = Workspace.class;
    private static WorkspaceDAO instance;

    private WorkspaceDAO() {
        super(persistentClass);
    }

    public static WorkspaceDAO getInstance() {
        if (instance == null)
            instance = new WorkspaceDAO();
        return instance;
    }
}
