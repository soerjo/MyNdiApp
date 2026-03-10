package com.soerjo.myndicam.presentation.fragment

import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.soerjo.ndi.NDIManager
import com.soerjo.ndi.NDISender
import com.soerjo.ndi.model.TallyState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class UsbCameraFragment : CameraFragment() {

    private var ndiSender: NDISender? = null
    private var isStreaming = false
    private var tallyBorder: View? = null
    private var previewDot: View? = null
    private var infoText: TextView? = null
    private var sourceName = "USB Camera"

    private var currentWidth = 0
    private var currentHeight = 0
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(com.soerjo.myndicam.R.layout.fragment_camera, container, false)
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return view?.findViewById(com.soerjo.myndicam.R.id.camera_container)!!
    }

    override fun getGravity(): Int = Gravity.CENTER

    override fun initData() {
        super.initData()
        initializeNDI()
    }

    private fun initializeNDI() {
        try {
            if (!NDIManager.isInitialized()) {
                NDIManager.initialize()
            }
            createNDISender(sourceName)
            Log.d(TAG, "NDI sender created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize NDI: ${e.message}")
        }
    }

    private fun createNDISender(name: String) {
        try {
            ndiSender?.release()
            ndiSender = NDIManager.createSender(name)
            Log.d(TAG, "NDI sender created: $name")

            ndiSender?.let { sender ->
                viewLifecycleOwner.lifecycleScope.launch {
                    sender.tallyState.collect { tallyState ->
                        updateTallyUI(tallyState)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create NDI sender: ${e.message}")
        }
    }

    private fun showSettingsDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter NDI source name"
            setText(sourceName)
            setPadding(48, 32, 48, 32)
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("NDI Source Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    sourceName = newName
                    createNDISender(sourceName)
                    updateInfoText()
                    Toast.makeText(requireContext(), "Source name updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTallyUI(tallyState: TallyState) {
        Log.d(TAG, "Tally state: onPreview=${tallyState.isOnPreview}, onProgram=${tallyState.isOnProgram}")

        activity?.runOnUiThread {
            if (tallyState.isOnProgram) {
                tallyBorder?.visibility = View.VISIBLE
                previewDot?.visibility = View.GONE
            } else if (tallyState.isOnPreview) {
                tallyBorder?.visibility = View.GONE
                previewDot?.visibility = View.VISIBLE
                startBlinkAnimation(previewDot!!, Color.YELLOW)
            } else {
                tallyBorder?.visibility = View.GONE
                previewDot?.visibility = View.GONE
                tallyBorder?.clearAnimation()
                previewDot?.clearAnimation()
            }
        }
    }

    private fun updateInfoText() {
        if (currentWidth <= 0 || currentHeight <= 0) return

        val aspectRatio = if (currentHeight > 0) {
            String.format("%.2f", currentWidth.toFloat() / currentHeight.toFloat())
        } else "0"

        val info = "$sourceName\n${currentWidth}x${currentHeight} | ${currentFps}fps | $aspectRatio"

        activity?.runOnUiThread {
            infoText?.text = info
        }
    }

    private fun createControlButton(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(32, 16, 32, 16)
            setBackgroundColor(Color.parseColor("#444444"))
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
                marginEnd = 8
            }
            layoutParams = params
        }
    }

    private fun createStreamButton(): TextView {
        return TextView(requireContext()).apply {
            text = "START soerjo"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(48, 16, 48, 16)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8
                marginEnd = 8
            }
            layoutParams = params
            
            setOnClickListener {
                isStreaming = !isStreaming
                if (isStreaming) {
                    text = "STOP"
                    setBackgroundColor(Color.RED)
                } else {
                    text = "START"
                    setBackgroundColor(Color.parseColor("#4CAF50"))
                }
                Log.d(TAG, "Streaming: $isStreaming")
            }
        }
    }

    private fun startBlinkAnimation(view: View?, color: Int) {
        view?.let {
            it.clearAnimation()
            (it.background as? ShapeDrawable)?.paint?.color = color
            
            val blink = object : android.view.animation.Animation() {
                override fun applyTransformation(interpolatedTime: Float, t: android.view.animation.Transformation) {
                    it.alpha = if (interpolatedTime < 0.5f) 1f else 0.3f
                }
            }.apply {
                duration = 500
                repeatMode = android.view.animation.Animation.REVERSE
                repeatCount = android.view.animation.Animation.INFINITE
            }
            it.startAnimation(blink)
        }
    }

    private fun showCameraSelectionDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Camera")
            .setMessage("Internal cameras coming soon.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun stopCurrentCamera() {
        closeCamera()
    }

    private fun startInternalCamera(cameraType: com.soerjo.myndicam.domain.model.CameraType) {
        Toast.makeText(requireContext(), "Internal cameras coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun getDefaultCamera(): android.hardware.usb.UsbDevice? {
        return null
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                val request = self.getCameraRequest()
                Log.d(TAG, "USB Camera opened: ${request?.previewWidth}x${request?.previewHeight}")

                self.addPreviewDataCallBack(object : IPreviewDataCallBack {
                    override fun onPreviewData(
                        data: ByteArray?,
                        width: Int,
                        height: Int,
                        format: IPreviewDataCallBack.DataFormat
                    ) {
                        if (width != currentWidth || height != currentHeight) {
                            currentWidth = width
                            currentHeight = height
                            updateInfoText()
                        }

                        frameCount++
                        val now = System.currentTimeMillis()
                        if (now - lastFpsTime >= 1000) {
                            currentFps = frameCount
                            frameCount = 0
                            lastFpsTime = now
                            updateInfoText()
                        }

                        if (data == null || !isStreaming) return

                        try {
                            val uyvyData = convertToUyvy(data, width, height, format)
                            ndiSender?.sendFrame(uyvyData, width, height, width * 2)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending frame: ${e.message}")
                        }
                    }
                })
            }
            ICameraStateCallBack.State.CLOSED -> {
                Log.d(TAG, "Camera closed")
            }
            ICameraStateCallBack.State.ERROR -> {
                Log.e(TAG, "Camera error: $msg")
            }
            else -> {
                Log.d(TAG, "Camera state: $code")
            }
        }
    }

    private fun convertToUyvy(data: ByteArray, width: Int, height: Int, format: IPreviewDataCallBack.DataFormat): ByteArray {
        return when (format) {
            IPreviewDataCallBack.DataFormat.NV21 -> convertNv21ToUyvy(data, width, height)
            IPreviewDataCallBack.DataFormat.RGBA -> convertRgbaToUyvy(data, width, height)
        }
    }

    private fun convertNv21ToUyvy(nv21Data: ByteArray, width: Int, height: Int): ByteArray {
        val uyvyData = ByteArray(width * height * 2)

        val ySize = width * height
        val vuOffset = ySize

        for (y in 0 until height step 2) {
            for (x in 0 until width step 2) {
                val yIndex00 = y * width + x
                val yIndex01 = y * width + (x + 1)
                val yIndex10 = (y + 1) * width + x
                val yIndex11 = (y + 1) * width + (x + 1)

                val y00 = nv21Data[yIndex00].toInt() and 0xFF
                val y01 = nv21Data[yIndex01].toInt() and 0xFF
                val y10 = nv21Data[yIndex10].toInt() and 0xFF
                val y11 = nv21Data[yIndex11].toInt() and 0xFF

                val vuIndex = vuOffset + (y / 2) * width + x
                val v = nv21Data[vuIndex].toInt() and 0xFF
                val u = nv21Data[vuIndex + 1].toInt() and 0xFF

                uyvyData[y * width * 2 + x * 2] = u.toByte()
                uyvyData[y * width * 2 + x * 2 + 1] = y00.toByte()
                uyvyData[y * width * 2 + (x + 1) * 2] = v.toByte()
                uyvyData[(y + 1) * width * 2 + x * 2] = u.toByte()
                uyvyData[(y + 1) * width * 2 + (x + 1) * 2] = y01.toByte()
                uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y10.toByte()
                uyvyData[(y + 1) * width * 2 + x * 2 + 1] = y11.toByte()
            }
        }

        return uyvyData
    }

    private fun convertRgbaToUyvy(rgbaData: ByteArray, width: Int, height: Int): ByteArray {
        val uyvyData = ByteArray(width * height * 2)

        for (y in 0 until height) {
            for (x in 0 until width step 2) {
                val idx0 = (y * width + x) * 4
                val idx1 = (y * width + x + 1) * 4

                val r0 = rgbaData[idx0].toInt() and 0xFF
                val g0 = rgbaData[idx0 + 1].toInt() and 0xFF
                val b0 = rgbaData[idx0 + 2].toInt() and 0xFF

                val r1 = rgbaData[idx1].toInt() and 0xFF
                val g1 = rgbaData[idx1 + 1].toInt() and 0xFF
                val b1 = rgbaData[idx1 + 2].toInt() and 0xFF

                val y0 = ((66 * r0 + 129 * g0 + 25 * b0 + 128) shr 8) + 16
                val y1 = ((66 * r1 + 129 * g1 + 25 * b1 + 128) shr 8) + 16
                val u = ((-38 * r0 - 74 * g0 + 112 * b0 + 128) shr 8) + 128
                val v = ((112 * r0 - 94 * g0 - 18 * b1 + 128) shr 8) + 128

                val outIdx = y * width * 2 + x * 2
                uyvyData[outIdx] = u.toByte()
                uyvyData[outIdx + 1] = y0.toByte()
                uyvyData[outIdx + 2] = v.toByte()
                uyvyData[outIdx + 3] = y1.toByte()
            }
        }

        return uyvyData
    }

    override fun initView() {
        super.initView()

        view?.let { rootView ->
            val container = rootView.findViewById<ViewGroup>(com.soerjo.myndicam.R.id.camera_container)

            // Green border for LIVE (floating)
            tallyBorder = View(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                background = ShapeDrawable(RectShape()).apply {
                    paint.color = Color.GREEN
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 16f
                }
                setPadding(16, 16, 16, 16)
                visibility = View.GONE
            }
//            container?.addView(tallyBorder)

            // Info card with blinking dot (top right)
            val infoCard = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 12, 16, 12)
                setBackgroundColor(Color.parseColor("#CC000000"))
                gravity = Gravity.CENTER_VERTICAL
            }

            // Blinking dot (yellow for preview, green for live)
            previewDot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                    marginEnd = 12
                }
                background = ShapeDrawable(android.graphics.drawable.shapes.OvalShape()).apply {
                    paint.color = Color.YELLOW
                }
                visibility = View.GONE
            }
            infoCard.addView(previewDot)

            // Add info card to container (top right)
//            container?.addView(infoCard)

            // Info text
            infoText = TextView(requireContext()).apply {
                text = "Waiting for camera..."
                setTextColor(Color.WHITE)
                textSize = 12f
                setPadding(12, 8, 12, 8)
                setBackgroundColor(Color.parseColor("#80000000"))
            }
            val infoParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = 16
                marginEnd = 16
            }
//            container?.addView(infoText, infoParams)

            // Bottom control bar
            val controlBar = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#CC000000"))
                setPadding(24, 16, 24, 16)
            }

            // Camera button
            val cameraBtn = createControlButton("Camera").apply {
                setOnClickListener { showCameraSelectionDialog() }
            }
//            controlBar.addView(cameraBtn)

            // Streaming toggle button (larger, red when streaming)
            val streamBtn = createStreamButton()
//            controlBar.addView(streamBtn)

            // Settings button
            val settingsBtn = createControlButton("Settings")
            settingsBtn.setOnClickListener { showSettingsDialog() }
//            controlBar.addView(settingsBtn)

            // Add control bar at bottom
            val controlParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            container?.addView(controlBar, controlParams)
        }
    }

    companion object {
        private const val TAG = "UsbCameraFragment"
    }
}
