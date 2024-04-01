package com.github.standobyte.jrpg.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.lwjgl.glfw.GLFW;

import com.github.standobyte.jojo.client.ui.actionshud.ActionsOverlayGui;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.general.Vector2i;
import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.client.battleui.BattleScreen;
import com.github.standobyte.jrpg.init.ModEntityAttributes;
import com.github.standobyte.jrpg.party.Party;
import com.github.standobyte.jrpg.stats.EntityRPGData;
import com.github.standobyte.jrpg.stats.RPGStat;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = ModMain.MOD_ID, value = Dist.CLIENT)
public class PartyHud extends AbstractGui {
    
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void render(RenderGameOverlayEvent.Pre event) {
        // hp bar
        Minecraft mc = Minecraft.getInstance();
        MatrixStack matrixStack = event.getMatrixStack();
        PlayerEntity player = mc.player;
        
        if (event.getType() == ElementType.HEALTH) {
            event.setCanceled(true);
            
            mc.textureManager.bind(BattleScreen.TEXTURE);
            float health = player.getHealth();
            float maxHp = player.getMaxHealth();
            int x = mc.getWindow().getGuiScaledWidth() / 2 - 108;
            int y = mc.getWindow().getGuiScaledHeight() - 40;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1, 0, 0, 1);;
            blit(matrixStack, x + 17, y + 2, 0, 100, Math.max((int) ((health * 80) / maxHp), 1), 6, 256, 256);
            RenderSystem.color4f(1, 1, 1, 1);
            blit(matrixStack, x + 16, y + 1, 0, 48, 82, 8, 256, 256);
            RenderSystem.disableBlend();
            ITextComponent hpString = new StringTextComponent(String.valueOf((int) health));
            mc.font.drawShadow(matrixStack, hpString, x + 14 - mc.font.width(hpString), y + 1, 0xFF0000);
            mc.textureManager.bind(ForgeIngameGui.GUI_ICONS_LOCATION);
            
            ForgeIngameGui.left_height += 10;
            
            return;
        }
        
        
        
        if (ClientStuff.getCurrentBattle() != null
                || event.getType() != ElementType.ALL) return;
        
        ChunkPos chunkPos = new ChunkPos(player.blockPosition());
        Vector2i areaLevel = ClientStuff.CHUNK_LEVELS.computeIfAbsent(ClientUtil.getClientWorld().dimension(), d -> new HashMap<>()).get(chunkPos);
        if (areaLevel != null) {
            mc.font.drawShadow(matrixStack, new TranslationTextComponent("jrpg.chunk_lvl", areaLevel.x, areaLevel.y), 
                    10, 10, 0xFFFFFF);
        }
        
        Optional<Party> party = ClientStuff.currentParty;
        List<LivingEntity> partyMembers = new ArrayList<>();
        party.ifPresent(p -> Iterables.addAll(partyMembers, p.getActiveMembers()));
        if (!partyMembers.contains(player)) {
            partyMembers.add(player);
        }
        
        int x = 4;
        int yStarting = 50;
        int y = yStarting;
        int maxWidth = -1;
        
