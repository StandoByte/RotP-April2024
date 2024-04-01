package com.github.standobyte.jrpg.party;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;

public class PartyHandler {
    private Map<UUID, Party> allParties = new HashMap<>();
    private final MinecraftServer server;
    
    public PartyHandler(MinecraftServer server) {
        this.server = server;
    }
    
    
    public boolean addToParty(LivingEntity partyLeader, LivingEntity newMember) {
        UUID newMemberId = newMember.getUUID();
        boolean alreadyInParty = allParties.values().stream().anyMatch(
                p -> p.containsMember(newMemberId));
        if (alreadyInParty) {
            return false;
        }
        
        Party party = allParties.get(partyLeader.getUUID());
        if (party == null) {
            boolean notLeader = allParties.values().stream().anyMatch(
                    p -> p.containsMember(partyLeader.getUUID()) && !partyLeader.getUUID().equals(p.getLeader()));
            if (notLeader) {
                return false;
            }
            
            party = new Party(partyLeader);
            party.syncParty(server, true);
            allParties.put(partyLeader.getUUID(), party);
        }
        
        party.addNewMember(newMember);
        party.setActive(newMember, true);
        return true;
    }
    
    @Nullable
    public Party getParty(LivingEntity partyLeader) {
        return allParties.get(partyLeader.getUUID());
    }
    
    public Optional<Party> findParty(UUID member) {
        return allParties.values().stream().filter(party -> party.containsMember(member)).findFirst();
    }
    
    public void tick() {
        for (Party party : allParties.values()) {
            party.serverTick(server);
        }
    }
    
    
    public CompoundNBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        for (Map.Entry<UUID, Party> partyEntry : allParties.entrySet()) {
            nbt.put(partyEntry.getKey().toString(), partyEntry.getValue().toNBT());
        }
        return nbt;
    }
    
    public void fromNBT(CompoundNBT nbt) {
        for (String key : nbt.getAllKeys()) {
            UUID partyLeaderId;
            try {
                partyLeaderId = UUID.fromString(key);
            }
            catch (IllegalArgumentException e) {
                continue;
            }
            Party party = Party.fromNBT(nbt.getCompound(key));
            allParties.put(partyLeaderId, party);
        }
    }
    
    
    // TODO nbt
    
}
