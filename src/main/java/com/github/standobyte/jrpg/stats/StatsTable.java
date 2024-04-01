package com.github.standobyte.jrpg.stats;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.util.ResourceLocation;

public class StatsTable {
    public static final int MAX_LVL = 100;
    
    public static final int[] XP_TO_NEXT_LVL = {
            0,      7,      9,      14,     24,     40,     70,     100,    150,    200,
            300,    360,    480,    600,    780,    960,    1200,   1500,   1800,   2400,
            3750,   4500,   5250,   6000,   7500,   9000,   10500,  12000,  13500,  15000,
            15750,  16500,  17550,  18600,  19950,  21300,  22950,  24600,  27300,  30000,
            41700,  43800,  46300,  49200,  52500,  56200,  60300,  64800,  69700,  75000,
            76500,  78800,  81300,  84200,  87500,  91200,  95300,  99800,  104700, 110000,
            112200, 115000, 118400, 122400, 127000, 132200, 138000, 144400, 151400, 160000,
            163300, 167200, 171700, 176800, 182500, 188800, 195700, 203200, 211300, 220000,
            336600, 344400, 353400, 363600, 375000, 387600, 401400, 416400, 432600, 450000,
            460950, 474150, 489600, 507300, 527250, 549450, 573900, 600600, 629250, 660000
    };
    
    public static final int[] XP_DROP = {
            1,     2,     3,     4,     5,     6,     7,     10,    15,    20, 
            25,    30,    40,    50,    65,    80,    100,   125,   150,   200, 
            250,   300,   350,   400,   500,   600,   700,   800,   900,   1000, 
            1050,  1100,  1170,  1240,  1330,  1420,  1530,  1640,  1820,  2000, 
            2085,  2190,  2315,  2460,  2625,  2810,  3015,  3240,  3485,  3750, 
            3825,  3940,  4065,  4210,  4375,  4560,  4765,  4990,  5235,  5500, 
            5610,  5750,  5920,  6120,  6350,  6610,  6900,  7220,  7570,  8000, 
            8165,  8360,  8585,  8840,  9125,  9440,  9785,  10160, 10565, 11000, 
            11220, 11480, 11780, 12120, 12500, 12920, 13380, 13880, 14420, 15000, 
            15365, 15805, 16320, 16910, 17575, 18315, 19130, 20020, 20975, 22000
    };
    
    public static final double[] HP_GAIN = {
            20, 4,  4,  4,  4,  6,  6,  6,  6,  6,
            8,  8,  8,  8,  8,  10, 10, 10, 10, 10,
            12, 12, 12, 12, 12, 14, 14, 14, 14, 14,
            16, 16, 16, 16, 16, 18, 18, 18, 18, 18,
            20, 20, 20, 20, 20, 22, 22, 22, 22, 22,
            24, 24, 24, 24, 24, 26, 26, 26, 26, 26,
            28, 28, 28, 28, 28, 30, 30, 30, 30, 30,
            32, 32, 32, 32, 32, 34, 34, 34, 34, 34,
            36, 36, 36, 36, 36, 38, 38, 38, 38, 38,
            40, 40, 40, 40, 40, 42, 42, 42, 42, 42 // 2316
    };
    
    public static final double[] STRENGTH_GAIN = {
            4,   1,   0,   1,   1,   1,   1,   1,   1,   1,
            1,   1,   1,   1,   1,   1,   1.5, 1.5, 1.5, 1.5,
            1.5, 1.5, 1.5, 1.5, 2,   2,   2,   2,   2,   2,
            2,   2,   2.5, 2.5, 2.5, 2.5, 2.5, 2.5, 3,   3,
            3,   3,   3,   3,   4,   4,   4,   4,   4,   4,
            5,   5,   5,   5,   5,   6,   6,   6,   6,   6,
            7,   7,   7,   7,   7,   7,   7,   7,   7,   8,
            8,   8,   8,   8,   8,   8,   8,   8,   8,   9,
            9,   9,   9,   9,   9,   9,   9,   9,   9,   10,
            10,  10,  10,  10,  10,  10,  10,  10,  10,  10 // 501
    };
    
    public static final double[] SPEED_GAIN = {
            4,  0,  1,  0,  2,  1,  1,  1,  0,  0,
            1,  1,  1,  2,  2,  2,  2,  2,  2,  2,
            3,  3,  3,  3,  3,  3,  3,  4,  4,  4,
            4,  4,  4,  4,  4,  5,  5,  5,  5,  5,
            6,  6,  6,  6,  6,  7,  7,  7,  7,  7,
            8,  8,  8,  8,  8,  8,  8,  8,  8,  8,
            9,  9,  9,  9,  9,  9,  9,  9,  9,  9,
            9,  9,  9,  9,  9,  9,  9,  9,  9,  9,
            10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
            10, 10, 10, 10, 10, 10, 10, 10, 10, 10 // 630
    };
    
