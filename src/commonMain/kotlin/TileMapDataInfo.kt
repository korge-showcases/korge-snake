import korlibs.datastructure.*
import korlibs.korge.view.tiles.*

val TileMap.map: TileMapDataInfo get() = TileMapDataInfo(this.stackedIntMap)

// @TODO: Add stackedIntMap.version

inline class TileMapDataInfo(val data: IStackedIntArray2) {
    /** Annotation of where in [startX] this stack would be placed in a bigger container, not used for set or get methods */
    val startX: Int get() = data.startX
    /** Annotation of where in [startY] this stack would be placed in a bigger container, not used for set or get methods */
    val startY: Int get() = data.startY

    /** [width] of the data available here, get and set methods use values in the range x=0 until [width] */
    val width: Int get() = data.width
    /** [height] of the data available here, get and set methods use values in the range y=0 until [height] */
    val height: Int get() = data.height

    /** The [empty] value that will be returned if the specified cell it out of bounds, or empty */
    val empty: TileInfo get() = korlibs.korge.view.tiles.TileInfo(data.empty)
    /** The maximum level of layers available on the whole stack */
    val maxLevel: Int get() = data.maxLevel

    /** Shortcut for [IStackedIntArray2.startX] + [IStackedIntArray2.width] */
    val endX: Int get() = startX + width
    /** Shortcut for [IStackedIntArray2.startY] + [IStackedIntArray2.height] */
    val endY: Int get() = startY + height

    operator fun set(x: Int, y: Int, data: TileInfo) = setLast(x, y, data)

    operator fun set(x: Int, y: Int, level: Int, data: TileInfo) {
        if (this.data.inside(x, y)) {
            this.data[x, y, level] = data.data
        }
    }

    operator fun get(x: Int, y: Int): TileInfo = getLast(x, y)

    operator fun get(x: Int, y: Int, level: Int): TileInfo = korlibs.korge.view.tiles.TileInfo(this.data[x, y, level])

    /** Number of values available at this [x], [y] */
    fun getStackLevel(x: Int, y: Int): Int = this.data.getStackLevel(x, y)

    /** Adds a new [value] on top of [x], [y] */
    fun push(x: Int, y: Int, value: TileInfo) {
        this.data.push(x, y, value.data)
    }

    /** Removes the last value at [x], [y] */
    fun removeLast(x: Int, y: Int) {
        this.data.removeLast(x, y)
    }

    /** Set the first [value] of a stack in the cell [x], [y] */
    fun setFirst(x: Int, y: Int, value: TileInfo) {
        set(x, y, 0, value)
    }

    /** Gets the first value of the stack in the cell [x], [y] */
    fun getFirst(x: Int, y: Int): TileInfo {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, 0)
    }

    /** Gets the last value of the stack in the cell [x], [y] */
    fun getLast(x: Int, y: Int): TileInfo {
        val level = getStackLevel(x, y)
        if (level == 0) return empty
        return get(x, y, level - 1)
    }

    fun setLast(x: Int, y: Int, value: TileInfo) {
        if (!inside(x, y)) return
        val level = (getStackLevel(x, y) - 1).coerceAtLeast(0)
        set(x, y, level, value)
    }

    /** Checks if [x] and [y] are inside this array in the range x=0 until [width] and y=0 until [height] ignoring startX and startY */
    fun inside(x: Int, y: Int): Boolean = x >= 0 && y >= 0 && x < width && y < height
}
