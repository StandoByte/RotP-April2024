package com.github.standobyte.jrpg.client.entity;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.entity.MobStandUserEntity;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;

public class MobStandUserRenderer extends BipedRenderer<MobStandUserEntity, PlayerModel<MobStandUserEntity>> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(ModMain.RES_NAMESPACE, "textures/entity/steve.png");

    public MobStandUserRenderer(EntityRendererManager renderManager) {
        super(renderManager, new PlayerModel<>(0, false), 0.5F);
    }
    
    @Override
    public void render(MobStandUserEntity pEntity, float pEntityYaw, float pPartialTicks, 
            MatrixStack pMatrixStack, IRenderTypeBuffer pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MobStandUserEntity pEntity) {
        return TEXTURE;
    }

}
