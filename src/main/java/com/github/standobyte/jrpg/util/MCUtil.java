package com.github.standobyte.jrpg.util;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.TrResetDeathTimePacket;
import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;

public class MCUtil {

    public static <T extends Entity> List<T> getEntitiesOfClass(World world, 
            Class<? extends T> clazz, AxisAlignedBB area, @Nullable Predicate<? super T> filter) {
        world.getProfiler().incrementCounter("getEntities");
        int minX = MathHelper.floor((area.minX - world.getMaxEntityRadius()) / 16.0D);
        int maxX = MathHelper.ceil((area.maxX + world.getMaxEntityRadius()) / 16.0D);
        int minZ = MathHelper.floor((area.minZ - world.getMaxEntityRadius()) / 16.0D);
        int maxZ = MathHelper.ceil((area.maxZ + world.getMaxEntityRadius()) / 16.0D);
        List<T> list = Lists.newArrayList();
        AbstractChunkProvider chunkSource = world.getChunkSource();

        for (int x = minX; x < maxX; ++x) {
            for (int z = minZ; z < maxZ; ++z) {
                Chunk chunk = chunkSource.getChunk(x, z, false);
                if (chunk != null) {
                    chunk.getEntitiesOfClass(clazz, area, list, filter);
                }
            }
        }

        return list;
    }
    
    
    
    public static void onEntityResurrect(LivingEntity entity) {
        entity.deathTime = 0;
        if (!entity.level.isClientSide()) {
            PacketManager.sendToClientsTrackingAndSelf(new TrResetDeathTimePacket(entity.getId()), entity);
            
            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                if (!player.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !player.isSpectator()) {
                    player.setExperienceLevels(0);
                    player.setExperiencePoints(0);
                }
            }
        }
    }
}
