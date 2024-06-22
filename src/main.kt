import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.bitmap.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.image.text.*
import korlibs.image.tiles.*
import korlibs.io.file.std.*
import korlibs.korge.animate.*
import korlibs.korge.input.*
import korlibs.korge.time.*
import korlibs.korge.tween.*
import korlibs.korge.view.align.*
import korlibs.korge.view.tiles.*
import korlibs.math.geom.*
import korlibs.math.geom.slice.*
import korlibs.math.random.*
import korlibs.time.*
import kotlin.random.*
import kotlin.time.Duration.Companion.seconds

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
    val sceneContainer = sceneContainer()

    sceneContainer.changeTo { MyScene() }
}

val EMPTY = 0
val APPLE = 1
val WALL = 2
val SNAKE = 3

object AppleProvider : ISimpleTileProvider by (SimpleTileProvider(value = APPLE).also {
    it.rule(SimpleRule(Tile(12)))
})

object WallProvider : ISimpleTileProvider by (SimpleTileProvider(value = WALL).also {
    it.rule(SimpleRule(Tile(16)))
    it.rule(SimpleRule(Tile(17), right = true))
    it.rule(SimpleRule(Tile(18), left = true, right = true))
    it.rule(SimpleRule(Tile(19), left = true, down = true))
    it.rule(SimpleRule(Tile(20), left = true, up = true, down = true))
    it.rule(SimpleRule(Tile(21), left = true, up = true, down = true, right = true))
})

data class GameInfo(
    val score: Int = 0,
    val hiScore: Int = 0,
) {
    fun withIncrementedScore(increment: Int = +1): GameInfo {
        val newScore = score + increment
        return GameInfo(score = newScore, hiScore = maxOf(hiScore, newScore))
    }
}

open class GameInfoUpdatedEvent(val gameInfo: GameInfo) : TypedEvent<GameInfoUpdatedEvent>(GameInfoUpdatedEvent) {
    companion object : EventType<GameInfoUpdatedEvent>
}

open class GameStartEvent() : TypedEvent<GameStartEvent>(GameStartEvent) {
    companion object : EventType<GameStartEvent>
}

class OverlayScene : Scene() {
    override suspend fun SContainer.sceneMain() {
        val scoreText = text("").xy(8, 8)
        var current = GameInfo()
        var hiScoreBeatShown = false

        fun updateGameInfo(old: GameInfo, new: GameInfo) {
            scoreText.text = "Score: ${new.score}, HI-Score: ${new.hiScore}"
            if (new.hiScore > old.hiScore && !hiScoreBeatShown) {
                hiScoreBeatShown = true
                val text = sceneView.text("Hi-Score beaten", textSize = 32.0).centerOnStage().alpha(0.0)
                sceneView.animator {
                    parallel {
                        show(text)
                        this.moveBy(text, 0.0, 32.0)
                    }
                    hide(text)
                    block { text.removeFromParent() }
                }
            }
        }

        onEvent(GameStartEvent) {
            hiScoreBeatShown = false
        }

        onEvent(GameInfoUpdatedEvent) {
            updateGameInfo(current, it.gameInfo)
            current = it.gameInfo
        }
    }
}

class MyScene : PixelatedScene(32 * 16, 32 * 16, sceneSmoothing = true), StateScene by StateScene.Mixin() {
    lateinit var tilesBmp: Bitmap32
    val tiles by lazy { tilesBmp.slice().splitInRows(16, 16) }
    val tileSet by lazy { TileSet.fromBitmapSlices(16, 16, tiles, border = 0) }
    val tileMap by lazy { sceneView.tileMap(TileMapData(32, 32, tileSet = tileSet)) }
    val tileMapRules by lazy { CombinedRuleMatcher(WallProvider, AppleProvider) }
    val intMap by lazy { IntArray2(tileMap.map.width, tileMap.map.height, EMPTY).observe {
        IntGridToTileGrid(this.base as IntArray2, tileMapRules, tileMap.map, it)
    } }
    var hiScoreBeatShown = false

