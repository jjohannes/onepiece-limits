package software.onepiece.limits.spec

import java.io.Serializable

data class Coordinates2Spec(val projectName: String, val typeName: String, val xType: CoordinateSpec, val yType: CoordinateSpec) : Serializable, CoordinatesSpec {
    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "$typeName.of(${xType.typeName}.${xType.literalPrefix}0, ${yType.typeName}.${yType.literalPrefix}0)"
    override fun generateSizeFields() = "const val width = ${xType.limit}; const val height = ${yType.limit}"
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
                        pool[key] = $typeName(x, y)
                    }
                    return pool[key]!!
                }

                val zero = of(${xType.typeName}.${xType.literalPrefix}0, ${yType.typeName}.${yType.literalPrefix}0)

                val values: Iterable<$typeName> = AllIterable
            }

            private object AllIterable: Iterable<$typeName> {
                override fun iterator() = AllIterator()
            }

            private class AllIterator: Iterator<$typeName> {
                private var x = ${xType.typeName}.${xType.literalPrefix}0
                private var y = ${yType.typeName}.${yType.literalPrefix}0
                private var hasNext = true

                override fun hasNext() = hasNext

                override fun next() = of(x, y).also {
                    if (x == ${xType.typeName}.${xType.literalPrefix}${xType.limit - 1} && y == ${yType.typeName}.${yType.literalPrefix}${yType.limit - 1}) {
                        hasNext = false
                    } else if (x == ${xType.typeName}.${xType.literalPrefix}${xType.limit - 1}) {
                        x = ${xType.typeName}.${xType.literalPrefix}0
                        y += ${yType.typeName}.${yType.literalPrefix}1
                    } else {
                        x += ${xType.typeName}.${xType.literalPrefix}1
                    }
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