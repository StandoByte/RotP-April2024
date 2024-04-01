package com.github.standobyte.jrpg.client.battleui.range;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;

public class RangeLimitParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite sprites;

    protected RangeLimitParticle(ClientWorld world, double x, double y, double z, IAnimatedSprite pSprites) {
        super(world, x, y, z);
        quadSize = 0.125f;
        this.sprites = pSprites;
        setSpriteFromAge(pSprites);
    }
    
    @Override
    public void tick() {
        super.tick();
//        if (!removed) {
//            this.setSpriteFromAge(this.sprites);
//        }
    }
    
    

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite sprite) {
            this.spriteSet = sprite;
        }

        @Override
        public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            RangeLimitParticle particle = new RangeLimitParticle(world, x, y, z, spriteSet);
            particle.setLifetime(20);
            return particle;
        }
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
}
