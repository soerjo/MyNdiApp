package com.soerjo.myndicam.data.datasource

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbConstants
import android.util.Log
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.utils.CameraUtils
import com.jiangdg.usb.DeviceFilter
import com.jiangdg.usb.USBMonitor
import com.soerjo.myndicam.domain.model.CameraInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for USB camera operations using libausbc MultiCameraClient (The Connection Layer)
 */
@Singleton
class UsbCameraDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "UsbCameraDataSource"

    private var cameraClient: MultiCameraClient? = null
    
    private val _detectedUsbCameras = MutableStateFlow<Map<Int, CameraInfo.Usb>>(emptyMap())
    val detectedUsbCameras = _detectedUsbCameras.asStateFlow()

    private var externalListener: IDeviceConnectCallBack? = null

    /**
     * Initialize MultiCameraClient (The Connection Layer)
     */
    fun initialize() {
        if (cameraClient == null) {
            cameraClient = MultiCameraClient(context, object : IDeviceConnectCallBack {
                override fun onAttachDev(device: UsbDevice?) {
                    device?.let {
                        // Debug: Log ALL USB devices to diagnose capture card issues
                        Log.d(TAG, "=== USB DEVICE ATTACHED ===")
                        Log.d(TAG, "  Name: ${it.deviceName}")
                        Log.d(TAG, "  VendorId: ${String.format("0x%04X", it.vendorId)}")
                        Log.d(TAG, "  ProductId: ${String.format("0x%04X", it.productId)}")
                        Log.d(TAG, "  DeviceClass: ${it.deviceClass} (${getDeviceClassName(it.deviceClass)})")
                        Log.d(TAG, "  InterfaceCount: ${it.interfaceCount}")
                        
                        // Log all interfaces
                        for (i in 0 until it.interfaceCount) {
                            val intf = it.getInterface(i)
                            Log.d(TAG, "    Interface[$i]: class=${intf.interfaceClass} (${getInterfaceClassName(intf.interfaceClass)}), protocol=${intf.interfaceProtocol}")
                        }
                        
                        // Check if it would be detected as USB camera
                        val isVideoDevice = CameraUtils.isUsbCamera(it)
                        val isFiltered = CameraUtils.isFilterDevice(context, it)
                        Log.d(TAG, "  Is USB Camera (UVC): $isVideoDevice")
                        Log.d(TAG, "  Is Filtered Device: $isFiltered")
                        Log.d(TAG, "============================")

                        // Only process if it's a recognized USB camera
                        if (!isVideoDevice && !isFiltered) {
                            Log.w(TAG, "Device not recognized as USB camera or filtered device, skipping...")
                            return@let
                        }

                        val cameraInfo = CameraInfo.Usb(
                            deviceId = it.deviceId,
                            name = it.deviceName ?: "USB Camera ${it.vendorId}:${it.productId}",
                            vendorId = it.vendorId,
                            productId = it.productId
                        )
                        val current = _detectedUsbCameras.value.toMutableMap()
                        current[it.deviceId] = cameraInfo
                        _detectedUsbCameras.value = current
                        Log.d(TAG, "USB camera added to list: ${cameraInfo.name}")
                        
                        externalListener?.onAttachDev(it)
                    }
                }

                override fun onDetachDec(device: UsbDevice?) {
                    device?.let {
                        val current = _detectedUsbCameras.value.toMutableMap()
                        current.remove(it.deviceId)
                        _detectedUsbCameras.value = current
                        Log.d(TAG, "USB camera detached: ${it.deviceName}")
                        externalListener?.onDetachDec(it)
                    }
                }

                override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                    Log.d(TAG, "USB camera connected: ${device?.deviceName}")
                    externalListener?.onConnectDev(device, ctrlBlock)
                }

                override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                    Log.d(TAG, "USB camera disconnected: ${device?.deviceName}")
                    externalListener?.onDisConnectDec(device, ctrlBlock)
                }

                override fun onCancelDev(device: UsbDevice?) {
                    Log.d(TAG, "USB camera cancelled: ${device?.deviceName}")
                    externalListener?.onCancelDev(device)
                }
            })
            // Add custom device filter for capture cards
            // MACROSILICON capture cards: Vendor 21325, Product 8457 (MS2109/MS2130)
            // Constructor: DeviceFilter(vendorId, productId, subclass, protocol, interfaceClass, manufacturer, product, serial)
            val macrosiliconFilter = DeviceFilter(
                21325, 8457, // vendorId, productId
                -1, -1,       // subclass, protocol (match any)
                -1,           // interfaceClass (match any)
                "MACROSILICON", "USB Video", null // manufacturer, product, serial
            )
            cameraClient?.addDeviceFilters(listOf(macrosiliconFilter))
            cameraClient?.register()
            Log.d(TAG, "MultiCameraClient registered with MACROSILICON filter")
        }
    }

    /**
     * Set an external listener for device connection events
     */
    fun setDeviceConnectCallBack(callback: IDeviceConnectCallBack) {
        this.externalListener = callback
    }

    /**
     * Get list of detected USB cameras
     */
    fun getDetectedCameras(): List<CameraInfo.Usb> {
        return _detectedUsbCameras.value.values.toList()
    }

    /**
     * Request permission for a USB device
     */
    fun requestPermission(device: UsbDevice) {
        if (cameraClient?.hasPermission(device) == false) {
            cameraClient?.requestPermission(device)
        }
    }

    /**
     * Check if device has USB permission
     */
    fun hasPermission(device: UsbDevice): Boolean {
        return cameraClient?.hasPermission(device) == true
    }

    /**
     * Get USB device list from the client
     */
    fun getDeviceList(): List<UsbDevice> {
        return cameraClient?.getDeviceList() ?: emptyList()
    }
    
    /**
     * Get context
     */
    fun getContext(): Context = context

    /**
     * Cleanup USB monitoring
     */
    fun cleanup() {
        cameraClient?.unRegister()
        cameraClient?.destroy()
        cameraClient = null
        _detectedUsbCameras.value = emptyMap()
        Log.d(TAG, "MultiCameraClient cleaned up")
    }

    /**
     * Debug: Get all USB devices currently connected (before filtering)
     */
    fun getAllUsbDevices(): List<UsbDevice> {
        return cameraClient?.getDeviceList() ?: emptyList()
    }

    private fun getDeviceClassName(classCode: Int): String {
        return when (classCode) {
            UsbConstants.USB_CLASS_PER_INTERFACE -> "PER_INTERFACE"
            UsbConstants.USB_CLASS_AUDIO -> "AUDIO"
            UsbConstants.USB_CLASS_COMM -> "COMM"
            UsbConstants.USB_CLASS_HID -> "HID"
            0x05 -> "PHYSICAL" // USB_CLASS_PHYSICAL
            0x06 -> "IMAGE"    // USB_CLASS_IMAGE
            UsbConstants.USB_CLASS_PRINTER -> "PRINTER"
            UsbConstants.USB_CLASS_MASS_STORAGE -> "MASS_STORAGE"
            UsbConstants.USB_CLASS_HUB -> "HUB"
            UsbConstants.USB_CLASS_CDC_DATA -> "CDC_DATA"
            0x10 -> "SMART_CARD" // USB_CLASS_SMART_CARD
            0x11 -> "CONTENT_SEC"
            UsbConstants.USB_CLASS_VIDEO -> "VIDEO"
            UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> "WIRELESS_CONTROLLER"
            UsbConstants.USB_CLASS_MISC -> "MISC"
            0xDC -> "DIAGNOSTIC" // USB_CLASS_DIAGNOSTIC
            UsbConstants.USB_CLASS_VENDOR_SPEC -> "VENDOR_SPEC"
            else -> "UNKNOWN_$classCode"
        }
    }

    private fun getInterfaceClassName(classCode: Int): String {
        return getDeviceClassName(classCode)
    }
}
