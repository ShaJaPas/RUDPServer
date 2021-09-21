package main.player.gameManager;

import com.badlogic.gdx.physics.box2d.Body;
import main.player.balance.BalancedPlayer;

public class Bullet {
    public Body body;
    public BalancedPlayer player;
    public BalancedPlayer foe;
    public boolean destroyed = false;

    public Bullet(){

    }
}
