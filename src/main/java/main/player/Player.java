package main.player;

import main.StarterClass;
import main.player.chest.DailyItem;
import main.tools.chance.ChanceTaker;
import org.msgpack.annotation.Ignore;
import org.msgpack.annotation.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "players")
public class Player implements Serializable {

    @Id
    @Column(name = "email", unique=true, columnDefinition="VARCHAR(64)")
    @Ignore
    public String email;

    @Ignore
    public String password;

    @Index(0)
    public Date registrationDate;

    @Index(1)
    public Date lastOnlineDate;

    @Index(2)
    public String nickName;

    @Index(3)
    public int battleCount;

    @Index(4)
    public int victoriesCount;

    @Index(5)
    public Tank[] tanks;

    @Index(6)
    public int xp;

    @Index(7)
    public int rankLevel;

    @Index(8)
    public int coins;

    @Index(9)
    public int diamonds;

    @Ignore
    public Date dailyItemsTime;

    @Ignore
    public DailyItem[] dailyTanks;

    @Ignore
    public String[] friends;

    @Index(10)
    public float accuracy;

    @Index(11)
    public int damageDealt;

    @Index(12)
    public int damageTaken;

    @Index(13)
    public int trophies;

    public Player(String email, String password, Date registrationDate, Date lastOnlineDate, String nickName, int battleCount, int victoriesCount, Tank[] tanks, int xp, int rankLevel, String[] friends) throws Exception {
        this.email = email;
        this.password = password;
        this.registrationDate = registrationDate;
        this.lastOnlineDate = lastOnlineDate;
        this.nickName = nickName;
        this.battleCount = battleCount;
        this.victoriesCount = victoriesCount;
        this.tanks = tanks;
        this.xp = xp;
        this.rankLevel = rankLevel;
        this.friends = friends;
        this.dailyItemsTime = new Date();
        this.dailyTanks = getDailyTanks(this);
    }

    public static DailyItem[] getDailyTanks(Player player) throws Exception {
        DailyItem[] dailyItems = new DailyItem[4];

        int[] dailyTanks = new int[4];
        TanksInfoChanced[] infoChanced = StarterClass.tanks.stream().map(c -> new TanksInfoChanced(c, c.characteristics.rarity.chance)).toArray(TanksInfoChanced[]::new);
        dailyTanks[0] = ChanceTaker.getElementFromArray(Arrays.stream(infoChanced).filter(c -> c.info.characteristics.rarity == TankRarity.COMMON).toArray(TanksInfoChanced[]::new)).info.id;
        dailyTanks[1] = ChanceTaker.getElementFromArray(Arrays.stream(infoChanced).filter(c -> c.info.characteristics.rarity == TankRarity.RARE).toArray(TanksInfoChanced[]::new)).info.id;
        dailyTanks[2] = ChanceTaker.getElementFromArray(Arrays.stream(infoChanced).filter(c -> c.info.characteristics.rarity == TankRarity.EPIC).toArray(TanksInfoChanced[]::new)).info.id;
        dailyTanks[3] = ChanceTaker.getElementFromArray(Arrays.stream(infoChanced).filter(c -> c.info.characteristics.rarity == TankRarity.MYTHICAL).toArray(TanksInfoChanced[]::new)).info.id;
        boolean a = Arrays.stream(player.tanks).anyMatch(c -> c.id == dailyTanks[0]);
        dailyItems[0] = a ? new DailyItem(ThreadLocalRandom.current().nextInt(40, 50), dailyTanks[0], ThreadLocalRandom.current().nextInt(40, 50)) :
                new DailyItem(ThreadLocalRandom.current().nextInt(80, 100), dailyTanks[0], 0);
        a = Arrays.stream(player.tanks).anyMatch(c -> c.id == dailyTanks[1]);
        dailyItems[1] = a ? new DailyItem(ThreadLocalRandom.current().nextInt(40, 50), dailyTanks[1], ThreadLocalRandom.current().nextInt(40, 50)) :
                new DailyItem(ThreadLocalRandom.current().nextInt(160, 200), dailyTanks[1], 0);
        a = Arrays.stream(player.tanks).anyMatch(c -> c.id == dailyTanks[2]);
        dailyItems[2] = a ? new DailyItem(ThreadLocalRandom.current().nextInt(40, 50), dailyTanks[2], ThreadLocalRandom.current().nextInt(40, 50)) :
                new DailyItem(ThreadLocalRandom.current().nextInt(300, 360), dailyTanks[2], 0);
        a = Arrays.stream(player.tanks).anyMatch(c -> c.id == dailyTanks[3]);
        dailyItems[3] = a ? new DailyItem(ThreadLocalRandom.current().nextInt(40, 50), dailyTanks[3], ThreadLocalRandom.current().nextInt(40, 50)) :
                new DailyItem(ThreadLocalRandom.current().nextInt(500, 600), dailyTanks[3], 0);
        return dailyItems;
    }

    public Player(){

    }

    public static void checkDailyItems(Player player){
        for (int i = 0; i < player.dailyTanks.length; i++) {
            int finalI = i;
            if(!player.dailyTanks[i].bought && Arrays.stream(player.tanks).anyMatch(c -> c.id == player.dailyTanks[finalI].tankId) && player.dailyTanks[finalI].count == 0)
                player.dailyTanks[i] = new DailyItem(ThreadLocalRandom.current().nextInt(40, 50), player.dailyTanks[i].tankId, ThreadLocalRandom.current().nextInt(40, 50));
        }
    }

    public float getEfficiency(){
        float res = (float) victoriesCount / battleCount * (accuracy + 0.5f) * ((float) damageDealt / damageTaken);
        return Float.isNaN(res) ? 0 : res;
    }
}
