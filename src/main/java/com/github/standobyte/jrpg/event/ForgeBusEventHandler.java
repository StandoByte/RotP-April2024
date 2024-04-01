package com.github.standobyte.jrpg.event;

import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

import com.github.standobyte.jojo.entity.itemprojectile.StandArrowEntity;
import com.github.standobyte.jojo.util.mc.reflection.CommonReflection;
import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.capability.chunk.ChunkLevel;
import com.github.standobyte.jrpg.capability.chunk.ChunkLevelProvider;
import com.github.standobyte.jrpg.capability.world.DimensionFirstChunkProvider;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.combat.action.PlayerBowAction;
import com.github.standobyte.jrpg.entity.BowmanEntity;
import com.github.standobyte.jrpg.entity.MobStandUserEntity;
import com.github.standobyte.jrpg.init.ModEffects;
import com.github.standobyte.jrpg.init.ModEntityAttributes;
import com.github.standobyte.jrpg.init.ModEntityTypes;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.network.packets.server.ChunkLevelPacket;
import com.github.standobyte.jrpg.network.packets.server.RPGDataPacket;
import com.github.standobyte.jrpg.party.Party;
import com.github.standobyte.jrpg.server.ServerBattlesProvider;
import com.github.standobyte.jrpg.stats.EntityRPGData;
import com.github.standobyte.jrpg.stats.RPGStat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.DifficultyChangeEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.SpecialSpawn;
import net.minecraftforge.event.entity.living.PotionEvent.PotionAddedEvent;
import net.minecraftforge.event.entity.living.PotionEvent.PotionRemoveEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = ModMain.MOD_ID)
public class ForgeBusEventHandler {
    
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        DamageSource dmgSource = event.getSource();
        if (dmgSource.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) dmgSource.getEntity();
            EntityCombat attackerData = EntityCombat.get(attacker).get();
            LivingEntity target = event.getEntityLiving();
            EntityCombat targetData = EntityCombat.get(target).get();
            
            Party attackerParty = EntityRPGData.get(attacker).map(d -> d.getParty()).orElse(null);
            Party defenderParty = EntityRPGData.get(target).map(d -> d.getParty()).orElse(null);
            if (attackerParty != null && attackerParty == defenderParty) {
                event.setCanceled(true);
                return;
            }
            
