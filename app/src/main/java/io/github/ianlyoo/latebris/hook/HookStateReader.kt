package io.github.ianlyoo.latebris.hook

import android.os.SystemClock
import android.util.Log
import de.robv.android.xposed.XSharedPreferences
import io.github.ianlyoo.latebris.BuildConfig

/**
 * Reads the [HookState] that Latebris publishes, from inside a target app.
 *
 * The file is checked at most once per [TTL_MILLIS], so a hooked getter costs a field read
 * instead of a file or binder access. A state whose heartbeat is older than
 * [HookState.HEARTBEAT_TIMEOUT_MILLIS] counts as inactive.
 */
object HookStateReader {
    private const val TAG = "LatebrisHookState"
    private const val TTL_MILLIS = 1_000L

    private val preferences by lazy { XSharedPreferences(BuildConfig.APPLICATION_ID, HookState.PREF_NAME) }

    @Volatile
    private var cached: HookState = HookState.INACTIVE

    @Volatile
    private var refreshedAt: Long? = null

    @Volatile
    private var reportedUnreadable = false

    /** The live state, or null when the simulation is off or Latebris stopped responding. */
    fun current(): HookState? {
        val now = SystemClock.elapsedRealtime()
        if (isDue(now)) {
            refresh(now)
        }
        return cached.takeIf { it.isLive(System.currentTimeMillis()) }
    }

    private fun isDue(now: Long): Boolean = refreshedAt.let { it == null || now - it >= TTL_MILLIS }

    @Synchronized
    private fun refresh(now: Long) {
        if (!isDue(now)) {
            return
        }
        refreshedAt = now
        val prefs = preferences
        if (!prefs.file.canRead()) {
            if (!reportedUnreadable) {
                reportedUnreadable = true
                Log.w(TAG, "Hook state is not readable at ${prefs.file}; is the module enabled in LSPosed?")
            }
            cached = HookState.INACTIVE
            return
        }
        if (prefs.hasFileChanged()) {
            prefs.reload()
        }
        cached = HookState.decode(prefs.getString(HookState.KEY_STATE, null))
    }
}
