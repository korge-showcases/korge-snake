import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.tiles.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.view.tiles.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()

	sceneContainer.changeTo { MyScene() }
}

class MyScene : PixelatedScene(320, 240) {
	override suspend fun SContainer.sceneMain() {
        val tilesIDC = resourcesVfs["tiles.ase"].readImageDataContainer(ASE)
        val tiles = tilesIDC.mainBitmap.slice()

        val tileSet = TileSet(tiles.splitInRows(16, 16).mapIndexed { index, slice -> TileSetTileInfo(index, slice) })
        val tileMap = tileMap(IntArray2(32, 32, 0), tileSet)

        for (n in 0 until 64) {
            tileMap.map[n, 0] = TileInfo(n, SliceOrientation.NORMAL)
        }

        //var tile = 1
        //var tile = 11
        var tile = 4
        var orientation = SliceOrientation.NORMAL
        tileMap.map[4, 4] = TileInfo(tile, orientation)

        fun updateOrientation(update: (SliceOrientation) -> SliceOrientation) {
            orientation = update(orientation)
            println("orientation=$orientation")
            tileMap.lock {
                tileMap.map[4, 4] = TileInfo(tile, orientation)
            }
        }

        keys {
            down(Key.LEFT) { updateOrientation { it.rotatedLeft() } }
            down(Key.RIGHT) { updateOrientation { it.rotatedRight() } }
            down(Key.Y) { updateOrientation { it.flippedY() } }
            down(Key.X) { updateOrientation { it.flippedX() } }
        }
        //tileMap.map[5, 4] = TileInfo(11, SliceOrientation.MIRROR_HORIZONTAL_ROTATE_180)

        //ImageOrientation.ROTATE_0.rotatedRight()
        //image(tiles)
    }
}

fun TileInfo(tile: Int, rotation: SliceOrientation = SliceOrientation.NORMAL, offsetX: Int = 0, offsetY: Int = 0): TileInfo {
    val flipX = listOf(0, 1, 1, 0,   1, 1, 0, 0)
    val flipY = listOf(0, 0, 1, 1,   0, 1, 1, 0)
    val rot   = listOf(0, 1, 0, 1,   0, 1, 0, 1)
    return TileInfo(tile, offsetX, offsetY, flipX[rotation.raw] != 0, flipY[rotation.raw] != 0, rot[rotation.raw] != 0)
}

val TileMap.map: TileMapDataInfo get() = TileMapDataInfo(this)

inline class TileMapDataInfo(val tileMap: TileMap) {
    val data get() = tileMap.stackedIntMap

    /** Annotation of where in [startX] this stack would be placed in a bigger container, not used for set or get methods */
    val startX: Int get() = data.startX
    /** Annotation of where in [startY] this stack would be placed in a bigger container, not used for set or get methods */
    val startY: Int get() = data.startY

    /** [width] of the data available here, get and set methods use values in the range x=0 until [width] */
    val width: Int get() = data.width
    /** [height] of the data available here, get and set methods use values in the range y=0 until [height] */
    val height: Int get() = data.height

    /** The [empty] value that will be returned if the specified cell it out of bounds, or empty */
    val empty: TileInfo get() = TileInfo(data.empty)
    /** The maximum level of layers available on the whole stack */
    val maxLevel: Int get() = data.maxLevel

    operator fun set(x: Int, y: Int, data: TileInfo) = set(x, y, 0, data)

    operator fun set(x: Int, y: Int, level: Int, data: TileInfo) {
        if (this.data.inside(x, y)) {
            this.data[x, y, level] = data.data
            tileMap.invalidate()
        }
    }
}
