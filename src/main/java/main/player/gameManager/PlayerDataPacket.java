package main.player.gameManager;

import org.msgpack.annotation.Index;

import java.io.Serializable;

public class PlayerDataPacket implements Serializable {
    @Index(0)
    public GamePlayerData myData;
    @Index(1)
    public GamePlayerData foeData;
    @Index(2)
    public long timeLeft;
    @Index(3)
    public long id;

    public PlayerDataPacket(){
    }
}
