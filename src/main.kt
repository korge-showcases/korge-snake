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

        var snake = Snake(listOf(PointInt(5, 5)))
            .withExtraMove(SnakeMove.RIGHT)
            .withExtraMove(SnakeMove.RIGHT)
            .withExtraMove(SnakeMove.DOWN)
            .withExtraMove(SnakeMove.DOWN)
            .withExtraMove(SnakeMove.RIGHT)
            .withExtraMove(SnakeMove.UP)
        snake.render(tileMap.map)

        fun updateOrientation(
            updateOffset: (Point) -> Point = { it },
            update: (SliceOrientation) -> SliceOrientation = { it }
        ) {
            offset = updateOffset(offset)
            orientation = update(orientation)
            println("orientation=$orientation")
            tileMap.map[4, 4] = Tile(tile, orientation, offset.x.toInt(), offset.y.toInt())
        }

        fun snakeMove(move: SnakeMove) {
            snake.clear(tileMap.map)
            snake = snake.withExtraMove(move)
            snake.render(tileMap.map)
        }

        keys {
            down(Key.LEFT) { snakeMove(SnakeMove.LEFT) }
            down(Key.RIGHT) { snakeMove(SnakeMove.RIGHT) }
            down(Key.UP) { snakeMove(SnakeMove.UP) }
            down(Key.DOWN) { snakeMove(SnakeMove.DOWN) }


            down(Key.LEFT) { updateOrientation { it.rotatedLeft() } }
            down(Key.RIGHT) { updateOrientation { it.rotatedRight() } }
            down(Key.Y) { updateOrientation { it.flippedY() } }
            down(Key.X) { updateOrientation { it.flippedX() } }
            downFrame(Key.W, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(0, -1) }) }
            downFrame(Key.A, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(-1, 0) }) }
            downFrame(Key.S, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(0, +1) }) }
            downFrame(Key.D, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(+1, 0) }) }
        }
    }
}

enum class SnakeMove(val dir: PointInt) {
    LEFT(PointInt(-1, 0)),
    RIGHT(PointInt(+1, 0)),
    UP(PointInt(0, -1)),
    DOWN(PointInt(0, +1)),
    ;
    val isHorizontal get() = dir.x != 0
    val isVertical get() = dir.y != 0
}

data class Snake(val pos: List<PointInt>) {
    fun withExtraMove(move: SnakeMove): Snake {
        return Snake((pos + (pos.last() + move.dir)).takeLast(10))
    }

    fun pos(index: Int): PointInt = pos.getOrElse(index) { PointInt(0, 0) }
    fun dir(index: Int): SnakeMove {
        val p0 = pos(index)
        val p1 = pos(index + 1)
        return when {
            p1.x > p0.x -> SnakeMove.RIGHT
            p1.x < p0.x -> SnakeMove.LEFT
            p1.y > p0.y -> SnakeMove.DOWN
            p1.y < p0.y -> SnakeMove.UP
            else -> SnakeMove.RIGHT
        }
    }

    fun TileMapData.clearAt(x: Int, y: Int) {
        if (inside(x, y)) {
            while (getStackLevel(x, y) > 0) {
                removeLast(x, y)
            }
        }
    }

    fun clear(map: TileMapData) {
        for (p in pos) {
            map.clearAt(p.x, p.y)
            //map.push()
            //map[p.x, p.y] = Tile(0)
        }
    }

    fun render(map: TileMapData) {
        for ((i, p) in pos.withIndex()) {
            val isFirst = i == 0
            val isLast = i == pos.size - 1
            val p0 = dir(i - 1)
            val p1 = dir(i)
            val tile = when {
                isFirst -> {
                    Tile(1,
                        when (p1) {
                            SnakeMove.RIGHT -> SliceOrientation.NORMAL
                            SnakeMove.LEFT -> SliceOrientation.ROTATE_180
                            SnakeMove.DOWN -> SliceOrientation.ROTATE_90
                            SnakeMove.UP -> SliceOrientation.ROTATE_270
                        }
                        , 0, 0)
                }
                isLast -> Tile(4,
                    when (p0) {
                        SnakeMove.RIGHT -> SliceOrientation.NORMAL
                        SnakeMove.LEFT -> SliceOrientation.ROTATE_180
                        SnakeMove.DOWN -> SliceOrientation.ROTATE_90
                        SnakeMove.UP -> SliceOrientation.ROTATE_270
                    }
                , 0, 0)
                else -> {
                    if (p0 == p1) {
                        Tile(2, when (p0) {
                            SnakeMove.RIGHT -> SliceOrientation.NORMAL
                            SnakeMove.LEFT -> SliceOrientation.ROTATE_180
                            SnakeMove.DOWN -> SliceOrientation.ROTATE_90
                            SnakeMove.UP -> SliceOrientation.ROTATE_270
                        }, 0, 0)
                    } else {
                        Tile(3, when (p0 to p1) {
                            SnakeMove.RIGHT to SnakeMove.DOWN -> SliceOrientation.NORMAL
                            SnakeMove.UP to SnakeMove.LEFT -> SliceOrientation.NORMAL

                            SnakeMove.RIGHT to SnakeMove.UP -> SliceOrientation.ROTATE_90
                            SnakeMove.DOWN to SnakeMove.LEFT -> SliceOrientation.ROTATE_90

                            SnakeMove.LEFT to SnakeMove.UP -> SliceOrientation.ROTATE_180
                            SnakeMove.DOWN to SnakeMove.RIGHT -> SliceOrientation.ROTATE_180

                            SnakeMove.LEFT to SnakeMove.DOWN -> SliceOrientation.ROTATE_270
                            SnakeMove.UP to SnakeMove.RIGHT -> SliceOrientation.ROTATE_270


                            else -> SliceOrientation.ROTATE_270
                        }, 0, 0)
                    }
                    //println("$p0, $p1")
                }
            }
            map.push(p.x, p.y, tile)
        }
    }
}
