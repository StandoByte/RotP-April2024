package com.github.standobyte.jrpg.init;

import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

public class StandRPGActions {
    
    public static final Map<ResourceLocation, Object2IntMap<ActionType<?>>> LEVELING = Util.make(new HashMap<>(), map -> {
        Object2IntMap<ActionType<?>> starPlatinum = new Object2IntArrayMap<>();
        map.put(new ResourceLocation("jojo", "star_platinum"), starPlatinum);

        Object2IntMap<ActionType<?>> hierophantGreen = new Object2IntArrayMap<>();
        map.put(new ResourceLocation("jojo", "hierophant_green"), hierophantGreen);

        Object2IntMap<ActionType<?>> magiciansRed = new Object2IntArrayMap<>();
        map.put(new ResourceLocation("jojo", "magicians_red"), magiciansRed);

        Object2IntMap<ActionType<?>> silverChariot = new Object2IntArrayMap<>();
        map.put(new ResourceLocation("jojo", "silver_chariot"), silverChariot);

        Object2IntMap<ActionType<?>> theWorld = new Object2IntArrayMap<>();
        map.put(new ResourceLocation("jojo", "the_world"), theWorld);
    });
    
    public static final Object2IntMap<ActionType<?>> COMMON_ACTIONS = Util.make(new Object2IntArrayMap<>(), map -> {
//        map.put(ModCombatActions.RESOLVE, 20);
    });
}
