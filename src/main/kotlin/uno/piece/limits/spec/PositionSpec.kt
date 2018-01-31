package uno.piece.limits.spec

import java.io.Serializable

data class PositionSpec(val projectName: String, val limit: Int, val typeName: String, val literalPrefix: String) : Serializable, TypeSpec {
    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "$typeName.${literalPrefix}0"
    override fun generateSizeFields() = "val size = $limit"
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
            }

            operator fun plus(other: $typeName) = saveIncrease(ordinal + other.ordinal)

            operator fun minus(other: $typeName) = saveReduce(ordinal - other.ordinal)

            operator fun times(other: $typeName) = saveIncrease(ordinal * other.ordinal)

            operator fun div(other: $typeName) = saveReduce(ordinal / other.ordinal)

            internal fun saveIncrease(value: Int) = values()[Math.min(${limit - 1}, value)]

            internal fun saveReduce(value: Int) =  values()[Math.max(0, value)]
        }

        fun Int.to$typeName() = $typeName.${literalPrefix}0.saveIncrease(this)
        """.trimIndent()


}