package com.github.standobyte.jrpg;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jrpg.capability.chunk.ChunkLevel;
import com.github.standobyte.jrpg.capability.chunk.ChunkLevelProvider;
import com.github.standobyte.jrpg.capability.entity.CombatDataProvider;
import com.github.standobyte.jrpg.capability.entity.RPGDataProvider;
import com.github.standobyte.jrpg.capability.world.DimensionFirstChunk;
import com.github.standobyte.jrpg.capability.world.DimensionFirstChunkProvider;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.server.ServerBattlesProvider;
import com.github.standobyte.jrpg.server.ServerStuff;
import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = ModMain.MOD_ID)
public class CapRegistration {
    private static final ResourceLocation LIVING_COMBAT_CAP = new ResourceLocation(ModMain.RES_NAMESPACE, "combat_data");
    private static final ResourceLocation LIVING_RPG_CAP = new ResourceLocation(ModMain.RES_NAMESPACE, "rpg_data");
    private static final ResourceLocation SERVER_DATA = new ResourceLocation(ModMain.RES_NAMESPACE, "server_battles");
    private static final ResourceLocation CHUNK_DATA = new ResourceLocation(ModMain.RES_NAMESPACE, "chunk_level");
    private static final ResourceLocation WORLD_DATA = new ResourceLocation(ModMain.RES_NAMESPACE, "world_first_chunk");
    
    @SubscribeEvent
    public static void onAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof LivingEntity && !(entity instanceof StandEntity)) {
            LivingEntity living = (LivingEntity) entity;
            event.addCapability(LIVING_COMBAT_CAP, new CombatDataProvider(living));
            event.addCapability(LIVING_RPG_CAP, new RPGDataProvider(living));
        }
    }
    
    @SubscribeEvent
    public static void onAttachCapabilitiesWorld(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();
        if (!world.isClientSide()) {
            if (world.dimension() == World.OVERWORLD) {
                event.addCapability(SERVER_DATA, new ServerBattlesProvider((ServerWorld) world));
            }
        }
        event.addCapability(WORLD_DATA, new DimensionFirstChunkProvider(world));
    }
    
    @SubscribeEvent
    public static void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<Chunk> event) {
        event.addCapability(CHUNK_DATA, new ChunkLevelProvider(event.getObject()));
    }
    
    public static void registerCapabilities() {
        CapabilityManager.INSTANCE.register(EntityCombat.class, new IStorage<EntityCombat>() {
            @Override public INBT writeNBT(Capability<EntityCombat> capability, EntityCombat instance, Direction side) { return null; }
            @Override public void readNBT(Capability<EntityCombat> capability, EntityCombat instance, Direction side, INBT nbt) { }
        }, () -> new EntityCombat(null));

        CapabilityManager.INSTANCE.register(ServerStuff.class, new IStorage<ServerStuff>() {
            @Override public INBT writeNBT(Capability<ServerStuff> capability, ServerStuff instance, Direction side) { return instance.toNBT(); }
            @Override public void readNBT(Capability<ServerStuff> capability, ServerStuff instance, Direction side, INBT nbt) { instance.fromNBT(nbt); }
        }, () -> new ServerStuff(null));

        CapabilityManager.INSTANCE.register(EntityRPGData.class, new IStorage<EntityRPGData>() {
            @Override public INBT writeNBT(Capability<EntityRPGData> capability, EntityRPGData instance, Direction side) { return instance.toNBT(); }
            @Override public void readNBT(Capability<EntityRPGData> capability, EntityRPGData instance, Direction side, INBT nbt) { instance.fromNBT(nbt); }
        }, () -> new EntityRPGData(null));

        CapabilityManager.INSTANCE.register(ChunkLevel.class, new IStorage<ChunkLevel>() {
            @Override public INBT writeNBT(Capability<ChunkLevel> capability, ChunkLevel instance, Direction side) { return instance.toNBT(); }
            @Override public void readNBT(Capability<ChunkLevel> capability, ChunkLevel instance, Direction side, INBT nbt) { instance.fromNBT(nbt); }
        }, () -> new ChunkLevel(null));

        CapabilityManager.INSTANCE.register(DimensionFirstChunk.class, new IStorage<DimensionFirstChunk>() {
            @Override public INBT writeNBT(Capability<DimensionFirstChunk> capability, DimensionFirstChunk instance, Direction side) { return instance.toNBT(); }
            @Override public void readNBT(Capability<DimensionFirstChunk> capability, DimensionFirstChunk instance, Direction side, INBT nbt) { instance.fromNBT(nbt); }
        }, () -> new DimensionFirstChunk(null));
    }
}
