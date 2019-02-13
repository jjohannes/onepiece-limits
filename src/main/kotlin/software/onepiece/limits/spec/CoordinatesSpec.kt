package software.onepiece.limits.spec

interface CoordinatesSpec : Spec {
    override fun generateEmpty() = "${typeName()}.zero"
    override fun emptyCheck() = " == ${typeName()}.zero"
    fun generateSizeFields(): String
    fun generateSizeFieldsSum(): String
    fun generateIndexIteratorEntry(): String
}