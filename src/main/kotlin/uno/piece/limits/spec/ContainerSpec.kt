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
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName(private val map: Map<${coordinateType.typeName()}, ${refType.typeName()}> = mapOf()${if (containedType != refType) ", val ${containedType.typeName().decapitalize()}: ${containedType.typeName()} = ${containedType.typeName()}()" else ""}): Iterable<${coordinateType.typeName()}> {

            companion object {
                val empty = $typeName()

                ${coordinateType.generateSizeFields()}
            }

            override fun iterator(): Iterator<${coordinateType.typeName()}> = IndexIterator(map)

            operator fun get(position: ${coordinateType.typeName()}) = if (map.containsKey(position)) map[position]!! else ${refType.generateEmpty()}

            fun with${refType.typeName()}(entry: Pair<${coordinateType.typeName()}, ${refType.typeName()}>): $typeName = copy(map = map + entry${if (containedType != refType) ", ${containedType.typeName().decapitalize()} = ${containedType.typeName().decapitalize()}" else ""})

            fun without${refType.typeName()}(entry: ${coordinateType.typeName()}): $typeName = copy(map = map - entry${if (containedType != refType) ", ${containedType.typeName().decapitalize()} = ${containedType.typeName().decapitalize()}" else ""})
            ${generateCopyFunctions()}
            private class IndexIterator(val map: Map<${coordinateType.typeName()}, ${refType.typeName()}>) : Iterator<${coordinateType.typeName()}> {
                var idx = 0
                var next: ${coordinateType.typeName()}? = null

                override fun hasNext(): Boolean {
                    if (next != null) {
                        return true
                    }
                    if (idx == ${coordinateType.generateSizeFieldsSum()}) {
                        return false
                    }
                    val coordinate = ${coordinateType.generateIndexIteratorEntry()}
                    if (map.containsKey(coordinate)) {
                        next = coordinate
                    }
                    idx++
                    return hasNext()
                }

                override fun next(): ${coordinateType.typeName()} {
                    if (next == null) {
                        hasNext()
                    }
                    val v = next!!
                    next = null
                    return v
                }
            }

            ${generateLoopIterator()}
        }
        """.trimIndent()

    private fun generateLoopIterator() = if (coordinateType is PositionSpec) """
            fun loopIterator() = LoopIterator()

            class LoopIterator : Iterator<${coordinateType.typeName()}> {
                var idx = 0

                override fun hasNext(): Boolean = idx < ${coordinateType.generateSizeFieldsSum()}

                fun current() = ${coordinateType.typeName()}.values()[idx]

                override fun next(): ${coordinateType.typeName()} {
                    if (idx == size - 1) {
                        idx = 0
                    } else {
                        idx++
                    }
                    return ${coordinateType.generateIndexIteratorEntry()}
                }

                fun prev(): ${coordinateType.typeName()} {
                    if (idx == 0) {
                        idx = size - 1
                    } else {
                        idx--
                    }
                    return ${coordinateType.generateIndexIteratorEntry()}
                }
            }
        """ else ""

    private fun generateImports(basePackageName: String) =
            (childrenHierarchy(this).map { setOf(it.coordinateType.projectName(), it.refType.projectName(), it.containedType.projectName()) }.flatten().toSet() - setOf(projectName, "")).joinToString(separator = "; ") { "import $basePackageName.entities.$it.*" }

    private fun generateCopyFunctions(): String {
        var result = ""
        val hierarchy = childrenHierarchy(this)
        val is1to1Container = hierarchy[0].containedType != hierarchy[0].refType
        (1 until hierarchy.size).forEach { index ->
            result += """
            fun with${hierarchy[index].refType.typeName()}(${(0 until index).filter { hierarchy[it].coordinateType != hierarchy[index].coordinateType }.map {
                hierarchy[it].coordinateType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}: $it, "
                    }}entry: Pair<${hierarchy[index].coordinateType.typeName()}, ${hierarchy[index].refType.typeName()}>) =
                ${if (is1to1Container)
                    "copy(map = map, ${hierarchy[0].containedType.typeName().decapitalize()} = ${hierarchy[0].containedType.typeName().decapitalize()}"
                else
                    "with${refType.typeName()}(${hierarchy[0].coordinateType.typeName().decapitalize()} to this[${hierarchy[0].coordinateType.typeName().decapitalize()}]"
                }.with${hierarchy[index].refType.typeName()}(${(1 until index).filter { hierarchy[it].coordinateType != hierarchy[index].coordinateType }.map {
                hierarchy[it].coordinateType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}, "
                    }}entry))

            fun without${hierarchy[index].refType.typeName()}(${(0 until index).filter { hierarchy[it].coordinateType != hierarchy[index].coordinateType }.map {
                hierarchy[it].coordinateType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}: $it, "
                    }}entry: ${hierarchy[index].coordinateType.typeName()}) =
                ${if (is1to1Container)
                    "copy(map = map, ${hierarchy[0].containedType.typeName().decapitalize()} = ${hierarchy[0].containedType.typeName().decapitalize()}"
                else
                    "with${refType.typeName()}(${hierarchy[0].coordinateType.typeName().decapitalize()} to this[${hierarchy[0].coordinateType.typeName().decapitalize()}]"
                }.without${hierarchy[index].refType.typeName()}(${(1 until index).filter { hierarchy[it].coordinateType != hierarchy[index].coordinateType }.map {
                hierarchy[it].coordinateType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}, "
                    }}entry))
            """
        }
        return result
    }

    private fun childrenHierarchy(childType: TypeSpec): List<ContainerSpec> = when (childType) {
        is ContainerSpec -> listOf(childType) + childrenHierarchy(childType.containedType)
        else -> listOf()
    }

}