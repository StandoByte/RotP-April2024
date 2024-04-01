package com.github.standobyte.jrpg.util;

import net.minecraft.util.ResourceLocation;

public class ObjWithId {
    ResourceLocation id = null;

    public final ResourceLocation getId() {
        if (id == null) {
            throw new IllegalStateException("ID was not initialized!");
        }
        return id;
    }
    
}
