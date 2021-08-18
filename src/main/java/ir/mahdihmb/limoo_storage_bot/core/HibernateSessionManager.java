package ir.mahdihmb.limoo_storage_bot.core;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.internal.ManagedSessionContext;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class HibernateSessionManager {

    protected static final String MAPPINGS_LOCATION = "hbm";
    private static SessionFactory sessionFactory;

    private HibernateSessionManager() {
    }

    protected static void init() throws IOException, URISyntaxException {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        // managed: org.hibernate.context.internal.ManagedSessionContext
        properties.setProperty("hibernate.current_session_context_class", "managed");

        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL82Dialect");
        properties.setProperty("hibernate.connection.driver_class", DataSourceManager.getDriverClassName());
        properties.setProperty("hibernate.connection.url", DataSourceManager.getDbUrl());
        properties.setProperty("hibernate.connection.username", ConfigService.get("db.username"));
        properties.setProperty("hibernate.connection.password", ConfigService.get("db.password"));

        Configuration configuration = new Configuration().addProperties(properties);

        Consumer<String> handleHbmFile = resource -> {
            if (resource.endsWith(".hbm.xml")) {
                configuration.addResource(resource);
            }
        };

        final File jarFile = new File(HibernateSessionManager.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        if (jarFile.isFile()) {  // Run with JAR file
            final JarFile jar = new JarFile(jarFile);
            final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            while (entries.hasMoreElements()) {
                final String name = entries.nextElement().getName();
                if (name.startsWith(MAPPINGS_LOCATION + "/")) { //filter according to the path
                    handleHbmFile.accept(name);
                }
            }
            jar.close();
        } else { // Run with IDE
            final URL url = HibernateSessionManager.class.getResource("/" + MAPPINGS_LOCATION);
            if (url != null) {
                final File hbmDir = new File(url.toURI());
                for (String name : hbmDir.list()) {
                    handleHbmFile.accept(MAPPINGS_LOCATION + "/" + name);
                }
            }
        }

        sessionFactory = configuration.buildSessionFactory();
    }

    public static Session openSession() {
        if (ManagedSessionContext.hasBind(sessionFactory))
            return getCurrentSession();
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        return session;
    }

    public static Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public static void closeSession(Session session) {
        if (session != null) {
            session.close();
            if (ManagedSessionContext.hasBind(sessionFactory))
                ManagedSessionContext.unbind(sessionFactory);
        }
    }

    public static void closeCurrentSession() {
        if (ManagedSessionContext.hasBind(sessionFactory))
            closeSession(getCurrentSession());
    }
}
