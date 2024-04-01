package com.github.standobyte.jrpg.stats;

import java.util.UUID;

import com.github.standobyte.jrpg.init.ModEntityAttributes;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum RPGStat {
    HEALTH(Attributes.MAX_HEALTH,                           StatsTable.HP_GAIN,         UUID.fromString("78431cb9-11b0-4dea-9c4a-e8ea45857d7b"), new TranslationTextComponent("jrpg.stat.hp")),
    STRENGTH(Attributes.ATTACK_DAMAGE,                      StatsTable.STRENGTH_GAIN,   UUID.fromString("a66b8ca6-a17b-49bb-bb43-92e6933d7abc"), new TranslationTextComponent("jrpg.stat.strength")),
    SPEED(Attributes.ATTACK_SPEED,                          StatsTable.SPEED_GAIN,      UUID.fromString("b56754fb-0dbb-4ce4-8930-d5e04376d17d"), new TranslationTextComponent("jrpg.stat.speed")),
    DURABILITY(ModEntityAttributes.DURABILITY.get(),        StatsTable.DURABILITY_GAIN, UUID.fromString("da1f16c4-99ee-47c8-9a74-26f464e0ce66"), new TranslationTextComponent("jrpg.stat.durability")),
    PRECISION(ModEntityAttributes.PRECISION.get(),          StatsTable.PRECISION_GAIN,  UUID.fromString("17972ceb-a4bd-4c51-946f-c0699483912f"), new TranslationTextComponent("jrpg.stat.precision")),
    SPIRIT(ModEntityAttributes.SPIRIT.get(),                StatsTable.SPIRIT_GAIN,     UUID.fromString("3e52d9f2-bdbd-4383-a798-6693f3359405"), new TranslationTextComponent("jrpg.stat.spirit")),
    STAND_STAMINA(ModEntityAttributes.STAND_STAMINA.get(),  StatsTable.SP_GAIN,         UUID.fromString("2f9d02b3-7526-4d89-b826-35221737916e"), new TranslationTextComponent("jrpg.stat.sp"));
    
    public final Attribute attribute;
    public final double[] lvlIncreaseTable;
    public final UUID modifierId;
    public final TextComponent name;
    
    private RPGStat(Attribute attribute, double[] lvlIncreaseTable, UUID modifierId, TextComponent name) {
        this.attribute = attribute;
        this.lvlIncreaseTable = lvlIncreaseTable;
        this.modifierId = modifierId;
        this.name = name;
    }
}
