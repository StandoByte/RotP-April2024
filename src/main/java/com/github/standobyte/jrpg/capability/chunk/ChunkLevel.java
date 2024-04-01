package com.github.standobyte.jrpg.capability.chunk;

import java.util.Optional;
import java.util.Random;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.capability.world.DimensionFirstChunkProvider;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.network.packets.server.ChunkLevelPacket;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;

public class ChunkLevel {
    private static final Random RANDOM = new Random();
    private final Chunk chunk;
    private boolean initialized;
    public int levelMin;
    public int levelMax;
    
    public ChunkLevel(Chunk chunk) {
        this.chunk = chunk;
    }
    
    public static Optional<ChunkLevel> getChunkLevel(Chunk chunk) {
        return chunk.getCapability(ChunkLevelProvider.CAPABILITY).resolve();
    }
    
    
    
    public void onChunkLoad(ServerPlayerEntity player) {
        ModNetworkManager.sendToClient(new ChunkLevelPacket(chunk.getPos(), levelMin, levelMax), player);
    }
    
    
    
    public void initLevel(ServerWorld world) {
        if (!initialized) {
            initialized = true;
            ChunkPos chunkPos = world.getCapability(DimensionFirstChunkProvider.CAPABILITY).resolve()
                    .map(cap -> cap.getFirstChunk()).orElseGet(() -> {
                        if (world instanceof ServerWorld && world.dimension() == World.OVERWORLD) {
                            BlockPos spawn = ((ServerWorld) world).getSharedSpawnPos();
                            if (spawn != null) {
                                return new ChunkPos(spawn);
                            }
                        }
                        return new ChunkPos(0, 0);
                    });
            int chunkDist = chunk.getPos().getChessboardDistance(chunkPos);
            
            int minDimensionLvl;
            int maxDimensionLvl;
            int minLvlChunks;
            int lvlZoneChunks;
            int lvlDiff;
            if (world.dimension() == World.OVERWORLD) {
                minDimensionLvl = 1;
                minLvlChunks = 20;
                lvlZoneChunks = 10;
                lvlDiff = 2;
                maxDimensionLvl = 65;
            }
            else if (world.dimension() == World.NETHER) {
                minDimensionLvl = 66;
                minLvlChunks = 8;
                lvlZoneChunks = 4;
                lvlDiff = 3;
                maxDimensionLvl = 90;
            }
            else if (world.dimension() == World.END) {
                minDimensionLvl = 90;
                minLvlChunks = 16;
                lvlZoneChunks = 16;
                lvlDiff = 2;
                maxDimensionLvl = 100;
            }
            else {
                minDimensionLvl = 30;
                minLvlChunks = 12;
                lvlZoneChunks = 6;
                lvlDiff = 3;
                maxDimensionLvl = 80;
            }
            levelMin = MathHelper.clamp(minDimensionLvl + (MathHelper.floor(
                    (double) (Math.max(chunkDist - minLvlChunks, 0)) / (double) lvlZoneChunks)) * lvlDiff, 
                    1, maxDimensionLvl);
            levelMax = Math.min(levelMin + 4, 100);
            ModMain.getLogger().debug("    Lvl {} - {} (chunk dist == {})", levelMin, levelMax, chunkDist);
        }
    }
    
    public int getRandomLevel(float effectiveDifficulty) {
        int maxDiff = (int) (effectiveDifficulty * (levelMax - levelMin + 1));
        return maxDiff <= 0 ? levelMin : levelMin + RANDOM.nextInt(maxDiff);
    }
    
    public INBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("LevelMin", levelMin);
        nbt.putInt("LevelMax", levelMax);
        return nbt;
    }
    
    public void fromNBT(INBT inbt) {
        CompoundNBT nbt = (CompoundNBT) inbt;
        this.initialized = true;
        this.levelMin = nbt.getInt("LevelMin");
        this.levelMax = nbt.getInt("LevelMax");
    }
}
