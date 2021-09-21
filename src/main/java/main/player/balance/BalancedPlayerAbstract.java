package main.player.balance;

import com.badlogic.gdx.physics.box2d.Body;
import main.network.Client;
import main.player.Player;
import main.player.Tank;
import main.player.TanksInfo;

import java.time.Instant;

public abstract class BalancedPlayerAbstract {
    public int skillPoints() {
        return 0;
    }
    public Instant joinTime;
    public Player player;
    public Body body;
    public boolean ready = false;
    public boolean disconnected = false;
    public int shots = 0;
    public int succeededShots = 0;
    public int damageDealt;
    public int damageTaken;
    public float gunRotation;
    public int hp;
    public int damage;
    public float gunAngle;
    public float bodyAngle;
    public boolean inMove;
    public Client client;
    public TanksInfo info;
    public Tank tank;
    public int reloadingLeft;
}
