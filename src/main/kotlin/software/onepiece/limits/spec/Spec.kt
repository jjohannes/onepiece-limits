package software.onepiece.limits.spec

interface Spec {
    fun projectName(): String
    fun typeName(): String
    fun generateEmpty(): String
    fun propertyName() = typeName().decapitalize()
    fun generate(packageName: String) = ""
}