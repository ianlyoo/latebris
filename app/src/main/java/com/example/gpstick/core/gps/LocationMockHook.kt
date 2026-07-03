package com.example.gpstick.core.gps

import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.example.gpstick.data.preset.GpsPreset
import com.example.gpstick.service.SimulationStateStore
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.WeakHashMap
import java.util.concurrent.Executor
import java.util.function.Consumer

class LocationMockHook : IXposedHookLoadPackage {
    private val listenerWrappers = WeakHashMap<LocationListener, LocationListener>()

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        installLocationGetterHooks(lpparam)
        installLocationManagerHooks(lpparam)
        installFusedLocationHooks(lpparam)
    }

    private fun installLocationGetterHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookLocationGetter(lpparam, "getLatitude") { preset, _ -> preset.latitude }
        hookLocationGetter(lpparam, "getLongitude") { preset, _ -> preset.longitude }
        hookLocationGetter(lpparam, "getAltitude") { preset, _ -> preset.altitude }
        hookLocationGetter(lpparam, "getAccuracy") { preset, _ -> preset.accuracyMeters }

        hookLocationFlag(lpparam, "isFromMockProvider")
        hookLocationFlag(lpparam, "isMock")
    }

    private fun hookLocationGetter(
        lpparam: XC_LoadPackage.LoadPackageParam,
        methodName: String,
        resultProvider: (GpsPreset, Location) -> Any,
    ) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                Location::class.java,
                methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val location = param.thisObject as? Location ?: return
                        val preset = activeGpsPreset(location) ?: return
                        sanitizeLocation(location, preset)
                        param.result = resultProvider(preset, location)
                    }
                },
            )
            Log.i(TAG, "Installed $methodName hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install $methodName hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun hookLocationFlag(
        lpparam: XC_LoadPackage.LoadPackageParam,
        methodName: String,
    ) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                Location::class.java,
                methodName,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val location = param.thisObject as? Location ?: return
                        val preset = activeGpsPreset(location) ?: return
                        sanitizeLocation(location, preset)
                        param.result = false
                    }
                },
            )
            Log.i(TAG, "Installed $methodName hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install $methodName hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun installLocationManagerHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookLastKnownLocation(lpparam)
        hookCurrentLocation(lpparam)
        hookRequestLocationUpdates(lpparam)
        hookRemoveUpdates(lpparam)
        hookRequestFlush(lpparam)
    }

    private fun hookLastKnownLocation(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java,
                "getLastKnownLocation",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val providerName = param.args.firstOrNull() as? String
                        val location = param.result as? Location ?: return
                        val preset = activeGpsPreset(location, providerName) ?: return
                        param.result = createSanitizedLocation(location, preset, providerName)
                    }
                },
            )
            Log.i(TAG, "Installed getLastKnownLocation hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install getLastKnownLocation hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun hookCurrentLocation(lpparam: XC_LoadPackage.LoadPackageParam) {
        val consumerClass = Consumer::class.java
        runCatching {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java,
                "getCurrentLocation",
                String::class.java,
                CancellationSignal::class.java,
                Executor::class.java,
                consumerClass,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val providerName = param.args.firstOrNull() as? String
                        @Suppress("UNCHECKED_CAST")
                        val consumer = param.args[3] as? Consumer<Location> ?: return
                        param.args[3] = wrapConsumer(consumer, providerName)
                    }
                },
            )
            Log.i(TAG, "Installed getCurrentLocation(legacy) hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install getCurrentLocation(legacy) hook for ${lpparam.packageName}", throwable)
        }

        runCatching {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java,
                "getCurrentLocation",
                String::class.java,
                LocationRequest::class.java,
                CancellationSignal::class.java,
                Executor::class.java,
                consumerClass,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val providerName = param.args.firstOrNull() as? String
                        @Suppress("UNCHECKED_CAST")
                        val consumer = param.args[4] as? Consumer<Location> ?: return
                        param.args[4] = wrapConsumer(consumer, providerName)
                    }
                },
            )
            Log.i(TAG, "Installed getCurrentLocation(request) hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install getCurrentLocation(request) hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun hookRequestLocationUpdates(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookLocationUpdatesWithListener(
            lpparam,
            String::class.java,
            Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            LocationListener::class.java,
        )
        hookLocationUpdatesWithListener(
            lpparam,
            String::class.java,
            Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            LocationListener::class.java,
            Looper::class.java,
        )
        hookLocationUpdatesWithListener(
            lpparam,
            String::class.java,
            Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Executor::class.java,
            LocationListener::class.java,
        )
        hookLocationUpdatesWithListener(
            lpparam,
            Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Criteria::class.java,
            LocationListener::class.java,
            Looper::class.java,
        )
        hookLocationUpdatesWithListener(
            lpparam,
            Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Criteria::class.java,
            Executor::class.java,
            LocationListener::class.java,
        )
        hookLocationUpdatesWithListener(
            lpparam,
            String::class.java,
            LocationRequest::class.java,
            Executor::class.java,
            LocationListener::class.java,
        )
    }

    private fun hookLocationUpdatesWithListener(
        lpparam: XC_LoadPackage.LoadPackageParam,
        vararg parameterTypes: Any?,
    ) {
        runCatching {
            val signature = parameterTypes.toMutableList()
            signature += object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val listenerIndex = param.args.indexOfFirst { it is LocationListener }
                    if (listenerIndex < 0) {
                        return
                    }
                    val original = param.args[listenerIndex] as? LocationListener ?: return
                    param.args[listenerIndex] = wrapLocationListener(original)
                }
            }
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java,
                "requestLocationUpdates",
                *signature.toTypedArray(),
            )
            Log.i(TAG, "Installed requestLocationUpdates hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install requestLocationUpdates hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun hookRemoveUpdates(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java,
                "removeUpdates",
                LocationListener::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val original = param.args.firstOrNull() as? LocationListener ?: return
                        listenerWrappers[original]?.let { wrapped ->
                            param.args[0] = wrapped
                        }
                    }
                },
            )
            Log.i(TAG, "Installed removeUpdates hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install removeUpdates hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun hookRequestFlush(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                LocationManager::class.java,
                "requestFlush",
                String::class.java,
                LocationListener::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val original = param.args.getOrNull(1) as? LocationListener ?: return
                        param.args[1] = wrapLocationListener(original)
                    }
                },
            )
            Log.i(TAG, "Installed requestFlush hook for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install requestFlush hook for ${lpparam.packageName}", throwable)
        }
    }

    private fun installFusedLocationHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            val locationResultClass = Class.forName(
                "com.google.android.gms.location.LocationResult",
                false,
                lpparam.classLoader,
            )
            XposedHelpers.findAndHookMethod(
                locationResultClass,
                "getLastLocation",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val location = param.result as? Location ?: return
                        val preset = activeGpsPreset(location) ?: return
                        param.result = createSanitizedLocation(location, preset)
                    }
                },
            )
            XposedHelpers.findAndHookMethod(
                locationResultClass,
                "getLocations",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        @Suppress("UNCHECKED_CAST")
                        val locations = param.result as? List<Location> ?: return
                        val updated = locations.map { location ->
                            val preset = activeGpsPreset(location) ?: return@map location
                            createSanitizedLocation(location, preset)
                        }
                        param.result = updated
                    }
                },
            )
            Log.i(TAG, "Installed fused location result hooks for ${lpparam.packageName}")
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to install fused location result hooks for ${lpparam.packageName}", throwable)
        }
    }

    private fun wrapConsumer(
        consumer: Consumer<Location>,
        providerName: String?,
    ): Consumer<Location> {
        return Consumer { location ->
            val preset = activeGpsPreset(location, providerName)
            val sanitized = if (preset != null) {
                createSanitizedLocation(location, preset, providerName)
            } else {
                location
            }
            consumer.accept(sanitized)
        }
    }

    private fun wrapLocationListener(listener: LocationListener): LocationListener {
        return listenerWrappers.getOrPut(listener) {
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val preset = activeGpsPreset(location)
                    val sanitized = if (preset != null) {
                        createSanitizedLocation(location, preset)
                    } else {
                        location
                    }
                    listener.onLocationChanged(sanitized)
                }

                override fun onLocationChanged(locations: MutableList<Location>) {
                    val sanitized = locations.map { location ->
                        val preset = activeGpsPreset(location)
                        if (preset != null) {
                            createSanitizedLocation(location, preset)
                        } else {
                            location
                        }
                    }
                    listener.onLocationChanged(sanitized)
                }

                override fun onFlushComplete(requestCode: Int) {
                    listener.onFlushComplete(requestCode)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    listener.onStatusChanged(provider, status, extras)
                }

                override fun onProviderEnabled(provider: String) {
                    listener.onProviderEnabled(provider)
                }

                override fun onProviderDisabled(provider: String) {
                    listener.onProviderDisabled(provider)
                }
            }
        }
    }

    private fun createSanitizedLocation(
        source: Location,
        preset: GpsPreset,
        providerHint: String? = null,
    ): Location {
        return Location(source).apply {
            latitude = preset.latitude
            longitude = preset.longitude
            altitude = preset.altitude
            accuracy = preset.accuracyMeters
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            providerHint?.takeIf { it.isNotBlank() }?.let { provider = it }
            sanitizeMockFlags(this)
        }
    }

    private fun sanitizeLocation(location: Location, preset: GpsPreset) {
        location.latitude = preset.latitude
        location.longitude = preset.longitude
        location.altitude = preset.altitude
        location.accuracy = preset.accuracyMeters
        sanitizeMockFlags(location)
    }

    private fun sanitizeMockFlags(location: Location) {
        runCatching {
            val hiddenMethod = Location::class.java.getDeclaredMethod(
                "setIsFromMockProvider",
                Boolean::class.javaPrimitiveType,
            )
            hiddenMethod.isAccessible = true
            hiddenMethod.invoke(location, false)
        }
        runCatching {
            location.setMock(false)
        }
        runCatching {
            val extras = location.extras?.let(::Bundle) ?: Bundle()
            extras.remove("mockLocation")
            extras.remove("is_mock")
            location.extras = extras
        }
    }

    private fun activeGpsPreset(
        location: Location?,
        providerHint: String? = null,
    ): GpsPreset? {
        if (!shouldOverrideProvider(providerHint ?: location?.provider)) {
            return null
        }

        val context = currentApplicationContext() ?: return null
        val snapshot = SimulationStateStore.readSnapshotFromProvider(context)
        val state = snapshot.controlState
        if (!state.isRunning || !state.activeFeaturesEnabled || !state.activeGpsMockEnabled) {
            return null
        }
        return snapshot.activePreset?.gps
    }

    private fun shouldOverrideProvider(provider: String?): Boolean {
        val normalized = provider?.lowercase() ?: return true
        return normalized == LocationManager.GPS_PROVIDER ||
            normalized == LocationManager.NETWORK_PROVIDER ||
            normalized == LocationManager.PASSIVE_PROVIDER ||
            normalized.contains("fused")
    }

    private fun currentApplicationContext(): android.content.Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val method = activityThread.getDeclaredMethod("currentApplication")
        method.isAccessible = true
        method.invoke(null) as? android.content.Context
    }.getOrNull()

    private companion object {
        const val TAG = "GpStickLocationHook"
    }
}
