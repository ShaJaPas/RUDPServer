package main.player.balance;

import main.StarterClass;
import main.network.Client;
import main.player.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

public class BalancedPlayer extends BalancedPlayerAbstract implements Comparable<BalancedPlayer> {
    private final float efficiency;

    public BalancedPlayer(Player player, Client client, int tankId){
        efficiency = player.getEfficiency();
        info = StarterClass.tanks.stream().filter(c -> c.id == tankId).findAny().get();
        tank = Arrays.stream(player.tanks).filter(c -> c.id == tankId).findAny().get();
        hp = (int)(info.characteristics.hp + info.characteristics.hp * 0.25f * (tank.level - 1));
        damage = (int)(info.characteristics.damage + info.characteristics.damage * 0.2f * (tank.level - 1));
        this.reloadingLeft = (int) (info.characteristics.reloading * 1000);
        this.player = player;
        this.client = client;
        this.joinTime = Instant.now().atZone(ZoneId.of("Europe/Moscow")).toInstant();
    }

    @Override
    public int skillPoints() {
        return player.trophies;
    }

    @Override
    public int compareTo(BalancedPlayer o) {
        return joinTime.compareTo(o.joinTime);
    }
}
