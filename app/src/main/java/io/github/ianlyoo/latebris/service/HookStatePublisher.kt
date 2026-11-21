package io.github.ianlyoo.latebris.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import io.github.ianlyoo.latebris.data.preset.PresetRepository
import io.github.ianlyoo.latebris.hook.HookState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Mirrors every simulation state change (including the 2 s heartbeat) into the
 * [HookState.PREF_NAME] preferences file, which the hooks read inside target apps.
 */
class HookStatePublisher(
    context: Context,
    private val stateStore: SimulationStateStore,
    private val presetRepository: PresetRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val preferences = openSharedWithHooks(context.applicationContext)

    fun start() {
        scope.launch {
            stateStore.state.collect { state ->
                val hookState = HookState.from(state, presetRepository::getPreset)
                preferences.edit().putString(HookState.KEY_STATE, hookState.encode()).apply()
            }
        }
    }

    private companion object {
        /**
         * With the module active, LSPosed (meta-data `xposedsharedprefs`) accepts
         * MODE_WORLD_READABLE and stores the file where scoped apps can read it through
         * XSharedPreferences. Without LSPosed the platform rejects the mode; the hooks are
         * not running then, so a private file is the right place.
         */
        @SuppressLint("WorldReadableFiles")
        fun openSharedWithHooks(context: Context): SharedPreferences = try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(HookState.PREF_NAME, Context.MODE_WORLD_READABLE)
        } catch (notUnderLsposed: SecurityException) {
            context.getSharedPreferences(HookState.PREF_NAME, Context.MODE_PRIVATE)
        }
    }
}
