package com.github.standobyte.jrpg.party;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;

import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.network.NetworkUtil;
import com.github.standobyte.jrpg.network.packets.server.PartyPacket;
import com.github.standobyte.jrpg.network.packets.server.UIMessagePacket;
import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;

public class Party {
    private Map<UUID, LivingEntity> partyMembers = new LinkedHashMap<>();
    private UUID partyLeader;
    private Map<UUID, LivingEntity> activeMembers = new LinkedHashMap<>();
    
    public Party(LivingEntity leader) {
        if (leader != null) {
            this.partyLeader = leader.getUUID();
            partyMembers.put(partyLeader, leader);
            activeMembers.put(partyLeader, leader);
            doSetActive(leader, true);
        }
    }
    
    Party(UUID loaderId) {
        this.partyLeader = loaderId;
        partyMembers.put(partyLeader, null);
        activeMembers.put(partyLeader, null);
    }
    
    public Iterable<LivingEntity> getActiveMembers() {
        return activeMembers.values();
    }
    
    public void serverTick(MinecraftServer server) {
        if (updatePartyEntities(server)) {
            syncParty(server, false);
        }
    }
    
    public void forEachPlayer(Consumer<ServerPlayerEntity> action) {
        for (LivingEntity member : partyMembers.values()) {
            if (member instanceof ServerPlayerEntity) {
                action.accept((ServerPlayerEntity) member);
            }
        }
    }
    
    private void sendToEachPlayer(Object packet) {
        forEachPlayer(player -> ModNetworkManager.sendToClient(packet, player));
    }
    
    private boolean updatePartyEntities(MinecraftServer server) {
        boolean updated = false;
        for (Map.Entry<UUID, LivingEntity> member : partyMembers.entrySet()) {
            LivingEntity entity = member.getValue();
            if (entity != null && entity.removed) {
                updateMemberParty(entity, false);
                entity = null;
                member.setValue(null);
                if (activeMembers.containsKey(member.getKey())) {
                    activeMembers.put(member.getKey(), null);
                }
            }
            
            if (entity == null) {
                Entity e = null;
                for (ServerWorld level : server.getAllLevels()) {
                    e = level.getEntity(member.getKey());
                    if (e instanceof LivingEntity) {
                        break;
                    }
                }
                if (e instanceof LivingEntity) {
                    entity = (LivingEntity) e;
                    member.setValue(entity);
                    if (activeMembers.containsKey(member.getKey())) {
                        activeMembers.put(member.getKey(), entity);
                        updateMemberParty(entity, true);
                    }
                    updated = true;
                }
            }
        }
        
        return updated;
    }
    
    private void updateMemberParty(LivingEntity member, boolean setActive) {
        EntityRPGData.get(member).ifPresent(entityData -> {
            entityData.setParty(setActive ? this : null);
        });
    }
    
    public void syncParty(MinecraftServer server, boolean updateEntityRefs) {
        if (updateEntityRefs) updatePartyEntities(server);
        sendToEachPlayer(new PartyPacket(this));
    }
    
    public void addNewMember(LivingEntity member) {
        if (!partyMembers.containsKey(member.getUUID())) {
            partyMembers.put(member.getUUID(), member);
            if (!member.level.isClientSide()) {
                sendToEachPlayer(new UIMessagePacket(new TranslationTextComponent("jrpg.party_join", member.getDisplayName())));
                syncParty(((ServerWorld) member.level).getServer(), true);
            }
        }
    }
    
    public void removeMember(LivingEntity member) {
        if (partyLeader.equals(member.getUUID())) {
            return;
        }
        
        if (partyMembers.containsKey(member.getUUID())) {
            partyMembers.remove(member.getUUID());
            if (activeMembers.remove(member.getUUID()) != null) {
                updateMemberParty(member, false);
            }
            if (!member.level.isClientSide()) {
                sendToEachPlayer(new UIMessagePacket(new TranslationTextComponent("jrpg.party_left", member.getDisplayName())));
                syncParty(((ServerWorld) member.level).getServer(), true);
            }
        }
    }
    
    public UUID getLeader() {
        return partyLeader;
    }
    
    public LivingEntity getLeaderEntity() {
        return activeMembers.get(partyLeader);
    }
    
    public boolean containsMember(UUID member) {
        return partyMembers.containsKey(member);
    }
    
