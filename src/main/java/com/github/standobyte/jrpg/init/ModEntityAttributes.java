package com.github.standobyte.jrpg.init;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.stats.RPGStat;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = ModMain.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, ModMain.MOD_ID);

    public static final RegistryObject<Attribute> DURABILITY = ATTRIBUTES.register("durability", 
            () -> new RangedAttribute("jrpg.stat.durabiltiy", 4.0, 0.0, 32768.0).setSyncable(true));

    public static final RegistryObject<Attribute> PRECISION = ATTRIBUTES.register("precision", 
            () -> new RangedAttribute("jrpg.stat.precision", 4.0, 0.0, 32768.0).setSyncable(true));

    public static final RegistryObject<Attribute> SPIRIT = ATTRIBUTES.register("spirit", 
            () -> new RangedAttribute("jrpg.stat.spirit", 4.0, 0.0, 32768.0).setSyncable(true));

    public static final RegistryObject<Attribute> STAND_STAMINA = ATTRIBUTES.register("sp", 
            () -> new RangedAttribute("jrpg.stat.sp", 16.0, 0.0, 32768.0).setSyncable(true));
    
    
    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            for (RPGStat stat : RPGStat.values()) {
                if (!event.has(type, stat.attribute)) {
                    event.add(type, stat.attribute, stat.lvlIncreaseTable[0]);
                }
            }
        }
    }
}
