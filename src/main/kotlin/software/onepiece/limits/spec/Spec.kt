package software.onepiece.limits.spec

import java.io.Serializable

interface Spec : Serializable {
    fun projectName(): String
    fun typeName(): String
    fun generateEmpty(): String
    fun emptyCheck(): String
    fun propertyName(count: Int = 0) = typeName().decapitalize() + if (count == 0) "" else count.toString()
    fun generate(packageName: String) = ""
    fun generateCommandFactory(packageName: String) = ""
    fun generateDiffTool(packageName: String) = ""
    fun dataHashCall() = ".dataHash()"
}