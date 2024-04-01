package com.github.standobyte.jrpg.client.battleui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.standobyte.jojo.client.ui.tooltip.ITooltipLine;
import com.github.standobyte.jojo.client.ui.tooltip.TextTooltipLine;
import com.github.standobyte.jrpg.statuseffect.TurnBasedEffectInstance;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

public class EffectTooltipLine implements ITooltipLine {
    private final TextureAtlasSprite sprite;
    private final ITextComponent nameAmplifier;
    private final ITextComponent duration;
    
    public EffectTooltipLine(TurnBasedEffectInstance effect) {
        EffectInstance vanillaEff = effect.vanillaEffect;
        this.sprite = Minecraft.getInstance().getMobEffectTextures().get(vanillaEff.getEffect());
        
        String s = I18n.get(vanillaEff.getEffect().getDescriptionId());
        if (vanillaEff.getAmplifier() >= 1 && vanillaEff.getAmplifier() <= 9) {
            s = s + ' ' + I18n.get("enchantment.level." + (vanillaEff.getAmplifier() + 1));
        }
        this.nameAmplifier = new StringTextComponent(s)
                .withStyle(effect.getEffect().getCategory().getTooltipFormatting());
        
        if (vanillaEff.isNoCounter()) {
            this.duration = new StringTextComponent("**");
        }
        else {
            this.duration = new TranslationTextComponent("jrpg.turn_based_eff_duration", effect.getTurnsDuration());
        }
    }

    @Override
    public void draw(MatrixStack matrixStack, float x, float y, FontRenderer font) {
        Minecraft.getInstance().getTextureManager().bind(sprite.atlas().location());
        AbstractGui.blit(matrixStack, (int) x + 2, (int) y + 2, 0, 18, 18, sprite);
        IRenderTypeBuffer.Impl renderType = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
        font.drawInBatch(LanguageMap.getInstance().getVisualOrder(nameAmplifier), x + 22, y + 2, -1, 
                true, matrixStack.last().pose(), renderType, false, 0, 0xF000F0);
        font.drawInBatch(LanguageMap.getInstance().getVisualOrder(duration), x + 22, y + 12, -1, 
                true, matrixStack.last().pose(), renderType, false, 0, 0xF000F0);
        renderType.endBatch();
    }

    @Override
    public int getWidth(FontRenderer font) {
        return 24 + Math.max(font.width(nameAmplifier), font.width(duration));
    }

    @Override
    public int getHeight(FontRenderer font) {
        return 22;
    }
    
    @Override
    public List<ITooltipLine> split(int width, FontRenderer font, Style style) {
        List<ITooltipLine> list = new ArrayList<>();
        list.addAll(font.getSplitter().splitLines(nameAmplifier, width, style)
                .stream().map(TextTooltipLine::new).collect(Collectors.toList()));
        list.addAll(font.getSplitter().splitLines(duration, width, style)
                .stream().map(TextTooltipLine::new).collect(Collectors.toList()));
        return list;
    }
    
    @Override
    public Stream<ITextProperties> getTextOnly() {
        return Stream.of(nameAmplifier, duration);
    }

}
