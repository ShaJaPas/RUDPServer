package main.player.balance;

import main.player.Player;
import main.player.gameManager.BattleResults;
import main.player.gameManager.MapPacket;
import main.player.gameManager.PlayerDataPacket;

public interface Balanced {
    void balanceEnds(UnBalancedReason reason, Player caller);
    void mapFound(MapPacket mapPacket, BalancedPlayer caller);
    void battleEnds(BalancedPlayer winner, BalancedPlayer looser, BattleResults winnerResults, BattleResults looserResults);
    void battleStarted(long waitTime, BalancedPlayer caller);
    void packetForPlayer(PlayerDataPacket dataPacket, BalancedPlayer caller);
}
