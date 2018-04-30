package software.onepiece.limits.spec

import java.io.Serializable

data class ContainerSpec(
        val projectName: String,
        val typeName: String,
        val coordinatesType: CoordinatesSpec,
        val containedType: Spec,
        val containedLocation: ChainOfCoordinates? = null, //TODO could potentially be a list
        val additionalContainedTypes: List<Spec> = emptyList(),
        val superType: Spec? = null,
        val attributes: List<Spec> = emptyList()) : Serializable, TypeSpec {

    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName(private val map: Map<${coordinatesType.typeName()}, Any> = mapOf()${
            attributes.joinToString(separator = "") { ", val ${it.typeName().decapitalize()}: ${it.typeName()} = ${it.generateEmpty()}" }
        }): ${if (superType != null) "${superType.typeName()}, " else "" }Iterable<${coordinatesType.typeName()}> {

            companion object {
                val empty = $typeName()

                ${coordinatesType.generateSizeFields()}
            }
            ${if(superType != null) """
            override fun get(x: Int, y: Int, container: Any) = get(${coordinatesType.typeName()}.of(x, y)${if (containedLocation != null) ", container as ${containedLocation.rootContainer}" else ""})

            override fun xMax() = ${(coordinatesType as Coordinates2Spec).xType.limit}
            override fun yMax() = ${coordinatesType.yType.limit}
            """ else ""}
            override fun iterator(): Iterator<${coordinatesType.typeName()}> = IndexIterator${containedType.typeName()}(map)

            operator fun get(position: ${coordinatesType.typeName()}${if (containedLocation != null) ", ${containedLocation.rootContainer.decapitalize()}: ${containedLocation.rootContainer}" else ""}) = if (map[position] is ${containedType.typeName()}) map[position]!! as ${containedType.typeName()} ${if (containedLocation != null) "else if (map[position] is ${containedLocation.typeName()}) ${containedLocation.rootContainer.decapitalize()}${(0 until containedLocation.components.size).joinToString(separator = "") { "${if(containedLocation.componentAccess.containsKey(it)) ".${containedLocation.componentAccess[it]!!}" else ""}[(map[position] as ${containedLocation.typeName()}).${containedLocation.coordName(it)}]" }} " else ""}else ${containedType.generateEmpty()}

            fun isEmpty() = map.isEmpty()${attributes.joinToString(separator = "") { " && ${it.typeName().decapitalize()} == ${it.generateEmpty()}" }}

            ${if (additionalContainedTypes.isEmpty()) {
                "fun with${containedType.typeName()}(${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}, ${containedType.typeName().decapitalize()}: ${containedType.typeName()}) = ${if (containedType is ContainerSpec) "if (${containedType.typeName().decapitalize()}.isEmpty()) copy(map = map - ${coordinatesType.typeName().decapitalize()}) else " else ""}copy(map = map + (${coordinatesType.typeName().decapitalize()} to ${containedType.typeName().decapitalize()}))"
            } else
                ""
            }
            ${if (containedLocation != null) {
                "fun with${containedType.typeName()}(${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}, ${containedLocation.typeName().decapitalize()}: ${containedLocation.typeName()}) = copy(map = map + (${coordinatesType.typeName().decapitalize()} to ${containedLocation.typeName().decapitalize()}))"
            } else
                ""
            }
            ${additionalContainedTypes.joinToString(separator = "\n            ") { "fun with${it.typeName()}(${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}, ${it.typeName().decapitalize()}: ${it.typeName()}) = ${if (it is ContainerSpec) "if (${it.typeName().decapitalize()}.isEmpty()) copy(map = map - ${coordinatesType.typeName().decapitalize()}) else " else ""}copy(map = map + (${coordinatesType.typeName().decapitalize()} to ${it.typeName().decapitalize()}))" }}
            ${attributes.joinToString(separator = "\n            ") { "fun with${it.typeName()}(${it.typeName().decapitalize()}: ${it.typeName()}) = copy(${it.typeName().decapitalize()} = ${it.typeName().decapitalize()})" }}

            ${if (additionalContainedTypes.isEmpty()) {
                "fun without${containedType.typeName()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry)"
            } else
                ""
            }
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
            private class IndexIterator$type(val map: Map<${coordinatesType.typeName()}, Any>) : Iterator<${coordinatesType.typeName()}> {
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
            (collectChildTypes(buildTypeGraph(this)).distinct().map { setOf(it.coordinatesType.projectName(), it.containedType.projectName()) }.flatten().toSet() + additionalContainedTypes.map { it.projectName() } + attributes.map { it.projectName() } - setOf(projectName, "")).joinToString(separator = "; ") { "import $basePackageName.entities.$it.*" }

    private fun collectChildTypes(node: Node): List<ContainerSpec> = listOf(node.current) + node.children.map { collectChildTypes(it.value) }.flatten()


    private fun generateCopyFunctions() = visitGraph("", buildTypeGraph(this), emptyList())

    private fun visitGraph(name: String, node: Node, parents: List<Pair<String, ContainerSpec>>): String {
        var result = ""
        if (!parents.isEmpty() && node.current.containedType !is SuperContainerSpec) {
            val p = parents.subList(1, parents.size)
            val accessName = (if (parents.size > 1) parents[1].first else name)
            val isAttribute = attributes.any { it.typeName() == accessName }
            val accessCode = if (accessName != "") if (isAttribute) accessName.decapitalize() else "${parents[0].second.coordinatesType.typeName().decapitalize()}, ${accessName.decapitalize()}s[${parents[0].second.coordinatesType.typeName().decapitalize()}]" else "${parents[0].second.coordinatesType.typeName().decapitalize()}, this[${parents[0].second.coordinatesType.typeName().decapitalize()}]"

            val requiredCoordinates = if (isAttribute) p else parents

            with(node.current) {
                result += """
            fun with${containedType.typeName()}(${requiredCoordinates.joinToString(separator = "") {
                    "${it.second.coordinatesType.typeName().decapitalize()}: ${it.second.coordinatesType.typeName()}, "
                }}${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}, ${containedType.typeName().decapitalize()}: ${containedType.typeName()}) =
                with${if (accessName != "") accessName else parents[0].second.containedType.typeName()}($accessCode.with${containedType.typeName()}(${p.joinToString(separator = "") {
                    "${it.second.coordinatesType.typeName().decapitalize()}, "
                }}${coordinatesType.typeName().decapitalize()}, ${containedType.typeName().decapitalize()}))

            fun without${containedType.typeName()}(${requiredCoordinates.joinToString(separator = "") {
                    "${it.second.coordinatesType.typeName().decapitalize()}: ${it.second.coordinatesType.typeName()}, "
                }}${coordinatesType.typeName().decapitalize()}: ${coordinatesType.typeName()}) =
                with${if (accessName != "") accessName else parents[0].second.containedType.typeName()}($accessCode.without${containedType.typeName()}(${p.joinToString(separator = "") {
                    "${it.second.coordinatesType.typeName().decapitalize()}, "
                }}${coordinatesType.typeName().decapitalize()}))
            """
            }
        }
        node.children.forEach {
            result += visitGraph(it.key, it.value, parents + (name to node.current))
        }
        return result
    }

    private fun buildTypeGraph(childType: ContainerSpec): Node =
        Node(childType, emptyMap<String, Node>()
                + if (childType.containedType is ContainerSpec && childType.containedLocation == null && childType.additionalContainedTypes.isEmpty()) { mapOf("" to buildTypeGraph(childType.containedType)) } else { emptyMap() }
                + childType.additionalContainedTypes.filter { it is ContainerSpec }.map { it.typeName() to buildTypeGraph(it as ContainerSpec)}
                + childType.attributes.filter { it is ContainerSpec }.map { it.typeName() to buildTypeGraph(it as ContainerSpec) }
        )

    private data class Node(val current: ContainerSpec, val children: Map<String, Node> = emptyMap())

}