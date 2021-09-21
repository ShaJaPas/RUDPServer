package main.player.chest;

import main.StarterClass;
import main.player.Player;
import main.player.Tank;
import main.player.TankRarity;
import main.player.TanksInfoChanced;
import main.tools.chance.ChanceTaker;
import org.msgpack.annotation.Index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Chest {
    @Index(0)
    public ChestName name;
    @Index(1)
    public Tank[] loot;
    @Index(2)
    public int coins;
    @Index(3)
    public int diamonds;

    public Chest(){
        name = ChestName.STARTER;
        loot = null;
    }

    public void generateRandomLoot(ChestName name, Player player) throws Exception {
        switch (name){
            case COMMON:
                this.name = name;
                this.coins = ThreadLocalRandom.current().nextInt(20, 41);
                this.diamonds = ThreadLocalRandom.current().nextInt(0, 4);
                List<Tank> lootArr = new ArrayList<>();
                TanksInfoChanced[] infoChanced = StarterClass.tanks.stream().map(c -> new TanksInfoChanced(c, Arrays.stream(player.tanks).anyMatch(b -> b.id == c.id) ? 70 : c.characteristics.rarity.chance)).toArray(TanksInfoChanced[]::new);
                for (int i = 0; i < ThreadLocalRandom.current().nextInt(2, 5); i++) {
                    TanksInfoChanced info = ChanceTaker.getElementFromArray(infoChanced);
                    lootArr.add(new Tank(info.info.id, -1, info.chance == 70 ? ThreadLocalRandom.current().nextInt(30, 51) : 0));
                    infoChanced = Arrays.stream(infoChanced).filter(c -> !c.equals(info)).toArray(TanksInfoChanced[]::new);
                }
                loot = new Tank[lootArr.size()];
                lootArr.sort((a, b) -> a.count < b.count ? (a.count == b.count ? 0 : 1) : -1);
                lootArr.toArray(loot);
                break;
            case STARTER:
                this.name = name;
                this.coins = ThreadLocalRandom.current().nextInt(40, 61);
                this.diamonds = ThreadLocalRandom.current().nextInt(2, 6);
                lootArr = new ArrayList<>();
                infoChanced = StarterClass.tanks.stream().map(c -> new TanksInfoChanced(c, Arrays.stream(player.tanks).anyMatch(b -> b.id == c.id) ? 70 : c.characteristics.rarity != TankRarity.COMMON ? c.characteristics.rarity.chance * 2 : c.characteristics.rarity.chance)).toArray(TanksInfoChanced[]::new);
                for (int i = 0; i < ThreadLocalRandom.current().nextInt(1, 3); i++) {
                    TanksInfoChanced info = ChanceTaker.getElementFromArray(infoChanced);
                    lootArr.add(new Tank(info.info.id, -1, info.chance == 70 ? ThreadLocalRandom.current().nextInt(5, 8) : 0));
                    infoChanced = Arrays.stream(infoChanced).filter(c -> !c.equals(info)).toArray(TanksInfoChanced[]::new);
                }
                loot = new Tank[lootArr.size()];
                lootArr.sort((a, b) -> a.count < b.count ? (a.count == b.count ? 0 : 1) : -1);
                lootArr.toArray(loot);
                break;
        }
    }

    public void confirmToPlayer(Player player, boolean... NotsaveToDB){
        player.coins += coins;
        player.diamonds += diamonds;
        for (int i = 0; i < loot.length; i++) {
            int finalI = i;
            Supplier<Stream<Tank>> stream = () -> Arrays.stream(player.tanks).filter(c -> c.id == loot[finalI].id);
            if(stream.get().count() > 0){
                stream.get().findAny().get().count += loot[finalI].count;
            } else player.tanks = append(player.tanks, new Tank(loot[finalI].id, 1, loot[finalI].count));
        }
        if(NotsaveToDB.length == 0)
            StarterClass.db.update(player);
    }

    static <T> T[] append(T[] arr, T element) {
        final int N = arr.length;
        arr = Arrays.copyOf(arr, N + 1);
        arr[N] = element;
        return arr;
    }
}
