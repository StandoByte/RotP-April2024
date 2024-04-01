package com.github.standobyte.jrpg.init;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.entity.BowmanEntity;
import com.github.standobyte.jrpg.entity.MobStandUserEntity;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = JojoMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, ModMain.MOD_ID);

    
    
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(BOWMAN.get(), MobEntity.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25D).build());
        event.put(STAND_USER.get(), MobEntity.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25D).build());
    }
    
    
    public static final RegistryObject<EntityType<BowmanEntity>> BOWMAN = ENTITIES.register("bowman", 
            () -> EntityType.Builder.<BowmanEntity>of(BowmanEntity::new, EntityClassification.MISC).sized(0.6F, 1.95F)
            .build(new ResourceLocation(JojoMod.MOD_ID, "bowman").toString()));
    
    public static final RegistryObject<EntityType<MobStandUserEntity>> STAND_USER = ENTITIES.register("stand_user", 
            () -> EntityType.Builder.<MobStandUserEntity>of(MobStandUserEntity::new, EntityClassification.MISC).sized(0.6F, 1.95F)
            .build(new ResourceLocation(JojoMod.MOD_ID, "stand_user").toString()));

}
