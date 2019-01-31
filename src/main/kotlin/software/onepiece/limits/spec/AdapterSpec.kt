package software.onepiece.limits.spec

import java.io.Serializable

class AdapterSpec(
        val projectName: String,
        val typeName: String,
        val superType: SuperContainerSpec) : Serializable, Spec {

    override fun projectName() = projectName
    override fun typeName() = typeName
    override fun generateEmpty() = "${typeName()}.empty"

    override fun generate(packageName: String) = """
        package $packageName.entities.$projectName

        class $typeName(var wrapped: ${superType.typeName()} = NoOp): ${superType.typeName()} {

            object NoOp : StickerContainer {
                override fun get(x: Int, y: Int, container: Any) = Sticker.empty
                override fun xMax() = 0
                override fun yMax() = 0
            }

            companion object {
                val empty = NoOp
            }

            override fun get(x: Int, y: Int, container: Any) = wrapped[x, y, container]

            override fun xMax() = wrapped.xMax()
            override fun yMax() = wrapped.yMax()
        }
        """.trimIndent()
}