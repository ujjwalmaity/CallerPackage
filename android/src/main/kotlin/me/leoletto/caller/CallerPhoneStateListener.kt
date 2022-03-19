package me.leoletto.caller

import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import me.leoletto.caller.CallerPlugin.Companion.eventHandler
import java.time.Duration
import java.time.ZonedDateTime
import java.util.ArrayList

class CallerPhoneStateListener internal constructor() : PhoneStateListener() {
    private var previousState = 0
    private var time: ZonedDateTime? = null

    @RequiresApi(Build.VERSION_CODES.O)
    @Synchronized
    override fun onCallStateChanged(state: Int, incomingNumber: String) {
        // Prepare the arguments to be sent to the callback
        val arguments = ArrayList<Any?>()
        arguments.add(incomingNumber)

        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> {
                if (previousState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    val now = ZonedDateTime.now()
                    val duration = Duration.between(time, now)
                    Log.d(CallerPlugin.PLUGIN_NAME, "Phone State event CALL_ENDED with number $incomingNumber")
                    arguments.add("callEnded")
                    arguments.add(duration.toMillis() / 1000)
                    time = null
                    eventHandler.send("incomingCallEnded", arguments)
                } else if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(CallerPlugin.PLUGIN_NAME, "Phone State event INCOMING_CALL_MISSED $incomingNumber")
                    arguments.add("onMissedCall")
                    arguments.add(null)
                    eventHandler.send("onMissedCall", arguments)
                }
                previousState = TelephonyManager.CALL_STATE_IDLE
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                    Log.d(CallerPlugin.PLUGIN_NAME, "Phone State event INCOMING_CALL_ANSWERED $incomingNumber")
                    arguments.add("onIncomingCallAnswered")
                    arguments.add(null)
                    eventHandler.send("onIncomingCallAnswered", arguments)
                }
                previousState = TelephonyManager.CALL_STATE_OFFHOOK

                // Get the current time to count later the duration of the call
                time = ZonedDateTime.now()
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                if (previousState == TelephonyManager.CALL_STATE_IDLE) {
                    Log.d(CallerPlugin.PLUGIN_NAME, "Phone State event INCOMING_CALL_RECEIVED $incomingNumber")
                    arguments.add("onIncomingCallReceived")
                    arguments.add(null)
                    eventHandler.send("onIncomingCallReceived", arguments)
                }
                previousState = TelephonyManager.CALL_STATE_RINGING
            }
        }
    }
}