package main.player.chest;

public enum ChestName {
    COMMON(100), STARTER(0), RARE(240), EPIC(350), MYTHICAL(500), LEGENDARY(1000);

    public int value;

    ChestName(int name){
        value = name;
    }
}
