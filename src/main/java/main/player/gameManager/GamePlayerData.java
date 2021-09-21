package main.player.gameManager;

import org.msgpack.annotation.Index;

import java.io.Serializable;

public class GamePlayerData implements Serializable {
    public static class Bullet{
        @Index(0)
        public float x;
        @Index(1)
        public float y;
        @Index(2)
        public float rotation;
        @Index(3)
        public String name;

        public Bullet(){

        }
        public Bullet(float x, float y, float rotation, String name){
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.name = name;
        }
    }

    @Index(0)
    public float x;
    @Index(1)
    public float y;
    @Index(2)
    public float bodyRotation;
    @Index(3)
    public float gunRotation;
    @Index(4)
    public int hp;
    @Index(5)
    public float coolDown;
    @Index(6)
    public Bullet[] bullets;

    public GamePlayerData(){

    }
}
