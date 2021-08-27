package ir.mahdihmb.limoo_storage_bot.dao;

import ir.mahdihmb.limoo_storage_bot.entity.Feedback;

public class FeedbackDAO extends BaseDAO<Feedback> {

    private static final Class<Feedback> persistentClass = Feedback.class;
    private static FeedbackDAO instance;

    private FeedbackDAO() {
        super(persistentClass);
    }

    public static FeedbackDAO getInstance() {
        if (instance == null)
            instance = new FeedbackDAO();
        return instance;
    }

    public Feedback getByUserThreadRootId(String userThreadRootId) throws Throwable {
        return getByField("userThreadRootId", userThreadRootId);
    }

    public Feedback getByAdminThreadRootId(String adminThreadRootId) throws Throwable {
        return getByField("adminThreadRootId", adminThreadRootId);
    }
}
