package main.encryption;

import com.badlogic.gdx.math.Vector2;
import main.network.PacketConstants;
import main.player.*;
import main.player.balance.UnBalancedReason;
import main.player.chest.Chest;
import main.player.chest.ChestName;
import main.player.chest.DailyItem;
import main.player.gameManager.*;
import main.player.gameManager.map.MapObjects;
import org.msgpack.MessagePack;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;

public class SecureData {
    public EncryptionAES aes;
    private byte[] key;
    private final MessagePack msgPack;

    public SecureData() throws NoSuchPaddingException, NoSuchAlgorithmException {
        aes = new EncryptionAES();
        key = EncryptionAES.getRandomKey();
        msgPack = new MessagePack();
        msgPack.register(Tank.class);
        msgPack.register(DailyItem.class);
        msgPack.register(Player.class);
        msgPack.register(SignInData.class);
        msgPack.register(SignUpData.class);
        msgPack.register(UnBalancedReason.class);
        msgPack.register(ChestName.class);
        msgPack.register(Chest.class);
        msgPack.register(BattleResultsEnum.class);
        msgPack.register(BattleResults.class);
        msgPack.register(MapObjects.class);
        msgPack.register(main.player.gameManager.map.Map.class);
        msgPack.register(GamePlayerData.Bullet.class);
        msgPack.register(GamePlayerData.class);
        msgPack.register(PlayerDataPacket.class);
        msgPack.register(MapPacket.class);
        msgPack.register(RotationPacket.class);
        msgPack.register(ExplosionPacket.class);
    }

    public void setKey(byte[] key, byte[] iv) {
        this.key = key;
        aes.setKey(key, iv);
    }

    public byte[] getKey(){
        return key;
    }

    public byte[] makeDataSecure(byte[] array) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] iv = EncryptionAES.getRandomIV();
        aes.setIV(iv);
        array = aes.encrypt(array);
        byte[] result = new byte[array.length + 17];
        System.arraycopy(array, 0, result, 0, array.length);
        System.arraycopy(iv, 0, result, array.length, 16);
        result[result.length - 1] = PacketConstants.AES_KEY_PACKET;
        return result;
    }

    public byte[] makeDataSecureDefault(byte[] array) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException {
        byte[] iv = EncryptionAES.getRandomIV();
        EncryptionAES aes1 = new EncryptionAES();
        aes1.setKey(EncryptionAES.defaultKey, iv);
        array = aes1.encrypt(array);
        byte[] result = new byte[array.length + 17];
        System.arraycopy(array, 0, result, 0, array.length);
        System.arraycopy(iv, 0, result, array.length, 16);
        result[result.length - 1] = PacketConstants.AES_KEY_PACKET;
        return result;
    }

    public byte[] makeDataUnSecureDefault(byte[] data) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException {
        byte[] iv = new byte[16];
        EncryptionAES aes1 = new EncryptionAES();
        byte[] keydata = new byte[data.length - 17];
        System.arraycopy(data, data.length - 17, iv, 0,16);
        System.arraycopy(data, 0, keydata, 0, data.length - 17);
        aes1.setKey(EncryptionAES.defaultKey, iv);
        keydata = aes1.decrypt(keydata);
        return keydata;
    }

    public byte[] makeDataUnSecure(byte[] data) throws IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException {
        byte[] iv = new byte[16];
        byte[] keydata = new byte[data.length - 17];
        System.arraycopy(data, data.length - 17, iv, 0,16);
        System.arraycopy(data, 0, keydata, 0, data.length - 17);
        aes.setIV(iv);
        keydata = aes.decrypt(keydata);
        return keydata;
    }

    public <T> byte[] serialize(T o){
        try {
            return msgPack.write(o);
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public <T> T deserialize(byte[] array, Class<T> tClass) {
        try {
            return msgPack.read(array, tClass);
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public static String getRandomKey(Map<String, ConfCode> clients){
        String uuid = UUID.randomUUID().toString();
        boolean found = false;
        while(true) {
            for (Object key : clients.keySet()) {
                ConfCode value = clients.get(key);
                if (!value.update(System.currentTimeMillis()))
                    clients.remove(key);
                if (value.code.equals(uuid))
                    found = true;
            }
            if(found) {
                uuid = UUID.randomUUID().toString();
                found = false;
            } else break;
        }
        return uuid;
    }
}
