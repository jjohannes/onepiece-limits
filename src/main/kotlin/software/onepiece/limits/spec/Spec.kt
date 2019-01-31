package software.onepiece.limits.spec

interface Spec {
    fun projectName(): String
    fun typeName(): String
    fun generateEmpty(): String
    fun propertyName(count: Int = 0) = typeName().decapitalize() + if (count == 0) "" else count.toString()
    fun generate(packageName: String) = ""
}