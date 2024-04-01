package com.github.standobyte.jrpg.stats;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.github.standobyte.jojo.entity.mob.IMobPowerUser;
import com.github.standobyte.jojo.entity.mob.IMobStandUser;
import com.github.standobyte.jojo.power.IPower;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.OptionalFloat;
import com.github.standobyte.jrpg.capability.chunk.ChunkLevel;
import com.github.standobyte.jrpg.capability.entity.RPGDataProvider;
import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.entity.BowmanEntity;
import com.github.standobyte.jrpg.entity.MobStandUserEntity;
import com.github.standobyte.jrpg.init.ModEntityAttributes;
import com.github.standobyte.jrpg.init.ModEntityTypes;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.network.NetworkUtil;
import com.github.standobyte.jrpg.network.packets.server.RPGDataPacket;
import com.github.standobyte.jrpg.network.packets.server.StaminaPointsPacket;
import com.github.standobyte.jrpg.network.packets.server.UIMessagePacket;
import com.github.standobyte.jrpg.party.Party;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.server.ServerWorld;

public class EntityRPGData {
    public final LivingEntity entity;
    
    private boolean mobLvlInitialized;
    private int mobSpawnLevel;
    
    private int level = 1;
    private int xpOnLevel = 0;
    
    private double staminaPoints;
    
    private OptionalFloat fixHpSaved = OptionalFloat.empty();
    
    public Boolean bowmanEvent = null; // null - haven't met, false - should spawn, true - event over
    
    private Object2FloatMap<ResourceLocation> standLevels = new Object2FloatArrayMap<>();
    private Map<ResourceLocation, Object2DoubleMap<RPGStat>> standLevelingStats = new HashMap<>();
    
    private long lastStandUserDaySpawn = -1;
    
    public EntityRPGData(LivingEntity entity) {
        this.entity = entity;
    }
    
    public static Optional<EntityRPGData> get(LivingEntity entity) {
        return entity.getCapability(RPGDataProvider.CAPABILITY).resolve();
    }
    
