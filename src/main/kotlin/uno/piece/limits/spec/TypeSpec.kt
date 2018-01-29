package uno.piece.limits.spec

interface TypeSpec {
    fun projectName(): String
    fun typeName(): String
    fun generate(packageName: String) = ""
    fun generateEmpty() = ""
    fun generateSizeFields() = ""
    fun generateSizeFieldsSum() = ""
    fun generateIndexIteratorEntry() = ""
}