package main.encryption;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class EncryptionAES {
    public static byte[] defaultKey = {
            (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
            (byte)0x0e, (byte)0xc8, (byte)0xee, (byte)0x99,
            (byte)0x1e, (byte)0x01, (byte)0xbb, (byte)0x19,
            (byte)0x71, (byte)0x18, (byte)0x1e, (byte)0x69
    };

    private static SecureRandom random;
    private static SecureRandom randomIV;
    private final Cipher cipher;
    private IvParameterSpec parameterSpec;
    private SecretKey secret;

    public EncryptionAES() throws NoSuchPaddingException, NoSuchAlgorithmException {
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        random = new SecureRandom();
        randomIV = new SecureRandom();

        secret = new SecretKeySpec(defaultKey, "AES");
    }

    public byte[] encrypt(byte[] data) throws BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException {
        cipher.init(Cipher.ENCRYPT_MODE, secret, parameterSpec);
        return cipher.doFinal(data);
    }

    public byte[] decrypt(byte[] data) throws BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException, InvalidKeyException {
        cipher.init(Cipher.DECRYPT_MODE, secret, parameterSpec);
        return cipher.doFinal(data);
    }

    public void setKey(byte[] arr, byte[] iv){
        secret = new SecretKeySpec(arr, "AES");
        parameterSpec = new IvParameterSpec(iv);
    }

    public void setIV(byte[] iv){
        parameterSpec = new IvParameterSpec(iv);
    }

    public byte[] getIV(){
        return parameterSpec.getIV();
    }
    public static byte[] getRandomIV(){
        byte[] bytes = new byte[16];
        randomIV.nextBytes(bytes);
        return bytes;
    }

    public static byte[] getRandomKey(){
        byte[] a = new byte[16];
        random.nextBytes(a);
        return a;
    }
}
