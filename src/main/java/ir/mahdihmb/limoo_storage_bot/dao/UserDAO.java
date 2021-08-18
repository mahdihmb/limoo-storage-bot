package ir.mahdihmb.limoo_storage_bot.dao;

import ir.mahdihmb.limoo_storage_bot.entity.User;

import java.io.Serializable;

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

    public User getOrCreate(String id) {
        return (User) doTransaction((session) -> {
            final User existingEntity = session.get(persistentClass, id);
            if (existingEntity != null)
                return existingEntity;

            User newUser = new User(id);
            final Serializable newId = session.save(newUser);
            return session.get(persistentClass, newId);
        });
    }
}
