//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.zombie

import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.core.entity.EntityHumanoidRenderer

class ZombieRenderer(entity: EntityZombie) : EntityHumanoidRenderer(entity, Unit.let {
	MeshMaterial("zombie", mapOf(
			"albedoTexture" to "./models/human/zombie_s" + (entity.stage.ordinal + 1) + ".png"
	))
} )