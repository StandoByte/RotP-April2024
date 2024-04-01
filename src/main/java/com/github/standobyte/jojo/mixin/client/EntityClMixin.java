package com.github.standobyte.jojo.mixin.client;

import java.util.OptionalInt;

import javax.annotation.Nonnull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.standobyte.jojo.client.IEntityGlowColor;
import com.github.standobyte.jrpg.client.battleui.BattleScreen;

import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public class EntityClMixin implements IEntityGlowColor {
    private OptionalInt glowingColor = OptionalInt.empty();

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    public void jojoGlowingColor(CallbackInfoReturnable<Integer> ci) {
        OptionalInt target = BattleScreen.targetHighlight(this);
        if (target.isPresent()) {
            ci.setReturnValue(target.getAsInt());
        }
        else {
            glowingColor.ifPresent(ci::setReturnValue);
        }
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    public void jojoSpecialGlowing(CallbackInfoReturnable<Boolean> ci) {
        OptionalInt target = BattleScreen.targetHighlight(this);
        if (target.isPresent()) {
            ci.setReturnValue(true);
        }
        else if (glowingColor.isPresent()) {
            ci.setReturnValue(true);
        }
    }
    
    @Override
    public void setGlowColor(@Nonnull OptionalInt color) {
        this.glowingColor = color;
    }

    @Override
    public OptionalInt getGlowColor() {
        return glowingColor;
    }
}
