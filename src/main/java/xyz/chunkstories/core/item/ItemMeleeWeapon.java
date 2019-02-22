//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.client.LocalPlayer;
import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.TraitHitboxes;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth;
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.item.ItemDefinition;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.physics.Box;
import xyz.chunkstories.api.physics.EntityHitbox;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.api.world.cell.CellData;
import xyz.chunkstories.api.world.cell.EditableCell;
import xyz.chunkstories.core.entity.traits.TraitEyeLevel;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.Iterator;

public class ItemMeleeWeapon extends ItemWeapon {
    final long swingDuration;
    final long hitTime;
    final double range;

    final float damage;

    final float itemRenderScale;

    long currentSwingStart = 0L;
    boolean hasHitYet = false;
    long cooldownEnd = 0L;

    public ItemMeleeWeapon(ItemDefinition type) {
        super(type);

        swingDuration = Integer.parseInt(type.resolveProperty("swingDuration", "100"));
        hitTime = Integer.parseInt(type.resolveProperty("hitTime", "100"));

        range = Double.parseDouble(type.resolveProperty("range", "3"));
        damage = Float.parseFloat(type.resolveProperty("damage", "100"));

        itemRenderScale = Float.parseFloat(type.resolveProperty("itemRenderScale", "2"));
    }

    /*public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer) {
        ItemRenderer itemRenderer;

        String modelName = getDefinition().resolveProperty("modelObj", "none");
        if (!modelName.equals("none"))
            itemRenderer = new ItemModelRenderer(this, fallbackRenderer, modelName,
                    getDefinition().resolveProperty("modelDiffuse", "none"));
        else
            itemRenderer = new FlatIconItemRenderer(this, fallbackRenderer, getDefinition());

        itemRenderer = new MeleeWeaponRenderer(fallbackRenderer);

        return itemRenderer;
    }*/

    @Override
    public void tickInHand(@NotNull Entity owner, @NotNull ItemPile itemPile) {

        if (currentSwingStart != 0 && !hasHitYet && (System.currentTimeMillis() - currentSwingStart > hitTime)) {
            Controller controller = owner.traits.tryWith(TraitControllable.class, TraitControllable::getController);

            // For now only client-side players can trigger shooting actions
            if (controller != null && controller instanceof LocalPlayer) {
                if (!((LocalPlayer) controller).hasFocus())
                    return;

                LocalPlayer LocalPlayer = (LocalPlayer) controller;

                // Uses fake input to notify server/master of intention to attack.
                LocalPlayer.getInputsManager()
                        .onInputPressed(LocalPlayer.getInputsManager().getInputByName("shootGun"));

                hasHitYet = true;
            }

        }

    }

    @Override
    public boolean onControllerInput(@NotNull Entity entity, @NotNull ItemPile pile, @NotNull Input input, @NotNull Controller controller) {
        if (input.getName().startsWith("mouse.left")) {
            // Checks current swing is done
            if (System.currentTimeMillis() - currentSwingStart > swingDuration) {
                currentSwingStart = System.currentTimeMillis();
                hasHitYet = false;
            }

            return true;
        } else if (input.getName().equals("shootGun") && entity.getWorld() instanceof WorldMaster) {
            // Actually hits
            Vector3dc direction = entity.traits.tryWith(TraitRotation.class, TraitRotation::getDirectionLookingAt);

            Vector3d eyeLocation = new Vector3d(entity.getLocation());
            entity.traits.with(TraitEyeLevel.class, tel -> eyeLocation.x += tel.getEyeLevel());

            // Find wall collision
            Location shotBlock = entity.getWorld().getCollisionsManager().raytraceSolid(eyeLocation, direction, range);
            Vector3d nearestLocation = new Vector3d();

            // Loops to try and break blocks
            while (entity.getWorld() instanceof WorldMaster && shotBlock != null) {
                EditableCell peek = entity.getWorld().peekSafely(shotBlock);

                if (!peek.getVoxel().isAir() && peek.getVoxel().getVoxelMaterial().resolveProperty("bulletBreakable") != null
                        && peek.getVoxel().getVoxelMaterial().resolveProperty("bulletBreakable").equals("true")) {
                    // TODO: Spawn an event to check if it's okay

                    // Destroy it
                    peek.setVoxel(getDefinition().store().parent().voxels().air());

                    spawnDebris(entity, direction, shotBlock);
                    entity.getWorld().getSoundManager().playSoundEffect("sounds/environment/glass.ogg", Mode.NORMAL,
                            shotBlock, (float) Math.random() * 0.2f + 0.9f, 1.0f);

                    // Re-raytrace the ray
                    shotBlock = entity.getWorld().getCollisionsManager().raytraceSolid(eyeLocation, direction, range);
                } else
                    break;
            }

            if (shotBlock != null) {
                Location shotBlockOuter = entity.getWorld().getCollisionsManager().raytraceSolidOuter(eyeLocation,
                        direction, range);
                if (shotBlockOuter != null) {
                    Vector3d normal = shotBlockOuter.sub(shotBlock);

                    double NbyI2x = 2.0 * direction.dot(normal);
                    Vector3d NxNbyI2x = new Vector3d(normal);
                    NxNbyI2x.mul(NbyI2x);

                    Vector3d reflected = new Vector3d(direction);
                    reflected.sub(NxNbyI2x);

                    CellData peek = entity.getWorld().peekSafely(shotBlock);

                    // This seems fine
                    for (Box box : peek.getTranslatedCollisionBoxes()) {
                        Vector3dc thisLocation = box.lineIntersection(eyeLocation, direction);
                        if (thisLocation != null) {
                            if (nearestLocation == null
                                    || nearestLocation.distance(eyeLocation) > thisLocation.distance(eyeLocation))
                                nearestLocation.set(thisLocation);
                        }
                    }

                    // Position adjustements so shot blocks always shoot proper particles
                    if (shotBlock.x() - nearestLocation.x() <= -1.0)
                        nearestLocation.add(-0.01, 0.0, 0.0);
                    if (shotBlock.y() - nearestLocation.y() <= -1.0)
                        nearestLocation.add(0.0, -0.01, 0.0);
                    if (shotBlock.z() - nearestLocation.z() <= -1.0)
                        nearestLocation.add(0.0, 0.0, -0.01);

                    for (int i = 0; i < 25; i++) {
                        Vector3d untouchedReflection = new Vector3d(reflected);

                        Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0,
                                Math.random() * 2.0 - 1.0);
                        random.mul(0.5);
                        untouchedReflection.add(random);
                        untouchedReflection.normalize();

                        untouchedReflection.mul(0.25);

                        Vector3d ppos = new Vector3d(nearestLocation);
                        entity.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag", ppos,
                                untouchedReflection);
                        entity.getWorld().getSoundManager().playSoundEffect(entity.getWorld().peekSafely(shotBlock)
                                        .getVoxel().getVoxelMaterial().resolveProperty("landingSounds"), Mode.NORMAL, ppos, 1,
                                0.25f);
                    }

                    entity.getWorld().getDecalsManager().add(nearestLocation, normal.negate(), new Vector3d(0.5),
                            "bullethole");
                }
            }

