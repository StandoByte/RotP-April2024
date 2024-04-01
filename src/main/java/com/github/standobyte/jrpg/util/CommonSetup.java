package com.github.standobyte.jrpg.util;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.util.reflection.CommonReflection;

import net.minecraft.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = ModMain.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CommonSetup {
    
    @SubscribeEvent
    public static void onFMLCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CommonReflection.setMaxStackSize(Items.POTION, 16);
            CommonReflection.setMaxStackSize(Items.SPLASH_POTION, 16);
            CommonReflection.setMaxStackSize(Items.LINGERING_POTION, 16);
        });
    }
    
}
