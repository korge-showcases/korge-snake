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
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.korge.view.tiles.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*
import korlibs.math.random.*
import korlibs.time.*
import kotlin.random.*

suspend fun main() = Korge(windowSize = Size(256 * 2, 196 * 2), backgroundColor = Colors.DIMGRAY) {
    val sceneContainer = sceneContainer()

    sceneContainer.changeTo { MyScene() }
}

//class MirroredInt

class MyScene : PixelatedScene(256 * 2, 196 * 2) {
    override suspend fun SContainer.sceneMain() {
        val tilesIDC = resourcesVfs["tiles.ase"].readImageDataContainer(ASE)
        val tiles = tilesIDC.mainBitmap.slice()

        val tileSet = TileSet(tiles.splitInRows(16, 16).mapIndexed { index, slice -> TileSetTileInfo(index, slice) })
        val tileMap = tileMap(TileMapData(32, 24, tileSet = tileSet))
        val snakeMap = tileMap(TileMapData(32, 24, tileSet = tileSet))
        val ints = object : ObservableIntArray2(tileMap.map.width, tileMap.map.height, GROUND) {
            val rules = CombinedRuleMatcher(WallsProvider, AppleProvider)
            override fun updated(rect: RectangleInt) {
                IntGridToTileGrid(this.data, rules, tileMap.map, rect)
            }
        }
        ints.lock {
            ints[RectangleInt(0, 0, ints.width, 1)] = WALL
            ints[RectangleInt(0, 0, 1, ints.height)] = WALL
            ints[RectangleInt(0, ints.height - 1, ints.width, 1)] = WALL
            ints[RectangleInt(ints.width - 1, 0, 1, ints.height)] = WALL
            ints[RectangleInt(4, 4, ints.width / 2, 1)] = WALL
        }

        fun putRandomApple() {
            while (true) {
                val p: PointInt = Random[Rectangle(0, 0, ints.width, ints.height)].toInt()
                if (ints[p] == GROUND) {
                    ints[p] = APPLE
                    break
                }
            }
        }

        putRandomApple()

        //for (n in 0 until 64) tileMap.map[n, 0] = Tile(n, SliceOrientation.NORMAL)

        //var tile = 1
        //var tile = 11
        //var tile = 4
        //var tile = 5
        //var offset = Point(0, 0)
        //var orientation = SliceOrientation.NORMAL
        //tileMap.map.push(4, 4, Tile(7))
        //tileMap.map.push(4, 4, Tile(tile, orientation))

        var snake = Snake(listOf(PointInt(5, 5)), maxLen = 2)
            .withExtraMove(SnakeMove.RIGHT)
            //.withExtraMove(SnakeMove.RIGHT)
        snake.render(ints, snakeMap.map)

        //fun updateOrientation(
        //    updateOffset: (Point) -> Point = { it },
        //    update: (SliceOrientation) -> SliceOrientation = { it }
        //) {
        //    offset = updateOffset(offset)
        //    orientation = update(orientation)
        //    println("orientation=$orientation")
        //    tileMap.map[4, 4] = Tile(tile, orientation, offset.x.toInt(), offset.y.toInt())
        //}

        var direction: SnakeMove = SnakeMove.RIGHT

        fun snakeMove(move: SnakeMove) {
            val oldSnake = snake
            snake = snake.withExtraMove(move)
            val headPos = snake.pos.last()
            if (ints[headPos] == APPLE) {
                ints[headPos] = GROUND
                snake = snake.copy(maxLen = snake.maxLen + 1)
                putRandomApple()
            }
            oldSnake.clear(ints, snakeMap.map)
            snake.render(ints, snakeMap.map)
        }

        interval(0.1.fastSeconds) {
            snakeMove(direction)
        }

        keys {
            down(Key.LEFT) { direction = SnakeMove.LEFT }
            down(Key.RIGHT) { direction = SnakeMove.RIGHT }
            down(Key.UP) { direction = SnakeMove.UP }
            down(Key.DOWN) { direction = SnakeMove.DOWN }

            //down(Key.LEFT) { updateOrientation { it.rotatedLeft() } }
            //down(Key.RIGHT) { updateOrientation { it.rotatedRight() } }
            //down(Key.Y) { updateOrientation { it.flippedY() } }
            //down(Key.X) { updateOrientation { it.flippedX() } }
            //downFrame(Key.W, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(0, -1) }) }
            //downFrame(Key.A, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(-1, 0) }) }
            //downFrame(Key.S, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(0, +1) }) }
            //downFrame(Key.D, dt = 16.milliseconds) { updateOrientation(updateOffset = { it + Point(+1, 0) }) }
        }
    }
}