            // Hitreg takes place on server bois
            if (entity.getWorld() instanceof WorldMaster) {
                // Iterate over each found entities
                Iterator<Entity> shotEntities = entity.getWorld().getCollisionsManager().rayTraceEntities(eyeLocation,
                        direction, range);
                while (shotEntities.hasNext()) {
                    Entity shotEntity = shotEntities.next();
                    // Don't shoot itself & only living things get shot
                    if (!shotEntity.equals(entity)) {
                        TraitHitboxes hitboxes = shotEntity.traits.get(TraitHitboxes.class);
                        TraitHealth health = shotEntity.traits.get(TraitHealth.class);

                        if (health != null && hitboxes != null) {
                            // Get hit location
                            for (EntityHitbox hitBox : hitboxes.getHitBoxes()) {
                                Vector3dc hitPoint = hitBox.lineIntersection(eyeLocation, direction);

                                if (hitPoint == null)
                                    continue;

                                // Deal damage
                                health.damage(pileAsDamageCause(pile), hitBox, (float) damage);

                                // Spawn blood particles
                                Vector3d bloodDir = new Vector3d();
                                direction.normalize(bloodDir).mul(0.25);
                                for (int i = 0; i < 250; i++) {
                                    Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0,
                                            Math.random() * 2.0 - 1.0);
                                    random.mul(0.25);
                                    random.add(bloodDir);

                                    shotEntity.getWorld().getParticlesManager()
                                            .spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
                                }

                                // Spawn blood on walls
                                if (nearestLocation != null)
                                    shotEntity.getWorld().getDecalsManager().add(nearestLocation, bloodDir,
                                            new Vector3d(3.0), "blood");
                            }
                        }
                    }
                }
            }

        }
        return false;
    }

    static void spawnDebris(Entity entity, Vector3dc direction, Location shotBlock) {
        for (int i = 0; i < 25; i++) {
            Vector3d smashedVoxelParticleDirection = new Vector3d(direction);
            smashedVoxelParticleDirection.mul(2.0);
            smashedVoxelParticleDirection.add(Math.random() - 0.5, Math.random() - 0.5,
                    Math.random() - 0.5);
            smashedVoxelParticleDirection.normalize();

            entity.getWorld().getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag",
                    shotBlock, smashedVoxelParticleDirection);
        }
    }

    /*class MeleeWeaponRenderer extends ItemRenderer {
        MeleeWeaponRenderer(ItemRenderer fallbackRenderer) {
            super(fallbackRenderer);
        }

        @Override
        public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world,
                                      Location location, Matrix4f handTransformation) {
            Matrix4f matrixed = new Matrix4f(handTransformation);

            float rot = 0;

            ItemMeleeWeapon instance = (ItemMeleeWeapon) pile.getItem();

            if (System.currentTimeMillis() - instance.currentSwingStart < instance.swingDuration) {
                if (instance.hitTime == instance.swingDuration) {
                    // Whole thing over the same duration
                    rot = (float) (0 - Math.PI / 4f * (float) (System.currentTimeMillis() - instance.currentSwingStart)
                            / instance.hitTime);
                } else {
                    // We didn't hit yet
                    if (System.currentTimeMillis() - instance.currentSwingStart < instance.hitTime)
                        rot = (float) (0 - Math.PI / 4f
                                * (float) (System.currentTimeMillis() - instance.currentSwingStart) / instance.hitTime);
                        // We did
                    else
                        rot = (float) (0 - Math.PI / 4f + Math.PI / 4f
                                * (float) (System.currentTimeMillis() - instance.currentSwingStart - instance.hitTime)
                                / (instance.swingDuration - instance.hitTime));

                }
            }

            float dekal = -0.45f;
            matrixed.translate(new Vector3f(0, dekal, 0));
            matrixed.rotate(rot, new Vector3f(0, 0, 1));
            matrixed.translate(new Vector3f(0, 0.25f - dekal, 0));

            matrixed.scale(new Vector3f(instance.itemRenderScale));

            super.renderItemInWorld(renderingInterface, pile, world, location, matrixed);
        }
    }*/
}