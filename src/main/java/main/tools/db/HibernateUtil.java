package main.tools.db;

import main.player.Player;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HibernateUtil {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static final String DRIVER_NAME = "org.h2.Driver";

    public static SessionFactory getSessionFactory(){
        if(sessionFactory == null){
            try {
                StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();

                Map<String, String> settings = new HashMap<>();
                settings.put(Environment.DRIVER, DRIVER_NAME);
                settings.put(Environment.URL, "jdbc:h2:/" + new File("").getAbsolutePath().replace("\\", "/") + "/db/Players;CACHE_SIZE=32768;CACHE_TYPE=TQ;EARLY_FILTER=TRUE");
                settings.put(Environment.USER, "");
                settings.put(Environment.PASS, "");
                settings.put(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
                settings.put(Environment.SHOW_SQL, "false");
                settings.put("cache.provider_class", "org.hibernate.cache.internal.NoCacheProvider");
                settings.put(Environment.HBM2DDL_AUTO, "update");
                registryBuilder.applySettings(settings);
                registry = registryBuilder.build();

                MetadataSources sources = new MetadataSources(registry);
                sources.addAnnotatedClass(Player.class);

                Metadata metadata = sources.getMetadataBuilder().build();
                sessionFactory = metadata.getSessionFactoryBuilder().build();
            } catch (Exception e){
                e.printStackTrace();

                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
        }
        return sessionFactory;
    }

    public static void close() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            sessionFactory.close();
        }
    }
}
