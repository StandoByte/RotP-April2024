package com.github.standobyte.jrpg.util;

import java.util.Map;

import com.github.standobyte.jrpg.ModMain;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DynRegistry<V extends ObjWithId> extends JsonReloadListener {
    private final BiMap<ResourceLocation, V> defaultRegistry = HashBiMap.create();
    private final BiMap<ResourceLocation, V> current = HashBiMap.create();
    
    public DynRegistry(Gson gson, String directory) {
        super(gson, directory);
    }
    
    public <I extends V> I register(I action, ResourceLocation id) {
        action.id = id;
        if (defaultRegistry.containsKey(id)) {
            ModMain.getLogger().error("Action with id {} is already present!", id);
        }
        if (defaultRegistry.containsValue(action)) {
            ModMain.getLogger().error("Action instance is beind registered twice!");
        }
        defaultRegistry.put(id, action);
        current.put(id, action);
        return action;
    }
    
    public V getValue(ResourceLocation id) {
        return current.get(id);
    }
    
    
    
    @SubscribeEvent
    public void onDataPackLoad(AddReloadListenerEvent event) {
        event.addListener(this);
    }
    
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, IResourceManager resourceManager, IProfiler profiler) {
        current.clear();
        current.putAll(defaultRegistry);
    }
    
    @SubscribeEvent
    public void syncCustomData(OnDatapackSyncEvent event) {
        
    }
}