    public static final double[] DURABILITY_GAIN = {
            4,   0,  0,  1,  1,  1,   0,   1,   0,   2,
            1,   1,  1,  1,  1,  1.5, 1.5, 1.5, 1.5, 1.5,
            1.5, 2,  2,  2,  2,  2,   2,   3,   3,   3,
            3,   3,  4,  4,  4,  4,   4,   5,   5,   5,
            5,   5,  5,  5,  5,  5,   6,   6,   6,   6,
            6,   6,  6,  6,  6,  6,   6,   7,   7,   7,
            7,   7,  7,  7,  7,  7,   8,   8,   8,   8,
            8,   8,  9,  9,  9,  9,   9,   9,   9,   9,
            9,   9,  10, 10, 10, 10,  10,  10,  10,  10,
            10,  10, 10, 10, 10, 10,  10,  10,  10,  10 // 575
    };
    
    public static final double[] PRECISION_GAIN = {
            4,   0,   0,   1,   2,   1,   1,   1,   0,   0,
            0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 1,   1,   1,   1,
            1,   1,   1,   1,   1,   1,   1,   1.5, 1.5, 1.5,
            1.5, 1.5, 1.5, 1.5, 1.5, 2,   2,   2,   2,   2,
            2,   3,   3,   3,   3,   3,   3,   3,   4,   4,
            4,   4,   4,   4,   4,   5,   5,   5,   5,   5,
            6,   6,   6,   6,   6,   6,   6,   7,   7,   7,
            7,   7,   7,   7,   8,   8,   8,   8,   8,   8,
            9,   9,   9,   9,   9,   9,   9,   10,  10,  10,
            10,  10,  10,  10,  10,  10,  10,  10,  10,  10 // 454
    };
    
    public static final double[] SPIRIT_GAIN = {
            4,   0,   0,   0,   0,   0,   0,   0,   0,   6,
            1,   1,   1,   1,   1,   1,   1,   1,   1,   1,
            1,   1,   1,   1,   1,   1,   1,   1,   1,   1,
            1,   1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 1.5, 2,
            2,   2,   2,   2,   2,   2,   2,   2,   2,   2,
            2,   2,   2,   2,   2,   2,   2,   2,   2,   2,
            3,   3,   3,   3,   3,   3,   3,   3,   3,   3,
            3,   3,   4,   4,   4,   4,   4,   4,   4,   5,
            4,   5,   5,   4,   5,   5,   5,   6,   6,   6,
            6,   6,   6,   7,   7,   7,   7,   7,   7,   7 // 272
    };
    
