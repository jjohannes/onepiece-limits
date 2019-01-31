package software.onepiece.limits

import software.onepiece.limits.spec.Spec

open class LimitsPluginExtension {
    lateinit var packageName: String
    lateinit var specs: List<Spec>
}