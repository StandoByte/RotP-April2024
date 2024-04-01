package com.github.standobyte.jrpg.client;

import java.util.Deque;
import java.util.LinkedList;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;

import com.github.standobyte.jrpg.ModMain;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = ModMain.MOD_ID, value = Dist.CLIENT)
public class RpgMessages {
    
    private static Deque<Pair<ITextComponent, MutableInt>> messagesQueue = new LinkedList<>();
    
    public static void addMessage(ITextComponent message) {
        int ticks = messagesQueue.isEmpty() ? 60 : 20;
        messagesQueue.add(Pair.of(message, new MutableInt(ticks)));
    }
    
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void render(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == ElementType.ALL) {
            MatrixStack matrixStack = event.getMatrixStack();
            FontRenderer font = Minecraft.getInstance().font;
            int x = event.getWindow().getGuiScaledWidth() / 2;
            int y = event.getWindow().getGuiScaledHeight() - 98;
            for (Pair<ITextComponent, MutableInt> message : messagesQueue) {
                renderLine(matrixStack, message.getLeft(), font, Math.min(message.getRight().floatValue() / 20, 1), x, y);
                y -= font.lineHeight + 4;
                if (y <= 0) {
                    break;
                }
            }
        }
    }
    
    private static void renderLine(MatrixStack matrixStack, ITextComponent message, FontRenderer font, float alpha, float x, float y) {
        int i1 = (int)(alpha * 255.0F);
        if (i1 > 255) {
            i1 = 255;
        }

        if (i1 > 8) {
            RenderSystem.pushMatrix();
            RenderSystem.translatef(x, y, 0.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int k1 = 16777215;
            
            int k = i1 << 24 & -16777216;
            int l = font.width(message);
            
            int i = Minecraft.getInstance().options.getBackgroundColor(0.25f);
            if (i != 0) {
                int j = -l / 2;
                AbstractGui.fill(matrixStack, j - 2, -4 - 2, j + l + 2, -4 + 9 + 2, ColorHelper.PackedColor.multiply(i, 16777215 | k));
            }
            
            font.draw(matrixStack, message, (float)(-l / 2), -4.0F, k1 | k);
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void tick(ClientTickEvent event) {
        switch (event.phase) {
        case START:
            if (!messagesQueue.isEmpty()) {
                Pair<ITextComponent, MutableInt> lastMsg = messagesQueue.peek();
                if (lastMsg.getRight().decrementAndGet() <= 0) {
                    messagesQueue.pop();
                }
            }
            break;
        default:
            break;
        }
    }
    
}
