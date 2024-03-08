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
import kotlin.time.Duration.Companion.milliseconds

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()

	sceneContainer.changeTo { MyScene() }
}

class MyScene : PixelatedScene(256, 196) {
	override suspend fun SContainer.sceneMain() {
        val tilesIDC = resourcesVfs["tiles.ase"].readImageDataContainer(ASE)
        val tiles = tilesIDC.mainBitmap.slice()

        val tileSet = TileSet(tiles.splitInRows(16, 16).mapIndexed { index, slice -> TileSetTileInfo(index, slice) })
        val tileMap = tileMap(TileMapData(32, 32, tileSet = tileSet))

        for (n in 0 until 64) {
            tileMap.map[n, 0] = Tile(n, SliceOrientation.NORMAL)
        }

        //var tile = 1
        //var tile = 11
        //var tile = 4
        var tile = 5
        var offset = Point(0, 0)
        var orientation = SliceOrientation.NORMAL
        tileMap.map.push(4, 4, Tile(7))
        tileMap.map.push(4, 4, Tile(tile, orientation))

        fun updateOrientation(updateOffset: (Point) -> Point = { it }, update: (SliceOrientation) -> SliceOrientation = { it }) {
            offset = updateOffset(offset)
            orientation = update(orientation)
            println("orientation=$orientation")
            tileMap.map[4, 4] = Tile(tile, orientation, offset.x.toInt(), offset.y.toInt())
        }

        keys {
            down(Key.LEFT) { updateOrientation { it.rotatedLeft() } }
            down(Key.RIGHT) { updateOrientation { it.rotatedRight() } }
            down(Key.Y) { updateOrientation { it.flippedY() } }
            down(Key.X) { updateOrientation { it.flippedX() } }
            downFrame(Key.W, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(0, -1) }) }
            downFrame(Key.A, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(-1, 0) }) }
            downFrame(Key.S, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(0, +1) }) }
            downFrame(Key.D, dt = 16.milliseconds) { updateOrientation (updateOffset = { it + Point(+1, 0) }) }
        }
    }
}
