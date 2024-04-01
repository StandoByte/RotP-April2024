package com.github.standobyte.jrpg.client;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.world.World;

public class ClientUtil {

    public static PlayerEntity getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static World getClientWorld() {
        return Minecraft.getInstance().level;
    }

    public static Entity getEntityById(int entityId) {
        return Minecraft.getInstance().level.getEntity(entityId);
    }


    public static float[] rgb(int color) {
        int[] rgbInt = rgbInt(color);
        return new float[] {
                (float) rgbInt[0] / 255F,
                (float) rgbInt[1] / 255F,
                (float) rgbInt[2] / 255F
        };
    }
    
    public static int[] rgbInt(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return new int[] {red, green, blue};
    }
    
    public static int fromRgb(float r, float g, float b) {
        return ((int) (r * 255) << 16) + ((int) (g * 255) << 8) + (int) (b * 255);
    }
    
    public static int fromRgbInt(int r, int g, int b) {
        return (r << 16) + (g << 8) + b;
    }
    

    public static void drawTooltipRectangle(MatrixStack matrixStack, int x, int y, int width, int height, 
            int backgroundColor, int borderColorStart, int borderColorEnd, int zLevel) {
        RenderSystem.disableRescaleNormal();
        RenderSystem.enableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        matrixStack.pushPose();
        Matrix4f mat = matrixStack.last().pose();
        
        drawGradientRect(mat, zLevel, x - 3, y - 4, x + width + 3, y - 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, zLevel, x - 3, y + height + 3, x + width + 3, y + height + 4, backgroundColor, backgroundColor);
        drawGradientRect(mat, zLevel, x - 3, y - 3, x + width + 3, y + height + 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, zLevel, x - 4, y - 3, x - 3, y + height + 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, zLevel, x + width + 3, y - 3, x + width + 4, y + height + 3, backgroundColor, backgroundColor);
        drawGradientRect(mat, zLevel, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, borderColorStart, borderColorEnd);
        drawGradientRect(mat, zLevel, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, borderColorStart, borderColorEnd);
        drawGradientRect(mat, zLevel, x - 3, y - 3, x + width + 3, y - 3 + 1, borderColorStart, borderColorStart);
        drawGradientRect(mat, zLevel, x - 3, y + height + 2, x + width + 3, y + height + 3, borderColorEnd, borderColorEnd);

        matrixStack.popPose();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableRescaleNormal();
    }
    
    private static void drawGradientRect(Matrix4f mat, int zLevel, int left, int top, int right, int bottom, int startColor, int endColor) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
        float startBlue  = (float)(startColor       & 255) / 255.0F;
        float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
        float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
        float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
        float endBlue    = (float)(endColor         & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.vertex(mat, right,    top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        buffer.vertex(mat,  left,    top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        buffer.vertex(mat,  left, bottom, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
        buffer.vertex(mat, right, bottom, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
        tessellator.end();
    }
    
    
    public static double mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
    }
    
    public static double mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
    }

}
