package software.onepiece.limits

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import software.onepiece.limits.spec.Spec

abstract class LimitsPluginExtension {
    abstract val packageName: Property<String>
    abstract val specs: ListProperty<Spec>
}