    val initialGameInfo = GameInfo(score = 0)
    val initialSnake = Snake(listOf(PointInt(5, 5), PointInt(6, 5)), length = 4)
    var gameInfo: GameInfo by Observable(initialGameInfo, after = {
        //updateGameInfo(it)
        sceneView.dispatch(GameInfoUpdatedEvent(it))
    })
    var snake = initialSnake
    val random = Random(0)

    override suspend fun SContainer.sceneInit() {
        tilesBmp = resourcesVfs["gfx/tiles.ase"].readBitmap(ASE).toBMP32IfRequired()
        tileMap
        sceneContainer().changeTo { OverlayScene() }
    }

    override suspend fun SContainer.sceneMain() {
        runStates(::ingame)
    }

    suspend fun ingame(view: Container) {
        intMap[RectangleInt(0, 0, intMap.width, intMap.height)] = EMPTY
        intMap[RectangleInt(0, 0, intMap.width, 1)] = WALL
        intMap[RectangleInt(0, intMap.height - 1, intMap.width, 1)] = WALL
        intMap[RectangleInt(0, 0, 1, intMap.height)] = WALL
        intMap[RectangleInt(intMap.width - 1, 0, 1, intMap.height)] = WALL
        intMap[RectangleInt(8, 8, 3, 3)] = WALL
        intMap[RectangleInt(16, 20, 4, 10)] = WALL

        hiScoreBeatShown = false
        gameInfo = gameInfo.copy(score = 0)
        snake = initialSnake
        addRandomApple()
        renderSnake()

        sceneView.dispatch(GameStartEvent())

        intMap[4, 3] = WALL
        intMap[4, 4] = WALL
        intMap[3, 4] = WALL
        intMap[4, 5] = WALL
        //intMap[4, 4] = WALL

        var direction = SnakeDirection.RIGHT

        view.interval(0.1.seconds) {
            moveSnake(direction)
        }

        view.keys {
            down(Key.UP) { direction = SnakeDirection.UP }
            down(Key.DOWN) { direction = SnakeDirection.DOWN }
            down(Key.LEFT) { direction = SnakeDirection.LEFT }
            down(Key.RIGHT) { direction = SnakeDirection.RIGHT }
            down(Key.SPACE) {
                view.speed = if (view.speed == 0.0) 1.0 else 0.0
            }
        }
    }

    suspend fun gameOver(view: Container) = with(view) {
        //val views = view.stage!!.view
        val background = solidRect(width, height, Colors.BLACK).alpha(0.0)

        val text = text("GAME OVER", textSize = 1.0, alignment = TextAlignment.CENTER).xy(width * 0.5, height * 0.5)
        speed = 0.0

        keys {
            down {
                change(::ingame)
            }
        }

        tween(
            text::textSize[64.0].easeOut(),
            background::alpha[0.5],
            time = 0.5.seconds,
        )
    }

    fun renderSnake() {
        snake.render(tileMap.map, intMap)
    }

    fun addRandomApple() {
        while (true) {
            val point: Point = random[Rectangle(0, 0, intMap.width - 0.5, intMap.height - 0.5)]
            val ipoint = point.toInt()
            if (intMap[ipoint] == EMPTY) {
                intMap[ipoint] = APPLE
                return
            }
        }
    }

    fun moveSnake(dir: SnakeDirection) {
        val oldSnake = snake
        snake = snake.withMove(dir)
        val headValue = intMap[snake.headPos]
        var isApple = headValue == APPLE
        when {
            isApple -> {
                snake = snake.copy(length = snake.length + 1)
                gameInfo = gameInfo.withIncrementedScore(+1)
            }
            headValue == WALL || headValue == SNAKE -> {
                change(::gameOver)
                return
            }
        }
        oldSnake.clear(tileMap.map, intMap)
        snake.render(tileMap.map, intMap)
        if (isApple) {
            addRandomApple()
            //println(intMap.base)
        }
        //println(intMap.base)
    }
}

interface StateScene {
    var changeState: StateFunc?
    class Mixin : StateScene {
        override var changeState: StateFunc? = null
    }

