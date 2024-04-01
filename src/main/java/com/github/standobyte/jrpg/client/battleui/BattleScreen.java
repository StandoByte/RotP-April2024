package com.github.standobyte.jrpg.client.battleui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.glfw.GLFW;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.client.ui.screen.stand.ge.EntityTypeIcon;
import com.github.standobyte.jojo.client.ui.tooltip.CustomTooltipRender;
import com.github.standobyte.jojo.client.ui.tooltip.ITooltipLine;
import com.github.standobyte.jojo.client.ui.tooltip.TextTooltipLine;
import com.github.standobyte.jojo.util.general.Vector2i;
import com.github.standobyte.jrpg.ModMain;
import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.client.ClientUtil;
import com.github.standobyte.jrpg.client.entity.MobStandUserRenderer;
import com.github.standobyte.jrpg.combat.Battle;
import com.github.standobyte.jrpg.combat.EntityCombat;
import com.github.standobyte.jrpg.combat.Moveset;
import com.github.standobyte.jrpg.combat.action.CombatAction;
import com.github.standobyte.jrpg.combat.action.CombatAction.TargetingType;
import com.github.standobyte.jrpg.combat.action.PlayerBowAction;
import com.github.standobyte.jrpg.combat.action.UseItemAction;
import com.github.standobyte.jrpg.entity.MobStandUserEntity;
import com.github.standobyte.jrpg.init.ModCombatActions;
import com.github.standobyte.jrpg.init.ModEntityAttributes;
import com.github.standobyte.jrpg.network.ModNetworkManager;
import com.github.standobyte.jrpg.network.packets.client.ClStandUserChoisePacket;
import com.github.standobyte.jrpg.network.packets.client.ClUseActionPacket;
import com.github.standobyte.jrpg.stats.EntityRPGData;
import com.github.standobyte.jrpg.statuseffect.TurnBasedEffectInstance;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.KeybindTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = ModMain.MOD_ID, value = Dist.CLIENT)
public class BattleScreen extends Screen {
    public static final ResourceLocation TEXTURE = new ResourceLocation(ModMain.RES_NAMESPACE, "textures/gui/battle_ui.png");
    private final Battle battle;
    
    private Optional<Moveset> currentMoveset = Optional.empty();
    private Optional<Moveset.Category> currentCategory = Optional.empty();
    private Stack<Moveset.Category> prevCategories = new Stack<>();
    private List<BattleOptionButton> combatOptionButtons = new ArrayList<>();
    private int combatOptionsScroll = 0;
    private static final int MAX_LINES_FITTING = 6;
    
    @Nullable TargetSelectionPhase targetSelection = null;
    @Nullable private LivingEntity lastTargetEnemy = null;
    @Nullable private LivingEntity lastTargetAlly = null;
    
    private int tickCount;
    
    public BattleScreen(Battle battle) {
        super(StringTextComponent.EMPTY);
        this.battle = battle;
    }
    
    private class TargetSelectionPhase {
        CombatAction selectedAction;
        List<LivingEntity> possibleTargets = new ArrayList<>();
        Map<BattleOptionButton, LivingEntity> targetButtonsMap = new LinkedHashMap<>();
        private LivingEntity currentTarget;
        
//        Vector3d prevLook;
//        int targetChangeTicks;
//        boolean lookAtTarget;
        
        private TargetSelectionPhase(CombatAction action) {
            this.selectedAction = action;
            this.possibleTargets.addAll(action.targetSelection());
        }
        
        void onOpen() {
            if (!possibleTargets.isEmpty()) {
                if (lastTargetEnemy != null && possibleTargets.contains(lastTargetEnemy)) {
                    setTarget(lastTargetEnemy);
                }
                else if (lastTargetAlly != null && possibleTargets.contains(lastTargetAlly)) {
                    setTarget(lastTargetAlly);
                }
                else {
                    setTarget(possibleTargets.get(0));
                }
            }
        }
        
        void setTarget(LivingEntity target) {
            if (this.currentTarget != target) {
                LivingEntity turnEntity = battle.getCurrentTurnEntity();
                turnEntity.lookAt(EntityAnchorArgument.Type.EYES /*really?*/, target.getEyePosition(1.0F));
                this.currentTarget = target;
//                this.targetChangeTicks = 30;
//                this.lookAtTarget = true;
            }
        }
    }
    
