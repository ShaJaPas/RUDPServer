package main.player.chest;

import org.msgpack.annotation.Index;

import java.io.Serializable;

public class DailyItem implements Serializable {
    @Index(0)
    public int price;
    @Index(1)
    public int tankId;
    @Index(2)
    public int count;
    @Index(3)
    public boolean bought;

    public DailyItem(int price, int tankId, int count){
        this.price = price;
        this.tankId = tankId;
        this.count = count;
        this.bought = false;
    }

    public DailyItem(){

    }
}
