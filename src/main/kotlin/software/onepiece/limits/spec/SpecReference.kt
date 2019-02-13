package software.onepiece.limits.spec

class SpecReference(val typeName: String) : Spec {
    override fun generateEmpty() = throw RuntimeException("Spec reference needs to be resolved!")
    override fun emptyCheck() = throw RuntimeException("Spec reference needs to be resolved!")
    override fun projectName() = throw RuntimeException("Spec reference needs to be resolved!")
    override fun typeName() = throw RuntimeException("Spec reference needs to be resolved!")
}