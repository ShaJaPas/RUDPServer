package main.network;

public class PacketConstants {
    public static final byte AES_KEY_PACKET = 1;
    public static final byte SIGN_IN_PACKET = 2;
    public static final byte SIGN_UP_PACKET = 3;
    public static final byte SIGN_UP_PACKET_REQUEST = 4;
    public static final byte SIGN_UP_PACKET_REMOVE_KEY = 5;
    public static final byte SIGN_UP_SUCCESS = 6;
    public static final byte SIGN_UP_ERROR = 7;
    public static final byte SIGN_UP_OK = 8;
    public static final byte SIGN_IN_SUCCESS = 9;
    public static final byte SIGN_IN_SUCCESS_WITHOUT_NICK = 10;
    public static final byte GET_PLAYER_PROFILE = 11;
    public static final byte INSECURE = 69;
    //insecure
    public static final byte GET_VERSION = 70;
    public static final byte FIRST_NICKNAME_REQUEST = 71;
    public static final byte FIRST_NICKNAME_ERROR = 72;
    public static final byte FIRST_NICKNAME_OK = 73;
    public static final byte GET_FRIENDS = 74;
    public static final byte GET_ONLINE = 75;
    public static final byte GET_NICKNAME_SEARCH = 76;
    public static final byte GET_NICKNAME = 77;
    public static final byte GET_CHEST = 78;
    public static final byte GET_DAILY_ITEMS = 79;
    public static final byte GET_DAILY_TIME = 80;
    public static final byte BUY_DAILY_ITEM = 81;
    public static final byte BUY_DAILY_ITEM_OK = 82;
    public static final byte BUY_DAILY_ITEM_ERROR = 83;
    public static final byte UPGRADE_CARD = 84;
    public static final byte UPGRADE_CARD_OK = 85;
    public static final byte UPGRADE_CARD_ERROR = 86;
    public static final byte BUY_CHEST = 87;
    public static final byte BUY_CHEST_OK = 88;
    public static final byte BUY_CHEST_ERROR = 89;
    public static final byte JOIN_BALANCER = 90;
    public static final byte EXIT_BALANCER = 91;
    public static final byte BATTLE_ENDS = 92;
    public static final byte MAP_FOUND = 93;
    public static final byte MAP_READY = 94;
    public static final byte BATTLE_STARTED = 95;
    public static final byte BATTLE_DATA = 96;
    public static final byte BATTLE_EXISTS = 97;
    public static final byte ROTATION_PACKET = 98;
    public static final byte SHOOT_PACKET = 99;
    public static final byte BULLET_EXPLOSION = 100;
}
