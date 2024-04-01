package com.github.standobyte.jrpg.capability.world;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;

public class DimensionFirstChunk {
    private final World world;
    private ChunkPos firstChunk;
    
    public DimensionFirstChunk(World world) {
        this.world = world;
    }
    
    public boolean firstChunkInitialized() {
        return firstChunk != null;
    }
    
    public void setFirstChunk(ChunkPos chunkPos) {
        this.firstChunk = chunkPos;
    }
    
    public ChunkPos getFirstChunk() {
        return firstChunk;
    }
    
    
    public CompoundNBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        if (firstChunk != null) {
            nbt.putIntArray("Chunk", new int[] { firstChunk.x, firstChunk.z });
        }
        return nbt;
    }
    
    public void fromNBT(INBT inbt) {
        CompoundNBT nbt = (CompoundNBT) inbt;
        if (nbt.contains("Chunk", Constants.NBT.TAG_INT_ARRAY)) {
            int[] chunkPos = nbt.getIntArray("Chunk");
            if (chunkPos.length >= 2) {
                firstChunk = new ChunkPos(chunkPos[0], chunkPos[1]);
            }
        }
    }

}
