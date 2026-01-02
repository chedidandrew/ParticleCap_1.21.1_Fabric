package com.chedidandrew.particlecap;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class ParticleCapModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.of("Particle Cap Config"));

            ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder.startIntField(Text.of("Particle Limit"), ParticleCapConfig.instance.particleLimit)
                    .setDefaultValue(5000)
                    .setTooltip(Text.of("The maximum number of particles allowed."))
                    .setSaveConsumer(newValue -> ParticleCapConfig.instance.particleLimit = newValue)
                    .build());

            builder.setSavingRunnable(ParticleCapConfig::save);

            return builder.build();
        };
    }
}