    public static final double[] SP_GAIN = {
            16, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            2,  2, 2, 2, 2, 2, 2, 2, 2, 2,
            2,  2, 2, 2, 2, 2, 2, 2, 2, 2,
            3,  3, 3, 3, 3, 3, 3, 3, 3, 3,
            3,  3, 3, 3, 3, 3, 3, 3, 3, 3,
            3,  3, 3, 3, 3, 3, 3, 3, 3, 3,
            4,  4, 4, 4, 4, 4, 4, 4, 4, 4,
            4,  4, 4, 4, 4, 4, 4, 4, 4, 4,
            4,  4, 4, 4, 4, 4, 4, 4, 4, 4,
            4,  4, 4, 4, 4, 4, 4, 4, 4, 4 // 315

    };
    
    
    public static final int[] STATS_ON_STAND_LEVEL = {
            7,   10,  14,  17,  21,  24,  28,  31,  35,  38, 
            42,  45,  49,  52,  56,  59,  63,  66,  70,  73
    };
    public static final double CHANGED_STAND_MULTIPLIER = 0.25;
    
    
    private static void initEntitySpecificStats() {
        STAT_MULTIPLIERS = new HashMap<>();
        STAT_MULTIPLIERS_COND = new ArrayList<>();
        statMult(EntityType.WOLF, 
                RPGStat.STRENGTH, 0.75,
                RPGStat.SPEED, 1.5,
                RPGStat.PRECISION, 1.5);
        statMult(entity -> entity instanceof SpiderEntity, 
                RPGStat.SPEED, 1.5);
        statMult(entity -> entity instanceof SkeletonEntity, 
                RPGStat.PRECISION, 1.5);
        statMult(entity -> entity instanceof ZombieEntity, 
                RPGStat.DURABILITY, 1.5);
        statMult(entity -> entity instanceof CreeperEntity, 
                RPGStat.HEALTH, 1.5,
                RPGStat.SPEED, 0.5);
        statMult(entity -> entity instanceof SlimeEntity, 
                RPGStat.SPEED, 0.35,
                RPGStat.PRECISION, 0.6);
        statMult(EntityType.BLAZE, 
                RPGStat.SPIRIT, 1.5);
        statMult(EntityType.ILLUSIONER, 
                RPGStat.STRENGTH, 0.5, 
                RPGStat.DURABILITY, 0.75, 
                RPGStat.SPIRIT, 2);
        statMult(EntityType.EVOKER, 
                RPGStat.STRENGTH, 0.5, 
                RPGStat.DURABILITY, 0.75, 
                RPGStat.SPIRIT, 2);
        statMult(EntityType.WITCH, 
                RPGStat.STRENGTH, 0.25, 
                RPGStat.DURABILITY, 0.5, 
                RPGStat.PRECISION, 0.75, 
                RPGStat.SPIRIT, 2.5);

        STAND_STAT_MULTIPLIERS = new HashMap<>();
        standStats(new ResourceLocation("jojo", "star_platinum"), 
                RPGStat.STRENGTH, 0.25, 
                RPGStat.SPEED, 0.25, 
                RPGStat.DURABILITY, 0.1, 
                RPGStat.PRECISION, 0.3, 
                RPGStat.SPIRIT, 0.05, 
                RPGStat.STAND_STAMINA, 0.05);
    }
    
    
    private static Map<ResourceLocation, EnumMap<RPGStat, Double>> STAT_MULTIPLIERS;
    private static List<Pair<Predicate<LivingEntity>, EnumMap<RPGStat, Double>>> STAT_MULTIPLIERS_COND;
    private static Map<ResourceLocation, EnumMap<RPGStat, Double>> STAND_STAT_MULTIPLIERS;
    
    private static final Random RANDOM = new Random();
    public static RPGStat getRandomStatInc(ResourceLocation standId) {
        EnumMap<RPGStat, Double> map = STAND_STAT_MULTIPLIERS.get(standId);
        if (map != null) {
            double randomD = RANDOM.nextDouble();
            for (Map.Entry<RPGStat, Double> entry : map.entrySet()) {
                if (randomD < entry.getValue()) {
                    return entry.getKey();
                }
                randomD -= entry.getValue();
            }
        }
        
        return RPGStat.values()[RANDOM.nextInt(RPGStat.values().length - 1) + 1];
    }
    
    
    
    public static double statMultiplier(LivingEntity entity, RPGStat stat) {
        if (STAT_MULTIPLIERS == null) {
            initEntitySpecificStats();
        }
        
        double multiplier = 1;
        EnumMap<RPGStat, Double> multipliers = STAT_MULTIPLIERS.get(entity.getType().getRegistryName());
        if (multipliers != null && multipliers.containsKey(stat)) {
            multiplier *= multipliers.get(stat);
        }
        
        for (Pair<Predicate<LivingEntity>, EnumMap<RPGStat, Double>> conditional : STAT_MULTIPLIERS_COND) {
            if (conditional.getRight().containsKey(stat) && conditional.getLeft().test(entity)) {
                multiplier *= conditional.getRight().get(stat);
            }
        }
        
        return multiplier;
    }
    
    
    
    
    
    @SuppressWarnings("unused")
    private static void statMult(EntityType<?> entityType, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4, 
            RPGStat stat5, double value5, 
            RPGStat stat6, double value6, 
            RPGStat stat7, double value7
            ) {
        EnumMap<RPGStat, Double> map = STAT_MULTIPLIERS.computeIfAbsent(entityType.getRegistryName(), key -> new EnumMap<>(RPGStat.class));
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        map.put(stat5, value5);
        map.put(stat6, value6);
        map.put(stat7, value7);
    }

    @SuppressWarnings("unused")
    private static void statMult(EntityType<?> entityType, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4, 
            RPGStat stat5, double value5, 
            RPGStat stat6, double value6) {
        EnumMap<RPGStat, Double> map = STAT_MULTIPLIERS.computeIfAbsent(entityType.getRegistryName(), key -> new EnumMap<>(RPGStat.class));
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        map.put(stat5, value5);
        map.put(stat6, value6);
    }