            if (attackerData.getBattle() == targetData.getBattle()) {
                if (attackerData.getBattle() == null) {
                    if (!target.level.isClientSide() && !(target instanceof PlayerEntity && ((PlayerEntity) target).isCreative())) {
                        event.setCanceled(true);
                        Battle battle = Battle.createBattleOnAttack(attacker, target);
                        battle.startBattle();
                    }
                }
                else {
                    boolean cancelAttack = !(dmgSource.getDirectEntity() instanceof ProjectileEntity);
                    if (!target.level.isClientSide()) {
                        Battle battle = attackerData.getBattle();
                        if (battle.getCurrentTurnEntity() != attacker) {
                            if (cancelAttack) event.setCanceled(true);
                        }
                        else {
                            CombatAction action = battle.getCurrentAction(attacker);
                            if (action != null && action.isActionOver()) {
                                if (cancelAttack) event.setCanceled(true);
                            }
                            else {
                                if (attacker instanceof SlimeEntity && (battle.getCurrentAction(attacker) == null || battle.getCurrentAction(attacker).isActionOver())) {
                                    if (cancelAttack) event.setCanceled(true);
                                }
                                
                                double targetPrecision = target.getAttributes().hasAttribute(ModEntityAttributes.PRECISION.get()) ? target.getAttributeValue(ModEntityAttributes.PRECISION.get()) : 0;
                                double attackerPrecision = attacker.getAttributes().hasAttribute(ModEntityAttributes.PRECISION.get()) ? attacker.getAttributeValue(ModEntityAttributes.PRECISION.get()) : 0;
                                
                                double dodgeChance = MathHelper.clamp(0.05 + (targetPrecision - attackerPrecision) / 300, 0, 1);
                                if (target.getRandom().nextDouble() < dodgeChance) {
                                    double x = attacker.getX() - target.getX();
                                    double z;
                                    for (z = attacker.getZ() - target.getZ(); x * x + z * z < 1.0E-4D; z = (Math.random() - Math.random()) * 0.01D) {
                                        x = (Math.random() - Math.random()) * 0.01D;
                                    }
                                    target.knockback(0.5f, x, z);
                                    event.setCanceled(true);
                                    
                                    battle.sendMessage(new TranslationTextComponent("jrpg.dodge", target.getDisplayName()));
                                }
                                
                                battle.onAttack(attacker, target, dmgSource, event.getAmount());
                            }
                        }
                    }
                }
            }
            else {
                event.setCanceled(true);
            }
        }
    }
    
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        DamageSource dmgSource = event.getSource();
        if (dmgSource.getEntity() instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) dmgSource.getEntity();
            EntityCombat attackerData = EntityCombat.get(attacker).get();
            LivingEntity target = event.getEntityLiving();
            EntityCombat targetData = EntityCombat.get(target).get();
            
            if (attackerData.getBattle() == targetData.getBattle()) {
                if (attackerData.getBattle() != null) {
                    if (!target.level.isClientSide()) {
                        double attackerPrecision = attacker.getAttributes().hasAttribute(ModEntityAttributes.PRECISION.get()) ? attacker.getAttributeValue(ModEntityAttributes.PRECISION.get()) : 0;
                        
                        double critChance = Math.min(0.05 + attackerPrecision / 1500, 1);
                        if (target.getRandom().nextDouble() < critChance) {
                            targetData.getBattle().sendMessage(new TranslationTextComponent("jrpg.crit"));
                            event.setAmount(event.getAmount() * 2);
                        }
                        
                        attackerData.getBattle().onAttack(attacker, target, dmgSource, event.getAmount());
                    }
                }
            }
            else {
                event.setCanceled(true);
            }
        }
    }
    
    @SubscribeEvent
    public static void statsIncreaseDamage(LivingHurtEvent event) {
        DamageSource src = event.getSource();
        String id = src.msgId;
        if (src.getEntity() instanceof LivingEntity && 
                (src instanceof IndirectEntityDamageSource || !("mob".equals(id) || "player".equals(id)))) {
            LivingEntity attacker = (LivingEntity) src.getEntity();
            EntityRPGData.get(attacker).ifPresent(statsData -> {
                RPGStat stat = src.isBypassArmor() || src.isFire() || src.isMagic() ? RPGStat.STRENGTH : RPGStat.SPIRIT;
                double statValue = attacker.getAttributes().hasAttribute(stat.attribute) ? attacker.getAttributeValue(stat.attribute) : 0;
                if (statValue > 0) {
                    event.setAmount((float) (event.getAmount() / 4 * statValue));
                }
                else {
                    event.setAmount(0);
                    event.setCanceled(true);
                }
            });
        }
    }
    
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void statsReduceDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntityLiving();
        double stat = 0;
        if (event.getSource().isBypassArmor() || event.getSource().isFire() || event.getSource().isMagic()) {
            stat = target.getAttributes().hasAttribute(ModEntityAttributes.SPIRIT.get()) ? 
                    target.getAttributeValue(ModEntityAttributes.SPIRIT.get()) / 2000 : 0;
        }
        else {
            stat = target.getAttributes().hasAttribute(ModEntityAttributes.DURABILITY.get()) ? 
                    target.getAttributeValue(ModEntityAttributes.DURABILITY.get()) / 1000 : 0;
        }
        
        if (stat > 0) {
            event.setAmount((float) (event.getAmount() * Math.max(1 - stat, 0)));
            
            if (target.getType() == EntityType.PLAYER && event.getSource().getEntity() != null
                    && event.getSource().getEntity().getType() == ModEntityTypes.BOWMAN.get()) {
                event.setAmount(Math.min(event.getAmount(), target.getHealth() - 1));
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!event.getEntity().level.isClientSide()) {
            LivingEntity target = event.getEntityLiving();
            EntityCombat targetData = EntityCombat.get(target).get();
            DamageSource dmgSource = event.getSource();
            
            if (targetData.getBattle() != null) {
                targetData.getBattle().onDamage(target, dmgSource, event.getAmount());
            }
            
            if (dmgSource.getEntity() instanceof BowmanEntity && dmgSource.getDirectEntity() instanceof StandArrowEntity) {
                ((BowmanEntity) dmgSource.getEntity()).disappearNextTurn = true;
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level.isClientSide()) {
            LivingEntity target = event.getEntityLiving();
            EntityCombat targetData = EntityCombat.get(target).get();
            
            if (target.getType() == EntityType.PLAYER && targetData.getBattle() != null && targetData.getBattle()
                    .getEnemies(target).stream().anyMatch(entity -> entity.getType() == ModEntityTypes.BOWMAN.get())) {
                event.setCanceled(true);
                target.setHealth(1);
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void healMultiplier(LivingHealEvent event) {
        event.setAmount(event.getAmount() * 10);
    }
    
    private static final int[][] POTION_LVL = new int[][] {
        new int[] { 0,  20 },
        new int[] { 15, 35 },
        new int[] { 25, 60 },
        new int[] { 45, 80 },
        new int[] { 55, Integer.MAX_VALUE }
    };
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void addPotions(LivingDropsEvent event) {
        if (event.getEntityLiving() instanceof MobEntity && event.getEntityLiving().getRandom().nextFloat() < 0.05F) {
            int mobLevel = EntityRPGData.get(event.getEntityLiving()).map(EntityRPGData::getLevel).orElse(-1);
            
            int[] possiblePotion = new int[POTION_LVL.length];
            int i = 0;
            for (int potionLevel = 0; potionLevel < POTION_LVL.length; ++potionLevel) {
                int[] bounds = POTION_LVL[potionLevel];
                if (mobLevel >= bounds[0] && mobLevel <= bounds[1]) {
                    possiblePotion[i++] = potionLevel;
                }
            }
            
            int potionLevel = possiblePotion[event.getEntityLiving().getRandom().nextInt(i)];
            boolean spPotion = event.getEntityLiving().getRandom().nextBoolean();
            Potion potion = null;
            switch (potionLevel) { // ffs
            case 0:
                potion = spPotion ? ModEffects.ENERGY_POTION.get() : Potions.HEALING;
                break;
            case 1:
                potion = spPotion ? ModEffects.ENERGY_STRONG_POTION.get() : Potions.STRONG_HEALING;
                break;
            case 2:
                potion = spPotion ? ModEffects.ENERGY_VERY_STRONG_POTION.get() : ModEffects.HEAL_VERY_STRONG_POTION.get();
                break;
            case 3:
                potion = spPotion ? ModEffects.ENERGY_MEGA_STRONG_POTION.get() : ModEffects.HEAL_MEGA_STRONG_POTION.get();
                break;
            case 4:
                potion = spPotion ? ModEffects.ENERGY_GIGA_STRONG_POTION.get() : ModEffects.HEAL_GIGA_STRONG_POTION.get();
                break;
            }
            if (potion != null) {
                ItemStack item = new ItemStack(Items.POTION);
                PotionUtils.setPotion(item, potion);
                ItemEntity potionDropped = event.getEntityLiving().spawnAtLocation(item);
                if (potionDropped != null) {
                    event.getDrops().add(potionDropped);
                }
            }
        }
    }
    
    
    
    @SubscribeEvent
    public static void serverTick(ServerTickEvent event) {
        switch (event.phase) {
        case START:
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            ServerBattlesProvider.getServerBattlesData(server).tick();
            break;
        case END:
            break;
        }
    }
    
    @SubscribeEvent
    public static void entityTick(LivingUpdateEvent event) {
        EntityRPGData.get(event.getEntityLiving()).ifPresent(EntityRPGData::tick);
        EntityCombat.get(event.getEntityLiving()).ifPresent(EntityCombat::tick);
    }
    
    
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getWorld().isClientSide()) {
            if (event.getWorld() instanceof World) {
                World world = (World) event.getWorld();
                world.getCapability(DimensionFirstChunkProvider.CAPABILITY).ifPresent(cap -> {
                    if (!cap.firstChunkInitialized() && event.getChunk().getPos() != null) {
                        cap.setFirstChunk(event.getChunk().getPos());
                    }
                });
            }
            
            IChunk chunk = event.getChunk();
            if (chunk instanceof Chunk) {
                ChunkLevel.getChunkLevel((Chunk) chunk).ifPresent(lvl -> {
                    ServerWorld serverWorld = (ServerWorld) event.getWorld();
                    lvl.initLevel(serverWorld);
                    ChunkLevelPacket packet = new ChunkLevelPacket(event.getChunk().getPos(), lvl.levelMin, lvl.levelMax);
                    for (ServerPlayerEntity player : serverWorld.players()) {
                        ModNetworkManager.sendToClient(packet, player);
                    }
                });
            }
        }
    }
    
    
    

    
    @SubscribeEvent
    public static void edgeCreeper(LivingUpdateEvent event) {
        if (event.getEntity() instanceof CreeperEntity) {
            CreeperEntity creeper = (CreeperEntity) event.getEntity();
            int swellDir = creeper.getSwellDir();
            if (swellDir > 0 && creeper.swell >= 8) {
                EntityCombat.get(creeper).ifPresent(combat -> {
                    if (combat.isInBattle()) {
                        CombatAction action = combat.getBattle().getCurrentAction(creeper);
                        if (action != null && !action.isActionOver()) {
                            action.setActionEnded(23);
                        }
                    }
                });
                
                creeper.swell -= 7;
                creeper.oldSwell -= 7;
            }
        }
    }
    
    @SubscribeEvent
    public static void onMobSpawn(SpecialSpawn event) {
        Entity entity = event.getEntity();
        if (!entity.level.isClientSide() && entity instanceof MobEntity) {
            EntityRPGData.get((LivingEntity) entity).ifPresent(data -> data.onMobSpawn());
        }
    }
    
    @SubscribeEvent
    public static void onMobJoinWorld(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide() && event.getEntity() instanceof MobEntity) {
            MobEntity mob = (MobEntity) event.getEntity();
            if (mob instanceof SlimeEntity) {
                SlimeEntity slime = (SlimeEntity) event.getEntity();
                Battle.aaaaa.values().forEach(slimeCheck -> {
                    if (slimeCheck.size == slime.getSize()) {
                        slimeCheck.joinedSlimes.add(slime);
                    }
                });
                if (slime.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) < 0.5) {
                    slime.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.5);
                }
            }
            
            
            
            Set<PrioritizedGoal> goals = CommonReflection.getGoalsSet(mob.targetSelector);
            OptionalInt targetPlayerPriority = OptionalInt.empty();
            for (PrioritizedGoal prGoal : goals) {
                Goal goal = prGoal.getGoal();
                if (goal instanceof NearestAttackableTargetGoal) {
                    NearestAttackableTargetGoal<?> targetGoal = (NearestAttackableTargetGoal<?>) goal;

                    if (!targetPlayerPriority.isPresent() && CommonReflection.getTargetClass(targetGoal) == PlayerEntity.class) {
                        targetPlayerPriority = OptionalInt.of(prGoal.getPriority());
                    }
                    
                    EntityPredicate selector = CommonReflection.getTargetConditions(targetGoal);
                    if (selector != null) {
                        Predicate<LivingEntity> oldPredicate = CommonReflection.getTargetSelector(selector);
                        Predicate<LivingEntity> sameBattlePredicate = target -> {
                            return EntityCombat.get(target).map(EntityCombat::getBattle).orElse(null)
                                    == EntityCombat.get(mob).map(EntityCombat::getBattle).orElse(null);
                        };
                        CommonReflection.setTargetConditions(targetGoal, new EntityPredicate().range(CommonReflection.getTargetDistance(targetGoal)).selector(
                                oldPredicate != null ? oldPredicate.and(sameBattlePredicate) : sameBattlePredicate));
                    }
                }
            }
            
            if (targetPlayerPriority.isPresent()) {
                NearestAttackableTargetGoal<?> targetGoal = new NearestAttackableTargetGoal<>(mob, MobStandUserEntity.class, true);
                
                EntityPredicate selector = CommonReflection.getTargetConditions(targetGoal);
                if (selector != null) {
                    Predicate<LivingEntity> oldPredicate = CommonReflection.getTargetSelector(selector);
                    Predicate<LivingEntity> sameBattlePredicate = target -> {
                        return EntityCombat.get(target).map(EntityCombat::getBattle).orElse(null)
                                == EntityCombat.get(mob).map(EntityCombat::getBattle).orElse(null);
                    };
                    CommonReflection.setTargetConditions(targetGoal, new EntityPredicate().range(CommonReflection.getTargetDistance(targetGoal)).selector(
                            oldPredicate != null ? oldPredicate.and(sameBattlePredicate) : sameBattlePredicate));
                }
                
                mob.targetSelector.addGoal(targetPlayerPriority.getAsInt(), targetGoal);
            }
        }
    }
    
    @SubscribeEvent
    public static void onDifficultyChange(DifficultyChangeEvent event) {
        // fuck this game fuck this life fuck fuck fuck fuck fuck fuck fuck
//        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//        server.getAllLevels().forEach(world -> world.getEntities().forEach(entity -> {
//            if (entity instanceof MobEntity) {
//                EntityRPGData.get((LivingEntity) entity).ifPresent(EntityRPGData::updateMobLevel);
//            }
//        }));
    }
    
    
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkWatchEvent.Watch event) {
        Chunk chunk = event.getWorld().getChunkSource().getChunk(event.getPos().x, event.getPos().z, false);
        if (chunk != null) {
            chunk.getCapability(ChunkLevelProvider.CAPABILITY).ifPresent(cap -> cap.onChunkLoad(event.getPlayer()));
        }
    }
    
    

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        syncWithPlayer(player);
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        syncWithPlayer((ServerPlayerEntity) event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        syncWithPlayer((ServerPlayerEntity) event.getPlayer());
    }
    
    private static void syncWithPlayer(ServerPlayerEntity player) {
        EntityRPGData.get(player).ifPresent(data -> { 
            data.syncWithPlayer(player);
        });
    }
    
    @SubscribeEvent
    public static void onEntityTracking(PlayerEvent.StartTracking event) {
        Entity tracked = event.getTarget();
        if (tracked instanceof LivingEntity) {
            EntityRPGData.get((LivingEntity) tracked).ifPresent(data -> {
                ModNetworkManager.sendToClient(new RPGDataPacket(data, tracked.getId()), (ServerPlayerEntity) event.getPlayer());
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EntityRPGData.get(event.getOriginal()).ifPresent(oldData -> {
            EntityRPGData.get(event.getPlayer()).ifPresent(newData -> {
                newData.saveOnClone(oldData);
            });
        });
    }
    
    
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionAdded(PotionAddedEvent event) {
        EntityCombat.get(event.getEntityLiving()).ifPresent(effects -> {
            effects.addEffect(event.getPotionEffect());
        });
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPotionRemoved(PotionRemoveEvent event) {
        EntityCombat.get(event.getEntityLiving()).ifPresent(effects -> {
            effects.removeEffect(event.getPotion());
        });
    }
    
    

    
    @SubscribeEvent
    public static void usedBow(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntityLiving() instanceof PlayerEntity && event.getItem().getItem() instanceof BowItem) {
            EntityCombat.get(event.getEntityLiving()).map(c -> c.getBattle()).ifPresent(battle -> {
                CombatAction action = battle.getCurrentAction(event.getEntityLiving());
                if (action instanceof PlayerBowAction) {
                    action.setActionEnded();
                }
            });
        }
    }
}
