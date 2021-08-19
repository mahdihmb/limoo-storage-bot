package ir.mahdihmb.limoo_storage_bot.entity;

import java.io.Serializable;

public interface IdProvider {

    Serializable getId();

    void setId(Serializable id);
}
