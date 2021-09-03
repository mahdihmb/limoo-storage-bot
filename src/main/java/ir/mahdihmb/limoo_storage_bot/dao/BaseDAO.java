package ir.mahdihmb.limoo_storage_bot.dao;

import ir.mahdihmb.limoo_storage_bot.core.HibernateSessionManager;
import ir.mahdihmb.limoo_storage_bot.entity.IdProvider;
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

public class BaseDAO<T extends IdProvider> {

    private static final transient Logger logger = LoggerFactory.getLogger(BaseDAO.class);

    private final Class<T> persistentClass;

    public BaseDAO(Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

    protected Object doTransaction(Function<Session, ?> action) {
        Session session = HibernateSessionManager.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Object result = action.apply(session);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable throwable) {
                    logger.error("", throwable);
                }
            }
            throw e;
        }
    }

    public Serializable add(T entity) {
        return (Serializable) doTransaction((session) -> session.save(entity));
    }

    public void update(T entity) {
        doTransaction((session) -> {
            session.update(entity);
            return null;
        });
    }

    public void delete(Serializable id) {
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

    @SuppressWarnings({"unchecked"})
    public T getOrCreate(Serializable id) {
        return (T) doTransaction((session) -> {
            final T existingEntity = session.get(persistentClass, id);
            if (existingEntity != null)
                return existingEntity;

            try {
                T newEntity = persistentClass.newInstance();
                newEntity.setId(id);
                final Serializable newId = session.save(newEntity);
                return session.get(persistentClass, newId);
            } catch (InstantiationException | IllegalAccessException e) {
                logger.error("", e);
                return null;
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    public T getByField(String fieldName, Object fieldValue) {
        return (T) doTransaction((session) -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<T> cr = cb.createQuery(persistentClass);
            Root<T> root = cr.from(persistentClass);
            cr.select(root).where(cb.equal(root.get(fieldName), fieldValue));
            List<T> list = session.createQuery(cr).list();
            if (list.isEmpty())
                return null;
            return list.get(0);
        });
    }
}
