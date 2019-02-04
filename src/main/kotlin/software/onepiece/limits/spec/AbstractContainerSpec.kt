package software.onepiece.limits.spec

interface AbstractContainerSpec : Spec {
    fun coordinatesType(): CoordinatesSpec
    fun containedType(): Spec
    fun containedLocation(): ChainOfCoordinates?
    fun attributes(): List<Spec>
    fun containedSubTypes(): List<AbstractContainerSpec>
}