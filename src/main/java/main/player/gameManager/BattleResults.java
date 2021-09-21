package main.player.gameManager;

import org.msgpack.annotation.Index;

public class BattleResults {
    @Index(0)
    public boolean winner;
    @Index(1)
    public BattleResultsEnum results;
    @Index(2)
    public int trophies;
    @Index(3)
    public int xp;
    @Index(4)
    public int coins;
    @Index(5)
    public int damageDealt;
    @Index(6)
    public int damageTaken;
    @Index(7)
    public float accuracy;

    public BattleResults(){

    }

    public float getEfficiency(){
        float res = (accuracy + 0.5f) * ((float) damageDealt / damageTaken);
        return Float.isNaN(res) || Float.isInfinite(res) ? 0 : res;
    }
}
