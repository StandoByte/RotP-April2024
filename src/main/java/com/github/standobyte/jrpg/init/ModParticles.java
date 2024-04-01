package com.github.standobyte.jrpg.init;

import com.github.standobyte.jrpg.ModMain;

import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ModMain.RES_NAMESPACE);
    
    public static final RegistryObject<BasicParticleType> RANGE_LIMIT = PARTICLES.register("range_limit", () -> new BasicParticleType(false));

}
