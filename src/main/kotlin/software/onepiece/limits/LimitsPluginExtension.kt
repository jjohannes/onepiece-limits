package software.onepiece.limits

import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import software.onepiece.limits.spec.ContainerSpec
import software.onepiece.limits.spec.CoordinateSpec
import software.onepiece.limits.spec.Coordinates2Spec
import software.onepiece.limits.spec.CoordinatesSpec
import software.onepiece.limits.spec.NativePrimitiveSpec
import software.onepiece.limits.spec.NullSpec
import software.onepiece.limits.spec.Spec
import software.onepiece.limits.spec.SuperContainerSpec
import java.util.Locale

abstract class LimitsPluginExtension {
    abstract val basePackage: Property<String>
    abstract val projectName: Property<String>
    abstract val specs: ListProperty<Spec>

    fun root(name: String, coordinatesType: CoordinatesSpec, containedType: Spec, conf: Action<LimitsContainerDetails>): ContainerSpec {
        val attributes = mutableListOf<Spec>()
        val subTypes = mutableListOf<ContainerSpec>()
        conf.execute(LimitsContainerDetails(attributes, subTypes))
        val spec = ContainerSpec(projectName.get(), name.capitalize(Locale.ROOT), coordinatesType, containedType, emptyList(), null, attributes, true)
        specs.add(spec)
        return spec
    }

    fun container(name: String, coordinatesType: CoordinatesSpec, containedType: Spec, superType: SuperContainerSpec? = null, conf: Action<LimitsContainerDetails> = Action {}): ContainerSpec {
        val attributes = mutableListOf<Spec>()
        val subTypes = mutableListOf<ContainerSpec>()
        conf.execute(LimitsContainerDetails(attributes, subTypes))
        val spec = ContainerSpec(projectName.get(), name.capitalize(Locale.ROOT), coordinatesType, containedType, subTypes, superType, attributes)
        specs.add(spec)
        return spec
    }

    fun superContainer(name: String) = SuperContainerSpec(
        projectName.get(), name.capitalize(Locale.ROOT)
    ).also  {
        specs.add(it)
    }

    fun coordinate(name: String, limit: Int, literalPrefix: String): CoordinateSpec = CoordinateSpec(
        projectName.get(), name.capitalize(Locale.ROOT), limit, literalPrefix
    ).also  {
        specs.add(it)
    }

    fun coordinates(name: String, xType: CoordinateSpec, yType: CoordinateSpec): CoordinatesSpec = Coordinates2Spec(
        projectName.get(), name.capitalize(Locale.ROOT), xType, yType
    ).also  {
        specs.add(it)
    }

    fun primitive(name: String, nativeType: String, emptyValue: String = """"""""): Spec = NativePrimitiveSpec(
        name, nativeType, emptyValue
    )

    fun nothing(): CoordinatesSpec = NullSpec
}