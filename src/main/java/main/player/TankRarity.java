package main.player;

public enum TankRarity {
    COMMON(60),
    RARE(15),
    EPIC(2),
    MYTHICAL(0.15),
    LEGENDARY(0.015);

    public final double chance;

    TankRarity(double chance){
        this.chance = chance;
    }

}
