package software.onepiece.limits.spec

class Coordinates2Spec(private val projectName: String, private val typeName: String, private val xType: CoordinateSpec, private val yType: CoordinateSpec) : CoordinatesSpec {
    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateSizeFields() = "const val width = ${xType.limit}; const val height = ${yType.limit}"
    override fun generateSizeFieldsSum() = "width * height"
    override fun generateIndexIteratorEntry() = "$typeName.of(idx - (idx / width) * width, idx / width)"

    override fun generate(packageName: String): String = """
        package $packageName.entities.$projectName

        @kotlinx.serialization.Serializable 
        data class $typeName constructor(val x: ${xType.typeName()}, val y: ${yType.typeName()}) : Comparable<$typeName> {
            companion object {
                private val pool = mutableMapOf<Int, $typeName>()
                private val limit = ${xType.limit}
                fun of(x: ${xType.typeName()}, y: ${yType.typeName()}) : $typeName {
                    val key = x.ordinal + limit * y.ordinal
                    if (!pool.containsKey(key)) {
                        pool[key] = $typeName(x, y)
                    }
                    return pool[key]!!
                }

                fun of(x: Int, y: Int) = of(x.to${xType.typeName()}(), y.to${yType.typeName()}())

                fun of(string: String) = of(string.substring(1, 3).toInt(), string.substring(4, 6).toInt())

                val zero = of(${xType.typeName()}.${xType.literalPrefix}0, ${yType.typeName()}.${yType.literalPrefix}0)

                val values: Iterable<$typeName> = AllIterable
            }

            override fun toString() = "x${'$'}{x.toString().padStart(2, '0')}y${'$'}{y.toString().padStart(2, '0')}"

            private object AllIterable: Iterable<$typeName> {
                override fun iterator() = AllIterator()
            }

            private class AllIterator: Iterator<$typeName> {
                private var x = ${xType.typeName()}.${xType.literalPrefix}0
                private var y = ${yType.typeName()}.${yType.literalPrefix}0
                private var hasNext = true

                override fun hasNext() = hasNext

                override fun next() = of(x, y).also {
                    if (x == ${xType.typeName()}.${xType.literalPrefix}${xType.limit - 1} && y == ${yType.typeName()}.${yType.literalPrefix}${yType.limit - 1}) {
                        hasNext = false
                    } else if (x == ${xType.typeName()}.${xType.literalPrefix}${xType.limit - 1}) {
                        x = ${xType.typeName()}.${xType.literalPrefix}0
                        y += ${yType.typeName()}.${yType.literalPrefix}1
                    } else {
                        x += ${xType.typeName()}.${xType.literalPrefix}1
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

            fun dataHash() = x${xType.dataHashCall()} * 31 + y${yType.dataHashCall()}
        }
        """.trimIndent()
}