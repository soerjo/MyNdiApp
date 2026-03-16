package com.soerjo.myndicam.core.manager

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.usb.USBMonitor
import com.soerjo.myndicam.data.camera.UsbCameraController

class UsbCameraManager(
    private val context: Context,
    private val onFrameCallback: ((ByteArray, Int, Int, Int) -> Unit)?
) {
    private val TAG = "UsbCameraManager"

    private val controllers = mutableMapOf<Int, UsbCameraController>()
    private var activeController: UsbCameraController? = null
    private var deviceConnectCallback: IDeviceConnectCallBack? = null

    fun createController(
        device: UsbDevice,
        usbControlBlock: USBMonitor.UsbControlBlock
    ): UsbCameraController {
        val controller = UsbCameraController(context, device, usbControlBlock)
        controller.onFrameCallback = onFrameCallback

        controllers[device.deviceId] = controller
        activeController = controller

        Log.d(TAG, "USB camera controller created for device ${device.deviceName}")
        return controller
    }

    fun getController(deviceId: Int): UsbCameraController? {
        return controllers[deviceId]
    }

    fun setActiveController(controller: UsbCameraController?) {
        activeController = controller
    }

    fun getActiveController(): UsbCameraController? {
        return activeController
    }

    fun getControllerForDevice(device: UsbDevice): UsbCameraController? {
        return controllers[device.deviceId]
    }

    fun setDeviceConnectCallback(callback: IDeviceConnectCallBack) {
        deviceConnectCallback = callback
    }

    fun getDeviceConnectCallback(): IDeviceConnectCallBack? {
        return deviceConnectCallback
    }

    fun closeController(deviceId: Int) {
        val controller = controllers[deviceId]
        controller?.closeCamera()
        if (activeController?.getUsbDevice()?.deviceId == deviceId) {
            activeController = null
        }
        controllers.remove(deviceId)
        Log.d(TAG, "USB camera controller closed for device $deviceId")
    }

    fun cleanup() {
        controllers.values.forEach { controller -> controller.cleanup() }
        controllers.clear()
        activeController = null
        deviceConnectCallback = null
        Log.d(TAG, "USB camera manager cleaned up")
    }
}
