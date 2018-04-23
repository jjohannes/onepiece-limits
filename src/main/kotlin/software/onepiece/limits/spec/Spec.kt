package software.onepiece.limits.spec

interface Spec {
    fun projectName(): String
    fun typeName(): String
    fun generateEmpty(): String
    fun generate(packageName: String) = ""
}