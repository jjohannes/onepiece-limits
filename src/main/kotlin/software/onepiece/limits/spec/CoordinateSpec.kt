package software.onepiece.limits.spec

import java.io.Serializable

data class CoordinateSpec(val projectName: String, val limit: Int, val typeName: String, val literalPrefix: String) : Serializable, CoordinatesSpec {
    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "$typeName.${literalPrefix}0"
    override fun generateSizeFields() = "const val size = $limit"
    override fun generateSizeFieldsSum() =  "size"
    override fun generateIndexIteratorEntry() =  "$typeName.of(idx)"

    override fun generate(packageName: String): String = """
        package $packageName.entities.$projectName

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
                    return values()[unlimitedInt]
                }

                val zero = ${literalPrefix}0

                fun saveValue(ordinal: Int) = values()[Math.max(0, Math.min(${limit - 1}, ordinal))]
            }

            operator fun plus(other: $typeName) = saveValue(ordinal + other.ordinal)

            operator fun minus(other: $typeName) = saveValue(ordinal - other.ordinal)

            operator fun times(other: $typeName) = saveValue(ordinal * other.ordinal)

            operator fun div(other: $typeName) = saveValue(ordinal / other.ordinal)
        }

        fun Int.to$typeName() = $typeName.saveValue(this)
        """.trimIndent()


}