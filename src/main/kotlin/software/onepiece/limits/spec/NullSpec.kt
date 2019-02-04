package software.onepiece.limits.spec


object NullSpec : CoordinatesSpec {
    override fun projectName() = ""
    override fun generateSizeFields() = ""
    override fun generateSizeFieldsSum() = throw IllegalStateException()
    override fun generateIndexIteratorEntry() = throw IllegalStateException()
    override fun typeName() = throw IllegalStateException()
}