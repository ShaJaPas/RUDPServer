package main.network;

import com.whirvis.jraknet.peer.RakNetClientPeer;
import main.encryption.SecureData;

public class Client {

    public RakNetClientPeer peer;
    public SecureData secureData;
    public String email;
    public String nick;

    public Client(RakNetClientPeer peer) throws Exception{
        this.peer = peer;
        secureData = new SecureData();
    }
}
