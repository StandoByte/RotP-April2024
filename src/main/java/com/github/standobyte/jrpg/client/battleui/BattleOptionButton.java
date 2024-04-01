package com.github.standobyte.jrpg.client.battleui;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class BattleOptionButton extends Button {
    private final BattleScreen screen;
    private final FontRenderer font;
    public boolean isSelected;

    public BattleOptionButton(int pX, int pY, FontRenderer font, 
            ITextComponent pMessage, IPressable pOnPress, ITooltip pOnTooltip, 
            BattleScreen screen) {
        super(pX, pY, -1, font.lineHeight, pMessage, pOnPress, pOnTooltip);
        this.width = font.width(pMessage) + 20;
        this.font = font;
        this.screen = screen;
    }

    public BattleOptionButton(int pX, int pY, FontRenderer font, 
            ITextComponent pMessage, IPressable pOnPress, 
            BattleScreen screen) {
        this(pX, pY, font, pMessage, pOnPress, NO_TOOLTIP, screen);
    }

    @Override
    public void renderButton(MatrixStack pMatrixStack, int pMouseX, int pMouseY, float pPartialTicks) {
        this.isHovered &= !screen.noMouseHover;
        isSelected = isHovered() || isFocused();
        
        if (isHovered) {
            font.draw(pMatrixStack, new StringTextComponent("> ").append(getMessage()), x, y, 0xFFFF55);
        }
        else {
            font.draw(pMatrixStack, getMessage(), x, y, 0xFFFFFF);
        }
        
        if (isHovered) {
            renderToolTip(pMatrixStack, pMouseX, pMouseY);
        }
    }
}
