package software.onepiece.limits.spec

interface TypeSpec {
    fun projectName(): String
    fun typeName(): String
    fun generate(packageName: String) = ""
    fun generateEmpty() = ""
    fun generateSizeFields() = ""
    fun generateSizeFieldsSum() = ""
    fun generateIndexIteratorEntry() = ""
}