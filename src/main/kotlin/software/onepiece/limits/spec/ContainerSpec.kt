package software.onepiece.limits.spec

import java.io.Serializable

data class ContainerSpec(
        val projectName: String,
        val typeName: String,
        val coordinatesType: CoordinatesSpec,
        val containedType: Spec,
        val containedLocation: ChainOfCoordinates? = null, //TODO could potentially be a list
        val additionalContainedTypes: List<Spec> = emptyList(),
        val attributes: List<Spec> = emptyList()) : Serializable, TypeSpec {

    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName(private val map: Map<${coordinatesType.typeName()}, Any> = mapOf()${
            attributes.joinToString(separator = "") { ", val ${it.typeName().decapitalize()}: ${it.typeName()} = ${it.generateEmpty()}" }
        }): Iterable<${coordinatesType.typeName()}> {

            companion object {
                val empty = $typeName()

                ${coordinatesType.generateSizeFields()}
            }

            override fun iterator(): Iterator<${coordinatesType.typeName()}> = IndexIterator(map)

            operator fun get(position: ${coordinatesType.typeName()}${if (containedLocation != null) ", ${containedLocation.rootContainer.decapitalize()}: ${containedLocation.rootContainer}" else ""}) = if (map[position] is ${containedType.typeName()}) map[position]!! as ${containedType.typeName()} ${if (containedLocation != null) "else if (map[position] is ${containedLocation.typeName()}) ${containedLocation.rootContainer.decapitalize()}[${containedLocation.components.joinToString(separator = "][") { "(map[position] as ${containedLocation.typeName()}).${it.typeName().decapitalize()}" }}] " else ""}else ${containedType.generateEmpty()}

            fun isEmpty() = map.isEmpty()${attributes.joinToString(separator = "") { " && ${it.typeName().decapitalize()} == ${it.generateEmpty()}" }}

            fun with${containedType.typeName()}(${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}, ${containedType.typeName().decapitalize()}: ${containedType.typeName()}) = ${if (containedType is ContainerSpec) "if (${containedType.typeName().decapitalize()}.isEmpty()) copy(map = map - ${coordinatesType.typeName().decapitalize()}) else " else ""}copy(map = map + (${coordinatesType.typeName().decapitalize()} to ${containedType.typeName().decapitalize()}))
            ${if (containedLocation != null) {
                "fun with${containedType.typeName()}(${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}, ${containedLocation.typeName().decapitalize()}: ${containedLocation.typeName()}) = copy(map = map + (${coordinatesType.typeName().decapitalize()} to ${containedLocation.typeName().decapitalize()}))"
            } else
                ""
            }
            ${additionalContainedTypes.joinToString(separator = "\n            ") { "fun with${it.typeName()}(${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}, ${it.typeName().decapitalize()}: ${it.typeName()}) = ${if (it is ContainerSpec) "if (${it.typeName().decapitalize()}.isEmpty()) copy(map = map - ${coordinatesType.typeName().decapitalize()}) else " else ""}copy(map = map + (${coordinatesType.typeName().decapitalize()} to ${it.typeName().decapitalize()}))" }}

            fun without${containedType.typeName()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry)
            ${additionalContainedTypes.joinToString(separator = "\n            ") { "fun without${it.typeName()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry)" }}
            ${generateCopyFunctions()}
            ${generateIndexIterator(containedType.typeName())}
            ${additionalContainedTypes.joinToString(separator = "\n            ") { generateAccess(it) }}
            ${additionalContainedTypes.joinToString(separator = "\n            ") { generateIndexIterator(it.typeName()) }}
            ${generateLoopIterator()}
        }
        """.trimIndent()

    private fun generateAccess(type: Spec) = """
            val ${type.typeName().decapitalize()}s = ${type.typeName()}Access(map)

            class ${type.typeName()}Access(val map: Map<${coordinatesType.typeName()}, Any>) : Iterable<${coordinatesType.typeName()}> {
                override fun iterator(): Iterator<${coordinatesType.typeName()}> = IndexIterator${type.typeName()}(map)

                operator fun get(position: ${coordinatesType.typeName()}) = if (map[position] is ${type.typeName()}) map[position]!! as ${type.typeName()} else ${type.generateEmpty()}
            }
        """

    private fun generateIndexIterator(type: String) = """
            private class IndexIterator${if (type != containedType.typeName()) type else ""}(val map: Map<${coordinatesType.typeName()}, Any>) : Iterator<${coordinatesType.typeName()}> {
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
                    if (map[coordinates] is $type${if (type == containedType.typeName() && containedLocation != null) "|| map[coordinates] is ${containedLocation.typeName()}" else ""}) {
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
        """

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
            (childrenHierarchy(this).map { setOf(it.coordinatesType.projectName(), it.containedType.projectName()) }.flatten().toSet() + additionalContainedTypes.map { it.projectName() } + attributes.map { it.projectName() } - setOf(projectName, "")).joinToString(separator = "; ") { "import $basePackageName.entities.$it.*" }

    private fun generateCopyFunctions(): String {
        var result = ""
        val hierarchy = childrenHierarchy(this)
        (1 until hierarchy.size).forEach { index ->
            result += """
            fun with${hierarchy[index].containedType.typeName()}(${(0 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}: $it, "
                    }}${hierarchy[index].coordinatesType.typeName().decapitalize()}: ${hierarchy[index].coordinatesType.typeName()}, ${hierarchy[index].containedType.typeName().decapitalize()}: ${hierarchy[index].containedType.typeName()}) =
                with${containedType.typeName()}(${hierarchy[0].coordinatesType.typeName().decapitalize()}, this[${hierarchy[0].coordinatesType.typeName().decapitalize()}].with${hierarchy[index].containedType.typeName()}(${(1 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}, "
                    }}${hierarchy[index].coordinatesType.typeName().decapitalize()}, ${hierarchy[index].containedType.typeName().decapitalize()}))

            fun without${hierarchy[index].containedType.typeName()}(${(0 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}: $it, "
                    }}${hierarchy[index].coordinatesType.typeName().decapitalize()}: ${hierarchy[index].coordinatesType.typeName()}) =
                with${containedType.typeName()}(${hierarchy[0].coordinatesType.typeName().decapitalize()}, this[${hierarchy[0].coordinatesType.typeName().decapitalize()}].without${hierarchy[index].containedType.typeName()}(${(1 until index).filter { hierarchy[it].coordinatesType != hierarchy[index].coordinatesType }.map {
                hierarchy[it].coordinatesType.typeName()
            }.distinct().joinToString(separator = "") {
                        "${it.decapitalize()}, "
                    }}${hierarchy[index].coordinatesType.typeName().decapitalize()}))
            """
        }
        return result
    }

    private fun childrenHierarchy(childType: Spec): List<ContainerSpec> = when (childType) {
        is ContainerSpec ->
            listOf(childType) + if (childType.containedLocation == null) childrenHierarchy(childType.containedType) else emptyList()
        else -> emptyList()
    }

}