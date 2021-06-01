package software.onepiece.limits

import software.onepiece.limits.spec.ContainerSpec
import software.onepiece.limits.spec.Spec

class LimitsContainerDetails(
    private val attributes: MutableList<Spec>,
    private val subTypes: MutableList<ContainerSpec>
) {

    fun attribute(attrSpec: Spec) {
        attributes.add(attrSpec)
    }

    fun subType(subTypeSpec: ContainerSpec) {
        subTypes.add(subTypeSpec)
    }
}
