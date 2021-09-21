package main.network;

import com.whirvis.jraknet.Packet;
import com.whirvis.jraknet.RakNetPacket;
import com.whirvis.jraknet.peer.RakNetClientPeer;
import com.whirvis.jraknet.protocol.Reliability;
import com.whirvis.jraknet.server.RakNetServer;
import com.whirvis.jraknet.server.RakNetServerListener;

import io.netty.buffer.ByteBuf;
import main.StarterClass;
import main.encryption.EncryptionAES;
import main.encryption.SecureData;
import main.network.email.EmailSender;
import main.player.*;
import main.player.balance.Balanced;
import main.player.balance.BalancedPlayer;
import main.player.balance.UnBalancedReason;
import main.player.chest.Chest;
import main.player.chest.ChestName;
import main.player.chest.DailyItem;
import main.player.gameManager.BattleResults;
import main.player.gameManager.MapPacket;
import main.player.gameManager.PlayerDataPacket;
import main.player.gameManager.RotationPacket;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class BaseRakNetServerListener implements RakNetServerListener {

    List<Client> clients = new CopyOnWriteArrayList<>();
    Map<String, ConfCode> confCodes = new HashMap<>();
    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static final Pattern VALID_PASSWORD_REGEX =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}$", Pattern.CASE_INSENSITIVE);

    public static final long twelveHours = 1000 * 60 * 60 * 12;
    public static final int[] cardFull = {10, 50, 100, 300, 500, 1000, 2000, 3000, 5000, 10000};
    public static final int[] upgradeCards = {20, 70, 150, 400, 700, 1500, 3000, 5000, 8000, 13000};

    @Override
    public void onLogin(RakNetServer server, RakNetClientPeer peer) {
        try {
            Client client = new Client(peer);
            clients.add(client);
            client.peer.setTimeout(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer, String reason) {
        try {
            Optional<Client> cli = clients.stream().filter(client -> client.peer.equals(peer)).findAny();
            if(cli.isPresent()) {
                Client cl = cli.get();
                if (cl.email != null) {
                    Player player = StarterClass.db.getData(cl.email);
                    player.lastOnlineDate = new Date();
                    StarterClass.balancer.removePlayer(player, UnBalancedReason.CANCELLED);
                    StarterClass.db.update(player);
                }
                clients.remove(cl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleUnknownMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
        handleMessage(server, peer, packet, channel);
    }

    public static Packet newPacket(byte[] content){
        Packet packet = new Packet();
        packet.writeUnsignedByte(RakNetPacket.ID_USER_PACKET_ENUM);
        packet.write(content);
        return packet;
    }

    @Override
    public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
        try {
            byte[] data = packet.read(packet.remaining());
            Optional<Client> optionalClient = clients.stream().filter(client -> client.peer.equals(peer)).findAny();
            Client client;
            if(!optionalClient.isPresent())
                return;
            client = optionalClient.get();
            byte id = data[data.length - 1];
            if(id == PacketConstants.AES_KEY_PACKET){
                Packet packet2 = newPacket(client.secureData.makeDataSecureDefault(client.secureData.getKey()));
                client.secureData.setKey(client.secureData.getKey(), EncryptionAES.getRandomIV());
                client.peer.sendMessage(Reliability.RELIABLE_ORDERED, packet2);
            } else
            if(id < PacketConstants.INSECURE) {
                byte[] iv = new byte[16];
                byte[] newdata = new byte[data.length - 17];
                System.arraycopy(data, data.length - 17, iv, 0, 16);
                System.arraycopy(data, 0, newdata, 0, data.length - 17);
                client.secureData.aes.setIV(iv);
                switch (id) {
                    case PacketConstants.SIGN_UP_PACKET:
                        SignUpData signUpData = client.secureData.deserialize(client.secureData.aes.decrypt(newdata), SignUpData.class);
                        if (confCodes.get(signUpData.email).code.equals(signUpData.confirmationCode)) {
                            confCodes.remove(signUpData.email);
                            clients.removeIf(c -> c.email != null && c.email.equals(signUpData.email));
                            if (!StarterClass.db.keyExists(signUpData.email)) {
                                client.email = signUpData.email;
                                newdata = client.secureData.makeDataSecure(client.secureData.serialize(signUpData));
                                newdata[newdata.length - 1] = PacketConstants.SIGN_UP_SUCCESS;
                                client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(newdata));
                                Player player = new Player(signUpData.email, signUpData.password,
                                        new Date(), new Date(), "", 0, 0,
                                        new Tank[]{}, 0, 1, new String[] {});
                                StarterClass.db.insert(player);
                            }
                        } else {
                            byte[] f = client.secureData.serialize("Your confirmation code is wrong, maybe it is not valid(timeout of code is 5 minutes)");
                            client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(f, PacketConstants.SIGN_UP_ERROR)));
                        }
                        break;
                    case PacketConstants.SIGN_UP_PACKET_REQUEST:
                        SignInData signInData = client.secureData.deserialize(client.secureData.aes.decrypt(newdata), SignInData.class);
                        if (!StarterClass.db.keyExists(signInData.email)) {
                            if (VALID_EMAIL_ADDRESS_REGEX.matcher(signInData.email).find()) {
                                if (VALID_PASSWORD_REGEX.matcher(signInData.password).find() && signInData.password.matches("[a-zA-Z0-9]*")) {
                                    ConfCode code = new ConfCode(SecureData.getRandomKey(confCodes), System.currentTimeMillis());
                                    confCodes.put(signInData.email, code);
                                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(new byte[]{PacketConstants.SIGN_UP_OK}));
                                    EmailSender.sendConfirmationCode(signInData.email, code.code);
                                } else {
                                    byte[] f = client.secureData.serialize("Your password is incorrect, it must contains lower/upper case letters and digits.\nPassword length must be at least 8 characters");
                                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(f, PacketConstants.SIGN_UP_ERROR)));
                                }
                            } else {
                                byte[] f = client.secureData.serialize("Your email seems to be invalid, check it and try again");
                                client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(f, PacketConstants.SIGN_UP_ERROR)));
                            }
                        } else {
                            byte[] f = client.secureData.serialize("An account with this email is already exists");
                            client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(f, PacketConstants.SIGN_UP_ERROR)));
                        }
                        break;
                    case PacketConstants.SIGN_UP_PACKET_REMOVE_KEY:
                        SignInData signIn = client.secureData.deserialize(client.secureData.aes.decrypt(newdata), SignInData.class);
                        confCodes.remove(signIn.email);
                        break;
                    case PacketConstants.SIGN_IN_PACKET:
                        SignInData signInData2 = client.secureData.deserialize(client.secureData.aes.decrypt(newdata), SignInData.class);
                        String pass = StarterClass.db.keyExists(signInData2.email) ? StarterClass.db.getData(signInData2.email).password : null;
                        if (signInData2.password.equals(pass)) {
                            clients.removeIf(c -> c.email != null && c.email.equals(signInData2.email));
                            Player player = StarterClass.db.getData(signInData2.email);
                            client.email = signInData2.email;
                            client.nick = player.nickName;
                            newdata = client.secureData.makeDataSecure(client.secureData.serialize(signInData2));
                            newdata[newdata.length - 1] = !player.nickName.equals("") ? PacketConstants.SIGN_IN_SUCCESS : PacketConstants.SIGN_IN_SUCCESS_WITHOUT_NICK;
                            client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(newdata));
                            if(newdata[newdata.length - 1] == PacketConstants.SIGN_IN_SUCCESS && player.tanks.length == 0){
                                Chest starterChest = new Chest();
                                starterChest.generateRandomLoot(ChestName.STARTER, player);
                                starterChest.confirmToPlayer(player);
                                client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(starterChest), PacketConstants.GET_CHEST)));
                            }
                        } else {
                            byte[] f = client.secureData.serialize("Email or password are incorrect, try another password or email");
                            client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(f, PacketConstants.SIGN_UP_ERROR)));
                        }
                        break;
                    case PacketConstants.GET_PLAYER_PROFILE:
                        Player player = StarterClass.db.getDataByNickname(client.secureData.deserialize(client.secureData.aes.decrypt(newdata), String.class));
                        if(player != null){
                          newdata = client.secureData.makeDataSecure(client.secureData.serialize(player));
                          newdata[newdata.length - 1] = PacketConstants.GET_PLAYER_PROFILE;
                          client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(newdata));
                        }
                        break;
                }
            } else switch (id){
                case PacketConstants.GET_VERSION:
                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED,
                            newPacket(addId(client.secureData.serialize(StarterClass.VERSION), PacketConstants.GET_VERSION)));
                    break;
                case PacketConstants.FIRST_NICKNAME_REQUEST:
                    byte[] newdata = new byte[data.length - 1];
                    System.arraycopy(data, 0, newdata, 0, newdata.length);
                    String nick = client.secureData.deserialize(newdata, String.class);
                    if(nick.matches("[a-zA-Z0-9]*") && nick.length() > 2  && nick.length() < 16){
                        if(!StarterClass.db.nickExists(nick)) {
                            Player player = StarterClass.db.getData(client.email);
                            player.nickName = nick;
                            client.nick = nick;
                            StarterClass.db.update(player);
                            client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(new byte[]{PacketConstants.FIRST_NICKNAME_OK}));
                            if (player.tanks.length == 0) {
                                Chest starterChest = new Chest();
                                starterChest.generateRandomLoot(ChestName.STARTER, player);
                                starterChest.confirmToPlayer(player);
                                client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(starterChest), PacketConstants.GET_CHEST)));
                            }
                        } else {
                            byte[] f = client.secureData.serialize("Nickname is already exists");
                            client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(f, PacketConstants.FIRST_NICKNAME_ERROR)));
                        }
                    } else {
                        byte[] f = client.secureData.serialize("Nickname length must be in range from 3 to 15  and only contains English letters and digits");
                        client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(f, PacketConstants.FIRST_NICKNAME_ERROR)));
                    }
                    break;
                case PacketConstants.GET_FRIENDS:
                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED,
                            newPacket(addId(client.secureData.serialize(StarterClass.db.getData(client.email).friends), PacketConstants.GET_FRIENDS)));
                    break;
                case PacketConstants.GET_ONLINE:
                    newdata = new byte[data.length - 1];
                    System.arraycopy(data, 0, newdata, 0, newdata.length);
                    String[] nicknames = client.secureData.deserialize(newdata, String[].class);
                    boolean[] online = new boolean[nicknames.length];
                    for(int i = 0; i < online.length; i++) {
                        int finalI = i;
                        online[i] = clients.stream().anyMatch(c -> nicknames[finalI].equals(c.nick));
                    }
                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(online), PacketConstants.GET_ONLINE)));
                    break;
                case PacketConstants.GET_NICKNAME_SEARCH:
                    newdata = new byte[data.length - 1];
                    System.arraycopy(data, 0, newdata, 0, newdata.length);
                    String substring = client.secureData.deserialize(newdata, String.class);
                    Player player = StarterClass.db.getData(client.email);
                    String[] nicks = StarterClass.db.nickSelect(substring).stream().filter(c -> !c.nickName.equals(client.nick) && Arrays.stream(player.friends).noneMatch(y -> y.equals(c.nickName))).map(c -> c.nickName).toArray(String[]::new);
                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(nicks), PacketConstants.GET_NICKNAME_SEARCH)));
                    break;
                case PacketConstants.GET_NICKNAME:
                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(client.nick), PacketConstants.GET_NICKNAME)));
                    break;
                case PacketConstants.GET_DAILY_TIME:
                    player = StarterClass.db.getData(client.email);
                    long timeLeft = Math.max(twelveHours - (Instant.now().atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli() - player.dailyItemsTime.toInstant().atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()), 0);
                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(timeLeft), PacketConstants.GET_DAILY_TIME)));
                    break;
                case PacketConstants.GET_DAILY_ITEMS:
                    player = StarterClass.db.getData(client.email);
                    if(player.dailyItemsTime == null){
                        player.dailyItemsTime = new Date();
                    }
                    timeLeft = Math.max(twelveHours - (Instant.now().atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli() - player.dailyItemsTime.toInstant().atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli()), 0);
                    if(timeLeft == 0){
                        player.dailyItemsTime = new Date();
                        player.dailyTanks = Player.getDailyTanks(player);
                    }
                    Player.checkDailyItems(player);
                    StarterClass.db.update(player);
                    client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(player.dailyTanks), PacketConstants.GET_DAILY_ITEMS)));
                    break;
                case PacketConstants.BUY_DAILY_ITEM:
                    player = StarterClass.db.getData(client.email);
                    newdata = new byte[data.length - 1];
                    System.arraycopy(data, 0, newdata, 0, newdata.length);
                    DailyItem item = client.secureData.deserialize(newdata, DailyItem.class);
                    Optional<DailyItem> dailyItemOptional = Arrays.stream(player.dailyTanks).filter(c -> c.tankId == item.tankId).findAny();
                    if(dailyItemOptional.isPresent() && !dailyItemOptional.get().bought && player.coins - dailyItemOptional.get().price >= 0){
                        dailyItemOptional.get().bought = true;
                        player.coins -= dailyItemOptional.get().price;
                        Optional<Tank> tankOptional = Arrays.stream(player.tanks).filter(c -> c.id == dailyItemOptional.get().tankId).findAny();
                        if(tankOptional.isPresent()){
                            tankOptional.get().count += dailyItemOptional.get().count;
                        } else player.tanks = append(player.tanks, new Tank(dailyItemOptional.get().tankId, 1, 0));
                        StarterClass.db.update(player);
                        client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(new byte[]{PacketConstants.BUY_DAILY_ITEM_OK}));
                    } else client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(new byte[]{PacketConstants.BUY_DAILY_ITEM_ERROR}));
                    break;
                case PacketConstants.UPGRADE_CARD:
                    player = StarterClass.db.getData(client.email);
                    newdata = new byte[data.length - 1];
                    System.arraycopy(data, 0, newdata, 0, newdata.length);
                    int tankId = client.secureData.deserialize(client.secureData.makeDataUnSecure(newdata), Integer.class);
                    Optional<Tank> tankOptional = Arrays.stream(player.tanks).filter(c -> c.id == tankId).findAny();
                    if(tankOptional.isPresent() && player.coins >= upgradeCards[tankOptional.get().level - 1] && tankOptional.get().count >= cardFull[tankOptional.get().level - 1]){
                        tankOptional.get().count -= cardFull[tankOptional.get().level - 1];
                        player.coins -= upgradeCards[tankOptional.get().level - 1];
                        player.xp += tankOptional.get().level * 50;
                        if(player.xp >= player.rankLevel * 50){
                            player.xp -= player.rankLevel * 50;
                            player.rankLevel++;
                        }
                        tankOptional.get().level++;
                        StarterClass.db.update(player);
                        client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(new byte[]{PacketConstants.UPGRADE_CARD_OK}));
                    } else client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(new byte[]{PacketConstants.UPGRADE_CARD_ERROR}));
                    break;
                case PacketConstants.BUY_CHEST:
                    player = StarterClass.db.getData(client.email);
                    newdata = new byte[data.length - 1];
                    System.arraycopy(data, 0, newdata, 0, newdata.length);
                    ChestName chestName = client.secureData.deserialize(client.secureData.makeDataUnSecure(newdata), ChestName.class);
                    int price = chestName.value;
                    if(price == 0)
                        return;
                    if(player.coins - price >= 0){
                        Chest chest = new Chest();
                        chest.generateRandomLoot(chestName, player);
                        chest.confirmToPlayer(player, true);
                        player.coins -= price;
                        StarterClass.db.update(player);
                        client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(chest), PacketConstants.BUY_CHEST_ERROR)));
                    } else client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.serialize(null), PacketConstants.BUY_CHEST_ERROR)));
                    break;
                case PacketConstants.JOIN_BALANCER:
                    player = StarterClass.db.getData(client.email);
                    newdata = client.secureData.makeDataUnSecure(data);
                    StarterClass.balancer.addPlayer(player, client, client.secureData.deserialize(newdata, Integer.class), new Balanced(){
                        @Override
                        public void balanceEnds(UnBalancedReason reason, Player caller) {
                            Client client =  clients.stream().filter(c -> c.email.equals(caller.email)).findAny().get();
                            try {
                                client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.makeDataSecure(client.secureData.serialize(reason)),PacketConstants.EXIT_BALANCER)));
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void mapFound(MapPacket mapPacket, BalancedPlayer caller) {
                            caller.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(caller.client.secureData.serialize(mapPacket), PacketConstants.MAP_FOUND)));
                        }

                        @Override
                        public void battleEnds(BalancedPlayer winner, BalancedPlayer looser, BattleResults winnerResults, BattleResults looserResults) {
                            Client winnerClient = winner.client;
                            Client looserClient = looser.client;
                            winnerClient.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(winnerClient.secureData.serialize(winnerResults), PacketConstants.BATTLE_ENDS)));
                            looserClient.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(looserClient.secureData.serialize(looserResults), PacketConstants.BATTLE_ENDS)));
                        }

                        @Override
                        public void battleStarted(long waitTime, BalancedPlayer caller) {
                            caller.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(caller.client.secureData.serialize(waitTime), PacketConstants.BATTLE_STARTED)));
                        }

                        @Override
                        public void packetForPlayer(PlayerDataPacket dataPacket, BalancedPlayer caller) {
                            if (caller.client != null) {
                                Client client = caller.client;
                                client.peer.sendMessage(Reliability.UNRELIABLE_SEQUENCED, newPacket(addId(client.secureData.serialize(dataPacket), PacketConstants.BATTLE_DATA)));
                            }
                        }
                    });
                    break;
                case PacketConstants.ROTATION_PACKET:
                    newdata = new byte[data.length - 1];
                    System.arraycopy(data, 0, newdata, 0, newdata.length);
                    RotationPacket rotationPacket = client.secureData.deserialize(newdata, RotationPacket.class);
                    StarterClass.balancer.manager.rotationPacketArrived(client.nick, rotationPacket);
                    break;
                case PacketConstants.SHOOT_PACKET:
                    StarterClass.balancer.manager.shootPacketArrived(client.nick);
                    break;
                case PacketConstants.MAP_READY:
                    StarterClass.balancer.manager.playerReady(client.email);
                    break;
                case PacketConstants.EXIT_BALANCER:
                    player = StarterClass.db.getData(client.email);
                    StarterClass.balancer.removePlayer(player, UnBalancedReason.CANCELLED);
                    break;
                case PacketConstants.BATTLE_EXISTS:
                    StarterClass.balancer.manager.playerConnect(client, new Balanced(){
                        @Override
                        public void balanceEnds(UnBalancedReason reason, Player caller) {
                            Client client =  clients.stream().filter(c -> c.email.equals(caller.email)).findAny().get();
                            try {
                                client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(client.secureData.makeDataSecure(client.secureData.serialize(reason)),PacketConstants.EXIT_BALANCER)));
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void mapFound(MapPacket mapPacket, BalancedPlayer caller) {
                            caller.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(caller.client.secureData.serialize(mapPacket), PacketConstants.MAP_FOUND)));
                        }

                        @Override
                        public void battleEnds(BalancedPlayer winner, BalancedPlayer looser, BattleResults winnerResults, BattleResults looserResults) {
                            Client winnerClient = winner.client;
                            Client looserClient = looser.client;
                            winnerClient.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(winnerClient.secureData.serialize(winnerResults), PacketConstants.BATTLE_ENDS)));
                            looserClient.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(looserClient.secureData.serialize(looserResults), PacketConstants.BATTLE_ENDS)));
                        }

                        @Override
                        public void battleStarted(long waitTime, BalancedPlayer caller) {
                            caller.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, newPacket(addId(caller.client.secureData.serialize(waitTime), PacketConstants.BATTLE_STARTED)));
                        }

                        @Override
                        public void packetForPlayer(PlayerDataPacket dataPacket, BalancedPlayer caller) {
                            if (caller.client != null) {
                                Client client = caller.client;
                                client.peer.sendMessage(Reliability.UNRELIABLE_SEQUENCED, newPacket(addId(client.secureData.serialize(dataPacket), PacketConstants.BATTLE_DATA)));
                            }
                        }
                    });
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] addId(byte[] arr, byte id){
        byte[] result = new byte[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, arr.length);
        result[result.length - 1] = id;
        return result;
    }

    static <T> T[] append(T[] arr, T element) {
        final int N = arr.length;
        arr = Arrays.copyOf(arr, N + 1);
        arr[N] = element;
        return arr;
    }
}
