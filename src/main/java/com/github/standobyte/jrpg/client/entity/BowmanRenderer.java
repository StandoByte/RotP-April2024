package com.github.standobyte.jrpg.client.entity;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.entity.BowmanEntity;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class BowmanRenderer extends BipedRenderer<BowmanEntity, BowmanModel<BowmanEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(ModMain.RES_NAMESPACE, "textures/entity/steve_shadow.png");

    public BowmanRenderer(EntityRendererManager renderManager) {
        super(renderManager, new BowmanModel<>(0, false), 0.5F);
    }
    
    @Override
    public void render(BowmanEntity pEntity, float pEntityYaw, float pPartialTicks, 
            MatrixStack pMatrixStack, IRenderTypeBuffer pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BowmanEntity pEntity) {
        return TEXTURE;
    }

}
