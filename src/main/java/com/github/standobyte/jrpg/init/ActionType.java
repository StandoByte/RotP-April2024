package com.github.standobyte.jrpg.init;

import javax.annotation.Nullable;

import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.util.DynRegistry;
import com.github.standobyte.jrpg.util.ObjWithId;
import com.google.gson.GsonBuilder;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.MinecraftForge;

public class ActionType<T extends CombatAction> extends ObjWithId {
    
    public ActionType() {}

    private ActionFactory<T> factory;
    
    public static <T extends CombatAction> ActionType<T> fromConstructor(ActionFactory<T> constructor) {
        ActionType<T> type = new ActionType<>();
        type.factory = constructor;
        return type;
    }
    
    @Nullable
    public T createAction(LivingEntity entity, Battle battle) {
        T action = factory.create(this, entity, battle);
        if (action != null) {
            action.afterInit();
        }
        return action;
    }
    
    private String tlKey;
    public ITextComponent getName() {
        if (tlKey == null) {
            tlKey = Util.makeDescriptionId("action", getId());
        }
        return new TranslationTextComponent(tlKey);
    }
    
    
    
    @FunctionalInterface
    public static interface ActionFactory<T extends CombatAction> {
        T create(ActionType<?> type, LivingEntity user, Battle battle);
    }
    
    
    
    private static DynRegistry<ActionType<?>> registry;
    public static void initRegistry() {
        if (registry == null) {
            registry = new DynRegistry<ActionType<?>>(new GsonBuilder().create(), "jrpg_actions");
            MinecraftForge.EVENT_BUS.register(registry);
        }
    }
    
    public static DynRegistry<ActionType<?>> registry() {
        return registry;
    }
}
