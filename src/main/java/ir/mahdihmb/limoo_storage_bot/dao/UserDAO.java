package ir.mahdihmb.limoo_storage_bot.dao;

import ir.mahdihmb.limoo_storage_bot.entity.User;

public class UserDAO extends BaseDAO<User> {

    private static final Class<User> persistentClass = User.class;
    private static UserDAO instance;

    private UserDAO() {
        super(persistentClass);
    }

    public static UserDAO getInstance() {
        if (instance == null)
            instance = new UserDAO();
        return instance;
    }
}
