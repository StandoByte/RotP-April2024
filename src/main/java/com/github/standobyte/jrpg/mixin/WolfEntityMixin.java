package com.github.standobyte.jrpg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.standobyte.jrpg.party.Party;
import com.github.standobyte.jrpg.party.PartyHandler;
import com.github.standobyte.jrpg.server.ServerBattlesProvider;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

@Mixin(WolfEntity.class)
public abstract class WolfEntityMixin extends TameableEntityMixin {
    
    protected WolfEntityMixin(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
    }
    
    @Override
    public void turnBasedOnTamedSit(boolean orderedToSit, CallbackInfo ci) {
        if (!level.isClientSide()) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                PartyHandler partyHandler = ServerBattlesProvider.getServerBattlesData(((ServerWorld) level).getServer()).partyHandler;
                if (orderedToSit) {
                    Party party = partyHandler.getParty(owner);
                    if (party != null) {
                        party.removeMember(this);
                    }
                }
                else {
                    boolean added = partyHandler.addToParty(owner, this);
                }
            }
        }
    }
    
    @Inject(method = "setTame", at = @At("TAIL"))
    public void turnBasedOnWolfTame(boolean tamed, CallbackInfo ci) {
        this.setHealth((float) this.getAttribute(Attributes.MAX_HEALTH).getValue()); // fuck my life
    }
}
