package com.github.standobyte.jrpg.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.ModItems;
import com.github.standobyte.jojo.item.StandDiscItem;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.entity.MobStandUserEntity;
import com.github.standobyte.jrpg.init.ModEntityTypes;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.network.packets.server.BattleEndPacket;
import com.github.standobyte.jrpg.network.packets.server.BattleStartPacket;
import com.github.standobyte.jrpg.network.packets.server.GiveChoisePacket;
import com.github.standobyte.jrpg.network.packets.server.NextTurnPacket;
import com.github.standobyte.jrpg.network.packets.server.UIMessagePacket;
import com.github.standobyte.jrpg.party.Party;
import com.github.standobyte.jrpg.server.ServerBattlesProvider;
import com.github.standobyte.jrpg.stats.EntityRPGData;
import com.github.standobyte.jrpg.util.MCUtil;
import com.google.common.collect.Streams;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class Battle {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    public final World world;
    public final int battleId;
    
    private final List<LivingEntity> team1 = new ArrayList<>();
    private final List<LivingEntity> team2 = new ArrayList<>();
    
    public final BattleTurnQueue turnQueue = new BattleTurnQueue(this);
    private LivingEntity currentTurnEntity;
    private CombatAction currentAction;
    
    private boolean battleStarted = false;
    private boolean battleOver = false;
    
    public static Battle createBattleServerSide(World world) {
        return new Battle(world, ID_COUNTER.incrementAndGet());
    }
    
    public static Battle createBattleClientSide(World world, int id) {
        return new Battle(world, id);
    }
    
    private Battle(World world, int id) {
        this.world = world;
        this.battleId = id;
    }
    
    public boolean addToTeam1(LivingEntity entity) {
        return addToTeam(entity, team1);
    }
    
    public boolean addToTeam2(LivingEntity entity) {
        return addToTeam(entity, team2);
    }
    
    public boolean addToTeamOf(LivingEntity newEntity, LivingEntity teammate) {
        if (team1.contains(teammate)) {
            return addToTeam(newEntity, team1);
        }
        if (team2.contains(teammate)) {
            return addToTeam(newEntity, team2);
        }
        
        return false;
    }
    
    private boolean addToTeam(LivingEntity entity, List<LivingEntity> team) {
        if (team.contains(entity) || entity instanceof StandEntity) {
            return false;
        }
        
        EntityCombat data = EntityCombat.get(entity).get();
        if (data != null && data.getBattle() == null) {
            if (entity instanceof MobEntity) {
                MobEntity mob = (MobEntity) entity;
                if (mob.isNoAi()) {
                    return false;
                }
            }
            
            data.setBattle(this);
            team.add(entity);
            
            if (entity.getType() == EntityType.ENDER_DRAGON) {
//                List<EnderCrystalEntity> list = entity.level.getEntitiesOfClass(EnderCrystalEntity.class, 
//                        entity.getBoundingBox().inflate(96.0D).expandTowards(0, 256, 0));
            }
            
            return true;
        }
        
        return false;
    }
    
    
    public static Battle createBattleOnAttack(LivingEntity attacker, LivingEntity target) {
        Battle battle = Battle.createBattleServerSide(attacker.level);
        battle.addToTeam1(attacker);
        battle.addToTeam2(target);
        
        EntityRPGData.get(attacker).ifPresent(data -> {
            if (data.getParty() != null) {
                for (LivingEntity teammate : data.getParty().getActiveMembers()) {
                    battle.addToTeamOf(teammate, attacker);
                }
            }
        });
        EntityRPGData.get(target).ifPresent(data -> {
            if (data.getParty() != null) {
                for (LivingEntity teammate : data.getParty().getActiveMembers()) {
                    battle.addToTeamOf(teammate, target);
                }
            }
        });
        
        if (attacker instanceof MobEntity) {
            boolean sameClass = attacker.getClass().equals(target.getClass());
            addMobTeammates(attacker, attacker.getClass(), battle, 
                    sameClass ? teammate -> attacker.getRandom().nextFloat() < 0.5F : null);
        }
        if (target instanceof MobEntity) {
            addMobTeammates(target, target.getClass(), battle, null);
        }
        if (attacker instanceof MonsterEntity) {
            boolean targetAlsoMonster = target instanceof MonsterEntity;
            addMobTeammates(attacker, MonsterEntity.class, battle, 
                    targetAlsoMonster ? teammate -> attacker.getRandom().nextFloat() < 0.5F : null);
        }
        if (target instanceof MonsterEntity) {
            addMobTeammates(target, MonsterEntity.class, battle, null);
        }
        
        return battle;
    }
    
    private static <T extends LivingEntity> void addMobTeammates(LivingEntity mob, 
            Class<T> teammatesClass, Battle battle, @Nullable Predicate<T> extraFilter) {
        List<T> teammates = getPotentialTeammates(mob, teammatesClass);
        for (T entity : teammates) {
            if (EntityCombat.get(entity).map(e -> !e.isInBattle()).orElse(false) && 
                    (extraFilter == null || extraFilter.test(entity))) {
                battle.addToTeamOf(entity, mob);
            }
        }
    }
    
    public static <T extends LivingEntity> List<T> getPotentialTeammates(LivingEntity mob, Class<T> teammatesClass) {
        return MCUtil.getEntitiesOfClass(mob.level, teammatesClass, 
                mob.getBoundingBox().inflate(8, 4, 8), e -> e != mob && e.isAlive());
    }
    
    
    public boolean startBattle() {
        if (!battleStarted) {
            if (!world.isClientSide()) {
                ServerBattlesProvider.getServerBattlesData(((ServerWorld) world).getServer()).addBattle(this);
                ModMain.tmpLog("battle {} started", battleId);
                for (Entity entity : team1) {
                    ModMain.tmpLog("  {}", entity);
                }
                ModMain.tmpLog("VS");
                for (Entity entity : team2) {
                    ModMain.tmpLog("  {}", entity);
                }
                
                BattleStartPacket packet = new BattleStartPacket(battleId, team1, team2, false);
                forEachPlayer((player, team) -> ModNetworkManager.sendToClient(packet, (ServerPlayerEntity) player));
                
                team1.forEach(turnQueue::addEntity);
                team2.forEach(turnQueue::addEntity);
                team1.forEach(aliveEntities::add);
                team2.forEach(aliveEntities::add);
                turnQueue.giveInitiative(team1);
                turnQueue.onBattleStart();
                
                srvNextTurnEntity();
            }
            battleStarted = true;
            return true;
        }
        return false;
    }
    
    public boolean endBattle() {
        if (battleStarted && !battleOver) {
            if (!world.isClientSide()) {
                ModMain.tmpLog("battle {} ended", battleId);
                ModMain.tmpLog("");
                
                BattleEndPacket packet = new BattleEndPacket(battleId);
                forEachPlayer((player, team) -> ModNetworkManager.sendToClient(packet, (ServerPlayerEntity) player));
            }
            forEachEntity((entity, team) -> {
                if (!entity.isAlive() && (doNotRevive == null || !doNotRevive.contains(entity))) {
                    if (!world.isClientSide()) {
                        MCUtil.onEntityResurrect(entity);
                        entity.setHealth(1);
                    }
                    entity.deathTime = 0;
                }
                EntityCombat.get(entity).ifPresent(extraData -> extraData.setBattle(null));
            });
            battleOver = true;
            return true;
        }
        return false;
    }
    
    private Set<LivingEntity> aliveEntities = new HashSet<>();
    
    public static Int2ObjectMap<SlimeCheck> aaaaa = new Int2ObjectArrayMap<>();
    
    public static class SlimeCheck {
        public final int size;
        public final List<LivingEntity> joinedSlimes = new ArrayList<>();
        private final int team;
        
        private SlimeCheck(int size, int team) {
            this.size = size;
            this.team = team;
        }
    }
    
    public void tickBattle() {
        Iterator<LivingEntity> aliveIter = aliveEntities.iterator();
        while (aliveIter.hasNext()) {
            LivingEntity entity = aliveIter.next();
            if (!entity.isAlive()) {
                aliveIter.remove();
                if (!entity.level.isClientSide()) {
                    if (entity instanceof SlimeEntity && ((SlimeEntity) entity).getSize() > 1) {
                        aaaaa.put(battleId, new SlimeCheck(((SlimeEntity) entity).getSize() / 2, team1.contains(entity) ? 1 : 2));
                    }
                    else {
                        sendMessage(new TranslationTextComponent("jrpg.k_o", entity.getDisplayName()));
                        ModMain.tmpLog("{} is retired", entity);
                    }
                }
            }
        }
        for (LivingEntity entity : turnQueue.allEntities) {
            if (entity.isAlive()) {
                aliveEntities.add(entity);
            }
        }
        
        if (!world.isClientSide()) {
            boolean team1Lost = !team1.stream().anyMatch(Entity::isAlive);
            boolean team2Lost = !team2.stream().anyMatch(Entity::isAlive);
            if (team1Lost || team2Lost) {
                List<LivingEntity> winners = team1Lost ? team2 : team1;
                List<LivingEntity> losers  = team1Lost ? team1 : team2;
                
                boolean needsChoise = losers.stream().anyMatch(entity -> entity.getType() == ModEntityTypes.STAND_USER.get());
                boolean endBattle = true;
                boolean revive = false;
                if (needsChoise) {
                    ServerPlayerEntity player = !winners.isEmpty() && winners.get(0) instanceof ServerPlayerEntity ? (ServerPlayerEntity) winners.get(0) : null;
                    if (player != null) {
                        if (spareOrKill != null) {
                            switch (spareOrKill) {
                            case SPARE:
                                revive = true;
                                for (LivingEntity loser : losers) {
                                    if (loser instanceof MobStandUserEntity) {
                                        ((MobStandUserEntity) loser).tame(player);
                                    }
                                }
                                break;
                            case KILL:
                                for (LivingEntity loser : losers) {
                                    if (loser instanceof MobStandUserEntity) {
                                        IStandPower.getStandPowerOptional(loser).ifPresent(power -> {
                                            if (power.hasPower()) {
                                                ItemStack disc = StandDiscItem.withStand(new ItemStack(ModItems.STAND_DISC.get()), power.getStandInstance().get());
                                                world.addFreshEntity(new ItemEntity(loser.level, loser.getX(), loser.getY(), loser.getZ(), disc));
                                            }
                                        });
                                    }
                                }
                                break;
                            }
                        }
                        else {
                            endBattle = false;
                            if (!sentChoisePacket) {
                                ModNetworkManager.sendToClient(new GiveChoisePacket(), player);
                                sentChoisePacket = true;
                            }
                        }
                    }
                }
                
                if (endBattle) {
                    double losersAvgLevel = losers.stream()
                            .map(EntityRPGData::get).filter(Optional::isPresent).map(Optional::get)
                            .mapToInt(EntityRPGData::getLevel)
                            .average().orElse(0);
                    boolean losersHadPowerfulOpponent = losers.stream()
                            .anyMatch(entity -> !entity.canChangeDimensions() || IStandPower.getStandPowerOptional(entity).map(power -> power.hasPower()).orElse(false));
                    for (LivingEntity winner : winners) {
                        EntityRPGData.get(winner).ifPresent(data -> {
                            int level = data.getLevel();
                            double diff = losersAvgLevel - (double) level;
                            double standLvlAdd = losersHadPowerfulOpponent ? 5 : 0.025;
                            
                            if (diff >= 0) { 
                                standLvlAdd *= diff / 5 + 1;
                            }
                            else {
                                standLvlAdd *= Math.pow(Math.E, diff / 10);
                            }
                            data.addStandLevel((float) standLvlAdd);
                        });
                    }
                    
                    int xp = losers.stream()
                            .map(EntityRPGData::get).filter(Optional::isPresent).map(Optional::get)
                            .mapToInt(e -> {
                                if (e.entity.getType() == EntityType.WITHER) {
                                    return 66666;
                                }
                                else if (e.entity.getType() == EntityType.ENDER_DRAGON) {
                                    return 150000;
                                }
                                int xpDropped = e.getXpDropped();
                                return xpDropped;
                            })
                            .sum();
                    winners.forEach(entity -> {
                        int xpToGive = entity.isAlive() ? xp : xp / 2;
                        EntityRPGData.get(entity).ifPresent(data -> data.giveXp(xpToGive));
                    });
                    
                    if (!revive) {
                        doNotRevive = losers;
                    }
                    
                    endBattle();
                }
                return;
            }
            
            
            if (aaaaa.containsKey(battleId)) {
                SlimeCheck slimes = aaaaa.get(battleId);
                if (!slimes.joinedSlimes.isEmpty()) {
                    switch (slimes.team) {
                    case 1:
                        for (LivingEntity slime : slimes.joinedSlimes) {
                            addToTeam1(slime);
                        }
                        break;
                    case 2:
                        for (LivingEntity slime : slimes.joinedSlimes) {
                            addToTeam2(slime);
                        }
                        break;
                    }
                    for (LivingEntity slime : slimes.joinedSlimes) {
                        turnQueue.addEntity(slime);
                        sendMessage(new TranslationTextComponent("jrpg.reinforcement", slime.getDisplayName()));
                    }
                    BattleStartPacket packet = new BattleStartPacket(battleId, team1, team2, true);
                    forEachPlayer((player, team) -> ModNetworkManager.sendToClient(packet, (ServerPlayerEntity) player));
                    aaaaa.remove(battleId);
                }
            }
        }
        
        if (currentTurnEntity != null && !currentTurnEntity.isAlive()) {
            srvNextTurnEntity();
        }
        else if (currentAction != null) {
            if (!currentAction.isAfterPerformDelay()) {
                currentAction.tickPerform();
            }
            currentAction.tickTimer();
            if (currentAction._actionOverFlag) {
                if (!world.isClientSide()) {
                    srvNextTurnEntity();
                }
            }
        }
    }
    
    public Choise spareOrKill = null;
    private boolean sentChoisePacket = false;
    
    public enum Choise {
        SPARE,
        KILL
    }
    
    private List<LivingEntity> doNotRevive;
    
    public List<LivingEntity> getTeammates(LivingEntity entity) {
        if (team1.contains(entity)) {
            return team1;
        }
        if (team2.contains(entity)) {
            return team2;
        }
        return null;
    }
    
    public List<LivingEntity> getEnemies(LivingEntity entity) {
        if (team1.contains(entity)) {
            return team2;
        }
        if (team2.contains(entity)) {
            return team1;
        }
        return null;
    }
    
    public boolean isInBattle(LivingEntity entity) {
        return team1.contains(entity) || team2.contains(entity);
    }
    
    public boolean areTeammates(LivingEntity entity1, LivingEntity entity2) {
        return getTeammates(entity1).contains(entity2);
    }
    
    public Stream<LivingEntity> getAllEntities() {
        return Streams.concat(team1.stream(), team2.stream());
    }
    
    private void srvNextTurnEntity() {
        if (currentTurnEntity != null) {
            EntityCombat.get(currentTurnEntity).ifPresent(handler -> handler.onTurnEnded());
        }
        
        currentAction = null;
        
        currentTurnEntity = turnQueue.calcNewTurnEntity();
        if (currentTurnEntity == null) {
            endBattle();
            return;
        }
        sendMessage(new TranslationTextComponent("jrpg.next_turn", currentTurnEntity.getDisplayName()));
        ModMain.tmpLog("{}'s turn", currentTurnEntity);
        
        EntityCombat.get(currentTurnEntity).ifPresent(handler -> {
            Optional<PlayerEntity> controllingPlayer = getControllingPlayer(currentTurnEntity);
            handler.onTurnStarted(controllingPlayer.isPresent());
            
            forEachPlayer((player, team) -> {
                boolean isControlling = controllingPlayer.map(e -> e == player).orElse(false);
                ModNetworkManager.sendToClient(NextTurnPacket.syncMoveset(battleId, currentTurnEntity.getId(), 
                        isControlling, isControlling ? Optional.ofNullable(handler.getMoveset()) : Optional.empty()), (ServerPlayerEntity) player);
            });
        });
    }
    
    public void battleMobAiTick(LivingEntity entity) {
        entity.setNoActionTime(0);
        EntityCombat.get(entity).ifPresent(handler -> handler.mobEntityAiTick());
    }
    
    public void mobAiEndTurn() {
        srvNextTurnEntity();
    }
    
    public LivingEntity getCurrentTurnEntity() {
        return currentTurnEntity;
    }
    
    public void clSetCurrentTurnEntity(LivingEntity entity) {
        if (this.currentTurnEntity != null) {
            EntityCombat.get(currentTurnEntity).ifPresent(handler -> handler.onTurnEnded());
        }
        this.currentTurnEntity = entity;
    }
    
    public Optional<PlayerEntity> getControllingPlayer(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        if (entity instanceof PlayerEntity) {
            return Optional.of((PlayerEntity) entity);
        }
        Optional<Party> party = EntityRPGData.get(entity).map(data -> data.getParty());
        return party.map(p -> {
            if (p.getLeaderEntity() instanceof PlayerEntity) {
                return (PlayerEntity) p.getLeaderEntity();
            }
            return null;
        });
    }
    
    public void setEntityAction(LivingEntity entity, CombatAction newActionInst, PacketBuffer extraInput) {
        if (currentTurnEntity == entity) {
            currentAction = newActionInst;
            if (newActionInst != null) {
                currentAction.onActionUsed(extraInput);
            }
        }
    }
    
    public CombatAction getCurrentAction(LivingEntity entity) {
        return currentTurnEntity == entity ? currentAction : null;
    }

    public void onAttack(LivingEntity attacker, LivingEntity target, DamageSource dmgSource, float amount) {
        if (attacker == currentTurnEntity) {
            EntityCombat.get(attacker).ifPresent(combatData -> combatData.onPerformAttack());
        }
    }
    
    public void onDamage(LivingEntity target, DamageSource dmgSource, float amount) {
        if (!world.isClientSide() && target != null && amount > 0) {
            float before = target.getHealth();
            float after = before - amount;
            int intAmount = (int) before - (int) after;
            sendMessage(new TranslationTextComponent("jrpg.combat_damage", target.getDisplayName(), intAmount));
            ModMain.tmpLog("{} takes {} damage", target, intAmount);
        }
//        if (target == currentTurnEntity) {
//            currentAction.setActionEnded();
//        }
    }
    
    
    
    
    private void forEachEntity(BiConsumer<LivingEntity, Integer> action) {
        for (LivingEntity entity : team1) {
            action.accept(entity, 1);
        }
        for (LivingEntity entity : team2) {
            action.accept(entity, 2);
        }
    }
    
    private void forEachPlayer(BiConsumer<PlayerEntity, Integer> action) {
        for (LivingEntity entity : team1) {
            if (entity instanceof PlayerEntity) {
                action.accept((PlayerEntity) entity, 1);
            }
        }
        for (LivingEntity entity : team2) {
            if (entity instanceof PlayerEntity) {
                action.accept((PlayerEntity) entity, 2);
            }
        }
    }
    
    public void sendMessage(ITextComponent message) {
        forEachPlayer((player, team) -> {
            ModNetworkManager.sendToClient(new UIMessagePacket(message), (ServerPlayerEntity) player);
        });
    }
    
    
    
    public boolean toBeRemoved() {
        return battleOver;
    }
}
