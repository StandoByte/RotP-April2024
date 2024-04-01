package com.github.standobyte.jrpg.client;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.client.battleui.range.RangeLimitParticle;
import com.github.standobyte.jrpg.client.entity.BowmanRenderer;
import com.github.standobyte.jrpg.client.entity.MobStandUserRenderer;
import com.github.standobyte.jrpg.init.ModEntityTypes;
import com.github.standobyte.jrpg.init.ModParticles;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ModMain.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    
    @SubscribeEvent
    public static void onFMLClientSetup(FMLClientSetupEvent event) {
        Minecraft mc = event.getMinecraftSupplier().get();
        
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.BOWMAN.get(), BowmanRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.STAND_USER.get(), MobStandUserRenderer::new);
        
        ClientStuff.initKeys();
    }

    @SubscribeEvent
    public static void onMcConstructor(ParticleFactoryRegisterEvent event) {
        Minecraft mc = Minecraft.getInstance();
        mc.particleEngine.register(ModParticles.RANGE_LIMIT.get(), RangeLimitParticle.Factory::new);
    }
}
