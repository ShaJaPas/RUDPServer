package main.player.gameManager;

import main.player.Player;
import main.player.Tank;
import main.player.gameManager.map.Map;
import org.msgpack.annotation.Index;

public class MapPacket {
    @Index(0)
    public Map map;
    @Index(1)
    public Player opponent;
    @Index(2)
    public int myTankId;
    @Index(3)
    public Tank opponentTank;
    @Index(4)
    public PlayerDataPacket dataPacket;

    public MapPacket(){

    }
}
