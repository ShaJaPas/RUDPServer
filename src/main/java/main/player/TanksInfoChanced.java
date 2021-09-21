package main.player;

import main.tools.chance.Chance;

public class TanksInfoChanced extends Chance {
    public TanksInfo info;

    public TanksInfoChanced(TanksInfo info, double chance){
        this.info = info;
        this.chance = chance;
    }
}
