package com.github.standobyte.jrpg.network.packets.server;

import java.util.List;
import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.network.NetworkUtil;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class BattleStartPacket {
    private final int battleId;
    private final int[] team1ids;
    private final int[] team2ids;
    private final boolean updateIds;
    
    public BattleStartPacket(int battleId, List<? extends Entity> team1, List<? extends Entity> team2, boolean updateOngoingBattleIds) {
        this.battleId = battleId;
        this.team1ids = new int[team1.size()];
        for (int i = 0; i < team1.size(); i++) {
            team1ids[i] = team1.get(i).getId();
        }
        
        this.team2ids = new int[team2.size()];
        for (int i = 0; i < team2.size(); i++) {
            team2ids[i] = team2.get(i).getId();
        }
        this.updateIds = updateOngoingBattleIds;
    }
    
    public BattleStartPacket(int battleId, int[] team1ids, int[] team2ids, boolean updateIds) {
        this.battleId = battleId;
        this.team1ids = team1ids;
        this.team2ids = team2ids;
        this.updateIds = updateIds;
    }
    
    
    public static class Handler implements IModPacketHandler<BattleStartPacket> {

        @Override
        public void encode(BattleStartPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.battleId);
            buf.writeBoolean(msg.updateIds);
            NetworkUtil.writeIntArray(buf, msg.team1ids);
            NetworkUtil.writeIntArray(buf, msg.team2ids);
        }

        @Override
        public BattleStartPacket decode(PacketBuffer buf) {
            int battleId = buf.readInt();
            boolean updateIds = buf.readBoolean();
            int[] team1ids = NetworkUtil.readIntArray(buf);
            int[] team2ids = NetworkUtil.readIntArray(buf);
            return new BattleStartPacket(battleId, team1ids, team2ids, updateIds);
        }

        @Override
        public void handle(BattleStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
            if (msg.updateIds) {
                Battle battle = ClientStuff.getCurrentBattle();
                if (battle != null && battle.battleId == msg.battleId) {
                    for (int id : msg.team1ids) {
                        Entity entity = ClientUtil.getEntityById(id);
                        if (entity instanceof LivingEntity) {
                            battle.addToTeam1((LivingEntity) entity);
                        }
                    }
                    for (int id : msg.team2ids) {
                        Entity entity = ClientUtil.getEntityById(id);
                        if (entity instanceof LivingEntity) {
                            battle.addToTeam2((LivingEntity) entity);
                        }
                    }
                }
            }
            else {
                Battle battle = Battle.createBattleClientSide(ClientUtil.getClientWorld(), msg.battleId);
                ClientStuff.setBattle(battle);
                for (int id : msg.team1ids) {
                    Entity entity = ClientUtil.getEntityById(id);
                    if (entity instanceof LivingEntity) {
                        battle.addToTeam1((LivingEntity) entity);
                    }
                }
                for (int id : msg.team2ids) {
                    Entity entity = ClientUtil.getEntityById(id);
                    if (entity instanceof LivingEntity) {
                        battle.addToTeam2((LivingEntity) entity);
                    }
                }
                
                battle.startBattle();
            }
        }

        @Override
        public Class<BattleStartPacket> getPacketClass() {
            return BattleStartPacket.class;
        }
    }
}
