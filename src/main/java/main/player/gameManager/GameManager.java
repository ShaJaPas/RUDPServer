package main.player.gameManager;

import com.alibaba.fastjson.JSON;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import main.network.Client;
import main.player.balance.Balanced;
import main.player.balance.BalancedPlayer;
import main.player.gameManager.map.Map;
import main.tools.physics.BodyEditorLoader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {

    private final List<Map> maps;
    private final List<GameWorld> worlds;
    private final BodyEditorLoader mapObjectsLoader;
    private final BodyEditorLoader playerBodiesLoader;
    private final BodyEditorLoader bulletsLoader;
    private boolean stopped = false;
    public static final java.util.Map<Integer, Vector2> objectsSizes = new ConcurrentHashMap<>();
    public static final java.util.Map<String, Vector2> bulletsSizes = new ConcurrentHashMap<>();

    public GameManager() throws IOException {
        Box2D.init();
        File tanksDir = new File("Maps/");
        maps = new ArrayList<>();
        for (File file : Objects.requireNonNull(Arrays.stream(Objects.requireNonNull(tanksDir.listFiles())).filter(c -> c.getName().endsWith(".json")).toArray(File[]::new))) {
            maps.add(JSON.parseObject(new String(Files.readAllBytes(file.toPath())), Map.class));
        }
        worlds = new CopyOnWriteArrayList<>();
        mapObjectsLoader = new BodyEditorLoader(new String(Files.readAllBytes(Paths.get("Maps/MapObjects/MapObjects.polygons"))));
        playerBodiesLoader = new BodyEditorLoader(new String(Files.readAllBytes(Paths.get("Tanks/TanksBodies.polygons"))));
        bulletsLoader = new BodyEditorLoader(new String(Files.readAllBytes(Paths.get("Tanks/Bullets.polygons"))));
    }

    static {
        objectsSizes.put(1, new Vector2(96, 32));
        objectsSizes.put(2, new Vector2(104, 32));
        objectsSizes.put(3, new Vector2(48, 48));
        objectsSizes.put(4, new Vector2(40, 56));
        objectsSizes.put(5, new Vector2(56, 56));
        objectsSizes.put(6, new Vector2(56, 56));
        objectsSizes.put(7, new Vector2(128, 128));
        objectsSizes.put(8, new Vector2(72, 72));

        bulletsSizes.put("1 (4)", new Vector2(16, 28));
        bulletsSizes.put("4", new Vector2(16, 52));
        bulletsSizes.put("2 (2)", new Vector2(16, 36));
        bulletsSizes.put("2 (3)", new Vector2(13, 29));
        bulletsSizes.put("3", new Vector2(24, 32));
    }

    public void addPlayersToMap(BalancedPlayer player1, BalancedPlayer player2, Balanced... events){
        Map map = maps.get(ThreadLocalRandom.current().nextInt(0, maps.size()));
        GameWorld world = new GameWorld(mapObjectsLoader, map, playerBodiesLoader, bulletsLoader);
        world.addPlayers(player1, player2, events);
        worlds.add(world);
        if(events.length > 1){
            MapPacket mapPacket1 = new MapPacket();
            PlayerDataPacket playerDataPacket1 = world.getPlayerData(player1.player.nickName);
            mapPacket1.map = map;
            mapPacket1.myTankId = player1.tank.id;
            mapPacket1.opponent = player2.player;
            mapPacket1.dataPacket = playerDataPacket1;
            mapPacket1.opponentTank = player2.tank;
            events[0].mapFound(mapPacket1, player1);
            MapPacket mapPacket2 = new MapPacket();
            PlayerDataPacket playerDataPacket2 = world.getPlayerData(player2.player.nickName);
            mapPacket2.map = map;
            mapPacket2.myTankId = player2.tank.id;
            mapPacket2.opponent = player1.player;
            mapPacket2.dataPacket = playerDataPacket2;
            mapPacket2.opponentTank = player1.tank;
            events[1].mapFound(mapPacket2, player2);
        }
    }

    public void playerReady(String email){
        worlds.forEach(c -> c.playerReady(email));
    }

    public void rotationPacketArrived(String nick, RotationPacket packet){
        worlds.forEach(c -> c.setPlayerRotation(nick, packet));
    }

    public void shootPacketArrived(String nick){
        worlds.forEach(c -> c.playerShoot(nick));
    }

    public void start(){
        new Thread(() ->{
            while (!stopped){
                for (GameWorld world : worlds) {
                    if(!world.destroyed())
                        world.update(6, 2);
                    else {
                        world.dispose();
                        worlds.removeIf(c -> c.equals(world));
                    }
                }
            }
        }).start();
    }

    public void playerConnect(Client client, Balanced... listeners) {
        worlds.forEach(c -> c.playerConnected(client, listeners));
    }

    public void stop(){
        stopped = true;
    }
}
