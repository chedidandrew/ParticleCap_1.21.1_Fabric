package com.chedidandrew.particlecap;

import net.fabricmc.api.ClientModInitializer;

public class ParticleCapMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// No setup required. Particle culling is handled by mixins.
	}
}