    @Override
    public void init() {
        setMoveset(currentMoveset);
    }
    
    private static final int BACKGROUND_COLOR = 0xB0100010;
    private static final int BORDER_COLOR_START = 0x505000FF;
    private static final int BORDER_COLOR_END = (BORDER_COLOR_START & 0xFEFEFE) >> 1 | BORDER_COLOR_START & 0xFF000000;
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        renderFrameCheckNoHover();
        boolean isPlayerControlling = currentMoveset.isPresent();
        renderBattleScreen(matrixStack, mouseX, mouseY, partialTick, isPlayerControlling);
        renderFrameCheckTarget();
        if (renderItemSelection) {
            renderItemSelection(matrixStack, mouseX, mouseY, partialTick);
        }
    }
    
    public void renderBattleScreen(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick, boolean active) {
        ClientUtil.drawTooltipRectangle(matrixStack, 
                10, height - 85, width - 20, 75, 
                active ? BACKGROUND_COLOR : BACKGROUND_COLOR - 0x40000000, 
                        BORDER_COLOR_START, BORDER_COLOR_END, 
                0);
        
        int partyMemberX = 13;
        int partyMemberY = height - 84;
        
        List<LivingEntity> allies = battle.getTeammates(minecraft.player);
        List<Runnable> renderLater = new ArrayList<>();
        for (LivingEntity partyMember : allies) {
            EntityRPGData data = EntityRPGData.get(partyMember).orElse(null);
            if (data == null) {
                continue;
            }
            
            renderEntityFace(partyMember, matrixStack, partyMemberX, partyMemberY, minecraft);
            
            // current turn entity highlight
            if (partyMember == battle.getCurrentTurnEntity()) {
                int highlightY = partyMemberY;
                renderLater.add(() -> {
                    RenderSystem.enableBlend();
                    float alpha = Math.abs(MathHelper.sin((tickCount + partialTick) / 16f));
                    minecraft.textureManager.bind(TEXTURE);
                    RenderSystem.color4f(1, 1, 1, alpha);
                    blit(matrixStack, partyMemberX - 3, highlightY - 3, 234, 16, 22, 22);
                    RenderSystem.color4f(1, 1, 1, 1);
                    RenderSystem.disableBlend(); 
                });
            }
            
            int health = (int) partyMember.getHealth();
            float maxHp = partyMember.getMaxHealth();
            double sp = data.usesStamina() ? data.getStaminaPoints() : 0;
            double maxSp = data.usesStamina()  && partyMember.getAttributes().hasAttribute(ModEntityAttributes.STAND_STAMINA.get()) ?
                    partyMember.getAttributeValue(ModEntityAttributes.STAND_STAMINA.get()) : 0;
            minecraft.textureManager.bind(TEXTURE);
            int mpColor = 0x0080FF;
            
            // status tooltip
            if (mouseY >= partyMemberY && mouseY < partyMemberY + 16) {
                if (mouseX >= partyMemberX && mouseX < partyMemberX + 16) {
                    renderLater.add(() -> {
                        EntityCombat.get(partyMember).ifPresent(
                                combatData -> renderCombatDataTooltip(matrixStack, partyMember, combatData, mouseX, mouseY, partialTick));
                    });
                }
                else if (mouseX >= partyMemberX + 16 && mouseX < partyMemberX + 90) {
                    // HP tooltip
                    if (mouseY <= partyMemberY + 8) {
                        renderLater.add(() -> {
                            renderTooltip(matrixStack, new TranslationTextComponent("jrpg.hp_bar", health, (int) maxHp), mouseX, mouseY);
                        });
                    }
                    // MP tooltip
                    else if (data.usesStamina()) {
                        renderLater.add(() -> {
                            renderTooltip(matrixStack, new TranslationTextComponent("jrpg.mp_bar", (int) sp, (int) maxSp), mouseX, mouseY);
                        });
                    }
                }
            }

            // hp and mp bars
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            if (partyMember.isAlive()) {
                RenderSystem.color3f(1, 0, 0);
                blit(matrixStack, partyMemberX + 18, partyMemberY + 3, 205, 6, MathHelper.clamp((int) ((health * 50) / maxHp), 1, 50), 4);
            }
            RenderSystem.color3f(1, 1, 1);
            blit(matrixStack, partyMemberX + 17, partyMemberY + 2, 204, 0, 52, 6);
            
            if (data.usesStamina()) {
                if (sp > 0 && maxSp > 0) {
                    float[] rgb = ClientUtil.rgb(mpColor);
                    RenderSystem.color3f(rgb[0], rgb[1], rgb[2]);
                    blit(matrixStack, partyMemberX + 18, partyMemberY + 11, 205, 6, MathHelper.clamp((int) ((sp * 50) / maxSp), 1, 50), 4);
                    RenderSystem.color3f(1, 1, 1);
                }
                blit(matrixStack, partyMemberX + 17, partyMemberY + 10, 204, 0, 52, 6);
            }
            
            RenderSystem.disableBlend();
            minecraft.font.drawShadow(matrixStack, new StringTextComponent(String.valueOf(health)), partyMemberX + 72, partyMemberY + 1, 0xFF0000);
            if (data.usesStamina()) {
                minecraft.font.drawShadow(matrixStack, new StringTextComponent(String.valueOf((int) sp)), partyMemberX + 72, partyMemberY + 9, mpColor);
            }
            
            partyMemberY += 19;
        }
            
        renderLater.forEach(Runnable::run);
        
        int nameX = width - 200;
        int nameY = height - 104;
        if (targetSelection != null && targetSelection.selectedAction != null) {
            font.drawShadow(matrixStack, targetSelection.selectedAction.getName(), nameX, nameY, 0xFFFFFF);
        }
        else {
            currentCategory.ifPresent(category -> {
                font.drawShadow(matrixStack, category.name, nameX, nameY, 0xFFFFFF);
            });
        }
        
        // active button description (in the UI, or in the tooltip if width is < 427)
        for (BattleOptionButton button : combatOptionButtons) {
            if (button.isSelected) {
                // TODO description
                ITextComponent desc = new TranslationTextComponent(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris molestie convallis est, non sodales augue suscipit ac. "
                        + "Integer iaculis dui mi, eleifend rhoncus est commodo ut.");
                desc = null;
                
                if (desc != null) {
                    if (width >= 427) {
                        int tooltipWidth = width - 331;
                        List<IReorderingProcessor> description = font.split(desc, tooltipWidth - 4);
                        
                        int x = width - tooltipWidth - 208;
                        int y = height - 81;
                        int descHeight = 67;
                        if (description.size() * 10 > descHeight) {
                            int newHeight = description.size() * 10;
                            y -= (newHeight - descHeight);
                            descHeight = newHeight;
                        }
                        ClientUtil.drawTooltipRectangle(matrixStack, 
                                x, y, tooltipWidth, descHeight, 
                                active ? BACKGROUND_COLOR : BACKGROUND_COLOR - 0x40000000, 
                                        BORDER_COLOR_START, BORDER_COLOR_END, 
                                0);
                        
                        int lineY = y;
                        for (int i = 0; i < description.size(); ++i) {
                            IReorderingProcessor line = description.get(i);
                            if (line != null) {
                                font.drawShadow(matrixStack, line, x + 2, lineY, 0xFFFFFFFF);
                                lineY += 10;
                            }
                        }
                    }
                    else {
                        List<IReorderingProcessor> description = font.split(desc, 150);
                        renderTooltip(matrixStack, description, mouseX, mouseY);
                    }
                }
            }
        }

        for (int i = 0; i < combatOptionButtons.size(); i++) {
            combatOptionButtons.get(i).visible = combatOptionButtons.size() <= MAX_LINES_FITTING
                    || i - combatOptionsScroll >= 0 && i - combatOptionsScroll < MAX_LINES_FITTING;
        }
        if (combatOptionButtons.size() > MAX_LINES_FITTING) {
            minecraft.textureManager.bind(ADDITIONAL_UI);
            
            int x = width - 200;
            int y = height - 99;
            AbstractGui.blit(matrixStack, x, y, 
                    16, combatOptionsScroll > 0 ? 144 : 160, 16, 16, 256, 256);
            AbstractGui.blit(matrixStack, x, y + 87, 
                    0, combatOptionsScroll < combatOptionButtons.size() - MAX_LINES_FITTING ? 144 : 160, 16, 16, 256, 256);
        }
        
        if (minecraft.player == battle.getCurrentTurnEntity()) {
            font.drawShadow(matrixStack, new TranslationTextComponent("jrpg.turn_based_toggle", new KeybindTextComponent(ClientStuff.walkingSwitch.getName())), 
                    6, height - 100, 0xFFFFFF);
        }
        
        super.render(matrixStack, mouseX, mouseY, partialTick);
    }
    public static final ResourceLocation ADDITIONAL_UI = new ResourceLocation(JojoMod.MOD_ID, "textures/gui/additional.png");
    
    
    private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation("textures/gui/container/generic_54.png");
    private boolean renderItemSelection = false;
    private void renderItemSelection(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(matrixStack);
        
        int imageWidth = 176;
        int imageHeight = 166;
        
        PlayerInventory inventory =  minecraft.player.inventory;
        
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bind(CONTAINER_BACKGROUND);
        int x = (this.width - 176) / 2;
        int y = (this.height - 166) / 2;
        this.blit(matrixStack, x, y, 0, 0, imageWidth, 10);
        this.blit(matrixStack, x, y + 10, 0, 132, imageWidth, 90);
        this.font.draw(matrixStack, inventory.getDisplayName(), 
                x + 8, y + 6, 4210752);
        
        int hovered = getHoveredItemSlot(mouseX, mouseY);
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack item = inventory.items.get(i);
            Vector2i coords = getSlotCoords(i);
            
            this.itemRenderer.renderAndDecorateItem(this.minecraft.player, item, coords.x, coords.y);
            this.itemRenderer.renderGuiItemDecorations(this.font, item, coords.x, coords.y, null);
            
            if (!canUseItem(item)) {
                RenderSystem.disableDepthTest();
                int j1 = coords.x;
                int k1 = coords.y;
                RenderSystem.colorMask(true, true, true, false);
                int slotColor = 0x80000000;
                this.fillGradient(matrixStack, j1, k1, j1 + 16, k1 + 16, slotColor, slotColor);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableDepthTest();
            }
            else if (i == hovered) {
                RenderSystem.disableDepthTest();
                int j1 = coords.x;
                int k1 = coords.y;
                RenderSystem.colorMask(true, true, true, false);
                int slotColor = 0x80FFFFFF;
                this.fillGradient(matrixStack, j1, k1, j1 + 16, k1 + 16, slotColor, slotColor);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableDepthTest();
            }
        }
        
        if (hovered >= 0 && hovered < inventory.items.size()) {
            ItemStack hoveredItem = inventory.items.get(hovered);
            if (canUseItem(hoveredItem)) {
                renderTooltip(matrixStack, hoveredItem, mouseX, mouseY);
            }
        }
    }
    
    private Vector2i getSlotCoords(int index) {
        int x = (this.width - 176) / 2;
        int y = (this.height - 166) / 2;
        
        int itemX = (index % 9) * 18 + x + 8;
        int row = index / 9;
        int itemY = row > 0 ? row * 18 + y : row + y + 76;
        return new Vector2i(itemX, itemY);
    }
    
    private int getHoveredItemSlot(int mouseX, int mouseY) {
        for (int i = 0; i < 36; i++) {
            Vector2i coords = getSlotCoords(i);
            if (mouseX >= coords.x && mouseX < coords.x + 18 && mouseY >= coords.y && mouseY < coords.y + 18) {
                return i;
            }
        }
        
        return -1;
    }
    
    private boolean canUseItem(ItemStack item) {
        return UseItemAction.canUseItem(item);
    }
    
    
    
    public static OptionalInt targetHighlight(Object entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BattleScreen) {
            BattleScreen screen = (BattleScreen) mc.screen;
            if (screen.targetSelection != null && screen.targetSelection.currentTarget == entity
                    && ClientStuff.getCurrentBattle() != null) {
                int color = ClientStuff.getCurrentBattle().areTeammates(mc.player, screen.targetSelection.currentTarget) ? 0x00FF00 : 0xFF0000;
                return OptionalInt.of(color);
            }
        }
        
        return OptionalInt.empty();
    }
    
    public void renderScreenInactive(MatrixStack matrixStack, float partialTick) {
        renderBattleScreen(matrixStack, -1, -1, partialTick, false);
    }
    
    public static void renderEntityFace(LivingEntity entity, MatrixStack matrixStack, int x, int y, Minecraft mc) {
        boolean redOverlay = entity.hurtTime > 0 || entity.deathTime > 0;
        redOverlay = true;
        if (entity.hurtTime > 0) {
            RenderSystem.color3f(1, 0.5f, 0.5f);
        }
        else if (entity.deathTime > 0) {
            RenderSystem.color3f(1, 0, 0);
        }
        
        if (entity instanceof AbstractClientPlayerEntity) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) entity;
            ResourceLocation playerFace = player.getSkinTextureLocation();
            mc.getTextureManager().bind(playerFace);
            AbstractGui.blit(matrixStack, x, y, 16, 16, 16, 16, 128, 128);
            
            if (player.isModelPartShown(PlayerModelPart.HAT)) {
                matrixStack.pushPose();
                matrixStack.translate(x, y, 0);
                matrixStack.scale(9F/8F, 9F/8F, 0);
                matrixStack.translate(-1, -1, 0);
                AbstractGui.blit(matrixStack, 0, 0, 80, 16, 16, 16, 128, 128);
                matrixStack.popPose();
            }
        }
        else if (entity instanceof MobStandUserEntity) {
            mc.getTextureManager().bind(MobStandUserRenderer.TEXTURE);
            AbstractGui.blit(matrixStack, x, y, 16, 16, 16, 16, 128, 128);
        }
        else {
            EntityTypeIcon.renderIcon(entity.getType(), matrixStack, x, y);
        }
        // TODO wolf
        
        if (redOverlay) {
            RenderSystem.color3f(1, 1, 1);
        }
    }
    
    private void renderCombatDataTooltip(MatrixStack matrixStack, LivingEntity entity, EntityCombat combatData, 
            int x, int y, float partialTick) {
        List<ITooltipLine> entityInfo = new ArrayList<>();
        
        entityInfo.add(new TextTooltipLine(entity.getDisplayName()));
        
        Collection<TurnBasedEffectInstance> effects = combatData.getTurnBasedEffects();
        effects = effects.stream()
                .sorted(Comparator.comparingInt(effect -> effect.vanillaEffect.getDuration()))
                .collect(Collectors.toList());
        for (TurnBasedEffectInstance effect : effects) {
            entityInfo.add(new EffectTooltipLine(effect));
        }
        
        CustomTooltipRender.renderWrappedToolTip(matrixStack, entityInfo, x, y, font);
    }
    
    public void setActive(boolean setActive) {
        if (setActive != isActive()) {
            if (setActive) {
                minecraft.setScreen(this);
            }
            else {
                minecraft.setScreen(null);
            }
        }
    }
    
    public boolean isActive() {
        return minecraft.screen == this;
    }
    
    
    public void setMoveset(Optional<Moveset> currentMoveset) {
        firstPerson = false;
        this.currentMoveset = currentMoveset;
        if (currentMoveset.isPresent()) {
            Moveset moveset = currentMoveset.get();
            setMovesetCategory(moveset.rootCategory, null);
        }
        else {
            prevCategories.clear();
            setMovesetCategory(null, null);
        }
        setActive(currentMoveset.isPresent());
    }
    
    private void clearCombatButtons() {
        combatOptionButtons.forEach(this::removeButton);
        combatOptionButtons.clear();
        combatOptionsScroll = 0;
    }
    
    private void setMovesetCategory(Moveset.Category category, @Nullable Moveset.Category prevCategory) {
        clearCombatButtons();
        
        renderItemSelection = false;
        
        if (category != null) {
            int x = width - 200;
            MutableInt y = new MutableInt(height - 84);
            
            for (Moveset.Category.Element element : category.elementsOrdered) {
                element.either(
                        action -> {
                            BattleOptionButton button = new BattleOptionButton(x, y.intValue(), minecraft.font, action.getName(), 
                                    b -> {
                                        selectAction(action);
                                    }, this);
                            combatOptionButtons.add(addButton(button));
                        }, 
                        subCategory -> {
                            BattleOptionButton button = new BattleOptionButton(x, y.intValue(), minecraft.font, subCategory.name, 
                                    b -> {
                                        this.currentCategory.ifPresent(prevCategories::push);
                                        setMovesetCategory(subCategory, this.currentCategory.orElse(null));
                                    }, this);
                            combatOptionButtons.add(addButton(button));
                        });
                y.add(font.lineHeight + 4);
            }
            
            if (prevCategory != null) {
                BattleOptionButton button = new BattleOptionButton(x, y.intValue(), minecraft.font, new TranslationTextComponent("category.jrpg.back"), 
                        b -> {
                            goBack();
                        }, this);
                combatOptionButtons.add(addButton(button));
                y.add(font.lineHeight + 4);
            }
            
            this.currentCategory = Optional.of(category);
        }
        else {
            this.currentCategory = Optional.empty();
        }
    }
    
    private CombatAction selectedAction;
    private void selectAction(CombatAction action) {
        firstPerson = false;
        if (action != null) {
            renderItemSelection = !clickedSlot && action.type == ModCombatActions.USE_ITEM;
            
            int x = width - 200;
            MutableInt y = new MutableInt(height - 84);
            clearCombatButtons();
            
            if (action instanceof PlayerBowAction && action.entity == ClientUtil.getClientPlayer()) {
                ClientStuff.allowedItem = item -> !item.isEmpty() && item.getItem() instanceof BowItem;
                firstPerson = true;
            }
            
            if (renderItemSelection) {
                
            }
            else if (action.isFreeAim()) {
                // TODO free aim
            }
            else {
                targetSelection = new TargetSelectionPhase(action);
                targetSelection.onOpen();
                if (targetSelection.possibleTargets.isEmpty()) {
                    useAction();
                    return;
                }
                else {
                    for (LivingEntity target : targetSelection.possibleTargets) {
                        BattleOptionButton button = new BattleOptionButton(x, y.intValue(), minecraft.font, target.getDisplayName(), 
                                b -> {
                                    useAction();
                                }, this);
                        targetSelection.targetButtonsMap.put(button, target);
                        combatOptionButtons.add(addButton(button));
                        y.add(font.lineHeight + 4);
                    }
                }
            }
            
            if (currentCategory.isPresent()) {
                prevCategories.push(currentCategory.get());
                BattleOptionButton button = new BattleOptionButton(x, y.intValue(), minecraft.font, new TranslationTextComponent("category.jrpg.back"), 
                        b -> {
                            goBack();
                        }, this);
                combatOptionButtons.add(addButton(button));
                y.add(font.lineHeight + 4);
            }
            
            selectedAction = action;
        }
    }
    
    private void renderFrameCheckTarget() {
        if (targetSelection != null) {
            for (Map.Entry<BattleOptionButton, LivingEntity> targetButton : targetSelection.targetButtonsMap.entrySet()) {
                BattleOptionButton button = targetButton.getKey();
                if (button.visible && button.active && button.isHovered()) {
                    targetSelection.setTarget(targetButton.getValue());
                }
            }
        }
    }
    
    protected <T extends Widget> void removeButton(T button) {
        this.buttons.remove(button);
        this.children.remove(button);
    }
    
    
    
    private void useAction() {
        if (targetSelection != null) {
            ModNetworkManager.sendToServer(new ClUseActionPacket(
                    battle.getCurrentTurnEntity().getId(), 
                    targetSelection.currentTarget != null ? targetSelection.currentTarget.getId() : -1, 
                    targetSelection.selectedAction));
            setActive(false);
            
            targetSelection = null;
            currentCategory = Optional.empty();
        }
    }
    
    
    private boolean goBack() {
        if (!prevCategories.isEmpty()) {
            Moveset.Category prevCategory = prevCategories.pop();
            setMovesetCategory(prevCategory, !prevCategories.isEmpty() ? prevCategories.peek() : null);
            targetSelection = null;
            renderItemSelection = false;
            return true;
        }
        
        return false;
    }
    
    
    public static int slotClicked = 69;
    private boolean clickedSlot = false;

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        clickedSlot = false;
        if (renderItemSelection) {
            int slot = getHoveredItemSlot((int) pMouseX, (int) pMouseY);
            if (slot > -1 && slot < minecraft.player.inventory.items.size()) {
                ItemStack item = minecraft.player.inventory.items.get(slot);
                if (canUseItem(item)) {
                    slotClicked = slot;
                    clickedSlot = true;
                    if (selectedAction != null && item.getItem() instanceof ThrowablePotionItem) {
                        selectedAction.targeting = TargetingType.ALL;
                    }
                    selectAction(selectedAction);
                }
            }
            
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        if (combatOptionButtons.size() > MAX_LINES_FITTING) {
            int newVal = MathHelper.clamp(combatOptionsScroll - (int) pDelta, 0, combatOptionButtons.size() - MAX_LINES_FITTING);
            if (combatOptionsScroll != newVal) {
                for (Button button : combatOptionButtons) {
                    button.y += (combatOptionsScroll - newVal) * (font.lineHeight + 4);
                }
                combatOptionsScroll = newVal;
            }
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.player == battle.getCurrentTurnEntity() && ClientStuff.walkingSwitch.matches(keyCode, scanCode)) {
            setActive(false);
            return true;
        }
        
        switch (keyCode) {
        case GLFW.GLFW_KEY_ESCAPE:
            if (!goBack() && minecraft.player == battle.getCurrentTurnEntity()) {
                setActive(false);
            }
            return true;
        case GLFW.GLFW_KEY_DOWN:
            changeFocus(true);
            break;
        case GLFW.GLFW_KEY_UP:
            changeFocus(false);
            break;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public boolean keyPressedInactive(int keyCode, int scanCode, int modifiers) {
        if (ClientStuff.walkingSwitch.matches(keyCode, scanCode)) {
            setActive(true);
            return true;
        }
        
        return false;
    }
    
    @Override
    public final boolean isPauseScreen() {
        return false;
    }
    
    private boolean firstPerson = false;
    @Override
    public void tick() {
        minecraft.options.setCameraType(firstPerson ? PointOfView.FIRST_PERSON : PointOfView.THIRD_PERSON_BACK);
        if (cameraMoveTicks > 0) {
            --cameraMoveTicks;
        }
        _2ndCameraObj.tick();
        
        tickCount++;
    }
    
    public boolean noMouseHover = false;
    private double mouseX;
    private double mouseY;
    @Override
    public boolean changeFocus(boolean forward) {
        if (super.changeFocus(forward)) {
            noMouseHover = true;
            mouseX = ClientUtil.mouseX();
            mouseY = ClientUtil.mouseY();
            return true;
        }
        return false;
    }
    
    private void renderFrameCheckNoHover() {
        if (noMouseHover && (mouseX != ClientUtil.mouseX() || mouseY != ClientUtil.mouseY())) {
            noMouseHover = false;
        }
    }
    
    
    
    public static BattleScreen instance;
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void battleCancelHud(RenderGameOverlayEvent.Pre event) {
        if (instance != null) {
            switch (event.getType()) {
            case ALL:
            case BOSSHEALTH: // All boss bars
            case BOSSINFO:    // Individual boss bar
            case PLAYER_LIST:
            case SUBTITLES:
            case POTION_ICONS:
            case VIGNETTE:
            case PORTAL:
            case CHAT:
            case TEXT:
            case DEBUG:
            case FPS_GRAPH:
                break;
            case HELMET:
            case CROSSHAIRS:
            case ARMOR:
            case HEALTH:
            case FOOD:
            case AIR:
            case HOTBAR:
            case EXPERIENCE:
            case HEALTHMOUNT:
            case JUMPBAR:
                event.setCanceled(true);
                break;
            }
            
            if (event.getType() == ElementType.ALL && Minecraft.getInstance().screen != instance) {
                instance.renderScreenInactive(event.getMatrixStack(), event.getPartialTicks());
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void inactiveScreenKeyPress(KeyInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && instance != null && !instance.isActive()) {
            instance.keyPressedInactive(event.getKey(), event.getScanCode(), event.getModifiers());
        }
    }

    private int cameraMoveTicksStarting;
    private int cameraMoveTicks;
    private Entity prevCameraEntity;
    private Entity cameraEntity;
    private ActiveRenderInfo _2ndCameraObj = new ActiveRenderInfo();
    @SubscribeEvent
    public static void cameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (instance != null) {
            ActiveRenderInfo camera = event.getInfo();
            Minecraft mc = instance.minecraft;
            double partialTick = event.getRenderPartialTicks();
            if (instance.cameraEntity == null) {
                instance.cameraEntity = mc.player;
            }
            Entity entity = instance.cameraEntity;
            PointOfView cameraMode = mc.options.getCameraType();

            camera.setup(mc.level, entity, !cameraMode.isFirstPerson(), cameraMode.isMirrored(), (float) partialTick);
            
            if (instance.cameraMoveTicks > 0 && instance.cameraMoveTicksStarting > 0) {
                if (instance.prevCameraEntity == null) {
                    instance.prevCameraEntity = mc.player;
                }
                Entity prevEntity = instance.prevCameraEntity;
                double cameraMoveAmount = (instance.cameraMoveTicksStarting - instance.cameraMoveTicks + partialTick) / instance.cameraMoveTicksStarting;
                
                ActiveRenderInfo camera0 = instance._2ndCameraObj;
                camera0.setup(mc.level, prevEntity, !cameraMode.isFirstPerson(), cameraMode.isMirrored(), (float) partialTick);
                
                Vector3d position = new Vector3d(
                        MathHelper.lerp(cameraMoveAmount, camera0.getPosition().x, camera.getPosition().x),
                        MathHelper.lerp(cameraMoveAmount, camera0.getPosition().y, camera.getPosition().y),
                        MathHelper.lerp(cameraMoveAmount, camera0.getPosition().z, camera.getPosition().z)
                        );
                float xRot = MathHelper.lerp((float) cameraMoveAmount, camera0.getXRot(), camera.getXRot());
                float yRot0 = camera0.getYRot();
                float yRot1 = camera.getYRot();
                while (Math.abs(yRot0 - yRot1) > 180) {
                    if (yRot1 > yRot0) {
                        yRot1 -= 360;
                    }
                    else {
                        yRot0 -= 360;
                    }
                }
                float yRot = MathHelper.lerp((float) cameraMoveAmount, yRot0, yRot1);
                
                camera.setPosition(position);
                camera.setRotation(yRot, xRot);
                camera.setAnglesInternal(yRot, xRot);
            }
            
            event.setYaw(camera.getYRot());
            event.setPitch(camera.getXRot());
        }
    }
    
    public void setCameraEntity(Entity entity) {
        this.prevCameraEntity = this.cameraEntity;
        this.cameraEntity = entity;
        cameraMoveTicksStarting = 10;
        cameraMoveTicks = cameraMoveTicksStarting;
    }
    
    
    
    public void giveChoise() {
        ITextComponent name1 = new TranslationTextComponent("jrpg.stand_user_spare");
        ITextComponent name2 = new TranslationTextComponent("jrpg.stand_user_kill");
        int x = width / 2;
        int y = height / 2;
        BattleOptionButton button1 = new BattleOptionButton(x - font.width(name1) / 2, y - 6, minecraft.font, name1, 
                b -> {
                    ModNetworkManager.sendToServer(new ClStandUserChoisePacket(true));
                }, this);
        
        BattleOptionButton button2 = new BattleOptionButton(x - font.width(name2) / 2, y + 6, minecraft.font, name2, 
                b -> {
                    ModNetworkManager.sendToServer(new ClStandUserChoisePacket(false));
                }, this);

        setActive(true);
        addButton(button1);
        addButton(button2);
    }
}