        for (LivingEntity partyMember : partyMembers) {
            BattleScreen.renderEntityFace(partyMember, matrixStack, x, y, mc);
            
            EntityRPGData data = EntityRPGData.get(partyMember).orElse(null);
            if (data != null) {
                float health = partyMember.getHealth();
                float maxHp = partyMember.getMaxHealth();
                double sp = data.usesStamina() ? data.getStaminaPoints() : 0;
                double maxSp = data.usesStamina()  && partyMember.getAttributes().hasAttribute(ModEntityAttributes.STAND_STAMINA.get()) ?
                        partyMember.getAttributeValue(ModEntityAttributes.STAND_STAMINA.get()) : 0;
                float xpRatio = data.getLevelProgress();
                int level = data.getLevel();
                mc.textureManager.bind(BattleScreen.TEXTURE);
                int spColor = 0x0080FF;
    
    
                // hp and sp bars
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
    
                if (partyMember.isAlive()) {
                    RenderSystem.color4f(1, 0, 0, 1);
                    blit(matrixStack, x + 18, y + 3, 205, 6, MathHelper.clamp((int) ((health * 50) / maxHp), 1, 50), 4, 256, 256);
                    RenderSystem.color4f(1, 1, 1, 1);
                }
                blit(matrixStack, x + 17, y + 2, 204, 0, 52, 6, 256, 256);
    
                if (data.usesStamina()) {
                    if (sp > 0 && maxSp > 0) {
                        float[] rgb = ClientUtil.rgb(spColor);
                        RenderSystem.color4f(rgb[0], rgb[1], rgb[2], 1);
                        blit(matrixStack, x + 18, y + 11, 205, 6, MathHelper.clamp((int) ((sp * 50) / maxSp), 1, 50), 4, 256, 256);
                        RenderSystem.color4f(1, 1, 1, 1);
                    }
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    blit(matrixStack, x + 17, y + 10, 204, 0, 52, 6, 256, 256);
                }
    
                blit(matrixStack, x + 17, y + 18, 194, 48, 61, 5, 256, 256);
                if (xpRatio > 0) {
                    blit(matrixStack, x + 18, y + 19, 195, 54, Math.max((int) (xpRatio * 59), 1), 3, 256, 256);
                }
    
                ITextComponent hpString = new StringTextComponent(String.valueOf((int) health));
                mc.font.drawShadow(matrixStack, hpString, x + 72, y + 1, 0xFF0000);
                maxWidth = Math.max(maxWidth, mc.font.width(hpString));
                
                if (data.usesStamina()) {
                    ITextComponent mpString = new StringTextComponent(String.valueOf((int) sp));
                    mc.font.drawShadow(matrixStack, mpString, x + 72, y + 9, spColor);
                    maxWidth = Math.max(maxWidth, mc.font.width(mpString));
                }
                
                ITextComponent levelText = new StringTextComponent(String.valueOf(level));
                mc.font.drawShadow(matrixStack, levelText, x + 8 - mc.font.width(levelText) / 2, y + 17, 0x8EC264);
    
                RenderSystem.disableBlend();
            }
            mc.font.drawShadow(matrixStack, partyMember.getDisplayName(), x, y - 9, 0xFFFFFF);
            y += 28;
            
            IStandPower standPower = IStandPower.getStandPowerOptional(partyMember).orElse(null);
            if (standPower != null && standPower.hasPower()) {
                mc.textureManager.bind(standPower.clGetPowerTypeIcon());
                blit(matrixStack, x - 1, y, 0, 0, 16, 16, 16, 16);
                int standLevel = (int) data.getStandLevel(standPower.getType().getRegistryName());
                mc.font.drawShadow(matrixStack, new StringTextComponent(String.valueOf(standLevel)), 
                        x + 18, y + 5, ActionsOverlayGui.getPowerUiColor(standPower));
                y += 16;
            }

            y += 18;
            // TODO status effect icons
        }

        y = yStarting;
        if (mc.screen == null && InputMappings.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_TAB)) {
            int xStats = x + maxWidth + 6;
            for (LivingEntity partyMember : partyMembers) {
                EntityRPGData data = EntityRPGData.get(partyMember).orElse(null);
                if (data == null) continue;
                
                RPGStat[] otherStats = new RPGStat[] {
                        RPGStat.STRENGTH,
                        RPGStat.SPEED,
                        RPGStat.DURABILITY,
                        RPGStat.PRECISION,
                        RPGStat.SPIRIT
                };
                int maxStatNameWidth = -1;
                int y1 = y;
                int y2 = y;
                for (RPGStat stat : otherStats) {
                    maxStatNameWidth = Math.max(maxStatNameWidth, mc.font.width(stat.name));
                    mc.font.drawShadow(matrixStack, stat.name, x + xStats + 72, y1 - 9, 0xFFFFFF);
                    y1 += 8;
                }
                for (RPGStat stat : otherStats) {
                    maxStatNameWidth = Math.max(maxStatNameWidth, mc.font.width(stat.name));
                    double statValue = partyMember.getAttributes().hasAttribute(stat.attribute) ? partyMember.getAttributeValue(stat.attribute) : 0;
                    mc.font.drawShadow(matrixStack, String.valueOf((int) statValue), x + xStats + 72 + maxStatNameWidth + 8, y2 - 9, 0xFFFFFF);
                    y2 += 8;
                }
                y += 46;
            }
        }
    }
}
