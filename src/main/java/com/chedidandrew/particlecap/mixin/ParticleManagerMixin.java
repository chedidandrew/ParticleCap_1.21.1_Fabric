package com.chedidandrew.particlecap.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    private static final int PARTICLE_LIMIT = 5000;

    @Shadow
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    @Shadow
    private Object2IntOpenHashMap<ParticleGroup> groupCounts;

    @Inject(method = "tick", at = @At("TAIL"))
    private void particlecap$enforceParticleLimit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int total = 0;
        for (Queue<Particle> q : particles.values()) {
            total += q.size();
        }
        if (total <= PARTICLE_LIMIT) return;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        // Keep the nearest PARTICLE_LIMIT particles using a fixed-size max-heap keyed by distance.
        final Particle[] heapParticles = new Particle[PARTICLE_LIMIT];
        final double[] heapDistSq = new double[PARTICLE_LIMIT];
        int heapSize = 0;

        for (Queue<Particle> q : particles.values()) {
            for (Particle p : q) {
                ParticleAccessor acc = (ParticleAccessor) p;
                double dx = acc.particlecap$getX() - px;
                double dy = acc.particlecap$getY() - py;
                double dz = acc.particlecap$getZ() - pz;
                double distSq = dx * dx + dy * dy + dz * dz;

                if (heapSize < PARTICLE_LIMIT) {
                    heapParticles[heapSize] = p;
                    heapDistSq[heapSize] = distSq;
                    heapSiftUp(heapParticles, heapDistSq, heapSize);
                    heapSize++;
                } else if (distSq < heapDistSq[0]) {
                    heapParticles[0] = p;
                    heapDistSq[0] = distSq;
                    heapSiftDown(heapParticles, heapDistSq, heapSize, 0);
                }
            }
        }

        Set<Particle> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        for (Queue<Particle> q : particles.values()) {
            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                if (!keep.contains(p)) {
                    it.remove();
                    p.markDead();
                    decrementGroupCount(p);
                }
            }
        }
    }

    private void decrementGroupCount(Particle p) {
        p.getGroup().ifPresent(group -> {
            int current = groupCounts.getInt(group);
            if (current <= 1) {
                groupCounts.removeInt(group);
            } else {
                groupCounts.put(group, current - 1);
            }
        });
    }

    // Max-heap helpers for parallel arrays (heapDistSq is the key, largest at root).
    private static void heapSiftUp(Particle[] ps, double[] ds, int idx) {
        while (idx > 0) {
            int parent = (idx - 1) >>> 1;
            if (ds[parent] >= ds[idx]) return;
            swap(ps, ds, parent, idx);
            idx = parent;
        }
    }

    private static void heapSiftDown(Particle[] ps, double[] ds, int size, int idx) {
        while (true) {
            int left = (idx << 1) + 1;
            if (left >= size) return;

            int right = left + 1;
            int largest = left;

            if (right < size && ds[right] > ds[left]) {
                largest = right;
            }

            if (ds[idx] >= ds[largest]) return;

            swap(ps, ds, idx, largest);
            idx = largest;
        }
    }

    private static void swap(Particle[] ps, double[] ds, int a, int b) {
        Particle tp = ps[a];
        ps[a] = ps[b];
        ps[b] = tp;

        double td = ds[a];
        ds[a] = ds[b];
        ds[b] = td;
    }
}
