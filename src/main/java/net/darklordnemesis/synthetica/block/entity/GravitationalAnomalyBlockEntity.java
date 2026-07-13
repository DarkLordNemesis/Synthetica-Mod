package net.darklordnemesis.synthetica.block.entity;

import net.darklordnemesis.synthetica.block.entity.ModBlockEntities;
import net.darklordnemesis.synthetica.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GravitationalAnomalyBlockEntity extends BlockEntity {
    private double mass = 10.0;

    public GravitationalAnomalyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITATIONAL_ANOMALY_BE.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state, GravitationalAnomalyBlockEntity be) {
        if (level.isClientSide) return;

        double radius = Math.sqrt(this.mass) * 1.5;
        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // --- 1. EVERY 10 TICKS: EAT 1-3 BLOCKS ---
        if (level.getGameTime() % 10 == 0) {
            int blocksToEat = level.random.nextInt(3) + 1; // 1 to 3 blocks
            int attempts = 0;
            int eatenCount = 0;

            // Performance fallback: stop searching after 20 random failed checks
            while (eatenCount < blocksToEat && attempts < 20) {
                attempts++;

                // Pick a random coordinate within the gravity sphere
                int rx = level.random.nextInt((int) (radius * 2 + 1)) - (int) radius;
                int ry = level.random.nextInt((int) (radius * 2 + 1)) - (int) radius;
                int rz = level.random.nextInt((int) (radius * 2 + 1)) - (int) radius;

                BlockPos targetPos = pos.offset(rx, ry, rz);
                double distSq = targetPos.distToCenterSqr(center.x, center.y, center.z);

                // Make sure it's within the spherical radius, not just the bounding box cube
                if (distSq <= radius * radius && !targetPos.equals(pos)) {
                    BlockState targetState = level.getBlockState(targetPos);

                    // Don't eat air, liquid, bedrock, or itself
                    if (!targetState.isAir() && targetState.getDestroySpeed(level, targetPos) >= 0
                            && !targetState.is(ModBlocks.GRAVITATIONAL_ANOMALY.get())) {

                        // Convert the solid block to air
                        level.setBlockAndUpdate(targetPos, Blocks.AIR.defaultBlockState());

                        // Spawn it as a falling block entity at the center of its old position
                        FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, targetPos, targetState);
                        fallingBlock.setNoGravity(true); // Turn off gravity so it can fly sideways/upwards!
                        fallingBlock.dropItem = false;  // Prevent it dropping as an item if it hits something on the way

                        // Give it an initial velocity kick towards the anomaly
                        Vec3 launchDir = center.subtract(fallingBlock.position()).normalize();
                        fallingBlock.setDeltaMovement(launchDir.scale(0.2));
                        fallingBlock.hurtMarked = true;

                        eatenCount++;
                    }
                }
            }
        }

        // --- 2. ENTITY PULLING LOOP ---
        AABB searchArea = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        List<Entity> entities = level.getEntities((Entity) null, searchArea, entity -> true);

        for (Entity entity : entities) {
            if (entity.isSpectator() || (entity instanceof net.minecraft.world.entity.player.Player p && p.isCreative())) {
                continue;
            }

            Vec3 entityPos = entity.position();
            double distance = center.distanceTo(entityPos);

            if (distance > 0.1) {
                Vec3 direction = center.subtract(entityPos).normalize();
                double pullForce = (radius - distance) / radius * 0.18; // Slightly boosted pull force

                Vec3 currentMovement = entity.getDeltaMovement();
                entity.setDeltaMovement(currentMovement.add(direction.x * pullForce, direction.y * pullForce, direction.z * pullForce));
                entity.hurtMarked = true;
            }

            // --- 3. THE CRUSH ZONE ---
            if (distance <= 0.85) {
                if (entity instanceof ItemEntity itemEntity) {
                    int count = itemEntity.getItem().getCount();
                    this.mass += count * 0.1; // Balanced: Items grant small mass
                    itemEntity.discard();
                    notifyClientOfMassChange();
                }
                else if (entity instanceof FallingBlockEntity fallingBlock) {
                    this.mass += 1.0; // Ripped blocks grant a huge full 1.0 mass!
                    fallingBlock.discard();
                    notifyClientOfMassChange();
                }
                else if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.hurt(level.damageSources().generic(), 5.0F);
                }
            }
        }
    }

    private void notifyClientOfMassChange() {
        this.setChanged();
        if (level != null) {
            // Tells the engine to send the packet data to clients tracking this block chunk
            level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("Mass", this.mass);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Mass")) {
            this.mass = tag.getDouble("Mass");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putDouble("Mass", this.mass);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public double getMass() {
        return this.mass;
    }

    public double getRadius() {
        return Math.sqrt(this.mass) * 1.5;
    }
}