import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.tiles.*
import korlibs.io.file.std.*
import korlibs.korge.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
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
        tileMap.map.push(4, 4, TileInfo(7))
        tileMap.map.push(4, 4, TileInfo(tile, orientation))

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