operator fun IntArray2.set(rect: RectangleInt, value: Int) {
    val l = rect.left.coerceIn(0, width)
    val r = rect.right.coerceIn(0, width)
    val u = rect.top.coerceIn(0, height)
    val d = rect.bottom.coerceIn(0, height)
    for (x in l until r) {
        for (y in u until d) {
            this.setOr(x, y, value)
        }
    }
}

enum class SnakeMove(val dir: PointInt) {
    LEFT(PointInt(-1, 0)),
    RIGHT(PointInt(+1, 0)),
    UP(PointInt(0, -1)),
    DOWN(PointInt(0, +1)),
    NONE(PointInt(0, 0)),
;

    val comingFromLeft get() = this == RIGHT
    val comingFromRight get() = this == LEFT
    val comingFromUp get() = this == DOWN
    val comingFromDown get() = this == UP

    val left get() = this == LEFT
    val right get() = this == RIGHT
    val up get() = this == UP
    val down get() = this == DOWN

    val isHorizontal get() = dir.x != 0
    val isVertical get() = dir.y != 0
    val isNone get() = !isHorizontal && !isVertical
}

data class Snake(val pos: List<PointInt>, val maxLen: Int = 10) {
    fun withExtraMove(move: SnakeMove): Snake {
        return Snake((pos + (pos.last() + move.dir)).takeLast(maxLen), maxLen)
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
            else -> SnakeMove.NONE
        }
    }

    fun TileMapData.clearAt(x: Int, y: Int) {
        if (inside(x, y)) {
            while (getStackLevel(x, y) > 0) {
                removeLast(x, y)
            }
        }
    }

    fun clear(ints: ObservableIntArray2, map: TileMapData) {
        for (p in pos) {
            ints[p] = GROUND
            map.clearAt(p.x, p.y)
            //map.push()
            //map[p.x, p.y] = Tile(0)
        }
    }

    fun render(ints: ObservableIntArray2, map: TileMapData) {
        for ((i, p) in pos.withIndex()) {
            val isFirst = i == 0
            val isLast = i == pos.size - 1
            val p0 = dir(i - 1)
            val p1 = dir(i)

            val tile = when {
                isFirst || isLast -> {
                    val p = if (isLast) p0 else p1
                    (if (isLast) SnakeHeadProvider else SnakeProvider).get(SimpleTileSpec(p.left, p.up, p.right, p.down))
                }
                else -> SnakeProvider.get(SimpleTileSpec(
                    left = p0.comingFromLeft || p1.left,
                    up = p0.comingFromUp || p1.up,
                    right = p0.comingFromRight || p1.right,
                    down = p0.comingFromDown || p1.down
                ))
            }
            map.pushInside(p.x, p.y, tile)
            ints[p] = 3
        }
    }
}

object SnakeProvider : ISimpleTileProvider by (SimpleTileProvider(value = 3).also {
    it.rule(SimpleRule(Tile(1), right = true))
    it.rule(SimpleRule(Tile(2), left = true, right = true))
    it.rule(SimpleRule(Tile(3), left = true, down = true))
})

object SnakeHeadProvider : ISimpleTileProvider by (SimpleTileProvider(value = 3).also {
    it.rule(SimpleRule(Tile(0)))
    it.rule(SimpleRule(Tile(4), right = true))
})

object AppleProvider : ISimpleTileProvider by (SimpleTileProvider(value = 2).also {
    it.rule(SimpleRule(Tile(12)))
})

object WallsProvider : ISimpleTileProvider by (SimpleTileProvider(value = 1).also {
    it.rule(SimpleRule(Tile(16)))
    it.rule(SimpleRule(Tile(17), right = true))
    it.rule(SimpleRule(Tile(18), left = true, right = true))
    it.rule(SimpleRule(Tile(19), left = true, down = true))
    it.rule(SimpleRule(Tile(20), up = true, left = true, down = true))
    it.rule(SimpleRule(Tile(21), up = true, left = true, right = true, down = true))
})

operator fun IntArray2.get(p: PointInt): Int = this.getOr(p.x, p.y)
operator fun IntArray2.set(p: PointInt, value: Int) { if (inside(p.x, p.y)) this[p.x, p.y] = value }
//fun IStackedIntArray2.get(p: PointInt): Int = this[p.x, p.y]
//fun IStackedLongArray2.get(p: PointInt): Long = this[p.x, p.y]

val GROUND = 0
val WALL = 1
val APPLE = 2
