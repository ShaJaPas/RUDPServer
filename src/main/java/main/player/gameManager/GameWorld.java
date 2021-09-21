package main.player.gameManager;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Disposable;
import main.StarterClass;
import main.network.Client;
import main.player.TanksInfo;
import main.player.balance.Balanced;
import main.player.balance.BalancedPlayer;
import main.player.gameManager.map.Map;
import main.player.gameManager.map.MapObjects;
import main.tools.physics.BodyEditorLoader;
import main.tools.physics.MyContactListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class GameWorld implements Disposable {
    public final World gameMap;
    public static final float UPDATE_TIME_MIN = 1000/30f;
    public static final float UPDATE_TIME_MAX = 1000/60f;
    public static final long WAIT_TIME = 5999;
    public static final long MAX_BATTLE_TIME = 1000 * 60 * 3;
    private long startTime;
    public final Map map;
    private long packetId;
    private boolean destroyed = false;
    public static final float SCALE = 0.1f;
    private final BodyEditorLoader bodiesLoader;
    private final BodyEditorLoader bulletsLoader;
    private Balanced player1Listener;
    private Balanced player2Listener;
    public BalancedPlayer player1, player2;
    private final List<Bullet> bullets = new CopyOnWriteArrayList<>();

    public GameWorld(BodyEditorLoader loader, Map map, BodyEditorLoader playerBodiesLoader, BodyEditorLoader playerBulletsLoader){
        gameMap = new World(Vector2.Zero, true);
        gameMap.setContactListener(new MyContactListener());
        this.map = map;
        bulletsLoader = playerBulletsLoader;
        createBox(map.width, map.height);
        for (MapObjects object : map.objects) {
            if (loader.bodyExists(String.valueOf(object.id))) {
                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;

                Body body = gameMap.createBody(bodyDef);

                FixtureDef fixtureDef = new FixtureDef();

                loader.attachFixture(body, String.valueOf(object.id), fixtureDef, new Vector2(GameManager.objectsSizes.get(object.id).x, GameManager.objectsSizes.get(object.id).x).cpy().scl(object.scale * SCALE), GameManager.objectsSizes.get(object.id).cpy().scl(0.5f * SCALE));
                body.setTransform(object.x * SCALE + GameManager.objectsSizes.get(object.id).cpy().scl(0.5f * SCALE).x, object.y * SCALE + GameManager.objectsSizes.get(object.id).cpy().scl(0.5f * SCALE).y, (float)Math.toRadians(180 + object.rotation));
            }
        }
        bodiesLoader = playerBodiesLoader;
    }

    public void addPlayers(BalancedPlayer player1, BalancedPlayer player2, Balanced... listener){
        if(listener.length > 0){
            player1Listener = listener[0];
        }
        if(listener.length > 1){
            player2Listener = listener[1];
        }
        TanksInfo info1 = player1.info;

        if(ThreadLocalRandom.current().nextBoolean()) {
            BalancedPlayer a = player1;
            player1 = player2;
            player2 = a;
        }
        BodyDef bodyDef1 = new BodyDef();
        bodyDef1.type = BodyDef.BodyType.DynamicBody;
        Body body1 = gameMap.createBody(bodyDef1);
        FixtureDef fixtureDef1 = new FixtureDef();
        bodiesLoader.attachFixture(body1, String.valueOf(info1.id), fixtureDef1, new Vector2(player1.info.graphicsInfo.tankWidth, player1.info.graphicsInfo.tankWidth).scl(SCALE), new Vector2(player1.info.graphicsInfo.tankWidth / 2f, player1.info.graphicsInfo.tankHeight / 2f).scl(SCALE));
        body1.setTransform((float) map.width / 2 * SCALE, map.player1Y * SCALE, 0);

        player1.body = body1;
        player1.body.setUserData(player1);
        this.player1 = player1;

        TanksInfo info2 = player2.info;

        BodyDef bodyDef2 = new BodyDef();
        bodyDef2.type = BodyDef.BodyType.DynamicBody;
        Body body2 = gameMap.createBody(bodyDef2);
        FixtureDef fixtureDef2 = new FixtureDef();
        bodiesLoader.attachFixture(body2, String.valueOf(info2.id), fixtureDef2, new Vector2(player2.info.graphicsInfo.tankWidth, player2.info.graphicsInfo.tankWidth).scl(SCALE), new Vector2(player2.info.graphicsInfo.tankWidth / 2f, player2.info.graphicsInfo.tankHeight / 2f).scl(SCALE));
        body2.setTransform((float) map.width / 2 * SCALE, map.player2Y * SCALE, (float) Math.toRadians(180));
        player2.gunAngle = 180;
        player2.gunRotation = 180;
        player2.bodyAngle = 180;

        player2.body = body2;
        player2.body.setUserData(player2);
        this.player2 = player2;
    }

    public void setStartTime(){
        startTime = System.currentTimeMillis();
    }

    public void setPlayerRotation(String nick, RotationPacket packet){
        if(containsPlayer(nick)){
            BalancedPlayer player = player1.player.nickName.equals(nick) ? player1 : player2;
            player.gunAngle = packet.gunRotation;
            player.bodyAngle = packet.bodyRotation;
            player.inMove = packet.inMove;
        }
    }

    private long startPoint = System.currentTimeMillis();

    private static double normalize360(double angle) {
        angle = angle % 360;
        if (angle < 0) {
            angle = angle + 360;
        }
        return angle;
    }

    public void update(int velocityIterations, int positionIterations){
        if(startTime != 0) {
            if (System.currentTimeMillis() - startPoint >= UPDATE_TIME_MIN) {
                if (getElapsedTime() >= WAIT_TIME) {
                    for (Bullet bullet : bullets) {
                        if(bullet.destroyed){
                            bullet.body.setActive(false);
                            gameMap.destroyBody(bullet.body);
                            bullets.remove(bullet);
                        }
                    }
                    long time = System.currentTimeMillis() - startPoint;
                    player1.reloadingLeft = player1.reloadingLeft - time > 0 ? (int) (player1.reloadingLeft - time) : 0;
                    player2.reloadingLeft = player2.reloadingLeft - time > 0 ? (int) (player2.reloadingLeft - time) : 0;
                    if (player1.gunRotation != player1.gunAngle && player1.gunAngle != 0) {
                        float step = Math.min(System.currentTimeMillis() - startPoint, UPDATE_TIME_MAX) / 1000f * player1.info.characteristics.gunRotateDegrees;
                        float right = (float) normalize360(360 - player1.gunRotation + player1.gunAngle);
                        if (player1.gunAngle > player1.gunRotation)
                            right = Math.min(right, player1.gunAngle - player1.gunRotation);
                        float left = (float) normalize360(360 - right);
                        if (right < left)
                            player1.gunRotation += step;
                        else player1.gunRotation -= step;
                        player1.gunRotation = (float) normalize360(player1.gunRotation);
                        if (Math.abs(player1.gunAngle - player1.gunRotation) < step)
                            player1.gunRotation = player1.gunAngle;
                    }

                    if (player2.gunRotation != player2.gunAngle && player2.gunAngle != 0) {
                        float step = Math.min(System.currentTimeMillis() - startPoint, UPDATE_TIME_MAX) / 1000f * player2.info.characteristics.gunRotateDegrees;
                        float right = (float) normalize360(360 - player2.gunRotation + player2.gunAngle);
                        if (player2.gunAngle > player2.gunRotation)
                            right = Math.min(right, player2.gunAngle - player2.gunRotation);
                        float left = (float) normalize360(360 - right);
                        if (right < left)
                            player2.gunRotation += step;
                        else player2.gunRotation -= step;
                        player2.gunRotation = (float) normalize360(player2.gunRotation);
                        if (Math.abs(player2.gunAngle - player2.gunRotation) < step)
                            player2.gunRotation = player2.gunAngle;
                    }

                    float step = Math.min(System.currentTimeMillis() - startPoint, UPDATE_TIME_MAX) / 1000f * player1.info.characteristics.bodyRotateDegrees;
                    if (Math.abs(player1.bodyAngle - normalize360(Math.toDegrees(player1.body.getAngle()))) < step) {
                        player1.body.setTransform(player1.body.getPosition(), (float) Math.toRadians(player1.bodyAngle));
                        player1.body.setAngularVelocity(0);
                        player1.body.setFixedRotation(true);
                    } else {
                        if (Math.abs(normalize360(Math.toDegrees(player1.body.getAngle())) - player1.bodyAngle) > 0.0001f && player1.bodyAngle != 0) {
                            player1.body.setFixedRotation(false);
                            float right = (float) normalize360(360 - Math.toDegrees(player1.body.getAngle()) + player1.bodyAngle);
                            if (player1.bodyAngle > normalize360(Math.toDegrees(player1.body.getAngle())))
                                right = Math.min(right, player1.bodyAngle - (float) normalize360(Math.toDegrees(player1.body.getAngle())));
                            float left = (float) normalize360(360 - right);
                            if (right < left)
                                player1.body.setAngularVelocity((float) Math.toRadians(player1.info.characteristics.bodyRotateDegrees));
                            else
                                player1.body.setAngularVelocity((float) Math.toRadians(-player1.info.characteristics.bodyRotateDegrees));
                        } else {
                            player1.body.setAngularVelocity(0);
                            player1.body.setFixedRotation(true);
                        }
                    }

                    step = Math.min(System.currentTimeMillis() - startPoint, UPDATE_TIME_MAX) / 1000f * player2.info.characteristics.bodyRotateDegrees;
                    if (Math.abs(player2.bodyAngle - normalize360(Math.toDegrees(player2.body.getAngle()))) < step) {
                        player2.body.setTransform(player2.body.getPosition(), (float) Math.toRadians(player2.bodyAngle));
                        player2.body.setAngularVelocity(0);
                        player2.body.setFixedRotation(true);
                    } else {
                        if (Math.abs(normalize360(Math.toDegrees(player2.body.getAngle())) - player2.bodyAngle) > 0.0001f && player2.bodyAngle != 0) {
                            player2.body.setFixedRotation(false);
                            float right = (float) normalize360(360 - Math.toDegrees(player2.body.getAngle()) + player2.bodyAngle);
                            if (player2.bodyAngle > normalize360(Math.toDegrees(player2.body.getAngle())))
                                right = Math.min(right, player2.bodyAngle - (float) normalize360(Math.toDegrees(player2.body.getAngle())));
                            float left = (float) normalize360(360 - right);
                            if (right < left)
                                player2.body.setAngularVelocity((float) Math.toRadians(player2.info.characteristics.bodyRotateDegrees));
                            else
                                player2.body.setAngularVelocity((float) Math.toRadians(-player2.info.characteristics.bodyRotateDegrees));
                        } else {
                            player2.body.setAngularVelocity(0);
                            player2.body.setFixedRotation(true);
                        }
                    }

                    if(player1.inMove)
                        player1.body.setLinearVelocity(new Vector2(0, player1.info.characteristics.velocity * SCALE).rotate(180 + (float) Math.toDegrees(player1.body.getAngle())));
                    else player1.body.setLinearVelocity(0,0);
                    if(player2.inMove)
                        player2.body.setLinearVelocity(new Vector2(0, player2.info.characteristics.velocity * SCALE).rotate(180 + (float) Math.toDegrees(player2.body.getAngle())));
                    else player2.body.setLinearVelocity(0,0);
                    gameMap.step(Math.min(System.currentTimeMillis() - startPoint, UPDATE_TIME_MAX) / 1000f, velocityIterations, positionIterations);
                }
                startPoint = System.currentTimeMillis();
                player1Listener.packetForPlayer(getPlayerData(player1.player.nickName), player1);
                player2Listener.packetForPlayer(getPlayerData(player2.player.nickName), player2);
                packetId++;
            }
        }
        if ((startTime != 0 && getElapsedTime() >= MAX_BATTLE_TIME + WAIT_TIME && player1.ready && player2.ready) || (startTime != 0 && (player1.hp == 0 || player2.hp == 0))) {
            battleEnds();
            destroyed = true;
            System.out.println("Map with player" + player1.player.nickName + " and " + player2.player.nickName + " has destroyed");
        }
    }

    private void battleEnds(){
        BalancedPlayer winner = player1.hp > 0 ? player1 : player2;
        BalancedPlayer looser = player1.hp == 0 ? player1 : player2;
        BattleResults winnerResults = new BattleResults();
        winnerResults.results = player1.hp > 0 && player2.hp > 0 ? BattleResultsEnum.DRAW : BattleResultsEnum.SUCCESS;
        if(winnerResults.results == BattleResultsEnum.SUCCESS)
            winnerResults.winner = true;
        winnerResults.accuracy = winner.succeededShots > 0 ? (float) winner.succeededShots / winner.shots : 0;
        winnerResults.damageDealt = winner.damageDealt;
        winnerResults.damageTaken = winner.damageTaken;
        if(winnerResults.results != BattleResultsEnum.DRAW){
            winnerResults.trophies = 30 + (int) (looser.player.getEfficiency() - winner.player.getEfficiency());
            winnerResults.coins = ThreadLocalRandom.current().nextInt(70, 100) + ThreadLocalRandom.current().nextInt(10, 15) * (looser.tank.level - winner.tank.level);
            winnerResults.xp = (int)(15 * winnerResults.getEfficiency());
        }
        BattleResults looserResults = new BattleResults();
        looserResults.winner = false;
        looserResults.results = player1.hp > 0 && player2.hp > 0 ? BattleResultsEnum.DRAW : BattleResultsEnum.SUCCESS;
        looserResults.accuracy = looser.succeededShots > 0 ? (float) looser.succeededShots / looser.shots : 0;
        looserResults.damageDealt = looser.damageDealt;
        looserResults.damageTaken = looser.damageTaken;
        if(looserResults.results != BattleResultsEnum.DRAW){
            looserResults.trophies = winnerResults.winner ? -(int) (winner.player.trophies / 100) + (int) (winner.player.getEfficiency() - looser.player.getEfficiency()) : 0;
            looserResults.coins = ThreadLocalRandom.current().nextInt(5, 10) + ThreadLocalRandom.current().nextInt(5, 10) * (looser.tank.level - winner.tank.level);
            looserResults.xp = (int)(15 * looserResults.getEfficiency());
        }

        winner.player.battleCount++;
        winner.player.trophies += winnerResults.trophies;
        winner.player.xp += winnerResults.xp;
        if(winner.player.xp >= winner.player.rankLevel * 50){
            winner.player.xp -= winner.player.rankLevel * 50;
            winner.player.rankLevel++;
        }
        winner.player.coins += winnerResults.coins;
        winner.player.accuracy = (winner.player.accuracy * (winner.player.battleCount - 1) + winnerResults.accuracy) / winner.player.battleCount;
        winner.player.damageDealt = (winner.player.damageDealt * (winner.player.battleCount - 1) + winnerResults.damageDealt) / winner.player.battleCount;
        winner.player.damageTaken = (winner.player.damageTaken * (winner.player.battleCount - 1) + winnerResults.damageTaken) / winner.player.battleCount;
        if(winnerResults.winner)
            winner.player.victoriesCount++;
        StarterClass.db.update(winner.player);

        looser.player.battleCount++;
        looser.player.trophies += looserResults.trophies;
        if(looser.player.trophies < 0)
            looser.player.trophies = 0;
        looser.player.xp += looserResults.xp;
        if(looser.player.xp >= looser.player.rankLevel * 50){
            looser.player.xp -= looser.player.rankLevel * 50;
            looser.player.rankLevel++;
        }
        looser.player.coins += looserResults.coins;
        looser.player.accuracy = (looser.player.accuracy * (looser.player.battleCount - 1) + looserResults.accuracy) / looser.player.battleCount;
        looser.player.damageDealt = (looser.player.damageDealt * (looser.player.battleCount - 1) + looserResults.damageDealt) / looser.player.battleCount;
        looser.player.damageTaken = (looser.player.damageTaken * (looser.player.battleCount - 1) + looserResults.damageTaken) / looser.player.battleCount;
        StarterClass.db.update(looser.player);

        player2Listener.battleEnds(winner, looser, winnerResults, looserResults);
    }


    public boolean destroyed(){
        return destroyed;
    }

    public boolean containsPlayer(String nickname){
        return player1.player.nickName.equals(nickname) || player2.player.nickName.equals(nickname);
    }

    public void playerShoot(String nick) {
        if (containsPlayer(nick)) {
            BalancedPlayer player = player1.player.nickName.equals(nick) ? player1 : player2;
            BalancedPlayer foe = !player1.player.nickName.equals(nick) ? player1 : player2;
            if(player.reloadingLeft == 0){
                player.reloadingLeft = (int) (player.info.characteristics.reloading * 1000);
                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.DynamicBody;
                Body body = gameMap.createBody(bodyDef);
                FixtureDef fixtureDef = new FixtureDef();
                Vector2 vector2 = ((new Vector2(player.info.graphicsInfo.bulletX, player.info.graphicsInfo.bulletY).scl(SCALE)).add(GameManager.bulletsSizes.get(player.info.graphicsInfo.bulletName).cpy().scl(0.5f * SCALE))).sub(new Vector2(player.info.graphicsInfo.tankWidth / 2f, player.info.graphicsInfo.tankHeight / 2f).scl(SCALE));
                bulletsLoader.attachFixture(body, String.valueOf(player.info.graphicsInfo.bulletName), fixtureDef, new Vector2(GameManager.bulletsSizes.get(player.info.graphicsInfo.bulletName).x, GameManager.bulletsSizes.get(player.info.graphicsInfo.bulletName).x).cpy().scl(SCALE), GameManager.bulletsSizes.get(player.info.graphicsInfo.bulletName).cpy().scl(0.5f * SCALE));
                body.setTransform(player.body.getPosition().sub(new Vector2(player.info.graphicsInfo.tankWidth / 2f, player.info.graphicsInfo.tankHeight / 2f).scl(SCALE)).add(new Vector2(player.info.graphicsInfo.tankWidth / 2f, player.info.graphicsInfo.tankHeight / 2f).scl(SCALE)).add(vector2.rotate(player.gunRotation + 180)), (float) Math.toRadians(player.gunRotation));
                body.setBullet(true);
                body.setLinearVelocity(new Vector2(0, player.info.characteristics.bulletSpeed * SCALE).rotate(180 + player.gunRotation));
                Bullet bullet = new Bullet();
                bullet.body = body;
                bullet.player = player;
                bullet.foe = foe;
                body.setUserData(bullet);
                bullets.add(bullet);
                player.shots++;
            }
        }
    }

    public PlayerDataPacket getPlayerData(String nickname){
        BalancedPlayer player = player1.player.nickName.equals(nickname) ? player1 : player2;
        BalancedPlayer foe = !player1.player.nickName.equals(nickname) ? player1 : player2;
        PlayerDataPacket playerDataPacket = new PlayerDataPacket();
        playerDataPacket.timeLeft = WAIT_TIME + MAX_BATTLE_TIME - getElapsedTime();
        playerDataPacket.foeData = new GamePlayerData();
        playerDataPacket.foeData.x = foe.body.getPosition().x / SCALE;
        playerDataPacket.foeData.y = foe.body.getPosition().y / SCALE;
        playerDataPacket.foeData.bodyRotation = (float) Math.toDegrees(foe.body.getAngle());
        playerDataPacket.foeData.gunRotation = foe.gunRotation;
        playerDataPacket.foeData.hp = foe.hp;
        playerDataPacket.myData = new GamePlayerData();
        playerDataPacket.myData.bullets = bullets.stream().map(c -> new GamePlayerData.Bullet(c.body.getPosition().x / SCALE, c.body.getPosition().y / SCALE, (float)Math.toDegrees(c.body.getAngle()), c.player.info.graphicsInfo.bulletName)).toArray(GamePlayerData.Bullet[]::new);
        playerDataPacket.myData.coolDown = player.reloadingLeft / 1000f;
        playerDataPacket.myData.hp = player.hp;
        playerDataPacket.myData.gunRotation = player.gunRotation;
        float a = player.body.getPosition().y;
        playerDataPacket.myData.x = player.body.getPosition().x / SCALE;
        playerDataPacket.myData.y = player.body.getPosition().y / SCALE;
        playerDataPacket.myData.bodyRotation = (float) Math.toDegrees(player.body.getAngle());
        playerDataPacket.id = packetId;
        return playerDataPacket;
    }

    private Body createBox(float width, float height) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        Body box = gameMap.createBody(def);
        PolygonShape poly = new PolygonShape();
        poly.set(new Vector2[]{new Vector2(0, 0), new Vector2(0, -1 * SCALE), new Vector2(width * SCALE, -1 * SCALE), new Vector2(width * SCALE, 0), new Vector2(0, 0)});
        box.createFixture(poly, (float) 1);
        poly.dispose();
        poly = new PolygonShape();
        poly.set(new Vector2[]{new Vector2(0, 0), new Vector2(0, height * SCALE), new Vector2(-1 * SCALE, height * SCALE), new Vector2(-1 * SCALE, 0)});
        box.createFixture(poly, (float) 1);
        poly.dispose();
        poly = new PolygonShape();
        poly.set(new Vector2[]{new Vector2(0, height * SCALE), new Vector2(width * SCALE, height * SCALE), new Vector2(width * SCALE, (height + 1) * SCALE), new Vector2(0, (height + 1) * SCALE)});
        box.createFixture(poly, (float) 1);
        poly.dispose();
        poly = new PolygonShape();
        poly.set(new Vector2[]{new Vector2(width * SCALE, height * SCALE), new Vector2(width * SCALE, 0), new Vector2((width + 1) * SCALE, 0), new Vector2((width + 1) * SCALE, height * SCALE)});
        box.createFixture(poly, (float) 1);
        poly.dispose();
        return box;
    }

    public void playerReady(String email){
        if(player1.player.email.equals(email))
            player1.ready = true;
        if(player2.player.email.equals(email))
            player2.ready = true;
        if(player1.ready && player2.ready && startTime == 0){
            setStartTime();
            player1Listener.battleStarted(WAIT_TIME, player1);
            player2Listener.battleStarted(WAIT_TIME, player2);
        }
    }

    public void playerConnected(Client client, Balanced... listeners){
        if(player1.player.email.equals(client.email)){
            player1.client = client;
            if(listeners.length > 0)
                player1Listener = listeners[0];
            MapPacket mapPacket = new MapPacket();
            mapPacket.map = map;
            mapPacket.myTankId = player1.tank.id;
            mapPacket.opponent = player2.player;
            mapPacket.opponentTank = player2.tank;
            mapPacket.dataPacket = getPlayerData(client.nick);
            player1Listener.mapFound(mapPacket, player1);
            player1Listener.battleStarted(WAIT_TIME, player1);
        }
        if(player2.player.email.equals(client.email)){
            player2.client = client;
            if(listeners.length > 0)
                player2Listener = listeners[0];
            MapPacket mapPacket = new MapPacket();
            mapPacket.map = map;
            mapPacket.myTankId = player2.tank.id;
            mapPacket.opponent = player1.player;
            mapPacket.opponentTank = player1.tank;
            mapPacket.dataPacket = getPlayerData(client.nick);
            player2Listener.mapFound(mapPacket, player2);
            player2Listener.battleStarted(WAIT_TIME, player2);
        }
    }

    public long getElapsedTime(){
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public void dispose(){
        gameMap.dispose();
    }
}
