package software.onepiece.limits.spec

import java.io.Serializable

data class ContainerSpec(
        val projectName: String,
        val typeName: String,
        val coordinatesType: CoordinatesSpec,
        val refType: Spec,
        val attributes: List<Spec> = emptyList(),
        val containedType: Spec = refType) : Serializable, TypeSpec {

    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName(private val map: Map<${coordinatesType.typeName()}, ${refType.typeName()}> = mapOf()${
            if (containedType != refType) ", val ${containedType.typeName().decapitalize()}: ${containedType.typeName()} = ${containedType.typeName()}()" else ""
        }${
            attributes.joinToString(separator = "") { ", val ${it.typeName().decapitalize()}: ${it.typeName()} = ${it.typeName()}.zero" }
        }): Iterable<${coordinatesType.typeName()}> {

            companion object {
                val empty = $typeName()

                ${coordinatesType.generateSizeFields()}
            }

            override fun iterator(): Iterator<${coordinatesType.typeName()}> = IndexIterator(map)

            operator fun get(position: ${coordinatesType.typeName()}) = if (map.containsKey(position)) map[position]!! else ${refType.generateEmpty()}

            fun isEmpty() = map.isEmpty()

            fun with${refType.typeName()}(entry: Pair<${coordinatesType.typeName()}, ${refType.typeName()}>) = ${if (refType is ContainerSpec) "if (entry.second.isEmpty()) copy(map = map - entry.first) else " else ""}copy(map = map + entry${if (containedType != refType) ", ${containedType.typeName().decapitalize()} = ${containedType.typeName().decapitalize()}" else ""})

            fun without${refType.typeName()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry${if (containedType != refType) ", ${containedType.typeName().decapitalize()} = ${containedType.typeName().decapitalize()}" else ""})
            ${generateCopyFunctions()}
            private class IndexIterator(val map: Map<${coordinatesType.typeName()}, ${refType.typeName()}>) : Iterator<${coordinatesType.typeName()}> {
                var idx = 0
                var next: ${coordinatesType.typeName()}? = null

                override fun hasNext(): Boolean {
                    if (next != null) {
                        return true
                    }
                    if (idx == ${coordinatesType.generateSizeFieldsSum()}) {
                        return false
                    }
                    val coordinates = ${coordinatesType.generateIndexIteratorEntry()}
                    if (map.containsKey(coordinates)) {
                        next = coordinates
                    }
                    idx++
                    return hasNext()
                }

                override fun next(): ${coordinatesType.typeName()} {
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

    private fun generateLoopIterator() = if (coordinatesType is CoordinateSpec) """
            fun loopIterator() = LoopIterator()

            class LoopIterator : Iterator<${coordinatesType.typeName()}> {
                var idx = 0

                override fun hasNext(): Boolean = idx < ${coordinatesType.generateSizeFieldsSum()}

                fun current() = ${coordinatesType.typeName()}.values()[idx]

                override fun next(): ${coordinatesType.typeName()} {
                    if (idx == size - 1) {
                        idx = 0
                    } else {
                        idx++
                    }
                    return ${coordinatesType.generateIndexIteratorEntry()}
                }

                fun prev(): ${coordinatesType.typeName()} {
                    if (idx == 0) {
                        idx = size - 1
                    } else {
                        idx--
                    }
                    return ${coordinatesType.generateIndexIteratorEntry()}
                }
            }
        """ else ""

    private fun generateImports(basePackageName: String) =
            (childrenHierarchy(this).map { setOf(it.coordinatesType.projectName(), it.refType.projectName(), it.containedType.projectName()) }.flatten().toSet() + attributes.map { it.projectName() } - setOf(projectName, "")).joinToString(separator = "; ") { "import $basePackageName.entities.$it.*" }

    private fun generateCopyFunctions(): String {
        var result = ""
        val hierarchy = childrenHierarchy(this)
        val is1to1Container = hierarchy[0].containedType != hierarchy[0].refType
        (1 until hierarchy.size).forEach { index ->
            result += """
            fun with${hierarchy[index].refType.typeName()}(${(0 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}: $it, "
                    }}entry: Pair<${hierarchy[index].coordinatesType.typeName()}, ${hierarchy[index].refType.typeName()}>) =
                ${if (is1to1Container)
                    "copy(map = map, ${hierarchy[0].containedType.typeName().decapitalize()} = ${hierarchy[0].containedType.typeName().decapitalize()}"
                else
                    "with${refType.typeName()}(${hierarchy[0].coordinatesType.typeName().decapitalize()} to this[${hierarchy[0].coordinatesType.typeName().decapitalize()}]"
                }.with${hierarchy[index].refType.typeName()}(${(1 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}, "
                    }}entry))

            fun without${hierarchy[index].refType.typeName()}(${(0 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}: $it, "
                    }}entry: ${hierarchy[index].coordinatesType.typeName()}) =
                ${if (is1to1Container)
                    "copy(map = map, ${hierarchy[0].containedType.typeName().decapitalize()} = ${hierarchy[0].containedType.typeName().decapitalize()}"
                else
                    "with${refType.typeName()}(${hierarchy[0].coordinatesType.typeName().decapitalize()} to this[${hierarchy[0].coordinatesType.typeName().decapitalize()}]"
                }.without${hierarchy[index].refType.typeName()}(${(1 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}, "
                    }}entry))
            """
        }
        return result
    }

    private fun childrenHierarchy(childType: Spec): List<ContainerSpec> = when (childType) {
        is ContainerSpec -> listOf(childType) + childrenHierarchy(childType.containedType)
        else -> listOf()
    }

}