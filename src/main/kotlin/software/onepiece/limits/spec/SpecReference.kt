package software.onepiece.limits.spec

class SpecReference(val typeName: String) : Spec {
    override fun generateEmpty(): String {
        throw RuntimeException("Spec reference needs to be resolved!")
    }

    override fun projectName(): String {
        throw RuntimeException("Spec reference needs to be resolved!")
    }

    override fun typeName(): String {
        throw RuntimeException("Spec reference needs to be resolved!")
    }

}