    suspend fun SContainer.runStates(startingFunc: StateFunc) {
        var func: StateFunc = startingFunc
        while (true) {
            val stateView = fixedSizeContainer(this.size)
            try {
                func(stateView)
                while (true) frame()
            } catch (e: ChangeSceneException) {
                func = e.func
            } finally {
                stateView.removeFromParent()
            }
        }
    }

    fun change(func: StateFunc) {
        changeState = func
        throw ChangeSceneException(func)
    }

    class ChangeSceneException(val func: StateFunc) : Throwable()

    suspend fun Container.frame() {
        val newState = changeState
        if (newState != null) {
            changeState = null
            throw ChangeSceneException(newState)
        }
        delay(16.milliseconds)
    }
}

enum class SnakeDirection(
    val delta: Vector2I,
    val isHorizontal: Boolean?,
) {
    UP(Vector2I(0, -1), isHorizontal = false),
    DOWN(Vector2I(0, +1), isHorizontal = false),
    LEFT(Vector2I(-1, 0), isHorizontal = true),
    RIGHT(Vector2I(+1, 0), isHorizontal = true),
    NONE(Vector2I(0, 0), isHorizontal = null),
    ;

    companion object {
        fun fromPoints(old: PointInt, new: PointInt): SnakeDirection = when {
            new.x > old.x -> RIGHT
            new.x < old.x -> LEFT
            new.y > old.y -> DOWN
            new.y < old.y -> UP
            else -> NONE
        }
    }
}

typealias StateFunc = suspend (Container) -> Unit

data class Snake(val list: List<PointInt>, val length: Int) {
    val headPos get() = list.last()

    fun withMove(dir: SnakeDirection): Snake =
        Snake(
            (list + (list.last() + dir.delta)).takeLast(length),
            length = length
        )

    fun clear(map: TileMapData, intMap: IntIArray2) {
        for ((index, point) in list.withIndex()) {
            intMap[point] = EMPTY
            if (map.inside(point.x, point.y)) {
                while (map.getStackLevel(point.x, point.y) > 0) {
                    map.removeLast(point.x, point.y)
                }
            }
        }
    }

    fun render(map: TileMapData, intMap: IntIArray2) {
        //println("-----")
        for ((index, point) in list.withIndex()) {
            intMap[point] = SNAKE
        }

        for ((index, point) in list.withIndex()) {
            val isTail = index == 0
            val isHead = index == list.size - 1
            val prevPoint = list.getOrElse(index - 1) { point }
            val nextPoint = list.getOrElse(index + 1) { point }
            val dir = SnakeDirection.fromPoints(prevPoint, point)
            val ndir = SnakeDirection.fromPoints(point, nextPoint)
            val turned = dir.isHorizontal != ndir.isHorizontal

            //println("Dir: $dir : $ndir")

            val finalTile = when {
                isHead || isTail || !turned -> {
                    val tile = Tile(
                        when {
                            isHead -> 4
                            isTail -> 1
                            else -> 2
                        }
                    )
                    when (if (isTail) ndir else dir) {
                        SnakeDirection.UP -> tile.rotatedLeft()
                        SnakeDirection.DOWN -> tile.rotatedRight()
                        SnakeDirection.LEFT -> tile.flippedX()
                        SnakeDirection.RIGHT -> tile
                        SnakeDirection.NONE -> tile
                    }
                }
                else -> {
                    //
                    val tile = Tile(3)
                    when {
                        dir == SnakeDirection.UP && ndir == SnakeDirection.RIGHT -> tile.flippedX()
                        dir == SnakeDirection.LEFT && ndir == SnakeDirection.DOWN -> tile.flippedX()

                        dir == SnakeDirection.RIGHT && ndir == SnakeDirection.UP -> tile.rotatedRight(1)
                        dir == SnakeDirection.DOWN && ndir == SnakeDirection.LEFT -> tile.rotatedRight(1)

                        dir == SnakeDirection.DOWN && ndir == SnakeDirection.RIGHT -> tile.rotatedRight(2)
                        dir == SnakeDirection.LEFT && ndir == SnakeDirection.UP -> tile.rotatedRight(2)

                        else -> tile
                    }
                }
            }

            //map[point.x, point.y] = finalTile
            map.push(point.x, point.y, finalTile)
        }
    }
}
