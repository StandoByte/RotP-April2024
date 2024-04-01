package com.github.standobyte.jrpg.entity;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.mob.IMobStandUser;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandPower;
import com.github.standobyte.jojo.power.impl.stand.type.StandType;
import com.github.standobyte.jojo.power.impl.stand.type.StandType.StandSurvivalGameplayPool;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jrpg.init.ModEntityTypes;
import com.github.standobyte.jrpg.party.Party;
import com.github.standobyte.jrpg.party.PartyHandler;
import com.github.standobyte.jrpg.server.ServerBattlesProvider;
import com.github.standobyte.jrpg.stats.EntityRPGData;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.NonTamedTargetGoal;
import net.minecraft.entity.ai.goal.OwnerHurtByTargetGoal;
import net.minecraft.entity.ai.goal.OwnerHurtTargetGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

public class MobStandUserEntity extends TameableEntity implements IMobStandUser {
    private final IStandPower standPower = new StandPower(this);

    public MobStandUserEntity(EntityType<? extends TameableEntity> type, World world) {
        super(type, world);
    }

    public MobStandUserEntity(World world) {
        this(ModEntityTypes.STAND_USER.get(), world);
    }

    @Override
    public IStandPower getStandPower() {
        return standPower;
    }
    
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new SitGoal(this));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(10, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new NonTamedTargetGoal<>(this, PlayerEntity.class, true, null));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, MonsterEntity.class, false));
    }

    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld pLevel, DifficultyInstance pDifficulty, SpawnReason pReason, @Nullable ILivingEntityData pSpawnData, @Nullable CompoundNBT pDataTag) {
        pSpawnData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        IForgeRegistry<StandType<?>> stands = RegistryManager.ACTIVE.getRegistry(StandType.class);
        if (!stands.isEmpty()) {
            List<StandType<?>> list = stands.getValues().stream()
                    .filter(stand -> stand.getSurvivalGameplayPool() == StandSurvivalGameplayPool.PLAYER_ARROW).collect(Collectors.toList());
            StandType<?> stand = list.get(random.nextInt(list.size()));
            standPower.givePower(stand);
        }
        return pSpawnData;
    }
    
    @Override
    public ActionResultType mobInteract(PlayerEntity pPlayer, Hand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        Item item = itemstack.getItem();
        if (this.level.isClientSide) {
            boolean flag = this.isOwnedBy(pPlayer) || this.isTame() && !this.isTame();
            return flag ? ActionResultType.CONSUME : ActionResultType.PASS;
        } else {
            if (this.isTame()) {
                if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                    if (!pPlayer.abilities.instabuild) {
                        itemstack.shrink(1);
                    }

                    this.heal((float) item.getFoodProperties().getNutrition());
                    EntityRPGData.get(this).ifPresent(data -> data.setStaminaPoints(data.getStaminaPoints() + 10));
                    return ActionResultType.SUCCESS;
                }
                
                ActionResultType actionresulttype = super.mobInteract(pPlayer, pHand);
                if ((!actionresulttype.consumesAction() || this.isBaby()) && this.isOwnedBy(pPlayer)) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget((LivingEntity)null);
                    return ActionResultType.SUCCESS;
                }

                return actionresulttype;
            }

            return super.mobInteract(pPlayer, pHand);
        }
    }
    
    @Override
    public boolean isFood(ItemStack pStack) {
        Item item = pStack.getItem();
        return item.isEdible() && item.getFoodProperties().getEffects().stream()
                .allMatch(eff -> eff.getFirst().getEffect().getCategory() != EffectType.HARMFUL);
    }

    @Override
    public AgeableEntity getBreedOffspring(ServerWorld pServerLevel, AgeableEntity pMate) {
        return null;
    }
    
    private static final String[] NAMES = {
            "afan",
            "RedgitReds",
            "YetDarker",
            "Brokenspace",
            "Geno",
            "Rin",
            "August_dr",
            "Yujin",
            "Wild Techies",
            "OlegCipoletti",
            "RaidenTokado",
            "KWiNTA",
            "Pr1tcha",
            "ArchLunatic",
            "TURBO",
            "Кхъ",
            "Scorpivan",
            "Regttoti",
            "Mephisto",
            "DanielGamer321",
            "Hoo",
            "hk47bit",
            "WhiteMind",
            "FriendlyCoop",
            "adel",
            "He",
            "A Vtuber Enjoyer",
            "Draconis",
            "Wooden Man",
            "999hh20",
            "The_Reverse_Flash",
            "wain",
            "Tobbi",
            "kev",
            "KrillCifer",
            "Maxeltov",
            "MemeShadow"
    };
    @Override
    public void setTame(boolean pTamed) {
        super.setTame(pTamed);
        if (pTamed && !hasCustomName()) {
            setCustomName(new StringTextComponent(NAMES[random.nextInt(NAMES.length)]));
        }
    }
    
    @Override
    public void setOrderedToSit(boolean sit) {
        super.setOrderedToSit(sit);
        if (!level.isClientSide()) {
            LivingEntity owner = getOwner();
            if (owner != null) {
                PartyHandler partyHandler = ServerBattlesProvider.getServerBattlesData(((ServerWorld) level).getServer()).partyHandler;
                if (sit) {
                    Party party = partyHandler.getParty(owner);
                    if (party != null) {
                        party.removeMember(this);
                    }
                }
                else {
                    boolean added = partyHandler.addToParty(owner, this);
                }
            }
        }
    }


    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.put("Stand", standPower.writeNBT());
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("Stand", MCUtil.getNbtId(CompoundNBT.class))) {
            standPower.readNBT(nbt.getCompound("Stand"));
        }
    }
}
