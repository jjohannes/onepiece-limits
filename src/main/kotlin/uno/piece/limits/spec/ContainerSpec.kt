package uno.piece.limits.spec

import java.io.Serializable

data class ContainerSpec(val projectName: String, val typeName: String, val coordinateType: TypeSpec, val refType: TypeSpec, val containedType: TypeSpec = refType) : Serializable, TypeSpec {
    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"
    override fun generateSizeFields() = ""
    override fun generateSizeFieldsSum() = ""
    override fun generateIndexIteratorEntry() = ""

    override fun generate(packageName: String) = """
        package $packageName.data.$projectName

        ${generateImports(packageName)}

        data class $typeName(private val map: Map<${coordinateType.typeName()}, ${refType.typeName()}> = mapOf()${if (containedType != refType) ", val ${containedType.typeName().decapitalize()}: ${containedType.typeName()} = ${containedType.typeName()}()" else ""}): Iterable<${coordinateType.typeName()}> {

            companion object {
                val empty = $typeName()

                ${coordinateType.generateSizeFields()}
            }

            override fun iterator(): Iterator<${coordinateType.typeName()}> = IndexIterator()

            operator fun get(position: ${coordinateType.typeName()}) = if (map.containsKey(position)) map[position]!! else ${refType.generateEmpty()}

            fun with${refType.typeName()}(entry: Pair<${coordinateType.typeName()}, ${refType.typeName()}>): $typeName = copy(map = map + entry${if (containedType != refType) ", ${containedType.typeName().decapitalize()} = ${containedType.typeName().decapitalize()}" else ""})
            ${generateCopyFunctions()}
            private class IndexIterator : Iterator<${coordinateType.typeName()}> {
                var idx = 0

                override fun hasNext(): Boolean = idx < ${coordinateType.generateSizeFieldsSum()}

                override fun next(): ${coordinateType.typeName()} {
                    idx++
                    return ${coordinateType.generateIndexIteratorEntry()}
                }
            }
        }
        """.trimIndent()

    private fun generateImports(basePackageName: String) =
            (childrenHierarchy(this).map { setOf(it.coordinateType.projectName(), it.refType.projectName(), it.containedType.projectName()) }.flatten().toSet() - setOf(projectName, "")).joinToString(separator = "; ") { "import $basePackageName.data.$it.*" }

    private fun generateCopyFunctions(): String {
        var result = ""
        val hierarchy = childrenHierarchy(this)
        (1 until hierarchy.size).forEach { index ->
            if (hierarchy[0].containedType == hierarchy[0].refType) {
                result += """
            fun with${hierarchy[index].refType.typeName()}(${(0 until index).filter { hierarchy[it].coordinateType != hierarchy[index].coordinateType }.joinToString { "${hierarchy[it].coordinateType.typeName().decapitalize()}: ${hierarchy[it].coordinateType.typeName()}"}}, entry: Pair<${hierarchy[index].coordinateType.typeName()}, ${hierarchy[index].refType.typeName()}>) =
                    with${refType.typeName()}(${hierarchy[0].coordinateType.typeName().decapitalize()} to this[${hierarchy[0].coordinateType.typeName().decapitalize()}].with${hierarchy[index].refType.typeName()}(${(1 until index).filter { hierarchy[it].coordinateType != hierarchy[index].coordinateType }.joinToString(separator = "") { "${hierarchy[it].coordinateType.typeName().decapitalize()}, " }}entry))
            """
            } else {
                result += """
            fun with${hierarchy[index].refType.typeName()}(entry: Pair<${hierarchy[index].coordinateType.typeName()}, ${hierarchy[index].refType.typeName()}>) = copy(map = map, ${hierarchy[0].containedType.typeName().decapitalize()} = ${hierarchy[0].containedType.typeName().decapitalize()}.with${hierarchy[index].refType.typeName()}(entry))
            """
            }
        }
        return result
    }

    private fun childrenHierarchy(childType: TypeSpec): List<ContainerSpec> = when (childType) {
        is ContainerSpec -> listOf(childType) + childrenHierarchy(childType.containedType)
        else -> listOf()
    }

}