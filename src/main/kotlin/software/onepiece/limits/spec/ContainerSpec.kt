package software.onepiece.limits.spec

import org.gradle.kotlin.dsl.support.unzipTo
import java.io.Serializable

class ContainerSpec(
        val projectName: String,
        val typeName: String,
        val coordinatesType: CoordinatesSpec,
        val containedType: Spec,
        val containedLocation: ChainOfCoordinates? = null, //TODO could potentially be a list
        val containedSubTypes: List<Spec> = emptyList(),
        val superType: SuperContainerSpec? = null,
        var attributes: List<Spec> = emptyList(),
        val recursionDepth: Int = 3) : Serializable, TypeSpec {

    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName(private val map: Map<${coordinatesType.typeName()}, Any> = mapOf()${
            attributes.joinToString(separator = "") { ", val ${it.propertyName()}: ${it.typeName()} = ${it.generateEmpty()}" }
        }): ${if (superType != null) "${superType.typeName()}, " else "" }Iterable<${coordinatesType.typeName()}> {

            companion object {
                val empty = $typeName()

                ${coordinatesType.generateSizeFields()}
            }
            ${if(superType != null) """
            override fun get(x: Int, y: Int, container: Any) = get(${coordinatesType.typeName()}.of(x${if (coordinatesType is Coordinates2Spec) ", y" else ""})${if (containedLocation != null) ", container as ${containedLocation.rootContainer}" else ""})

            override fun xMax() = ${(coordinatesType as? Coordinates2Spec)?.xType?.limit ?: (coordinatesType as CoordinateSpec).limit}
            override fun yMax() = ${(coordinatesType as? Coordinates2Spec)?.yType?.limit ?: 1}
            """ else ""}
            override fun iterator(): Iterator<${coordinatesType.typeName()}> = IndexIterator${containedType.typeName()}(map)

            operator fun get(position: ${coordinatesType.typeName()}${if (containedLocation != null) ", ${containedLocation.rootContainer.decapitalize()}: ${containedLocation.rootContainer}" else ""}) = if (map[position] is ${containedType.typeName()}) map[position]!! as ${containedType.typeName()} ${if (containedLocation != null) "else if (map[position] is ${containedLocation.typeName()}) ${containedLocation.rootContainer.decapitalize()}${(0 until containedLocation.components.size).joinToString(separator = "") { "${if(containedLocation.componentAccess.containsKey(it)) ".${containedLocation.componentAccess[it]!!}" else ""}[(map[position] as ${containedLocation.typeName()}).${containedLocation.coordName(it)}]" }} " else ""}else ${containedType.generateEmpty()}
            ${if (containedLocation != null) "fun getLocation(position: ${coordinatesType.typeName()}) = if (map[position] is ${containedLocation.typeName()}) map[position] as StickerPosition else null" else ""}

            fun isEmpty() = map.isEmpty()${attributes.joinToString(separator = "") { " && ${it.propertyName()} == ${it.generateEmpty()}" }}

            fun with${containedType.propertyName().capitalize()}(${coordinatesType.propertyName()}: ${coordinatesType.typeName()}, ${containedType.propertyName()}: ${containedType.typeName()}) = ${if (containedType is ContainerSpec) "if (${containedType.propertyName()}.isEmpty()) copy(map = map - ${coordinatesType.propertyName()}) else " else ""}copy(map = map + (${coordinatesType.propertyName()} to ${containedType.propertyName()}))
            ${if (containedLocation != null) {
                "fun with${containedType.propertyName().capitalize()}(${coordinatesType.propertyName()}: ${coordinatesType.typeName()}, ${containedLocation.propertyName()}: ${containedLocation.typeName()}) = copy(map = map + (${coordinatesType.propertyName()} to ${containedLocation.propertyName()}))"
            } else
                ""
            }
            ${containedSubTypes.joinToString(separator = "\n            ") { "fun with${it.propertyName().capitalize()}(${coordinatesType.propertyName()}: ${coordinatesType.typeName()}, ${it.propertyName()}: ${it.typeName()}) = ${if (it is ContainerSpec) "if (${it.propertyName()}.isEmpty()) copy(map = map - ${coordinatesType.propertyName()}) else " else ""}copy(map = map + (${coordinatesType.propertyName()} to ${it.propertyName()}))" }}
            ${attributes.joinToString(separator = "\n            ") { "fun with${it.propertyName().capitalize()}(${it.propertyName()}: ${it.typeName()}) = copy(${it.propertyName()} = ${it.propertyName()})" }}

            ${if (containedSubTypes.isEmpty()) {
                "fun without${containedType.propertyName().capitalize()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry)"
            } else
                ""
            }
            ${containedSubTypes.joinToString(separator = "\n            ") { "fun without${it.propertyName().capitalize()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry)" }}
            ${generateCopyFunctions()}
            ${generateIndexIterator(containedType.typeName())}
            ${containedSubTypes.joinToString(separator = "\n            ") { generateAccess(it) }}
            ${containedSubTypes.joinToString(separator = "\n            ") { generateIndexIterator(it.typeName()) }}
            ${generateLoopIterator()}
        }
        """.trimIndent().also { printNode(buildTypeGraph("", this, recursionDepth), "") }

    private fun printNode(n: Node, indent: String) {
        println(indent + n.current.typeName)
        n.children.forEach {
            printNode(it, "$indent  ")
        }
    }

    private fun generateAccess(type: Spec) = """
            val ${type.propertyName()}s = ${type.typeName()}Access(map)

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
            (collectChildTypes(buildTypeGraph("", this, 1)).distinct().map { setOf(it.coordinatesType.projectName(), it.containedType.projectName()) }.flatten().toSet() + containedSubTypes.map { it.projectName() } + attributes.map { it.projectName() } - setOf(projectName, "")).joinToString(separator = "; ") { "import $basePackageName.entities.$it.*" }

    private fun collectChildTypes(node: Node): List<ContainerSpec> = listOf(node.current) + node.children.map { collectChildTypes(it) }.flatten()


    private fun generateCopyFunctions() = visitGraph(buildTypeGraph("", this, recursionDepth), emptyList())

    private fun visitGraph(node: Node, parents: List<Node>): String {
        var result = ""
        if (!parents.isEmpty() && node.current.containedType !is SuperContainerSpec) {
            val accessName = if (parents.size > 1) parents[1].access else node.access
            val isAccessThroughAttribute = parents[0].current.attributes.any { it.typeName() == accessName }

            val accessCode = if (accessName != "") if (isAccessThroughAttribute) accessName.decapitalize() else "${parents[0].current.coordinatesType.propertyName()}, ${accessName.decapitalize()}s[${parents[0].current.coordinatesType.propertyName()}]" else "${parents[0].current.coordinatesType.propertyName()}, this[${parents[0].current.coordinatesType.propertyName()}]"

            var p = parents.subList(1, parents.size)
            var go = true
            while(go) {
                go = false
                if (p.size > 1) {
                    for (i in 1 until p.size) {
                        // nested through attribute
                        if (p[i - 1].current.attributes.any { it.typeName() == p[i].access }) {
                            p = p.subList(0, i - 1) + p.subList(i, p.size)
                            go = true
                            break
                        }
                    }
                }
            }

            val requiredCoordinates = if (isAccessThroughAttribute) p else listOf(parents[0]) + p

            with(node.current) {
                result += """
            fun with${containedType.propertyName().capitalize()}(${requiredCoordinates.joinToString(separator = "") {
                    "${it.current.coordinatesType.propertyName(it.recursion)}: ${it.current.coordinatesType.typeName()}, "
                }}${coordinatesType.propertyName(node.recursion)}: ${coordinatesType.typeName()}, ${containedType.propertyName(node.recursion)}: ${containedType.typeName()}) =
                with${if (accessName != "") accessName else parents[0].current.containedType.propertyName().capitalize()}($accessCode.with${containedType.propertyName().capitalize()}(${p.joinToString(separator = "") {
                    "${it.current.coordinatesType.propertyName(it.recursion)}, "
                }}${coordinatesType.propertyName(node.recursion)}, ${containedType.propertyName(node.recursion)}))

            fun without${containedType.propertyName().capitalize()}(${requiredCoordinates.joinToString(separator = "") {
                    "${it.current.coordinatesType.propertyName(it.recursion)}: ${it.current.coordinatesType.typeName()}, "
                }}${coordinatesType.propertyName(node.recursion)}: ${coordinatesType.typeName()}) =
                with${if (accessName != "") accessName else parents[0].current.containedType.propertyName().capitalize()}($accessCode.without${containedType.propertyName().capitalize()}(${p.joinToString(separator = "") {
                    "${it.current.coordinatesType.propertyName(it.recursion)}, "
                }}${coordinatesType.propertyName(node.recursion)}))
            """
            }
        }
        node.children.forEach { nextChild ->
            result += visitGraph(nextChild, parents + node)
        }
        return result
    }

    private fun buildTypeGraph(access: String, spec: ContainerSpec, depth: Int, recursionCounter: Map<ContainerSpec, Int> = emptyMap()): Node {
        return Node(access, spec, recursionCounter.getOrDefault(spec, 0), emptyList<Node>()
                + if (spec.containedType is ContainerSpec && recursionCounter.getOrDefault(spec.containedType, 0) < depth && spec.containedLocation == null && spec.containedSubTypes.isEmpty()) {
                        listOf(buildTypeGraph("", spec.containedType, depth, recursionCounter + (spec to recursionCounter.getOrDefault(spec, 0) + 1)))
                    } else {
                        emptyList()
                    }
                + spec.containedSubTypes.filter { it is ContainerSpec && recursionCounter.getOrDefault(it, 0) < depth }.map { buildTypeGraph(it.typeName(), it as ContainerSpec, depth, recursionCounter + (spec to recursionCounter.getOrDefault(spec, 0) + 1)) }
                + spec.attributes.filter { it is ContainerSpec && recursionCounter.getOrDefault(it, 0) < depth }.map { buildTypeGraph(it.typeName(), it as ContainerSpec, depth, recursionCounter + (spec to recursionCounter.getOrDefault(spec, 0) + 1)) }
        )
    }

    private data class Node(val access: String, val current: ContainerSpec, val recursion: Int, val children: List<Node> = emptyList())

}