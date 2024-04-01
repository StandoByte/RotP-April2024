package com.github.standobyte.jrpg.client.battleui;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.client.BlitFloat;
import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.init.ModEntityTypes;
import com.github.standobyte.jrpg.stats.EntityRPGData;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = ModMain.MOD_ID, value = Dist.CLIENT)
public class EntityNameplate {
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void renderNameplate(RenderNameplateEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            
            MutableBoolean lvlNameTag = new MutableBoolean(true);
            
            if (ClientStuff.getCurrentBattle() != null) { 
                EntityCombat.get(entity).ifPresent(extraData -> {
                    Battle entityBattle = extraData.getBattle();
                    boolean inSameBattle = entityBattle != null && entityBattle.battleId == ClientStuff.getCurrentBattle().battleId;
                    if (inSameBattle) {
                        if (!entityBattle.areTeammates(ClientUtil.getClientPlayer(), entity)) {
                            Minecraft mc = Minecraft.getInstance();
                            MatrixStack matrixStack = event.getMatrixStack();
                            matrixStack.pushPose();
                            matrixStack.translate(0, entity.getBbHeight() + 0.5, 0);
                            matrixStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
                            matrixStack.scale(-0.025f, -0.025f, 0.025f);
        
                            mc.textureManager.bind(BattleScreen.TEXTURE);
                            RenderSystem.enableBlend();
                            RenderSystem.defaultBlendFunc();
                            if (entity.isAlive()) {
                                float hpRatio = entity.getHealth() / entity.getMaxHealth();
                                int hpBarWidth = 80;
                                RenderSystem.color4f(1.0f, 0.0f, 0.0f, 1);
                                BlitFloat.blitFloat(matrixStack, -40, 1, 0, 100, Math.max(MathHelper.floor(hpRatio * hpBarWidth), 1), 6, 256, 256);
                                RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1);
                            }
                            BlitFloat.blitFloat(matrixStack, -41, 0, 0, 48, 82, 8, 256, 256);
                            RenderSystem.disableBlend();
        
                            matrixStack.popPose();
                            matrixStack.translate(0, 0.25, 0); 
                        }
                    }
                    else {
                        lvlNameTag.setFalse();
                    }
                });
            }
            
            if (lvlNameTag.booleanValue()) {
                EntityRPGData.get(entity).ifPresent(data -> {
                    if (entity != Minecraft.getInstance().player) {
                        event.setResult(Result.ALLOW);
                    }
                    String levelStr = entity.getType() == ModEntityTypes.BOWMAN.get() ? "???" : String.valueOf(data.getLevel());
                    event.setContent(new TranslationTextComponent("jrpg.entity_lvl_label", levelStr, entity.getDisplayName()));
                });
            }
        }
    }
}
