package snake.model

import korlibs.datastructure.*
import korlibs.image.tiles.*
import korlibs.math.geom.*


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
