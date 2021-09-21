package main.player;

public class TanksInfo {
    public int id;
    public TankGraphicsInfo graphicsInfo;
    public TankCharacteristics characteristics;

    public TanksInfo(){
        graphicsInfo = new TankGraphicsInfo();
        characteristics = new TankCharacteristics();
    }
}
