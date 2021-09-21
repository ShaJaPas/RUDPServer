package main.tools.db;

import main.player.Player;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataBase {

    private SessionFactory factory;

    public DataBase(){
        try{
            Class.forName(HibernateUtil.DRIVER_NAME);
            factory = HibernateUtil.getSessionFactory();
            System.out.println("Successfully connected to DB");
        } catch (ClassNotFoundException e) {
            System.out.println(("JDBC driver for H2 not found!"));
        }
    }

    public long insert(Player player){
        long start = System.currentTimeMillis();

        Session session = factory.openSession();
        try {
            session.beginTransaction();
            session.save(player);
            session.getTransaction().commit();
        } catch (Exception e){
            session.getTransaction().rollback();
        }
        session.close();

        return System.currentTimeMillis() - start;
    }

    public long update(Player player){
        long start = System.currentTimeMillis();

        Session session = factory.openSession();
        try {
            session.beginTransaction();
            session.update(player);
            session.getTransaction().commit();
        } catch (Exception e){
            session.getTransaction().rollback();
            e.printStackTrace();
        }
        session.close();

        return System.currentTimeMillis() - start;
    }

    public long delete(String id){
        long start = System.currentTimeMillis();
        Session session = factory.openSession();
        try {
            session.beginTransaction();
            Query q = session.createQuery("delete Player where email = :id");
            q.setString("id", id);
            q.executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e){
            session.getTransaction().rollback();
        }
        session.close();
        return System.currentTimeMillis() - start;
    }

    public long getRowsCount(){
        Session session = factory.openSession();
        long result = -1;
        try {
            session.beginTransaction();
            Query q = session.createQuery("select count(*) from Player pl");
            result = (Long) q.iterate().next();
            session.getTransaction().commit();
        } catch (Exception e){
            session.getTransaction().rollback();
        }
        session.close();
        return result;
    }

    public boolean keyExists(String key){
        Session session = factory.openSession();
        Query q = session.createQuery("select 1 from Player t where t.email = :key");
        q.setString("key", key);
        boolean result = q.uniqueResult() != null;
        session.close();
        return result;
    }

    public List<Player> nickSelect(String substring){
        Session session = factory.openSession();
        ArrayList<Player> result = new ArrayList<>();
        Query q = session.createQuery("select a from Player a");
        Iterator r = q.iterate();
        while (r.hasNext()){
            Player pl = (Player) r.next();
            if(pl.nickName.toLowerCase().startsWith(substring.toLowerCase()))
                result.add(pl);
        }
        session.close();
        return result;
    }

    public boolean nickExists(String value){
        Session session = factory.openSession();
        Query q = session.createQuery("select 1 from Player t where t.nickName = :key");
        q.setString("key", value);
        boolean result = q.uniqueResult() != null;
        session.close();
        return result;
    }

    public Player getData(String email){
        Session session = factory.openSession();
        Player result = null;
        try {
            session.beginTransaction();
            result = session.get(Player.class, email);
            session.getTransaction().commit();
        } catch (Exception e){
            session.getTransaction().rollback();
            e.printStackTrace();
        }
        session.close();
        return result;
    }

    public Player getDataByNickname(String nickname){
        Session session = factory.openSession();
        Player result = null;
        try {
            session.beginTransaction();
            Query q = session.createQuery("from Player t where t.nickName = :key");
            q.setString("key", nickname);
            result = (Player) q.uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e){
            session.getTransaction().rollback();
            e.printStackTrace();
        }
        session.close();
        return result;
    }
    public void close(){
        try {
            HibernateUtil.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
