package software.onepiece.limits.spec

class CoordinateSpec(private val projectName: String, private val typeName: String, val limit: Int, val literalPrefix: String) : CoordinatesSpec {
    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateSizeFields() = "const val size = $limit"
    override fun generateSizeFieldsSum() =  "size"
    override fun generateIndexIteratorEntry() =  "$typeName.of(idx)"

    override fun generate(packageName: String): String = """
        package $packageName.entities.$projectName

        @kotlinx.serialization.Serializable 
        enum class $typeName {
            ${(0 until limit).joinToString { literalPrefix + it }};

            companion object {
                fun of(unlimitedInt: Int): $typeName {
                    if (unlimitedInt < 0) {
                        return ${literalPrefix}0
                    }
                    if (unlimitedInt > ${limit - 1}) {
                        return $literalPrefix${limit - 1}
                    }
                    return values[unlimitedInt]
                }

                fun of(string: String) = of(string.toInt())

                val zero = ${literalPrefix}0

                val values = values().asList()

                fun saveValue(ordinal: Int) = values[kotlin.math.max(0, kotlin.math.min(${limit - 1}, ordinal))]
            }

            override fun toString() = ordinal.toString().padStart(2, '0')

            operator fun plus(other: $typeName) = saveValue(ordinal + other.ordinal)

            operator fun minus(other: $typeName) = saveValue(ordinal - other.ordinal)

            operator fun times(other: $typeName) = saveValue(ordinal * other.ordinal)

            operator fun div(other: $typeName) = saveValue(ordinal / other.ordinal)

            fun dataHash() = ordinal
        }

        fun Int.to$typeName() = $typeName.saveValue(this)
        """.trimIndent()


}