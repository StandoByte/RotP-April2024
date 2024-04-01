package com.github.standobyte.jrpg.init;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.potion.InstantSPEffect;

import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEffects {
    public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, ModMain.MOD_ID);
    
    
    public static final RegistryObject<Effect> SP_RESTORATION = EFFECTS.register("instant_sp", 
            () -> new InstantSPEffect(EffectType.BENEFICIAL, 0x0080FF));
    
    

    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTION_TYPES, ModMain.MOD_ID);
    
    public static final RegistryObject<Potion> ENERGY_POTION = POTIONS.register("instant_sp", 
            () -> new Potion(new EffectInstance(SP_RESTORATION.get(), 1)));
    
    public static final RegistryObject<Potion> ENERGY_STRONG_POTION = POTIONS.register("strong_instant_sp", 
            () -> new Potion(new EffectInstance(SP_RESTORATION.get(), 1, 1)));
    
    public static final RegistryObject<Potion> ENERGY_VERY_STRONG_POTION = POTIONS.register("strong2_instant_sp", 
            () -> new Potion(new EffectInstance(SP_RESTORATION.get(), 1, 2)));
    
    public static final RegistryObject<Potion> ENERGY_MEGA_STRONG_POTION = POTIONS.register("strong3_instant_sp", 
            () -> new Potion(new EffectInstance(SP_RESTORATION.get(), 1, 3)));
    
    public static final RegistryObject<Potion> ENERGY_GIGA_STRONG_POTION = POTIONS.register("strong4_instant_sp", 
            () -> new Potion(new EffectInstance(SP_RESTORATION.get(), 1, 4)));
    
    public static final RegistryObject<Potion> HEAL_VERY_STRONG_POTION = POTIONS.register("strong2_instant_heal", 
            () -> new Potion(new EffectInstance(Effects.HEAL, 1, 2)));
    
    public static final RegistryObject<Potion> HEAL_MEGA_STRONG_POTION = POTIONS.register("strong3_instant_heal", 
            () -> new Potion(new EffectInstance(Effects.HEAL, 1, 3)));
    
    public static final RegistryObject<Potion> HEAL_GIGA_STRONG_POTION = POTIONS.register("strong4_instant_heal", 
            () -> new Potion(new EffectInstance(Effects.HEAL, 1, 4)));

}
