package com.github.standobyte.jrpg.network.packets.server;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.network.NetworkUtil;
import com.github.standobyte.jrpg.network.packets.IModPacketHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.fml.network.NetworkEvent;

public class TurnBasedEffectsPacket {
    private final int entityId;
    private PacketType packetType;
    
    private Collection<EffectInstance> addEffects;
    private Effect removeEffect;
    
    public static TurnBasedEffectsPacket add(int entityId, EffectInstance effectInstance) {
        TurnBasedEffectsPacket packet = new TurnBasedEffectsPacket(entityId, PacketType.ADD);
        packet.addEffects = Collections.singletonList(effectInstance);
        
        return packet;
    }
    
    public static TurnBasedEffectsPacket remove(int entityId, Effect effect) {
        TurnBasedEffectsPacket packet = new TurnBasedEffectsPacket(entityId, PacketType.REMOVE);
        packet.removeEffect = effect;
        
        return packet;
        
    }
    
    public static TurnBasedEffectsPacket addMupltile(int entityId, Collection<EffectInstance> effects) {
        TurnBasedEffectsPacket packet = new TurnBasedEffectsPacket(entityId, PacketType.ADD_MULTIPLE);
        packet.addEffects = effects;
        
        return packet;
        
    }
    
    private TurnBasedEffectsPacket(int entityId, PacketType packetType) {
        this.entityId = entityId;
        this.packetType = packetType;
    }
    
    
    public static class Handler implements IModPacketHandler<TurnBasedEffectsPacket> {

        @Override
        public void encode(TurnBasedEffectsPacket msg, PacketBuffer buf) {
            buf.writeInt(msg.entityId);
            buf.writeEnum(msg.packetType);
            switch (msg.packetType) {
            case ADD:
                NetworkUtil.writeEffectInstance(buf, msg.addEffects.iterator().next());
                break;
            case ADD_MULTIPLE:
                NetworkUtil.writeCollection(buf, msg.addEffects, eff -> NetworkUtil.writeEffectInstance(buf, eff), false);
                break;
            case REMOVE:
                buf.writeByte(Effect.getId(msg.removeEffect));
                break;
            }
        }

        @Override
        public TurnBasedEffectsPacket decode(PacketBuffer buf) {
            int entityId = buf.readInt();
            PacketType packetType = buf.readEnum(PacketType.class);
            switch (packetType) {
            case ADD:
                return add(entityId, NetworkUtil.readEffectInstance(buf));
            case ADD_MULTIPLE:
                return addMupltile(entityId, NetworkUtil.readCollection(buf, () -> NetworkUtil.readEffectInstance(buf)));
            case REMOVE:
                return remove(entityId, Effect.byId(buf.readUnsignedByte()));
            default:
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void handle(TurnBasedEffectsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            Entity entity = ClientUtil.getEntityById(msg.entityId);
            if (entity instanceof LivingEntity) {
                EntityCombat.get((LivingEntity) entity).ifPresent(effects -> {
                    switch (msg.packetType) {
                    case ADD:
                        effects.addEffect(msg.addEffects.iterator().next());
                        break;
                    case REMOVE:
                        effects.removeEffect(msg.removeEffect);
                        break;
                    case ADD_MULTIPLE:
                        msg.addEffects.forEach(effects::addEffect);
                        break;
                    }
                });
            }
        }

        @Override
        public Class<TurnBasedEffectsPacket> getPacketClass() {
            return TurnBasedEffectsPacket.class;
        }
    }
    
    private enum PacketType {
        ADD,
        ADD_MULTIPLE,
        REMOVE
    }
}
