package software.onepiece.limits.spec

interface CoordinatesSpec : Spec {
    fun generateSizeFields(): String
    fun generateSizeFieldsSum(): String
    fun generateIndexIteratorEntry(): String
}