import kotlin.test.*

class SimpleTileProviderTest {
    @Test
    fun test() {
        println(WallsProvider.get(SimpleTileSpec(right = true)).toStringInfo())
        println(WallsProvider.get(SimpleTileSpec(left = true)).toStringInfo())
        println(WallsProvider.get(SimpleTileSpec(up = true)).toStringInfo())
        println(WallsProvider.get(SimpleTileSpec(down = true)).toStringInfo())
    }
}
