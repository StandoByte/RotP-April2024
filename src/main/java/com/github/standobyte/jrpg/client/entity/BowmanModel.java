package com.github.standobyte.jrpg.client.entity;

import com.github.standobyte.jrpg.entity.BowmanEntity;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;

public class BowmanModel<T extends BowmanEntity> extends PlayerModel<T> {

    public BowmanModel(float p_i46304_1_, boolean p_i46304_2_) {
        super(p_i46304_1_, p_i46304_2_);
    }

    @Override
    public void prepareMobModel(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTick) {
       this.rightArmPose = BipedModel.ArmPose.EMPTY;
       this.leftArmPose = BipedModel.ArmPose.EMPTY;
       ItemStack itemstack = pEntity.getItemInHand(Hand.MAIN_HAND);
       if (itemstack.getItem() == Items.BOW && pEntity.isAggressive()) {
          if (pEntity.getMainArm() == HandSide.RIGHT) {
             this.rightArmPose = BipedModel.ArmPose.BOW_AND_ARROW;
          } else {
             this.leftArmPose = BipedModel.ArmPose.BOW_AND_ARROW;
          }
       }

       super.prepareMobModel(pEntity, pLimbSwing, pLimbSwingAmount, pPartialTick);
    }

    @Override
    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
       super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
       ItemStack itemstack = pEntity.getMainHandItem();
       if (pEntity.isAggressive() && (itemstack.isEmpty() || itemstack.getItem() != Items.BOW)) {
          float f = MathHelper.sin(this.attackTime * (float)Math.PI);
          float f1 = MathHelper.sin((1.0F - (1.0F - this.attackTime) * (1.0F - this.attackTime)) * (float)Math.PI);
          this.rightArm.zRot = 0.0F;
          this.leftArm.zRot = 0.0F;
          this.rightArm.yRot = -(0.1F - f * 0.6F);
          this.leftArm.yRot = 0.1F - f * 0.6F;
          this.rightArm.xRot = (-(float)Math.PI / 2F);
          this.leftArm.xRot = (-(float)Math.PI / 2F);
          this.rightArm.xRot -= f * 1.2F - f1 * 0.4F;
          this.leftArm.xRot -= f * 1.2F - f1 * 0.4F;
          ModelHelper.bobArms(this.rightArm, this.leftArm, pAgeInTicks);
       }

    }

}
