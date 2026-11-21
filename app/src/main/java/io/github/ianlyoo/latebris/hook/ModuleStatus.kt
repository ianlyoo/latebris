package io.github.ianlyoo.latebris.hook

/**
 * Tells the Latebris UI whether LSPosed loaded the module into Latebris itself.
 * [ModuleEntryPoint] replaces [isActive] in Latebris's own process; without the
 * framework it stays false.
 */
object ModuleStatus {
    fun isActive(): Boolean = false
}
