package main.tools.physics;

import com.badlogic.gdx.physics.box2d.*;
import com.whirvis.jraknet.protocol.Reliability;
import main.network.BaseRakNetServerListener;
import main.network.PacketConstants;
import main.player.balance.BalancedPlayer;
import main.player.gameManager.Bullet;
import main.player.gameManager.ExplosionPacket;
import main.player.gameManager.GameWorld;


public class MyContactListener implements ContactListener {

    public MyContactListener(){
    }

    @Override
    public void endContact(Contact contact) {

    }

    @Override
    public void beginContact(Contact contact) {

    }

    private byte[] addId(byte[] arr, byte id){
        byte[] result = new byte[arr.length + 1];
        System.arraycopy(arr, 0, result, 0, arr.length);
        result[result.length - 1] = id;
        return result;
    }

    @Override
    public void preSolve (Contact contact, Manifold oldManifold){
        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();
        if(fa.getBody().getUserData() instanceof Bullet || fb.getBody().getUserData() instanceof Bullet){
            if(fb.getBody().getUserData() instanceof Bullet){
                Fixture a = fa;
                fa = fb;
                fb = a;
            }
            Bullet bullet = (Bullet) fa.getBody().getUserData();
            if(bullet.destroyed) {
                contact.setEnabled(false);
                return;
            }
            if(fb.getBody().getUserData() instanceof BalancedPlayer && ((BalancedPlayer)fb.getBody().getUserData()).client.email.equals(bullet.player.client.email)) {
                contact.setEnabled(false);
            } else {
                ExplosionPacket packet = new ExplosionPacket();
                packet.x = fa.getBody().getPosition().scl(1 / GameWorld.SCALE).x;
                packet.y = fa.getBody().getPosition().scl(1 / GameWorld.SCALE).y;
                packet.hit = false;
                bullet.destroyed = true;
                if(fb.getBody().getUserData() instanceof BalancedPlayer){
                    packet.hit = true;
                    if(bullet.foe.hp - bullet.player.damage > 0){
                        bullet.foe.hp -= bullet.player.damage;
                        bullet.foe.damageTaken += bullet.player.damage;
                        bullet.player.damageDealt += bullet.player.damage;
                    } else {
                        bullet.foe.damageTaken += bullet.foe.hp;
                        bullet.player.damageDealt += bullet.foe.hp;
                        bullet.foe.hp = 0;
                    }
                    bullet.player.succeededShots++;
                }
                bullet.player.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, BaseRakNetServerListener.newPacket(addId(bullet.player.client.secureData.serialize(packet), PacketConstants.BULLET_EXPLOSION)));
                bullet.foe.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, BaseRakNetServerListener.newPacket(addId(bullet.player.client.secureData.serialize(packet), PacketConstants.BULLET_EXPLOSION)));
            }
            if(fb.getBody().getUserData() instanceof Bullet){
                Bullet bullet2 = (Bullet) fb.getBody().getUserData();
                ExplosionPacket packet2 = new ExplosionPacket();
                packet2.x = fb.getBody().getPosition().scl(1 / GameWorld.SCALE).x;
                packet2.y = fb.getBody().getPosition().scl(1 / GameWorld.SCALE).y;
                packet2.hit = false;
                bullet2.player.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, BaseRakNetServerListener.newPacket(addId(bullet2.player.client.secureData.serialize(packet2), PacketConstants.BULLET_EXPLOSION)));
                bullet2.foe.client.peer.sendMessage(Reliability.RELIABLE_ORDERED, BaseRakNetServerListener.newPacket(addId(bullet2.player.client.secureData.serialize(packet2), PacketConstants.BULLET_EXPLOSION)));
                bullet2.destroyed = true;
            }
        }
    }

    @Override
    public void postSolve (Contact contact, ContactImpulse impulse){
    }
}
