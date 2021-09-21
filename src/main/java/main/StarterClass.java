package main;

import com.alibaba.fastjson.JSON;
import com.whirvis.jraknet.RakNet;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.server.RakNetServer;
import main.network.BaseRakNetServerListener;
import main.network.email.EmailSender;
import main.player.Player;
import main.player.Tank;
import main.player.TanksInfo;
import main.player.balance.Balancer;
import main.tools.db.DataBase;
import main.tools.system.SystemInfo;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.*;

public class StarterClass {

    public static RakNetServer server = null;
    public static DataBase db = null;
    public static final String VERSION = "1.0.0";
    public static ArrayList<TanksInfo> tanks;
    public static Balancer balancer;

    private static void init() throws IOException{
        SystemInfo.getCpuUsage();
        Configurator.setLevel("org.hibernate", Level.ERROR);
        File tanksDir = new File("Tanks/");
        tanks = new ArrayList<>();
        for (File file : Objects.requireNonNull(Arrays.stream(Objects.requireNonNull(tanksDir.listFiles())).filter(c -> c.getName().endsWith(".json")).toArray(File[]::new))) {
            tanks.add(JSON.parseObject(new String(Files.readAllBytes(file.toPath())), TanksInfo.class));
        }
    }

    public static void main(String[] args) throws Exception {
        init();
        System.out.println("[SERVER COMMAND PROMPT]");
        String command;
        boolean base = true;
        Scanner scanInput = new Scanner(System.in);
        while (true) {
            if(base || scanInput.hasNext()) {
                if(!base)
                    command = scanInput.nextLine().toLowerCase();
                else {
                    command = "/start 900";
                    base = false;
                }
                if (!command.contains("/"))
                    System.out.println("Invalid input format, use /help for more info");
                else {
                    switch (command.split(" ")[0]) {
                        case "/start":
                            if(server == null)
                                try {
                                    int port = Integer.parseInt(command.split(" ")[1]);
                                    server = new RakNetServer(port, RakNetServer.INFINITE_CONNECTIONS);
                                    RakNet.setMaxPacketsPerSecond(Long.MAX_VALUE);
                                    server.addListener(new BaseRakNetServerListener());
                                    db = new DataBase();
                                    EmailSender.init("*******@gmail.com", "*******");
                                    balancer = new Balancer();
                                    balancer.start();
                                    server.start();
                                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                    System.out.println("Invalid argument, command usage - /start [port]");
                                } catch (IllegalArgumentException e) {
                                    System.out.println("Port must be in range of 0-65535");
                                }
                            break;
                        case "/stop":
                            if (server != null && server.isRunning()) {
                                server.shutdown();
                                server = null;
                                balancer.stop();
                                EmailSender.dispose();
                            } else {
                                System.out.println("Server is not running");
                            }
                            break;
                        case "/address":
                            if (server != null && server.isRunning()) {
                                System.out.println("Server is running at: " + InetAddress.getLocalHost().getHostAddress() + ":" + server.getPort());
                            } else {
                                System.out.println("Server is not running");
                            }
                            break;
                        case "/cpu":
                            System.out.println("CPU usage (percents): " + SystemInfo.getCpuUsage() + " %");
                            break;
                        case "/mem":
                            System.out.println("Memory usage (percents): " + SystemInfo.getMemUsage() +
                                    " % (" + SystemInfo.getMemUsedMB() + " MB)");
                            break;
                        case "/exit":
                            if(server != null && server.isRunning()) {
                                server.shutdown();
                                balancer.stop();
                                db.close();
                                EmailSender.dispose();
                            }
                            System.out.println("Exiting SERVER COMMAND PROMPT");
                            System.exit(0);
                            break;
                        case "/forwardport":
                            if(server != null && server.isRunning()){
                                RakNet.UPnPResult result = server.forwardPort();
                                final int port = server.getPort();
                                result.onFinish(() -> {
                                    if(result.wasSuccessful())
                                        System.out.println(("Port " + port + " was successfully forwarded"));
                                    else System.out.println(("Error occurred while forwarding port " + port));
                                });
                            } else {
                                System.out.println("Server is not running");
                            }
                            break;
                        case "/closeport":
                            if(server != null && server.isRunning()){
                                RakNet.UPnPResult result = server.closePort();
                                final int port = server.getPort();
                                result.onFinish(() -> {
                                    if(result.wasSuccessful())
                                        System.out.println(("Port " + port + " was successfully closed"));
                                    else System.out.println(("Error occurred while closing port " + port));
                                });
                            } else {
                                System.out.println("Server is not running");
                            }
                            break;
                        case "/conncount":
                            if(server != null && server.isRunning())
                            {
                                System.out.println(("Connections count - " + server.getClientCount()));
                            } else
                            {
                                System.out.println("Server is not running");
                            }
                            break;
                        case "/remplayer":
                            try {
                                if (db != null) {
                                    String email = command.split(" ")[1];
                                    if (db.keyExists(email)) {
                                        long ms = db.delete(email);
                                        System.out.println("Successfully removed player with email " + email + " (" + ms + "ms)");
                                    } else System.out.println("Player is not exists");
                                } else System.out.println("Not connected to DB");
                            } catch (ArrayIndexOutOfBoundsException e){
                                System.out.println("Invalid argument, command usage - /remplayer [player]");
                            }
                            break;
                        case "/regcount":
                            if(db != null){
                                long count = db.getRowsCount();
                                System.out.println(count + (count == 1 ? " player is" : " players are") + " registered");
                            } else System.out.println("Not connected to DB");
                            break;
                        case "/gc":
                            System.gc();
                            System.runFinalization();
                            break;
                        case "/nullaccount":
                            try {
                                if (db != null) {
                                    String email = command.split(" ")[1];
                                    if (db.keyExists(email)) {
                                        Player lastPlayer = db.getData(email);
                                        Player player = new Player(email, lastPlayer.password,
                                                new Date(), new Date(), "", 0, 0,
                                                new Tank[]{}, 0, 1, new String[] {});
                                        long ms = db.update(player);
                                        System.out.println("Successfully nulled player with email " + email + " (" + ms + "ms)");
                                    } else System.out.println("Player is not exists");
                                } else System.out.println("Not connected to DB");
                            } catch (ArrayIndexOutOfBoundsException e){
                                System.out.println("Invalid argument, command usage - /nullaccount [player]");
                            }
                            break;
                        case "/threadscount":
                            System.out.println("Threads Count - " + Thread.activeCount());
                            break;
                        case "/packetsinfo":
                            int packetsSent = 0;
                            int packetsRecieved = 0;
                            for (RakNetClientPeer client : server.getClients()) {
                                packetsRecieved += client.getPacketsReceivedThisSecond();
                                packetsSent += client.getPacketsSentThisSecond();
                            }
                            System.out.println("Packets sent - " + packetsSent + ", recieved - " + packetsRecieved);
                            break;
                        default:
                            System.out.println("Unknown command, use /help for more info");
                            break;
                    }
                }
            }
        }
    }
}
