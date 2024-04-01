package com.github.standobyte.jrpg.client.battleui.range;

import java.util.ArrayList;
import java.util.List;

import com.github.standobyte.jojo.util.general.MathUtil;
import com.github.standobyte.jrpg.client.ClientStuff;
import com.github.standobyte.jrpg.init.ModParticles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class MovementRangeCircle {
    private final ParticleManager particleManager;
    private World world;
    private Vector3d center;
    private double range;
    private List<Vector3d> particlesPos;
    
    private int tickCount = 0;
    private List<Particle> particles = new ArrayList<>();
    
    public MovementRangeCircle(World world, LivingEntity entity) {
        this(world, entity.position(), entity.getAttributeValue(Attributes.MOVEMENT_SPEED) * (entity instanceof PlayerEntity ? 90 : 30));
    }
    
    public MovementRangeCircle(World world, Vector3d center, double range) {
        this.particleManager = Minecraft.getInstance().particleEngine;
        this.world = world;
        this.center = center;
        this.range = range;
        
        Vector3d rangeVec = new Vector3d(range, 0, 0);
        int n = (int) (range * 40);
        particlesPos = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double angle = (double) (i * 2) * Math.PI / n;
            Vector3d vec = center.add(rangeVec.yRot((float) angle));
            BlockPos blockPos = new BlockPos(vec.x, center.y, vec.z);
            
            particlesPos.add(new Vector3d(vec.x, blockPos.getY(), vec.z));
        }
    }
    
    
    public void tick() {
        if (tickCount++ % 11 == 0) {
            particles.clear();
            for (Vector3d pos : particlesPos) {
                Particle particle = particleManager.createParticle(ModParticles.RANGE_LIMIT.get(), pos.x, pos.y, pos.z, 0, 0, 0);
                particle.setColor(0.7f, 0.7f, 1.0f);
                particleManager.add(particle);
                particles.add(particle);
            }
        }
    }
    
    public static void remove() {
        if (ClientStuff.movementRange != null) {
            for (Particle particle : ClientStuff.movementRange.particles) {
                particle.remove();
            }
            ClientStuff.movementRange = null;
        }
    }
    
    public boolean isInRange(Vector3d pos) {
        pos = new Vector3d(pos.x, center.y, pos.z);
        return pos.distanceToSqr(center) <= range * range;
    }
    
    public Vector2f limitMovement(PlayerEntity player, Vector2f movementInput) {
        Vector3d movVec = new Vector3d(movementInput.x, 0, movementInput.y).yRot(-player.yRot * MathUtil.DEG_TO_RAD);
        Vector3d playerCurPos = new Vector3d(player.getX(), center.y, player.getZ());
        Vector3d playerNextPos = playerCurPos.add(movVec);
        double dist = playerCurPos.subtract(center).length();
        double distNext = playerNextPos.subtract(center).length();
        if (distNext > dist && distNext > range) {
            double len = Math.max(range - dist, 0);
            movVec = movVec.normalize().scale(len).yRot(player.yRot * MathUtil.DEG_TO_RAD);
            return new Vector2f((float) movVec.x, (float) movVec.z);
        }
        
        return movementInput;
    }
}
