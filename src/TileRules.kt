import korlibs.datastructure.*
import korlibs.image.tiles.*
import korlibs.memory.*

fun IntGridToTileGrid(grid: IntArray2, rules: IRuleMatcher, tiles: TileMapData) {
    for (y in 0 until grid.height) {
        for (x in 0 until grid.width) {
            tiles[x, y] = rules.get(grid, x, y)
        }
    }
}

fun IntArray2.getOr(x: Int, y: Int, default: Int = -1): Int {
    if (!inside(x, y)) return default
    return this[x, y]
}

fun IntArray2.setOr(x: Int, y: Int, value: Int) {
    if (!inside(x, y)) return
    this[x, y] = value
}

data class SimpleTileSpec(
    val left: Boolean = false,
    val up: Boolean = false,
    val right: Boolean = false,
    val down: Boolean = false,
) {
    val bits: Int = bits(left, up, right, down)

    companion object {
        fun bits(left: Boolean, up: Boolean, right: Boolean, down: Boolean): Int = 0
            .insert(left, 0).insert(up, 1).insert(right, 2).insert(down, 3)
    }
}

fun Tile.flippedX(): Tile = Tile(tile, orientation.flippedX(), offsetX, offsetY)
fun Tile.flippedY(): Tile = Tile(tile, orientation.flippedY(), offsetX, offsetY)
fun Tile.rotated(): Tile = Tile(tile, orientation.rotatedRight(), offsetX, offsetY)

data class SimpleRule(
    val tile: Tile,
    val spec: SimpleTileSpec,
) {
    val left get() = spec.left
    val right get() = spec.right
    val up get() = spec.up
    val down get() = spec.down

    constructor(tile: Tile, left: Boolean = false, up: Boolean = false, right: Boolean = false, down: Boolean = false) : this(tile, SimpleTileSpec(left, up, right, down))

    fun flippedX(): SimpleRule = SimpleRule(tile.flippedX(), right, up, left, down)
    fun flippedY(): SimpleRule = SimpleRule(tile.flippedY(), left, down, right, up)
    fun rotated(): SimpleRule = SimpleRule(tile.rotated(), up = left, right = up, down = right, left = down)
    //fun rotated(): SimpleRule = SimpleRule(tile.rotated(), up = right, right = down, down = left, left = up)

    fun match(spec: SimpleTileSpec): Boolean {
        return this.spec == spec
    }
}

interface ISimpleTileProvider : IRuleMatcher {
    fun get(spec: SimpleTileSpec): Tile
}

interface IRuleMatcher {
    val maxDist: Int
    fun get(ints: IntArray2, x: Int, y: Int): Tile
}

class CombinedRuleMatcher(val rules: List<IRuleMatcher>) : IRuleMatcher {
    constructor(vararg rules: IRuleMatcher) : this(rules.toList())

    override val maxDist: Int by lazy { rules.maxOf { it.maxDist } }

    override fun get(ints: IntArray2, x: Int, y: Int): Tile {
        for (rule in rules) {
            val tile = rule.get(ints, x, y)
            if (tile.isValid) return tile
        }
        return Tile.INVALID
    }

}

open class SimpleTileProvider(val value: Int) : ISimpleTileProvider, IRuleMatcher {
    override val maxDist: Int = 1

    //val rules = mutableSetOf<SimpleRule>()
    val ruleTable = arrayOfNulls<SimpleRule>(16)

    companion object {
        val FALSE = listOf(false)
        val BOOLS = listOf(false, true)
    }

    fun rule(
        rule: SimpleRule,
        registerFlipX: Boolean = true,
        registerFlipY: Boolean = true,
        registerRotated: Boolean = true,
    ) {
        for (fx in if (registerFlipX) BOOLS else FALSE) {
            for (fy in if (registerFlipY) BOOLS else FALSE) {
                for (rot in if (registerRotated) BOOLS else FALSE) {
                    var r = rule
                    if (rot) r = r.rotated()
                    if (fx) r = r.flippedX()
                    if (fy) r = r.flippedY()
                    val bits = r.spec.bits
                    if (ruleTable[bits] == null) ruleTable[bits] = r
                    //rules += r
                }
            }
        }
    }

    override fun get(spec: SimpleTileSpec): Tile {
        ruleTable[spec.bits]?.let { return it.tile }
        //for (rule in rules) {
        //    if (rule.match(spec)) {
        //        return rule.tile
        //    }
        //}
        return Tile.INVALID
    }

    override fun get(ints: IntArray2, x: Int, y: Int): Tile {
        if (ints.getOr(x, y) != value) return Tile.INVALID
        val left = ints.getOr(x - 1, y) == value
        val right = ints.getOr(x + 1, y) == value
        val up = ints.getOr(x, y - 1) == value
        val down = ints.getOr(x, y + 1) == value
        return get(SimpleTileSpec(left, up, right, down))
    }
}

fun TileMapData.pushInside(x: Int, y: Int, value: Tile) {
    if (inside(x, y)) {
        this.data.push(x, y, value.raw)
    }
}