    @SuppressWarnings("unused")
    private static void statMult(EntityType<?> entityType, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4, 
            RPGStat stat5, double value5) {
        EnumMap<RPGStat, Double> map = STAT_MULTIPLIERS.computeIfAbsent(entityType.getRegistryName(), key -> new EnumMap<>(RPGStat.class));
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        map.put(stat5, value5);
    }

    @SuppressWarnings("unused")
    private static void statMult(EntityType<?> entityType, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4) {
        EnumMap<RPGStat, Double> map = STAT_MULTIPLIERS.computeIfAbsent(entityType.getRegistryName(), key -> new EnumMap<>(RPGStat.class));
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
    }

    @SuppressWarnings("unused")
    private static void statMult(EntityType<?> entityType, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3) {
        EnumMap<RPGStat, Double> map = STAT_MULTIPLIERS.computeIfAbsent(entityType.getRegistryName(), key -> new EnumMap<>(RPGStat.class));
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
    }

    @SuppressWarnings("unused")
    private static void statMult(EntityType<?> entityType, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2) {
        EnumMap<RPGStat, Double> map = STAT_MULTIPLIERS.computeIfAbsent(entityType.getRegistryName(), key -> new EnumMap<>(RPGStat.class));
        map.put(stat1, value1);
        map.put(stat2, value2);
    }

    @SuppressWarnings("unused")
    private static void statMult(EntityType<?> entityType, 
            RPGStat stat1, double value1) {
        EnumMap<RPGStat, Double> map = STAT_MULTIPLIERS.computeIfAbsent(entityType.getRegistryName(), key -> new EnumMap<>(RPGStat.class));
        map.put(stat1, value1);
    }
    
    @SuppressWarnings("unused")
    private static void statMult(Predicate<LivingEntity> entityCondition, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4, 
            RPGStat stat5, double value5, 
            RPGStat stat6, double value6, 
            RPGStat stat7, double value7
            ) {
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        map.put(stat5, value5);
        map.put(stat6, value6);
        map.put(stat7, value7);
        STAT_MULTIPLIERS_COND.add(Pair.of(entityCondition, map));
    }
    
    @SuppressWarnings("unused")
    private static void statMult(Predicate<LivingEntity> entityCondition, 
            RPGStat stat1, double value1
            ) {
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        STAT_MULTIPLIERS_COND.add(Pair.of(entityCondition, map));
    }
    
    @SuppressWarnings("unused")
    private static void statMult(Predicate<LivingEntity> entityCondition, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2
            ) {
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        map.put(stat2, value2);
        STAT_MULTIPLIERS_COND.add(Pair.of(entityCondition, map));
    }
    
    @SuppressWarnings("unused")
    private static void statMult(Predicate<LivingEntity> entityCondition, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3
            ) {
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        STAT_MULTIPLIERS_COND.add(Pair.of(entityCondition, map));
    }
    
    @SuppressWarnings("unused")
    private static void statMult(Predicate<LivingEntity> entityCondition, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4
            ) {
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        STAT_MULTIPLIERS_COND.add(Pair.of(entityCondition, map));
    }
    
    @SuppressWarnings("unused")
    private static void statMult(Predicate<LivingEntity> entityCondition, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4, 
            RPGStat stat5, double value5
            ) {
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        map.put(stat5, value5);
        STAT_MULTIPLIERS_COND.add(Pair.of(entityCondition, map));
    }
    
    @SuppressWarnings("unused")
    private static void statMult(Predicate<LivingEntity> entityCondition, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4, 
            RPGStat stat5, double value5, 
            RPGStat stat6, double value6
            ) {
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        map.put(stat5, value5);
        map.put(stat6, value6);
        STAT_MULTIPLIERS_COND.add(Pair.of(entityCondition, map));
    }
    
    @SuppressWarnings("unused")
    private static void standStats(ResourceLocation standId, 
            RPGStat stat1, double value1, 
            RPGStat stat2, double value2, 
            RPGStat stat3, double value3, 
            RPGStat stat4, double value4, 
            RPGStat stat5, double value5, 
            RPGStat stat6, double value6
            ) {
        double sum = value1 + value2 + value3 + value4 + value5 + value6;
        value1 /= sum;
        value2 /= sum;
        value3 /= sum;
        value4 /= sum;
        value5 /= sum;
        value6 /= sum;
        EnumMap<RPGStat, Double> map = new EnumMap<>(RPGStat.class);
        map.put(stat1, value1);
        map.put(stat2, value2);
        map.put(stat3, value3);
        map.put(stat4, value4);
        map.put(stat5, value5);
        map.put(stat6, value6);
        STAND_STAT_MULTIPLIERS.put(standId, map);
    }
    
}
