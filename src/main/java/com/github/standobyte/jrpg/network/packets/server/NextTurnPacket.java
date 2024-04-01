package com.github.standobyte.jrpg.network.packets.server;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.client.battleui.BattleScreen;
import com.github.standobyte.jrpg.client.battleui.range.MovementRangeCircle;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.Moveset;
import com.github.standobyte.jrpg.network.NetworkUtil;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class NextTurnPacket {
    private final int battleId;
    private final int currentTurnEntity;
    private final boolean isControlling;
    private PacketBuffer bufRemaining;
    private Optional<Moveset.Category> movesetRootCategory;
    
    public static NextTurnPacket syncMoveset(int battleId, int currentTurnEntity, boolean isControlling, 
            Optional<Moveset> clientControlled) {
        return NextTurnPacket.encoding(battleId, currentTurnEntity, isControlling, clientControlled.map(moveset -> moveset.rootCategory));
    }
    
    private static NextTurnPacket encoding(int battleId, int currentTurnEntity, boolean isControlling, 
            Optional<Moveset.Category> clientControlled) {
        NextTurnPacket packet = new NextTurnPacket(battleId, currentTurnEntity, isControlling);
        packet.movesetRootCategory = clientControlled;
        return packet;
    }
    
    private static NextTurnPacket decoding(int battleId, int currentTurnEntity, boolean isControlling, 
            PacketBuffer lazy) {
        NextTurnPacket packet = new NextTurnPacket(battleId, currentTurnEntity, isControlling);
        packet.bufRemaining = lazy;
        return packet;
    }
    
    private NextTurnPacket(int battleId, int currentTurnEntity, boolean isControlling) {
        this.battleId = battleId;
        this.currentTurnEntity = currentTurnEntity;
        this.isControlling = isControlling;
    }
    
    
    public static class Handler implements IModPacketHandler<NextTurnPacket> {

        @Override
        public void encode(NextTurnPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.battleId);
            buf.writeInt(msg.currentTurnEntity);
            buf.writeBoolean(msg.isControlling);
            NetworkUtil.writeOptional(buf, msg.movesetRootCategory, moveset -> moveset.toBuf(buf));
        }

        @Override
        public NextTurnPacket decode(PacketBuffer buf) {
            int battleId = buf.readInt();
            int currentTurnEntity = buf.readInt();
            boolean isControlling = buf.readBoolean();
            return NextTurnPacket.decoding(battleId, currentTurnEntity, isControlling, buf);
        }

        @Override
        public void handle(NextTurnPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Entity entity = ClientUtil.getEntityById(msg.currentTurnEntity);
            
            Battle battle = ClientStuff.getCurrentBattle();
            if (battle != null && battle.battleId == msg.battleId) {
                if (entity instanceof LivingEntity) {
                    LivingEntity currentEntity = (LivingEntity) entity;
                    ClientStuff.getCurrentBattle().clSetCurrentTurnEntity(currentEntity);
                    if (BattleScreen.instance != null) {
                        BattleScreen.instance.setCameraEntity(entity);
                        msg.movesetRootCategory = NetworkUtil.readOptional(msg.bufRemaining, 
                                () -> Moveset.Category.fromBuf(msg.bufRemaining, currentEntity, battle));
                        BattleScreen.instance.setMoveset(msg.movesetRootCategory.map(root -> new Moveset(
                                root, currentEntity, ClientStuff.getCurrentBattle())));
                    }
                }
            }
            
            MovementRangeCircle.remove();
            if (msg.isControlling && entity instanceof LivingEntity) {
                ClientStuff.movementRange = new MovementRangeCircle(entity.level, (LivingEntity) entity);
            }
            
            ClientStuff.allowedItem = null;
        }

        @Override
        public Class<NextTurnPacket> getPacketClass() {
            return NextTurnPacket.class;
        }
    }
}
