package main.player.gameManager;

import org.msgpack.annotation.Index;

public class RotationPacket {
    @Index(0)
    public float bodyRotation;
    @Index(1)
    public float gunRotation;
    @Index(2)
    public boolean inMove;

    public RotationPacket(){

    }
}
