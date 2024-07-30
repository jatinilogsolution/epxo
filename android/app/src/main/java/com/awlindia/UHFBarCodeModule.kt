package com.awlindia

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.nlscan.android.scan.ScanManager
import com.nlscan.android.scan.ScanSettings
import com.nlscan.android.uhf.TagInfo
import com.nlscan.android.uhf.UHFCommonParams
import com.nlscan.android.uhf.UHFManager
import com.nlscan.android.uhf.UHFReader

@RequiresApi(Build.VERSION_CODES.O)
class UHFBarcodeModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

    private val scanManager: ScanManager = ScanManager.getInstance()
    private val uhfManager: UHFManager = UHFManager.getInstance()
    private val handler: Handler = Handler(Looper.getMainLooper())

    private val mResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d("BarcodeReceiver", "Action received: $action")
            if (action == "nlscan.action.SCANNER_RESULT") {
                val barcode1 = intent.getStringExtra("SCAN_BARCODE1")
                val barcode2 = intent.getStringExtra("SCAN_BARCODE2")
                val scanStatus = intent.getStringExtra("SCAN_STATE")

                Log.d("BarcodeReceiver", "Barcode1: $barcode1, Barcode2: $barcode2, Status: $scanStatus")

                if (scanStatus == "ok") {
                    val combinedBarcode = barcode1?.let { it1 -> barcode2?.let { it2 -> "$it1 $it2" } } ?: barcode1
                    val eventParams = Arguments.createMap()
                    eventParams.putString("scannedData", combinedBarcode)
                    sendEvent("onBarcodeScanned", eventParams)
                } else {
                    Toast.makeText(context, "Scan failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val privateBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action != "nlscan.intent.action.uhf.ACTION_RESULT") return

            val tagInfos = intent.getParcelableArrayExtra("tag_info")
            tagInfos?.forEach { parcel ->
                val tagInfo = parcel as TagInfo?
                tagInfo?.let {
                    val eventParams = Arguments.createMap()
                    eventParams.putString("scannedData", UHFReader.bytes_Hexstr(it.EpcId))
                    sendEvent("onUHFScanned", eventParams)
                }
            }
        }
    }

    init {
        val intentFilter = IntentFilter("nlscan.action.SCANNER_RESULT")
        reactContext.registerReceiver(mResultReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)

        val uhfIntentFilter = IntentFilter("nlscan.intent.action.uhf.ACTION_RESULT")
        reactContext.registerReceiver(privateBroadcastReceiver, uhfIntentFilter, Context.RECEIVER_NOT_EXPORTED)

        reactContext.addLifecycleEventListener(this)
    }

    override fun getName(): String {
        return "UHFBarcodeModule"
    }

    @ReactMethod
    fun toggleScanMode(isBarcode: Boolean, callback: Callback) {
        if (isBarcode) {
            startScanning()
            stopUHF()
        } else {
            stopScanning()
            startUHF()
        }
        callback.invoke("Toggled to ${if (isBarcode) "Barcode" else "UHF"} mode")
    }

    private fun startScanning() {
        handler.post {
            scanManager.setTriggerEnable(ScanSettings.Global.TRIGGER_MODE_BACK, true)
            handler.postDelayed({
                scanManager.setScanEnable(true)
            }, 500)
            Log.d("Scanning", "Scanning Enabled")
        }
    }

    private fun stopScanning() {
        handler.post {
            Log.d("Scanning", "Scanning disabled")
            scanManager.setTriggerEnable(ScanSettings.Global.TRIGGER_MODE_BACK, false)
            scanManager.setScanEnable(false)
        }
    }

    private fun startUHF() {
        handler.post {
            Log.d("UHF", "startUHF")
            val powerState = uhfManager.powerOn()
            if (powerState == UHFReader.READER_STATE.OK_ERR) {
                uhfManager.setTrigger(UHFCommonParams.TRIGGER_MODE.TRIGGER_MODE_BACK, true)
            }
        }
    }

    private fun stopUHF() {
        handler.post {
            Log.d("UHF", "stopUHF")
            if (uhfManager.isPowerOn) {
                uhfManager.powerOff()
            }
            uhfManager.setTrigger(UHFCommonParams.TRIGGER_MODE.TRIGGER_MODE_BACK, false)
        }
    }

    private fun processScannedData(barcode1: String?, barcode2: String?) {
        val combinedBarcode = barcode1?.let { it1 -> barcode2?.let { it2 -> "$it1 $it2" } } ?: barcode1
        val eventParams = Arguments.createMap()
        eventParams.putString("scannedData", combinedBarcode)
        sendEvent("onBarcodeScanned", eventParams)
    }

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Set up any upstream listeners or background tasks as necessary
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Remove upstream listeners, stop unnecessary background tasks
    }

    override fun onHostResume() {
        // Activity `onResume`
    }

    override fun onHostPause() {
        // Activity `onPause`
    }

    override fun onHostDestroy() {
        reactContext.unregisterReceiver(mResultReceiver)
        reactContext.unregisterReceiver(privateBroadcastReceiver)
    }
}
