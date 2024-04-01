package com.github.standobyte.jrpg.event;

import com.github.standobyte.jrpg.combat.Moveset;

import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;

public class JrpgTurnMovesetEvent extends LivingEvent {
    private final Moveset.Builder movesetBuilder;
    
    public JrpgTurnMovesetEvent(LivingEntity entity, Moveset.Builder movesetBuilder) {
        super(entity);
        this.movesetBuilder = movesetBuilder;
    }

    public Moveset.Builder getMovesetBuilder() { return movesetBuilder; }
}