    public void tick() {
        if (!entity.level.isClientSide()) {
            if (!mobLvlInitialized && entity instanceof MobEntity) {
                updateMobLevel();
                ModNetworkManager.sendToClientsTracking(new RPGDataPacket(this, entity.getId()), entity);
            }
            if (fixHpSaved.isPresent()) {
                entity.setHealth(fixHpSaved.getAsFloat());
                fixHpSaved = OptionalFloat.empty();
            }
            
            if (entity.getType() == EntityType.PLAYER && bowmanEvent == null && level > 10) {
                bowmanEvent = false;
            }
            
            if (bowmanEvent != null && !bowmanEvent
                    && entity.tickCount > 200 && entity instanceof ServerPlayerEntity 
                    && entity.getHealth() / entity.getMaxHealth() >= 0.9f
                    && !((PlayerEntity) entity).isCreative()) {
                ((ServerPlayerEntity) entity).displayClientMessage(new TranslationTextComponent("jrpg.bowman_msg"), true);
                BowmanEntity bowman = new BowmanEntity(entity.level);
                Vector3d pos = entity.position().add(entity.getLookAngle().scale(-6));
                bowman.setPos(pos.x, pos.y, pos.z);
                bowman.finalizeSpawn((ServerWorld) entity.level, 
                        entity.level.getCurrentDifficultyAt(entity.blockPosition()), SpawnReason.EVENT, (ILivingEntityData)null, (CompoundNBT)null);
                entity.level.addFreshEntity(bowman);
                bowmanEvent = true;
                
                lastStandUserDaySpawn = entity.level.getDayTime() / 24000;
            }
            
            if (lastStandUserDaySpawn > -1) {
                long curDay = entity.level.getDayTime() / 24000;
                if (curDay - lastStandUserDaySpawn >= 4) {
                    MobStandUserEntity enemy = new MobStandUserEntity(entity.level);
                    Vector3d pos = entity.position().add(entity.getLookAngle().scale(-6));
                    EntityRPGData.get(enemy).ifPresent(enemyData -> {
                        enemyData.mobLvlInitialized = true;
                        enemyData.mobSpawnLevel = Math.min(this.level + 5, StatsTable.MAX_LVL);
                    });
                    enemy.setPos(pos.x, pos.y, pos.z);
                    enemy.finalizeSpawn((ServerWorld) entity.level, 
                            entity.level.getCurrentDifficultyAt(entity.blockPosition()), SpawnReason.EVENT, (ILivingEntityData)null, (CompoundNBT)null);
                    entity.level.addFreshEntity(enemy);
                }
            }
        }
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getXpDropped() {
        int xp = StatsTable.XP_DROP[MathHelper.clamp(level - 1, 0, StatsTable.MAX_LVL)];
        if (!(entity.getType() == EntityType.PLAYER || entity.getClassification(false) == EntityClassification.MONSTER
                || entity instanceof IMobStandUser || entity instanceof IMobPowerUser)) {
            xp /= 20;
        }
        return xp;
    }
    
    public int getXpForNextLvl() {
        if (level <= 0 || level >= StatsTable.MAX_LVL) {
            return 0;
        }
        return StatsTable.XP_TO_NEXT_LVL[level];
    }
    
    public float getLevelProgress() {
        int xpForNextLvl = getXpForNextLvl();
        if (xpForNextLvl <= 0) {
            return 1;
        }
        return (float) xpOnLevel / xpForNextLvl;
    }
    
    public void giveXp(int xp) {
        int xpResult = this.xpOnLevel + xp;
        boolean levelUp = false;
        int prevLevel = this.level;
        while (this.level < StatsTable.MAX_LVL && xpResult >= StatsTable.XP_TO_NEXT_LVL[level]) {
            xpResult -= StatsTable.XP_TO_NEXT_LVL[level];
            this.level++;
            
            TextComponent msg = new TranslationTextComponent("jrpg.lvl_up_msg", entity.getDisplayName(), this.level);
            double[] prevStats = new double[RPGStat.values().length];
            
            for (RPGStat stat : RPGStat.values()) {
                prevStats[stat.ordinal()] = getStatFromLevelOnly(stat);
            }
            statAttributeBuffs();
            boolean comma = false;
            for (RPGStat stat : RPGStat.values()) {
                if (stat == RPGStat.STAND_STAMINA && !usesStamina()) continue;
                
                double newStat = getStatFromLevelOnly(stat);
                if ((int) newStat > (int) prevStats[stat.ordinal()]) {
                    int increase = (int) newStat - (int) prevStats[stat.ordinal()];
                    if (!comma) {
                        msg.append(" (+" + increase + " ").append(stat.name);
                    }
                    else {
                        msg.append(", +" + increase + " ").append(stat.name);
                    }
                    comma = true;
                }
            }
            if (comma) {
                msg.append(")");
            }
            
            toPartyAndPlayer(new UIMessagePacket(msg));
            
            levelUp = true;
            
            if (entity instanceof ServerPlayerEntity && level == 2) {
                ((ServerPlayerEntity) entity).displayClientMessage(new TranslationTextComponent("jrpg.stats_hint"), true);
            }
        }
        this.xpOnLevel = xpResult;
        
        toPartyAndPlayer(new RPGDataPacket(this, entity.getId()));
        toPartyAndPlayer(new UIMessagePacket(new TranslationTextComponent("jrpg.xp_msg", entity.getDisplayName(), xp)));
        if (levelUp) {
            fixHpSaved = OptionalFloat.of(entity.getMaxHealth());
            entity.setHealth(entity.getMaxHealth());
            staminaPoints = entity.getAttributes().hasAttribute(ModEntityAttributes.STAND_STAMINA.get()) ?
                    entity.getAttributeValue(ModEntityAttributes.STAND_STAMINA.get()) : 0;
        }
    }
    
    public void setLevel(int level) {
        this.level = level;
        this.xpOnLevel = 0;
        statAttributeBuffs();
        fixHpSaved = OptionalFloat.of(entity.getMaxHealth());
        entity.setHealth(entity.getMaxHealth());
        staminaPoints = entity.getAttributes().hasAttribute(ModEntityAttributes.STAND_STAMINA.get()) ?
                entity.getAttributeValue(ModEntityAttributes.STAND_STAMINA.get()) : 0;
    }
    
    public void addStandLevel(float levels) {
        ResourceLocation id = IStandPower.getStandPowerOptional(entity).resolve().map(power -> {
            return power.hasPower() ? power.getType().getRegistryName() : null;
        }).orElse(null);
        if (id != null) {
            setStandLevel(id, getStandLevel(id) + levels);
        }
    }
    
    public void setStandLevel(ResourceLocation standId, float level) {
        level = MathHelper.clamp(level, 0, 20);
        float prev;
        if (!standLevels.containsKey(standId)) {
            standLevels.put(standId, 1);
            prev = 1;
        }
        else {
            prev = standLevels.getFloat(standId);
        }
        
        standLevels.put(standId, level);
        if ((int) prev != (int) level) {
            Object2DoubleMap<RPGStat> statsRandom = new Object2DoubleArrayMap<>();
            
            for (int standLvl = (int) prev; standLvl < (int) level; standLvl++) {
                TextComponent msg = new TranslationTextComponent("jrpg.stand_lvl_up_msg", entity.getDisplayName(), standLvl);
                toPartyAndPlayer(new UIMessagePacket(msg));
                
                int stats = StatsTable.STATS_ON_STAND_LEVEL[standLvl];
                for (int i = 0; i < stats; i++) {
                    RPGStat stat = StatsTable.getRandomStatInc(standId);
                    statsRandom.put(stat, statsRandom.getOrDefault(stat, 0) + 1);
                }
            }
            
            Object2DoubleMap<RPGStat> statsPrev = standLevelingStats.get(standId);
            if (statsPrev == null) {
                standLevelingStats.put(standId, statsRandom);
            }
            else {
                for (Object2DoubleMap.Entry<RPGStat> entry : statsRandom.object2DoubleEntrySet()) {
                    statsPrev.put(entry.getKey(), statsPrev.getOrDefault(entry.getKey(), 0) + entry.getDoubleValue());
                }
            }
            
            statAttributeBuffs();
        }
    }
    
    public float getStandLevel(ResourceLocation standId) {
        if (!standLevels.containsKey(standId)) {
            standLevels.put(standId, 1);
            return 1;
        }
        return standLevels.getFloat(standId);
    }
    
    private void toPartyAndPlayer(Object packet) {
        Party party = getParty();
        if (party != null) {
            party.forEachPlayer(player -> ModNetworkManager.sendToClient(packet, player));
        }
        else if (entity instanceof ServerPlayerEntity) {
            ModNetworkManager.sendToClient(packet, (ServerPlayerEntity) entity);
        }
    }
    

    public void statAttributeBuffs() {
        if (entity.getType() == EntityType.WITHER) {
            for (RPGStat stat : RPGStat.values()) {
                entity.getAttribute(stat.attribute).setBaseValue(stat == RPGStat.HEALTH ? 6666 : 666);
            }
            return;
        }
        else if (entity.getType() == EntityType.ENDER_DRAGON) {
            for (RPGStat stat : RPGStat.values()) {
                entity.getAttribute(stat.attribute).setBaseValue(stat == RPGStat.HEALTH ? 9999 : 800);
            }
            return;
        }
        
        Object2DoubleMap<RPGStat> statsIncrease = new Object2DoubleArrayMap<>();
        
        if (level > 1) {
            for (RPGStat stat : RPGStat.values()) {
                ModifiableAttributeInstance attribute = entity.getAttribute(stat.attribute);
                double[] statTable = stat.lvlIncreaseTable;
                attribute.removePermanentModifier(stat.modifierId);
                double increase = IntStream.range(1, level).mapToDouble(i -> statTable[i]).sum();
                if (increase > 0) {
                    double baseStat = getBaseStat(stat);
                    if (baseStat > statTable[0] || stat == RPGStat.HEALTH) {
                        increase = increase / statTable[0] * baseStat;
                    }
                    increase *= StatsTable.statMultiplier(entity, stat);

                    statsIncrease.put(stat, increase);
                }
            }
        }
        
        if (!standLevelingStats.isEmpty()) {
            ResourceLocation entityStandId = IStandPower.getStandPowerOptional(entity).resolve().map(stand -> { // why does LazyOptional#map throw an exception when the mapper returns null (which isn't the case for Java's Optional)??
                return stand.hasPower() ? stand.getType().getRegistryName() : null;
            }).orElse(null);
            for (Map.Entry<ResourceLocation, Object2DoubleMap<RPGStat>> entry : standLevelingStats.entrySet()) {
                ResourceLocation entryStand = entry.getKey();
                Map<RPGStat, Double> standStats = entry.getValue();
                if (standStats != null) {
                    for (Map.Entry<RPGStat, Double> statEntry : standStats.entrySet()) {
                        double increase = statEntry.getValue();
                        if (increase > 0) {
                            if (entityStandId == null || !entryStand.equals(entityStandId)) {
                                increase *= StatsTable.CHANGED_STAND_MULTIPLIER;
                            }
                            statsIncrease.put(statEntry.getKey(), statsIncrease.getDouble(statEntry.getKey()) + increase);
                        }
                    }
                }
            }
        }
        
        for (Object2DoubleMap.Entry<RPGStat> entry : statsIncrease.object2DoubleEntrySet())  {
            ModifiableAttributeInstance attribute = entity.getAttribute(entry.getKey().attribute);
            if (attribute != null) {
                attribute.addPermanentModifier(new AttributeModifier(entry.getKey().modifierId, "", entry.getDoubleValue(), Operation.ADDITION));
            }
        }
        

        fixHpSaved = OptionalFloat.of(entity.getMaxHealth());
        entity.setHealth(entity.getMaxHealth());
    }
    
    public double getBaseStat(RPGStat stat) {
        ModifiableAttributeInstance attribute = entity.getAttribute(stat.attribute);
        if (attribute == null) {
            return 0;
        }
        return attribute.getBaseValue();
    }
    
    private double getStatFromLevelOnly(RPGStat stat) {
        ModifiableAttributeInstance attribute = entity.getAttribute(stat.attribute);
        if (attribute == null) {
            return 0;
        }
        double value = attribute.getBaseValue();
        AttributeModifier modifier = attribute.getModifier(stat.modifierId);
        if (modifier != null) {
            switch (modifier.getOperation()) {
            case ADDITION:
                value += modifier.getAmount();
                break;
            case MULTIPLY_BASE:
                value += value * modifier.getAmount();
                break;
            case MULTIPLY_TOTAL:
                value *= 1 + modifier.getAmount();
                break;
            }
        }
        return value;
    }
    
    private Party party;
    public void setParty(Party party) {
        this.party = party;
    }
    
    @Nullable
    public Party getParty() {
        if (entity.level.isClientSide() && entity == ClientUtil.getClientPlayer()) {
            return ClientStuff.currentParty.orElse(null);
        }
        return party;
    }
    
    
    
    public boolean usesStamina() {
        return IStandPower.getStandPowerOptional(entity).map(IPower::hasPower).orElse(false);
    }
    
    public double getStaminaPoints() {
        return staminaPoints;
    }
    
    public void setStaminaPoints(double sp) {
        sp = MathHelper.clamp(sp, 0, getMaxStaminaPoints());
        this.staminaPoints = sp;
        if (!entity.level.isClientSide()) {
            ModNetworkManager.sendToClientsTrackingAndSelf(new StaminaPointsPacket(entity.getId(), sp), entity);
        }
    }
    
    public double getMaxStaminaPoints() {
        if (entity.getAttributes().hasAttribute(ModEntityAttributes.STAND_STAMINA.get())) {
            return entity.getAttributeValue(ModEntityAttributes.STAND_STAMINA.get());
        }
        
        return 0;
    }
    
    
    
    public void onMobSpawn() {
        if (!entity.level.isClientSide() && !mobLvlInitialized && entity instanceof MobEntity) {
            updateMobLevel();
        }
    }
    
    public void updateMobLevel() {
        DifficultyInstance difficulty = entity.level.getCurrentDifficultyAt(entity.blockPosition());
        Optional<EntityRPGData> nearbyPlayer = Optional.ofNullable(getNearestPlayer(entity.position(), (ServerWorld) entity.level))
                .flatMap(EntityRPGData::get);
        
        if (!mobLvlInitialized) {
            if (entity.getType() == ModEntityTypes.BOWMAN.get() || entity.getType() == EntityType.ENDER_DRAGON) {
                this.mobSpawnLevel = StatsTable.MAX_LVL;
            }
            else if (entity.getType() == EntityType.WITHER) {
                this.mobSpawnLevel = StatsTable.MAX_LVL - 10;
            }
            else {
                Optional<Integer> sameClassMobMaxLvl = Battle.getPotentialTeammates(entity, entity.getClass()).stream()
                        .map(EntityRPGData::get)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(data -> data.mobLvlInitialized)
                        .map(EntityRPGData::getLevel)
                        .max(Integer::compare);
                this.mobSpawnLevel = 
                        sameClassMobMaxLvl
                        .orElse(ChunkLevel.getChunkLevel(entity.level.getChunkAt(entity.blockPosition())).map(chunk -> chunk.getRandomLevel(difficulty.getEffectiveDifficulty()))
                        .orElse(nearbyPlayer.map(EntityRPGData::getLevel)
                        .orElse(1)));
//                if (!sameClassMobMaxLvl.isPresent()) {
//                    boolean inStructure = false;
//                    if (inStructure) {
//                        this.mobSpawnLevel = Math.min(mobSpawnLevel + 5, StatsTable.MAX_LVL);
//                    }
//                }
            }
        }
        
        if (difficulty.getDifficulty() == Difficulty.PEACEFUL) {
            this.level = 1;
        }
        else {
            this.level = this.mobSpawnLevel;
            nearbyPlayer.ifPresent(playerLevel -> {
                switch (difficulty.getDifficulty()) {
                case EASY:
                    if (this.level > playerLevel.getLevel()) {
                        this.level = Math.max(playerLevel.getLevel() + (this.level - playerLevel.getLevel()) / 5, 1);
                    }
                    break;
                case HARD:
                    this.level = Math.max(playerLevel.getLevel() - 2, this.level);
                    break;
                default:
                    break;
                }
            });
        }
        if (mobLvlInitialized) {
            ModNetworkManager.sendToClientsTracking(new RPGDataPacket(this, entity.getId()), entity);
        }
        
        statAttributeBuffs();
        fixHpSaved = OptionalFloat.of(entity.getMaxHealth());
        entity.setHealth(entity.getMaxHealth());
        mobLvlInitialized = true;
    }
    
    private static PlayerEntity getNearestPlayer(Vector3d pos, ServerWorld world) {
        return world.getPlayers(ServerPlayerEntity::isAlive).stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(pos)))
                .orElse(null);
    }
    
    
    
    public void saveOnClone(EntityRPGData old) {
        this.level = old.level;
        this.xpOnLevel = old.xpOnLevel;
        this.staminaPoints = old.staminaPoints;
        this.bowmanEvent = old.bowmanEvent;
        this.standLevels = old.standLevels;
        this.standLevelingStats = old.standLevelingStats;
        this.lastStandUserDaySpawn = old.lastStandUserDaySpawn;
    }
    
    public void toBuf(PacketBuffer buf) {
        buf.writeVarInt(level);
        buf.writeInt(xpOnLevel);
        buf.writeDouble(staminaPoints);
        
        NetworkUtil.writeCollection(buf, standLevels.object2FloatEntrySet(), entry -> {
            buf.writeResourceLocation(entry.getKey());
            buf.writeFloat(entry.getFloatValue());
        }, false);
    }
    
    public void fromBuf(PacketBuffer buf) {
        level = buf.readVarInt();
        xpOnLevel = buf.readInt();
        staminaPoints = buf.readDouble();
        
        this.standLevels.clear();
        NetworkUtil.readCollection(buf, () -> {
            ResourceLocation standId = buf.readResourceLocation();
            float level = buf.readFloat();
            return Pair.of(standId, level);
        }).stream().forEach(pair -> {
            standLevels.put(pair.getKey(), pair.getValue().floatValue());
        });;
    }
    
    
    public void syncWithPlayer(ServerPlayerEntity asPlayer) {
        ModNetworkManager.sendToClient(new RPGDataPacket(this, asPlayer.getId()), asPlayer);
        statAttributeBuffs();
    }
    
    
    
    public INBT toNBT() {
        CompoundNBT nbt = new CompoundNBT();
        if (mobLvlInitialized) {
            nbt.putInt("MobLevel", mobSpawnLevel);
        }
        nbt.putInt("Level", level);
        nbt.putInt("Xp", xpOnLevel);
        nbt.putDouble("Sp", staminaPoints);
        if (bowmanEvent != null) {
            nbt.putBoolean("Bowman", bowmanEvent);
        }
        nbt.putLong("Day", lastStandUserDaySpawn);
        
        CompoundNBT standLeveling = new CompoundNBT();
        for (Object2FloatMap.Entry<ResourceLocation> entry : standLevels.object2FloatEntrySet()) {
            standLeveling.putFloat(entry.getKey().toString(), entry.getFloatValue());
        }
        nbt.put("StandLvl", standLeveling);
        
        CompoundNBT standStats = new CompoundNBT();
        for (Map.Entry<ResourceLocation, Object2DoubleMap<RPGStat>> entry : this.standLevelingStats.entrySet()) {
            Map<RPGStat, Double> statsMap = entry.getValue();
            if (statsMap != null) {
                CompoundNBT thisStandStats = new CompoundNBT();
                for (Map.Entry<RPGStat, Double> eachStatEntry : statsMap.entrySet()) {
                    thisStandStats.putDouble(eachStatEntry.getKey().name(), eachStatEntry.getValue());
                }
                standStats.put(entry.getKey().toString(), thisStandStats);
            }
        }
        nbt.put("StandStats", standStats);
        
        // fine, I'll do it myself
        nbt.putFloat("Hp", entity.getHealth());
        return nbt;
    }
    
    public void fromNBT(INBT inbt) {
        CompoundNBT nbt = (CompoundNBT) inbt;
        mobLvlInitialized = nbt.contains("MobLevel");
        if (mobLvlInitialized) {
            mobSpawnLevel = nbt.getInt("MobLevel");
        }
        level = MathHelper.clamp(nbt.getInt("Level"), 1, StatsTable.MAX_LVL);
        xpOnLevel = nbt.getInt("Xp");
        staminaPoints = nbt.getDouble("Sp");
        if (nbt.contains("Bowman")) {
            bowmanEvent = nbt.getBoolean("Bowman");
        }
        lastStandUserDaySpawn = nbt.getLong("Day");
        
        if (nbt.contains("StandLvl")) {
            CompoundNBT standLeveling = nbt.getCompound("StandLvl");
            for (String key : standLeveling.getAllKeys()) {
                standLevels.put(new ResourceLocation(key), standLeveling.getFloat(key));
            }
        }
        
        if (nbt.contains("StandStats")) {
            CompoundNBT standStats = nbt.getCompound("StandStats");
            for (String key : standStats.getAllKeys()) {
                Object2DoubleMap<RPGStat> statsMap = new Object2DoubleArrayMap<>();
                CompoundNBT thisStandStats = standStats.getCompound(key);
                for (RPGStat stat : RPGStat.values()) {
                    double statValue = thisStandStats.getDouble(stat.name());
                    if (statValue > 0) {
                        statsMap.put(stat, statValue);
                    }
                }
                if (!statsMap.isEmpty()) {
                    this.standLevelingStats.put(new ResourceLocation(key), statsMap);
                }
            }
        }
        
        float hp = nbt.getFloat("Hp");
        if (hp > 0) {
            fixHpSaved = OptionalFloat.of(hp);
        }
    }
}
