package uno.piece.limits.spec

import java.io.Serializable

data class CoordinateSpec(val projectName: String, val typeName: String, val xType: PositionSpec, val yType: PositionSpec) : Serializable, TypeSpec {
    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "$typeName.of(${xType.typeName}.${xType.literalPrefix}0, ${yType.typeName}.${yType.literalPrefix}0)"
    override fun generateSizeFields() = "val width = ${xType.limit}; val height = ${yType.limit}"
    override fun generateSizeFieldsSum() = "width * height"
    override fun generateIndexIteratorEntry() = "$typeName.of(${xType.typeName}.of(idx - (idx / width) * width), ${yType.typeName}.of(idx / width))"

    override fun generate(packageName: String): String = """
        package $packageName.entities.$projectName

        data class $typeName(val x: ${xType.typeName}, val y: ${yType.typeName}) : Comparable<$typeName> {
            companion object {
                private val pool = mutableMapOf<Int, $typeName>()
                private val limit = ${xType.limit}
                fun of(x: ${xType.typeName}, y: ${yType.typeName}) : $typeName {
                    val key = x.ordinal + limit * y.ordinal
                    if (!pool.containsKey(key)) {
                        pool.put(key, ${typeName}(x, y))
                    }
                    return pool[key]!!
                }
            }

            override fun compareTo(other: $typeName): Int {
                val xResult = x.compareTo(other.x)
                val yResult = y.compareTo(other.y)
                if (xResult == 0 && yResult == 0) {
                    return 0
                }
                if (yResult == 0) {
                    return xResult
                }
                return yResult
            }

            operator fun plus(other: $typeName) = of(x + other.x, y + other.y)

            operator fun minus(other: $typeName) = of(x - other.x, y - other.y)
        }
        """.trimIndent()
}