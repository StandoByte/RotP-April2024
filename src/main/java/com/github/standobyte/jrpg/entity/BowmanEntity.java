package com.github.standobyte.jrpg.entity;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.entity.mob.IMobStandUser;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.power.impl.stand.StandPower;
import com.github.standobyte.jojo.util.mc.MCUtil;
import com.github.standobyte.jrpg.ai.RangedBowAttackGoalFix;
import com.github.standobyte.jrpg.init.ModEntityTypes;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShootableItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class BowmanEntity extends CreatureEntity implements IMobStandUser, IRangedAttackMob {
    private final IStandPower standPower = new StandPower(this);

    public BowmanEntity(EntityType<? extends CreatureEntity> type, World world) {
        super(type, world);
        this.reassessWeaponGoal();
    }

    public BowmanEntity(World world) {
        this(ModEntityTypes.BOWMAN.get(), world);
    }

    @Override
    public IStandPower getStandPower() {
        return standPower;
    }
    
    @Override
    public void tick() {
        super.tick();
    }
    
    @Override
    protected void populateDefaultEquipmentSlots(DifficultyInstance pDifficulty) {
        super.populateDefaultEquipmentSlots(pDifficulty);
        ItemStack bow = new ItemStack(Items.BOW);
        this.setItemSlot(EquipmentSlotType.MAINHAND, bow);
    }

    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld pLevel, DifficultyInstance pDifficulty, SpawnReason pReason, @Nullable ILivingEntityData pSpawnData, @Nullable CompoundNBT pDataTag) {
        pSpawnData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        this.populateDefaultEquipmentSlots(pDifficulty);
        return pSpawnData;
    }
    
    

    public final RangedBowAttackGoalFix<BowmanEntity> bowGoal = new RangedBowAttackGoalFix<>(this, 1.0D, 20, 15.0F);
    public final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.2D, false) {
        @Override
        public void stop() {
            super.stop();
            BowmanEntity.this.setAggressive(false);
        }
        
        @Override
        public void start() {
            super.start();
            BowmanEntity.this.setAggressive(true);
        }
    };

    @Override
    protected void registerGoals() {
       this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
       this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
       this.goalSelector.addGoal(6, new LookRandomlyGoal(this));
       this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, WolfEntity.class, true));
       this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
       this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
    }

    public void reassessWeaponGoal() {
        if (this.level != null && !this.level.isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            ItemStack itemstack = this.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.item.BowItem));
            if (itemstack.getItem() == Items.BOW) {
                int i = 20;
                if (this.level.getDifficulty() != Difficulty.HARD) {
                    i = 40;
                }

                this.bowGoal.setMinAttackInterval(i);
                this.goalSelector.addGoal(4, this.bowGoal);
            } else {
                this.goalSelector.addGoal(4, this.meleeGoal);
            }

        }
    }

    @Override
    public void performRangedAttack(LivingEntity pTarget, float pVelocity) {
        ItemStack bow = this.getProjectile(this.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this, item -> item instanceof net.minecraft.item.BowItem)));
        AbstractArrowEntity abstractarrowentity = this.getArrow(bow, pVelocity);
        if (this.getMainHandItem().getItem() instanceof net.minecraft.item.BowItem)
            abstractarrowentity = ((net.minecraft.item.BowItem)this.getMainHandItem().getItem()).customArrow(abstractarrowentity);
        if (getOffhandItem().getItem() instanceof ArrowItem) {
            abstractarrowentity = ((ArrowItem) getOffhandItem().getItem()).createArrow(level, getOffhandItem(), this);
        }
        
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getY(0.3333333333333333D) - abstractarrowentity.getY();
        double d2 = pTarget.getZ() - this.getZ();
        double d3 = (double)MathHelper.sqrt(d0 * d0 + d2 * d2);
        abstractarrowentity.shoot(d0, d1 + d3 * (double)0.2F, d2, 1.6F, (float)(14 - this.level.getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addFreshEntity(abstractarrowentity);
    }

    protected AbstractArrowEntity getArrow(ItemStack pArrowStack, float pDistanceFactor) {
        return ProjectileHelper.getMobArrow(this, pArrowStack, pDistanceFactor);
    }

    @Override
    public boolean canFireProjectileWeapon(ShootableItem pProjectileWeapon) {
        return pProjectileWeapon == Items.BOW;
    }

    @Override
    public void setItemSlot(EquipmentSlotType pSlot, ItemStack pStack) {
        super.setItemSlot(pSlot, pStack);
        if (!this.level.isClientSide) {
            this.reassessWeaponGoal();
        }
    }
    
    public boolean disappearNextTurn = false;
    

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
        this.reassessWeaponGoal();
    }
    
    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

}