    public boolean setActive(LivingEntity member, boolean active) {
        if (!active && partyLeader.equals(member.getUUID())) {
            return false;
        }
        
        if (active && activeMembers.size() >= 4) {
            return false;
        }
        
        boolean setActive = doSetActive(member, active);
        if (setActive && !member.level.isClientSide()) {
            syncParty(((ServerWorld) member.level).getServer(), true);
        }
        return setActive;
    }
    
    private boolean doSetActive(LivingEntity member, boolean active) {
        if (partyMembers.values().contains(member)) {
            if (active) {
                activeMembers.put(member.getUUID(), member);
                updateMemberParty(member, true);
                return true;
            }
            else {
                if (activeMembers.remove(member.getUUID()) != null) {
                    updateMemberParty(member, false);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    
    public void toBuf(PacketBuffer buf) {
        int[] membersIds = new int[partyMembers.size()];
        int leaderId = -1;
        int[] activeIndices = new int[activeMembers.size()];
        Arrays.fill(membersIds, -1);
        Arrays.fill(activeIndices, -1);
        
        int i = 0;
        for (Map.Entry<UUID, LivingEntity> member : partyMembers.entrySet()) {
            LivingEntity entity = member.getValue();
            if (entity != null) {
                membersIds[i] = entity.getId();
            }
            
            if (member.getKey().equals(partyLeader)) {
                leaderId = i;
            }
            
            ++i;
        }

        i = 0;
        for (Map.Entry<UUID, LivingEntity> active : activeMembers.entrySet()) {
            LivingEntity entity = active.getValue();
            if (entity != null) {
                int activeIndex = ArrayUtils.indexOf(membersIds, entity.getId());
                if (activeIndex > -1) {
                    activeIndices[i] = activeIndex;
                }
            }
            
            ++i;
        }
        
        NetworkUtil.writeIntArray(buf, membersIds);
        buf.writeInt(leaderId);
        NetworkUtil.writeIntArray(buf, activeIndices);
    }
    
    public static Party fromBuf(PacketBuffer buf) {
        int[] memberIds = NetworkUtil.readIntArray(buf);
        int leaderIndex = buf.readInt();
        int[] activeIndices = NetworkUtil.readIntArray(buf);
        
        LivingEntity[] members = new LivingEntity[memberIds.length];
        LivingEntity leader = null;
        LivingEntity[] activeMembers = new LivingEntity[activeIndices.length];
        
        for (int i = 0; i < memberIds.length; i++) {
            Entity entity = ClientUtil.getClientWorld().getEntity(memberIds[i]);
            if (entity instanceof LivingEntity) {
                LivingEntity member = (LivingEntity) entity;
                members[i] = member;
                if (i == leaderIndex) {
                    leader = member;
                }
            }
        }
        
        for (int i = 0; i < activeIndices.length; i++) {
            int activeIndex = activeIndices[i];
            if (activeIndex > -1) {
                activeMembers[i] = members[i];
            }
        }
        
        Party party = new Party(leader);
        for (LivingEntity member : members) {
            if (member != null) {
                party.partyMembers.put(member.getUUID(), member);
            }
        }
        for (LivingEntity activeMember : activeMembers) {
            if (activeMember != null) {
                party.activeMembers.put(activeMember.getUUID(), activeMember);
            }
        }
        
        return party;
    }
    
    public CompoundNBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putUUID("Leader", partyLeader);
        
        CompoundNBT members = new CompoundNBT();
        int i = 0;
        for (UUID member : partyMembers.keySet()) {
            members.putUUID(String.valueOf(i++), member);
        }
        nbt.put("Members", members);
        
        CompoundNBT active = new CompoundNBT();
        int j = 0;
        for (UUID member : activeMembers.keySet()) {
            active.putUUID(String.valueOf(j++), member);
        }
        nbt.put("Active", active);
        
        return nbt;
    }
    
    public static Party fromNBT(CompoundNBT nbt) {
        if (nbt.hasUUID("Leader")) {
            UUID leader = nbt.getUUID("Leader");
            Party party = new Party(leader);
            
            CompoundNBT members = nbt.getCompound("Members");
            for (String key : members.getAllKeys()) {
                if (members.hasUUID(key)) {
                    party.partyMembers.put(members.getUUID(key), null);
                }
            }
            
            CompoundNBT active = nbt.getCompound("Active");
            for (String key : active.getAllKeys()) {
                if (active.hasUUID(key)) {
                    UUID member = active.getUUID(key);
                    if (party.partyMembers.containsKey(member)) {
                        party.activeMembers.put(member, null);
                    }
                }
            }
            
            return party;
        }
        
        return null;
    }
    
}
