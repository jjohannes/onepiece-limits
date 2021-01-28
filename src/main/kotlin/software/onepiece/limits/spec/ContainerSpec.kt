package software.onepiece.limits.spec

class ContainerSpec(
        val projectName: String,
        val typeName: String,
        val coordinatesType: CoordinatesSpec = NullSpec,
        val containedType: Spec = NullSpec,
        val containedSubTypes: List<ContainerSpec> = emptyList(),
        val superType: SuperContainerSpec? = null,
        var attributes: List<Spec> = emptyList(),
        val root: Boolean = false,
        val recursionDepth: Int = 3) : Spec {

    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"
    override fun emptyCheck() = ".isEmpty()"
    fun coordinatesType() = coordinatesType
    fun containedType() = containedType
    fun attributes() = attributes
    fun containedSubTypes() = containedSubTypes

    private var commandCounter = 0
    private val jsonParseCode = mutableListOf<String>()
    private val visitorFunctionTypes = mutableSetOf<ContainerSpec>()

    private var commandFunctions = ""

    override fun generateCommandFactory(packageName: String) = if (root) """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        object ${typeName}Commands {

            @kotlinx.serialization.Serializable
            sealed class Command {
                abstract fun apply(target: $typeName): $typeName
                
                ${generateCommandFunctions()}
            }
            ${commandFunctions.also { commandFunctions = "" }}
        }
    """.trimIndent() else ""

    override fun generateDiffTool(packageName: String) = if (root) """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        class ${typeName}DiffTool(private val visitor: Visitor? = null) {

            ${generateDiffFunctions()}

            interface Visitor {
                ${visitorFunctionTypes.joinToString("\n                ") { "fun visit(current: ${it.typeName()}, previous: ${it.typeName()}, path: List<Any>)" }}
            }
        }
    """.trimIndent() else ""

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        ${generateImports(packageName)}

        data class $typeName private constructor(${if (containedType == NullSpec) "" else "private val map: Map<${coordinatesType.typeName()}, ${containedType.typeName()}> = emptyMap()"}${
            (if (containedType != NullSpec && attributes.isNotEmpty()) ", " else "") + attributes.joinToString { "val ${it.propertyName()}: ${it.typeName()} = ${it.generateEmpty()}" }
        }): ${if (superType != null) superType.typeName() else "" }${if (superType != null && coordinatesType != NullSpec) ", " else ""}${if (coordinatesType != NullSpec) "Iterable<${coordinatesType.typeName()}>" else ""} {

            companion object {
                val empty = $typeName()

                ${coordinatesType.generateSizeFields()}
            }

            fun isEmpty() = ${if(containedType != NullSpec) "map.isEmpty()" else "true"}${attributes.joinToString(separator = "") { " && ${it.propertyName()} == ${it.generateEmpty()}" }}

            ${if(containedType != NullSpec) """
            override fun iterator(): Iterator<${coordinatesType.typeName()}> = IndexIterator${containedType.typeName()}(map)

            operator fun get(position: ${coordinatesType.typeName()}) = map.getOrElse(position) { ${containedType.generateEmpty()} }

            ${if (containedSubTypes.isEmpty()) """
            fun with${containedType.propertyName().capitalize()}(${coordinatesType.propertyName()}: ${coordinatesType.typeName()}, ${containedType.propertyName()}: ${containedType.typeName()}) = if (${containedType.propertyName()}${containedType.emptyCheck()}) copy(map = map - ${coordinatesType.propertyName()}) else copy(map = map + (${coordinatesType.propertyName()} to ${containedType.propertyName()}))

            fun without${containedType.propertyName().capitalize()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry)
            """ else "" }
            ${generateIndexIterator(containedType.typeName())}
            ${generateLoopIterator()}
            """ else ""}
            ${attributes.joinToString(separator = "\n            ") { "fun with${it.propertyName().capitalize()}(${it.propertyName()}: ${it.typeName()}) = copy(${it.propertyName()} = ${it.propertyName()})" }}
            ${containedSubTypes.joinToString(separator = "\n            ") { "fun with${it.propertyName().capitalize()}(${coordinatesType.propertyName()}: ${coordinatesType.typeName()}, ${it.propertyName()}: ${it.typeName()}) = if (${it.propertyName()}.isEmpty()) copy(map = map - ${coordinatesType.propertyName()}) else copy(map = map + (${coordinatesType.propertyName()} to ${it.propertyName()}))" }}
            ${containedSubTypes.joinToString(separator = "\n            ") { "fun without${it.propertyName().capitalize()}(entry: ${coordinatesType.typeName()}): $typeName = copy(map = map - entry)" }}
            ${generateCopyFunctions()}
            ${containedSubTypes.joinToString(separator = "\n            ") { generateAccess(it) }}
            ${containedSubTypes.joinToString(separator = "\n            ") { generateIndexIterator(it.typeName()) }}
            ${if (superType != null) "override " else "" }fun dataHash() = ${dataHashCall((if (containedType == NullSpec) emptyList() else listOf("this.map { it${coordinatesType.dataHashCall()} xor this[it]${containedType.dataHashCall()} }.sum()")) + attributes.map { it.propertyName() + it.dataHashCall() })}
        }
        """.trimIndent() //.also { printNode(buildTypeGraph("", this, recursionDepth), "") }

    private fun printNode(n: Node, indent: String) {
        println(indent + n.current.typeName())
        n.children.forEach {
            printNode(it, "$indent  ")
        }
    }

    private fun dataHashCall(properties: List<String>): String =  if (properties.size == 1) properties.last() else "(${dataHashCall(properties - properties.last())}) * 31 + ${properties.last()}"

    private fun generateAccess(type: ContainerSpec) = """
            val ${type.propertyName()}s = ${type.typeName()}Access(map)

            class ${type.typeName()}Access(val map: Map<${coordinatesType.typeName()}, ${containedType.typeName()}>) : Iterable<${coordinatesType.typeName()}> {
                override fun iterator(): Iterator<${coordinatesType.typeName()}> = IndexIterator${type.typeName()}(map)

                operator fun get(position: ${coordinatesType.typeName()}) = if (map[position] is ${type.typeName()}) map[position]!! as ${type.typeName()} else ${type.generateEmpty()}
            }
        """

    private fun generateIndexIterator(type: String) = """
            private class IndexIterator$type(val map: Map<${coordinatesType.typeName()}, ${containedType.typeName()}>) : Iterator<${coordinatesType.typeName()}> {
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
                    if (map[coordinates] is $type) {
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

                fun current() = ${coordinatesType.typeName()}.values[idx]

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
            (collectChildTypes(buildTypeGraph("", this, false, false, 1)).distinct().map { setOf(it.coordinatesType().projectName(), it.containedType().projectName()) }.flatten().toSet() + containedSubTypes.map { it.projectName() } + attributes.map { it.projectName() } - setOf(projectName, "")).joinToString(separator = "; ") { "import $basePackageName.entities.$it.*" }

    private fun collectChildTypes(node: Node): List<ContainerSpec> = listOf(node.current) + node.children.map { collectChildTypes(it) }.flatten()


    private fun generateCopyFunctions() = visitGraph(buildTypeGraph("", this), emptyList(), false, false)
    private fun generateCommandFunctions() = visitGraph(buildTypeGraph("", this), emptyList(), true, false)
    private fun generateDiffFunctions() = visitGraph(buildTypeGraph("", this), emptyList(), false, true)

    private fun visitGraph(node: Node, parents: List<Node>, asCommands: Boolean, asDiff: Boolean): String {
        var result = ""
        var commandResult = ""
        var diffResult = ""

        val isAccessThroughAttribute = if (parents.size > 1) parents[1].isAttribute else node.isAttribute

        val p = if (parents.isEmpty()) mutableListOf() else parents.subList(1, parents.size).toMutableList()
        val toRemove = mutableListOf<Int>()
        for (i in 1 until p.size) {
            // nested through attribute
            if (p[i].isAttribute) {
                toRemove.add(0, i -1)
            }
        }
        if (node.isAttribute && p.isNotEmpty()) {
            toRemove.add(0, p.lastIndex)
        }
        toRemove.forEach { i ->
            p.removeAt(i)
        }
        val requiredCoordinates = if (isAccessThroughAttribute || parents.isEmpty()) p else listOf(parents[0]) + p

        if (!parents.isEmpty()) {
            val accessName = if (parents.size > 1) parents[1].access else node.access
            val accessCode = if (accessName != "") {
                if (isAccessThroughAttribute) {
                    accessName.decapitalize()
                } else {
                    // sub-type access
                    "${parents[0].current.coordinatesType().propertyName()}, ${accessName.decapitalize()}s[${parents[0].current.coordinatesType().propertyName()}]"
                }
            } else {
                "${parents[0].current.coordinatesType().propertyName()}, this[${parents[0].current.coordinatesType().propertyName()}]"
            }

            val childPropertyName = if (accessName != "") accessName else parents[0].current.containedType().propertyName().capitalize()
            if (node.current.containedType() !is SuperContainerSpec && node.current.containedType() != NullSpec) {
                val propertyName = node.current.containedType().propertyName().capitalize()
                result += generateModifyFunction(propertyName, "with", childPropertyName, accessCode,
                        requiredCoordinates.map { it.current.coordinatesType() to it.recursion } + (node.current.coordinatesType() to node.recursion) + (node.current.containedType() to 0),
                        p.map { it.current.coordinatesType() to it.recursion } + (node.current.coordinatesType() to node.recursion) + (node.current.containedType() to 0))
                result += generateModifyFunction(propertyName, "without", childPropertyName, accessCode,
                        requiredCoordinates.map { it.current.coordinatesType() to it.recursion } + (node.current.coordinatesType() to node.recursion),
                        p.map { it.current.coordinatesType() to it.recursion } + (node.current.coordinatesType() to node.recursion))
            }
            node.current.attributes().filter { it !is ContainerSpec }.forEach { attribute ->
                result += generateModifyFunction(attribute.propertyName().capitalize(), "with", childPropertyName, accessCode,
                        requiredCoordinates.map { it.current.coordinatesType() to it.recursion } + (attribute to 0),
                        p.map { it.current.coordinatesType() to it.recursion } + (attribute to 0))
            }
        }

        if (asCommands && node.current.containedType() !is ContainerSpec && node.current.containedType() !is SuperContainerSpec && node.current.containedType() != NullSpec) {
            val propertyName = node.current.containedType().propertyName().capitalize()
            commandResult += generateCommand(propertyName, "with",
                    requiredCoordinates.map { it.current.coordinatesType() to it.recursion } + (node.current.coordinatesType() to node.recursion) + (node.current.containedType() to 0))
            commandResult += generateCommand(propertyName, "without",
                    requiredCoordinates.map { it.current.coordinatesType() to it.recursion } + (node.current.coordinatesType() to node.recursion))
        }
        if (asCommands) {
            node.current.attributes().filter { it !is ContainerSpec }.forEach { attribute ->
                commandResult += generateCommand(attribute.propertyName().capitalize(), "with",
                        requiredCoordinates.map { it.current.coordinatesType() to it.recursion } + (attribute to 0))
            }
        }

        if (asDiff) {
             diffResult += generateDiff(node,
                    requiredCoordinates.map { it.current.coordinatesType() to it.recursion })
        }

        node.children.forEach { nextChild ->
            when {
                asCommands -> commandResult += visitGraph(nextChild, parents + node, asCommands, asDiff)
                asDiff -> diffResult += visitGraph(nextChild, parents + node, asCommands, asDiff)
                else -> result += visitGraph(nextChild, parents + node, asCommands, asDiff)
            }
        }
        return if (asCommands) commandResult else if (asDiff) diffResult else result
    }

    private
    fun generateModifyFunction(propertyName: String, kind: String, childPropertyName: String, accessCode: String,
                               coordinates: List<Pair<Spec, Int>>, argCoordinates: List<Pair<Spec, Int>>): String {
        val paramListFunction =
                coordinates.joinToString(separator = ", ") { "${it.first.propertyName(it.second)}: ${it.first.typeName()}" }
        val argumentListFunction =
                argCoordinates.joinToString(separator = ", ") { it.first.propertyName(it.second) }

        return """
            fun $kind$propertyName($paramListFunction) =
                with$childPropertyName($accessCode.$kind$propertyName($argumentListFunction))
        """
    }

    private
    fun generateCommand(propertyName: String, kind: String, coordinates: List<Pair<Spec, Int>>): String {
        val paramListConstructor =
                coordinates.joinToString(separator = ", ") { "val ${it.first.propertyName(it.second)}: ${it.first.typeName()}" }
        val paramListFunction =
                coordinates.joinToString(separator = ", ") { "${it.first.propertyName(it.second)}: ${it.first.typeName()}" }
        val argumentListFunction =
                coordinates.joinToString(separator = ", ") { it.first.propertyName(it.second) }
        val argumentListFunctionFromString =
                coordinates.joinToString(separator = ", ") { if (it.first is NativePrimitiveSpec) """entries.getValue("${it.first.propertyName(it.second)}").to${it.first.typeName()}()""" else """${it.first.typeName()}.of(entries.getValue("${it.first.propertyName(it.second)}"))""" }

        return """
                @kotlinx.serialization.Serializable 
                data class C$commandCounter($paramListConstructor) : Command() {
                    override fun apply(target: $typeName) = target.$kind$propertyName($argumentListFunction)
                }
        """.also {
            commandFunctions += """
            fun $kind$propertyName($paramListFunction) : Command = Command.C$commandCounter($argumentListFunction)
        """
            jsonParseCode.add("$commandCounter -> $kind$propertyName($argumentListFunctionFromString)")
            commandCounter++
        }
    }

    private
    fun generateDiff(node: Node, coordinates: List<Pair<Spec, Int>>): String {
        val current = node.current
        val propertyName = current.propertyName().capitalize()

        val paramListFunction =
                coordinates.joinToString(separator = "") { "${it.first.propertyName(it.second)}: ${it.first.typeName()}, " }
        val argumentListFunction =
                coordinates.joinToString(separator = "") { "${it.first.propertyName(it.second)}, " }

        return """
            fun diff(${paramListFunction}current: $propertyName, previous: $propertyName, commands: MutableList<${typeName}Commands.Command>) {
                if (current != previous) {
                    visitor?.visit(current, previous, listOf(${argumentListFunction.substringBeforeLast(",").also { visitorFunctionTypes.add(current) }}))
                    ${current.attributes().filter { it !is ContainerSpec }.joinToString("\n                    ") { "if (current.${it.propertyName()} != previous.${it.propertyName()}) { commands.add(${typeName}Commands.with${it.propertyName().capitalize()}(${argumentListFunction}current.${it.propertyName()})) }" }}
                    ${current.attributes().filter { it is ContainerSpec  }.joinToString("\n                    ") { "if (current.${it.propertyName()} != previous.${it.propertyName()}) { diff(${argumentListFunction}current.${it.propertyName()}, previous.${it.propertyName()}, commands) }" }}
                    ${when {
                        (node.children.isEmpty() || current.containedType() == NullSpec) && (current.containedType() is ContainerSpec || current.containedType() is SuperContainerSpec || current.containedType() == NullSpec) -> "" /* end of recursion */
                        current.containedSubTypes().isEmpty() -> "current.union(previous).forEach { diff(${argumentListFunction}it, current[it], previous[it], commands) }"
                        else -> current.containedSubTypes().joinToString("\n                    ") { "current.${it.propertyName()}s.union(previous.${it.propertyName()}s).forEach { diff(${argumentListFunction}it, current.${it.propertyName()}s[it], previous.${it.propertyName()}s[it], commands) }" }
                    }}
                }
            }
        """ + if (current.containedType() is ContainerSpec || current.containedType() is SuperContainerSpec || current.containedType() is NullSpec) "" else """
            fun diff($paramListFunction${current.coordinatesType().propertyName(node.recursion)}: ${current.coordinatesType().typeName()}, current: ${current.containedType().typeName()}, previous: ${current.containedType().typeName()}, commands: MutableList<${typeName}Commands.Command>) {
                if (current != previous) {
                    commands.add(${typeName}Commands.with${current.containedType().propertyName().capitalize()}($argumentListFunction${current.coordinatesType().propertyName(node.recursion)}, current))
                }
            }
        """
    }

    private fun buildTypeGraph(access: String, spec: ContainerSpec, isSubtype: Boolean = false, isAttribute: Boolean = false, depth: Int = recursionDepth, recursionCounter: Map<ContainerSpec, Int> = emptyMap()): Node {
        return Node(access, spec, isSubtype, isAttribute, recursionCounter.getOrDefault(spec, 0), emptyList<Node>()
                + if (spec.containedType() is ContainerSpec && recursionCounter.getOrDefault(spec.containedType(), 0) < depth && spec.containedSubTypes().isEmpty()) {
                        listOf(buildTypeGraph("", spec.containedType() as ContainerSpec, false, false, depth, recursionCounter + (spec to recursionCounter.getOrDefault(spec, 0) + 1)))
                    } else {
                        emptyList()
                    }
                + spec.containedSubTypes().filter { recursionCounter.getOrDefault(it, 0) < depth }.map { buildTypeGraph(it.typeName(), it, true, false, depth, recursionCounter + (spec to recursionCounter.getOrDefault(spec, 0) + 1)) }
                + spec.attributes().filter { it is ContainerSpec && recursionCounter.getOrDefault(it, 0) < depth }.map { buildTypeGraph(it.typeName(), it as ContainerSpec, false, true, depth, recursionCounter + (spec to recursionCounter.getOrDefault(spec, 0) + 1)) }
        )
    }

    private data class Node(val access: String, val current: ContainerSpec, val isSubtype: Boolean, val isAttribute: Boolean, val recursion: Int, val children: List<Node> = emptyList())

}