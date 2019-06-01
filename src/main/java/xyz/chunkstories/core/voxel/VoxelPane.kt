//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.ChunkMeshRenderingInterface
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.CellData

class VoxelPane(definition: VoxelDefinition) : Voxel(definition) {

    private val baseModel: Model
    private val backPartMode: Model
    private val frontPartModel: Model
    private val mappedOverrides = mapOf(0 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")),
            1 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

    init {
        baseModel = definition.store.parent().models[definition.resolveProperty("model", "voxels/blockmodels/glass_pane/glass_pane.dae")]
        backPartMode = definition.store.parent().models[definition.resolveProperty("model", "voxels/blockmodels/glass_pane/glass_pane_back_half.dae")]
        frontPartModel = definition.store.parent().models[definition.resolveProperty("model", "voxels/blockmodels/glass_pane/glass_pane_front_half.dae")]

        customRenderingRoutine = { cell ->
            render(cell, this)
        }
    }

    fun render(cell: CellData, mesher: ChunkMeshRenderingInterface) {
        var vox: Voxel?
        vox = cell.getNeightborVoxel(0)
        val connectLeft = vox!!.solid && vox.opaque || vox == this
        vox = cell.getNeightborVoxel(1)
        val connectFront = vox!!.solid && vox.opaque || vox == this
        vox = cell.getNeightborVoxel(2)
        val connectRight = vox!!.solid && vox.opaque || vox == this
        vox = cell.getNeightborVoxel(3)
        val connectBack = vox!!.solid && vox.opaque || vox == this

        fun arity(b: Boolean): Int = if(b) 1 else 0

        val arity = arity(connectLeft) + arity(connectRight) + arity(connectFront) + arity((connectBack))

        when {
            arity == 0 -> {
                mesher.addModel(baseModel, materialsOverrides = mappedOverrides)
                val matrix = Matrix4f()
                matrix.translate(0.5f, 0f, 0.5f)
                matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
                matrix.translate(-0.5f, 0f, -0.5f)
                mesher.addModel(baseModel, matrix, mappedOverrides)
                return
            }
            arity == 2 -> {
                if(connectBack && connectFront) {
                    mesher.addModel(baseModel, materialsOverrides = mappedOverrides)
                    return
                }
                else if(connectLeft && connectRight) {
                    val matrix = Matrix4f()
                    matrix.translate(0.5f, 0f, 0.5f)
                    matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
                    matrix.translate(-0.5f, 0f, -0.5f)
                    mesher.addModel(baseModel, matrix, mappedOverrides)
                    return
                }
            }
        }

        if (connectBack) {
            val matrix = Matrix4f()
            matrix.translate(0.5f, 0f, 0.5f)
            //matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
            matrix.translate(-0.5f, 0f, -0.5f)
            mesher.addModel(backPartMode, materialsOverrides = mappedOverrides)
        }
        if (connectFront) {
            val matrix = Matrix4f()
            matrix.translate(0.5f, 0f, 0.5f)
            //matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
            matrix.translate(-0.5f, 0f, -0.5f)
            mesher.addModel(frontPartModel, materialsOverrides = mappedOverrides)
        }
        if (connectLeft) {
            val matrix = Matrix4f()
            matrix.translate(0.5f, 0f, 0.5f)
            matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
            matrix.translate(-0.5f, 0f, -0.5f)
            mesher.addModel(backPartMode, matrix, mappedOverrides)
        }
        if (connectRight) {
            val matrix = Matrix4f()
            matrix.translate(0.5f, 0f, 0.5f)
            matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
            matrix.translate(-0.5f, 0f, -0.5f)
            mesher.addModel(frontPartModel, matrix, mappedOverrides)
        }
    }

    override fun getCollisionBoxes(info: CellData): Array<Box>? {
        // System.out.println("kek");
        var boxes: Array<Box>? = null

        var vox: Voxel?
        vox = info.getNeightborVoxel(0)
        val connectLeft = vox!!.solid && vox.opaque || vox == this
        vox = info.getNeightborVoxel(1)
        val connectFront = vox!!.solid && vox.opaque || vox == this
        vox = info.getNeightborVoxel(2)
        val connectRight = vox!!.solid && vox.opaque || vox == this
        vox = info.getNeightborVoxel(3)
        val connectBack = vox!!.solid && vox.opaque || vox == this

        if (connectLeft && connectFront && connectRight && connectBack) {
            boxes = arrayOf(Box(0.45, 0.0, 0.0, 0.1, 1.0, 1.0), Box(0.0, 0.0, 0.45, 1.0, 1.0, 0.1))
        } else if (connectLeft && connectFront && connectRight)
            boxes = arrayOf(Box(0.0, 0.0, 0.45, 1.0, 1.0, 0.1), Box(0.1, 1.0, 0.5).translate(0.45, 0.0, 0.5))
        else if (connectLeft && connectFront && connectBack)
            boxes = arrayOf(Box(0.45, 0.0, 0.0, 0.1, 1.0, 1.0), Box(0.5, 1.0, 0.1).translate(0.0, 0.0, 0.45))
        else if (connectLeft && connectBack && connectRight)
            boxes = arrayOf(Box(0.0, 0.0, 0.45, 1.0, 1.0, 0.1), Box(0.1, 1.0, 0.5).translate(0.45, 0.0, 0.0))
        else if (connectBack && connectFront && connectRight)
            boxes = arrayOf(Box(0.45, 0.0, 0.0, 0.1, 1.0, 1.0), Box(0.5, 1.0, 0.1).translate(0.5, 0.0, 0.45))
        else if (connectLeft && connectRight)
            boxes = arrayOf(Box(0.0, 0.0, 0.45, 1.0, 1.0, 0.1))
        else if (connectFront && connectBack)
            boxes = arrayOf(Box(0.45, 0.0, 0.0, 0.1, 1.0, 1.0))
        else if (connectLeft && connectBack)
            boxes = arrayOf(Box(0.55, 1.0, 0.1).translate(0.0, 0.0, 0.45), Box(0.1, 1.0, 0.55).translate(0.45, 0.0, 0.0))
        else if (connectRight && connectBack)
            boxes = arrayOf(Box(0.55, 1.0, 0.1).translate(0.45, 0.0, 0.45), Box(0.1, 1.0, 0.55).translate(0.45, 0.0, 0.0))
        else if (connectLeft && connectFront)
            boxes = arrayOf(Box(0.55, 1.0, 0.1).translate(0.0, 0.0, 0.45), Box(0.1, 1.0, 0.55).translate(0.45, 0.0, 0.45))
        else if (connectRight && connectFront)
            boxes = arrayOf(Box(0.55, 1.0, 0.1).translate(0.45, 0.0, 0.45), Box(0.1, 1.0, 0.55).translate(0.45, 0.0, 0.45))
        else if (connectLeft)
            boxes = arrayOf(Box(0.0, 0.0, 0.45, 0.5, 1.0, 0.1).translate(0.0, 0.0, 0.0))
        else if (connectRight)
            boxes = arrayOf(Box(0.0, 0.0, 0.45, 0.5, 1.0, 0.1).translate(0.5, 0.0, 0.0))
        else if (connectFront)
            boxes = arrayOf(Box(0.45, 0.0, 0.0, 0.1, 1.0, 0.5).translate(0.0, 0.0, 0.5))
        else if (connectBack)
            boxes = arrayOf(Box(0.45, 0.0, 0.0, 0.1, 1.0, 0.5).translate(0.0, 0.0, 0.0))
        else
            boxes = arrayOf(Box(0.45, 0.0, 0.0, 0.1, 1.0, 1.0), Box(0.0, 0.0, 0.45, 1.0, 1.0, 0.1))

        // for (CollisionBox box : boxes)
        // box.translate(+0.5, -0, +0.5);

        return boxes
    }
}