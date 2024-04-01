package com.github.standobyte.jrpg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jrpg.init.ActionType;
import com.github.standobyte.jrpg.init.ModCombatActions;
import com.github.standobyte.jrpg.init.ModEffects;
import com.github.standobyte.jrpg.init.ModEntityAttributes;
import com.github.standobyte.jrpg.init.ModEntityTypes;
import com.github.standobyte.jrpg.init.ModParticles;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.util.reflection.CommonReflection;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ModMain.MOD_ID)
public class ModMain {
    public static final String MOD_ID = "jojo";
    public static final String RES_NAMESPACE = "jrpg";
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final boolean JRPG_MODE = true;
    
    public static Logger getLogger() {
        return LOGGER;
    }
    
    public static void tmpLog(String str, Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Entity) {
                args[i] = ((Entity) args[i]).getDisplayName().getString();
            }
        }
        LOGGER.debug(str, args);
    }
    
    private JojoMod jojoMod;
    public ModMain() {
        this.jojoMod = new JojoMod();
        
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(this);
        ActionType.initRegistry();
        
        ModCombatActions.load();
        
        ModEntityAttributes.ATTRIBUTES.register(modEventBus);
        CommonReflection.setMaxValue((RangedAttribute) Attributes.MAX_HEALTH, 32768);
        CommonReflection.setMaxValue((RangedAttribute) Attributes.ATTACK_DAMAGE, 32768);
        CommonReflection.setMaxValue((RangedAttribute) Attributes.ATTACK_SPEED, 32768);
        Attributes.ATTACK_DAMAGE.setSyncable(true);

        ModEntityTypes.ENTITIES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        ModEffects.POTIONS.register(modEventBus);
        ModParticles.PARTICLES.register(modEventBus);
    }
    
    @SubscribeEvent
    public void onFMLCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CapRegistration.registerCapabilities();
            ModNetworkManager.init();
        });
    }
}
