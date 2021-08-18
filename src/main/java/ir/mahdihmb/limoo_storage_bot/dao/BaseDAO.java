package ir.mahdihmb.limoo_storage_bot.dao;

import ir.mahdihmb.limoo_storage_bot.core.HibernateSessionManager;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

public class BaseDAO<T> {

    private static final transient Logger logger = LoggerFactory.getLogger(BaseDAO.class);

    private final Class<T> persistentClass;

    public BaseDAO(Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

    protected Object doTransaction(Function<Session, ?> action) {
        Session session = HibernateSessionManager.getCurrentSession();
        Transaction tx = null;
        Object result = null;
        try {
            tx = session.beginTransaction();
            result = action.apply(session);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null)
                tx.rollback();
            logger.error("", e);
        }
        return result;
    }

    public Serializable add(T entity) {
        return (Serializable) doTransaction((session) -> session.save(entity));
    }

    public void update(T entity){
        doTransaction((session) -> {
            session.update(entity);
            return null;
        });
    }

    public void delete(Serializable id){
        doTransaction((session) -> {
            session.delete(session.get(persistentClass, id));
            return null;
        });
    }

    @SuppressWarnings({"unchecked"})
    public T get(Serializable id) {
        return (T) doTransaction((session) -> session.get(persistentClass, id));
    }

    @SuppressWarnings({"unchecked"})
    public List<T> list() {
        return (List<T>) doTransaction((session) -> {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(persistentClass);
            Root<T> root = criteriaQuery.from(persistentClass);
            criteriaQuery.select(root);
            return session.createQuery(criteriaQuery).list();
        });
    }
}
