package main.player.balance;

import main.network.Client;
import main.player.Player;
import main.player.gameManager.GameManager;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Balancer {
    private final List<BalancedPlayer> players;
    private volatile boolean stopped = false;
    public static final long TIMEOUT = 1000 * 60;
    public static final int DIFFERENCE = 600;
    public Map<String, Balanced> listeners;
    public GameManager manager;

    public Balancer() throws IOException {
        players = new CopyOnWriteArrayList<>();
        listeners = new ConcurrentHashMap<>();
        manager = new GameManager();
    }

    public void start(){
        new Thread(() -> {
            while (!stopped){
                players.sort(BalancedPlayer::compareTo);
                for (int i = 0; i < players.size(); i++) {
                    BalancedPlayer player1 = players.get(i);
                    int bestIndex = -1;
                    int bestDifference = DIFFERENCE + 1;
                    for (int j = 0; j < players.size(); j++) {
                        if(j != i){
                            BalancedPlayer player2 = players.get(j);
                            int diff = Math.abs(player1.skillPoints() - player2.skillPoints());
                            if(diff <= DIFFERENCE && Math.min(diff, bestDifference) == diff){
                                bestDifference = diff;
                                bestIndex = j;
                            }
                        }
                    }
                    long time = Instant.now().atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli() - player1.joinTime.toEpochMilli();
                    if(bestIndex != -1){
                        BalancedPlayer player2 = players.get(bestIndex);
                        removePlayer(player1.player, UnBalancedReason.SUCCESS);
                        removePlayer(player2.player, UnBalancedReason.SUCCESS);
                        manager.addPlayersToMap(player1, player2, listeners.get(player1.player.email), listeners.get(player2.player.email));
                    } else if(time > TIMEOUT) {
                        removePlayer(player1.player, UnBalancedReason.TIMEOUT);
                    }
                }
            }
        }).start();
        manager.start();
        System.out.println("Balancer started");
    }

    public void stop(){
        stopped = true;
        manager.stop();
        System.out.println("Balancer stopped");
    }

    public void addPlayer(Player player, Client client, int tankId, Balanced... listener){
        if(Arrays.stream(player.tanks).anyMatch(c -> c.id == tankId)) {
            players.removeIf(c -> c.player.email.equals(player.email));
            players.add(new BalancedPlayer(player, client, tankId));
            if (listener.length > 0)
                listeners.put(player.email, listener[0]);
        } else if (listener.length > 0)
            listener[0].balanceEnds(UnBalancedReason.TIMEOUT, player);
    }


    public void removePlayer(Player player, UnBalancedReason reason){
        players.removeIf(c -> c.player.email.equals(player.email));
        Balanced balanced = listeners.get(player.email);
        if(balanced != null) {
            balanced.balanceEnds(reason, player);
            listeners.remove(balanced);
        }
    }
}
