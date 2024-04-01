package com.github.standobyte.jrpg.util.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.item.Item;
import net.minecraft.potion.EffectInstance;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class CommonReflection {
    
    private static final Method LIVING_ENTITY_ON_EFFECT_UPDATED = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "func_70695_b", 
            EffectInstance.class, boolean.class);
    public static void onEffectUpdated(LivingEntity entity, EffectInstance effect, boolean resetAttributes) {
        ReflectionUtil.invokeMethod(LIVING_ENTITY_ON_EFFECT_UPDATED, entity, effect, resetAttributes);
    }
    
    
    private static final Field RANGED_ATTRIBUTE_MAX_VALUE = ObfuscationReflectionHelper.findField(RangedAttribute.class, "field_111118_b");
    public static void setMaxValue(RangedAttribute attribute, double maxValue) {
        ReflectionUtil.setDoubleFieldValue(RANGED_ATTRIBUTE_MAX_VALUE, attribute, maxValue);
    }
    
    
    private static final Method CREEPER_ENTITY_EXPLODE = ObfuscationReflectionHelper.findMethod(CreeperEntity.class, "func_146077_cc");
    public static void explodeCreeper(CreeperEntity entity) {
        ReflectionUtil.invokeMethod(CREEPER_ENTITY_EXPLODE, entity);
    }
    
    
    private static final Field ITEM_MAX_STACK_SIZE = ObfuscationReflectionHelper.findField(Item.class, "field_77777_bU");
    public static void setMaxStackSize(Item item, int maxStackSize) {
        ReflectionUtil.setIntFieldValue(ITEM_MAX_STACK_SIZE, item, maxStackSize);
    }
}
