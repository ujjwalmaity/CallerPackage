package me.leoletto.caller

import android.Manifest

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.content.pm.PackageManager

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

import androidx.core.app.ActivityCompat

import androidx.core.content.ContextCompat

import android.content.ComponentName

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding

import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.PluginRegistry


/** CallerPlugin */
class CallerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
    companion object {
        const val PLUGIN_NAME = "me.leoletto.caller"
        val eventHandler = EventStreamHandler()
    }

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var method: MethodChannel
    private lateinit var event: EventChannel

    private var currentActivity: Activity? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPluginBinding) {
        method = MethodChannel(flutterPluginBinding.binaryMessenger, PLUGIN_NAME)
        event = EventChannel(flutterPluginBinding.binaryMessenger, "MyEvent")
        method.setMethodCallHandler(this)
        event.setStreamHandler(eventHandler)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> {
                if (!doCheckPermission()) {
                    result.error("MISSING_PERMISSION", null, null)
                    return
                }

                val context = currentActivity!!.applicationContext
                val receiver = ComponentName(context, CallerPhoneServiceReceiver::class.java)
                context.packageManager.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                )
                Log.d(PLUGIN_NAME, "Service initialized")
                Log.d(PLUGIN_NAME, receiver.toString())
                result.success(true)

            }
            "stopCaller" -> {
                val context: Context = currentActivity!!.applicationContext
                val receiver = ComponentName(context, CallerPhoneServiceReceiver::class.java)
                val packageManager: PackageManager = context.packageManager
                packageManager.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                )
                result.success(true)

            }
            "requestPermissions" -> {
                Log.d(PLUGIN_NAME, "Requesting permission")
                requestPermissions()

            }
            "checkPermissions" -> {
                val check = doCheckPermission()
                Log.d(PLUGIN_NAME, "Permission checked: $check")
                result.success(check)

            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        method.setMethodCallHandler(null)
    }

    private fun doCheckPermission(): Boolean {
        if (currentActivity != null && currentActivity!!.applicationContext != null) {
            val permPhoneState = ContextCompat.checkSelfPermission(currentActivity!!, Manifest.permission.READ_PHONE_STATE)
            val permReadCallLog = ContextCompat.checkSelfPermission(currentActivity!!, Manifest.permission.READ_CALL_LOG)
            val grantedCode = PackageManager.PERMISSION_GRANTED
            return permPhoneState == grantedCode && permReadCallLog == grantedCode
        }
        return false
    }

    private fun requestPermissions() {
        if (currentActivity != null && currentActivity!!.applicationContext != null) {
            val permPhoneState = ContextCompat.checkSelfPermission(currentActivity!!, Manifest.permission.READ_PHONE_STATE)
            val permReadCallLog = ContextCompat.checkSelfPermission(currentActivity!!, Manifest.permission.READ_CALL_LOG)
            val grantedCode = PackageManager.PERMISSION_GRANTED
            if (permPhoneState != grantedCode || permReadCallLog != grantedCode) {
                val permissions = arrayOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CALL_LOG
                )
                ActivityCompat.requestPermissions(currentActivity!!, permissions, 999)
            }
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        currentActivity = binding.activity
        binding.addRequestPermissionsResultListener(this)
        // requestPermissions()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        currentActivity = binding.activity
        binding.addRequestPermissionsResultListener(this)
        // requestPermissions()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        currentActivity = null
    }

    override fun onDetachedFromActivity() {
        currentActivity = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>?, grantResults: IntArray?): Boolean {
        return when (requestCode) {
            999 -> grantResults != null && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            else -> false
        }
    }
}

class EventStreamHandler : EventChannel.StreamHandler {

    private var eventSink: EventChannel.EventSink? = null

    override fun onListen(arguments: Any?, sink: EventChannel.EventSink) {
        eventSink = sink
    }

    fun send(event: String, arguments: List<Any?>) {
        val data = mapOf(
                "event" to event,
                "arguments" to arguments
        )
        Handler(Looper.getMainLooper()).post {
            eventSink?.success(data)
        }
    }

    override fun onCancel(p0: Any?) {
        eventSink = null
    }
}