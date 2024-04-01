package com.github.standobyte.jrpg.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.party.PartyHandler;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

public class ServerStuff {
    private final ServerWorld overworld;
    private final Collection<Battle> currentBattles = new ArrayList<>();
    public final PartyHandler partyHandler;
    
    public ServerStuff(ServerWorld overworld) {
        this.overworld = overworld;
        this.partyHandler = new PartyHandler(overworld.getServer());
    }
    
    public void addBattle(Battle battle) {
        currentBattles.add(battle);
    }
    
    public void tick() {
        Iterator<Battle> it = currentBattles.iterator();
        while (it.hasNext()) {
            Battle battle = it.next();
            battle.tickBattle();
            if (battle.toBeRemoved()) {
                it.remove();
            }
        }
        
        partyHandler.tick();
    }
    
    
    
    public CompoundNBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.put("Parties", partyHandler.toNBT());
        return nbt;
    }
    
    public void fromNBT(INBT inbt) {
        CompoundNBT nbt = (CompoundNBT) inbt;
        if (nbt.contains("Parties", Constants.NBT.TAG_COMPOUND)) {
            partyHandler.fromNBT(nbt.getCompound("Parties"));
        }
    }
}
