package main.player;

import org.msgpack.annotation.Index;

import java.io.Serializable;

public class Tank implements Serializable {
    @Index(0)
    public int id;
    @Index(1)
    public int level;
    @Index(2)
    public int count;

    public Tank(int id, int level, int count){
        this.id = id;
        this.level = level;
        this.count = count;
    }

    public Tank(){

    }
}
