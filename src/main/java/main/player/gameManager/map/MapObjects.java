package main.player.gameManager.map;

import org.msgpack.annotation.Index;

public class MapObjects {
    @Index(0)
    public int id;
    @Index(1)
    public int x;
    @Index(2)
    public int y;
    @Index(3)
    public float scale;
    @Index(4)
    public float rotation;

    public MapObjects(){

    }
}
