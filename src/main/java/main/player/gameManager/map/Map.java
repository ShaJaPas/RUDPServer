package main.player.gameManager.map;

import org.msgpack.annotation.Ignore;
import org.msgpack.annotation.Index;

import java.io.Serializable;

public class Map implements Serializable {
    @Index(0)
    public String name;
    @Index(1)
    public int width;
    @Index(2)
    public int height;
    @Ignore
    public int player1Y;
    @Ignore
    public int player2Y;
    @Index(3)
    public MapObjects[] objects;

    public Map(){

    }
}
