package com.example.stroll.presentation.fragment

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.stroll.MainActivity
import com.example.stroll.R
import com.example.stroll.databinding.FragmentARBinding
import com.example.stroll.other.ARUtils
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ViewRenderable
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException


class ARFragment : Fragment() {

    private val TAG = "ARCoreCamera"
    private val MIN_OPENGL_VERSION = 3.0
    private var installRequested = false
    private var hasFinishedLoading = false

    private var loadingMessageSnackbar: Snackbar? = null

    private var _binding: FragmentARBinding? = null
    private val binding get() = _binding!!

    private var arSceneView: ArSceneView? = null

    private var exampleLayoutRenderable: ViewRenderable? = null
    private var locationScene: LocationScene? = null

    private val args: ARFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentARBinding.inflate(inflater, container, false)
        arSceneView = binding.arSceneView

        checkIsSupportedDeviceOrFinish((activity as MainActivity))

        val exampleLayout: CompletableFuture<ViewRenderable> = ViewRenderable.builder()
            .setView(requireContext(), R.layout.example_layout)
            .build()

        val exampleLayout2: CompletableFuture<ViewRenderable> = ViewRenderable.builder()
            .setView(requireContext(), R.layout.example_layout)
            .build()

        CompletableFuture.allOf(
            exampleLayout,
            exampleLayout2
        )
            .handle<Any?> { notUsed: Void?, throwable: Throwable? ->
                if (throwable != null) {
                    ARUtils().displayError(requireContext(), "Unable to load renderables", throwable)
                    return@handle null
                }
                try {
                    exampleLayoutRenderable = exampleLayout.get()
                    hasFinishedLoading = true
                } catch (ex: InterruptedException) {
                    ARUtils().displayError(requireContext(), "Unable to load renderables", ex)
                } catch (ex: ExecutionException) {
                    ARUtils().displayError(requireContext(), "Unable to load renderables", ex)
                }
                null
            }

        arSceneView!!.scene.addOnUpdateListener { frameTime: FrameTime? ->
            if (!hasFinishedLoading) {
                return@addOnUpdateListener
            }
            if (locationScene == null) {
                locationScene = LocationScene(requireContext(), requireActivity(), arSceneView)

                val layoutLocationMarker =
                    LocationMarker(
                        args.latLng.long, args.latLng.lat, getExampleView(
                            exampleLayoutRenderable!!
                        )
                    )

                layoutLocationMarker.renderEvent =
                    LocationNodeRender { node ->
                        val eView = exampleLayoutRenderable!!.view
                        val distanceTextView = eView.findViewById<TextView>(R.id.textView2)
                        distanceTextView.text = node.distance.toString() + "M"
                        val nameView = eView.findViewById<TextView>(R.id.textView1)
                        nameView.text = args.poiName
                        val categoryView = eView.findViewById<TextView>(R.id.category)
                        categoryView.text = args.category

                    }

                locationScene!!.mLocationMarkers.add(layoutLocationMarker)
            }
            val frame = arSceneView!!.arFrame ?: return@addOnUpdateListener
            if (frame.camera.trackingState != TrackingState.TRACKING) {
                return@addOnUpdateListener
            }
            locationScene?.processFrame(frame)
            if (loadingMessageSnackbar != null) {
                for (plane in frame.getUpdatedTrackables(
                    Plane::class.java
                )) {
                }
            }
        }

        ARLocationPermissionHelper.requestPermission(requireActivity())

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (locationScene != null) {
            locationScene!!.resume()
        }
        if (arSceneView!!.session == null) {
            try {
                val session: Session? = ARUtils().createArSession((activity as MainActivity), installRequested)
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission((activity as MainActivity))
                    return
                }
                else {
                    arSceneView!!.setupSession(session)
                }
            } catch (e: UnavailableException) {
                ARUtils().handleSessionException((activity as MainActivity), e)
            }
        }

        try {
            arSceneView!!.resume()
        }
        catch (ex: CameraNotAvailableException) {
            ARUtils().displayError(requireContext(), "Unable to get camera", ex)
            return
        }
    }

    override fun onPause() {
        super.onPause()

        if (locationScene != null) {
            locationScene!!.pause()
        }

        arSceneView!!.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSceneView!!.destroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun getExampleView(renderable: ViewRenderable): Node? {
        val base = Node()
        base.renderable = renderable
        val c: Context? = context
        val eView = renderable.view
        eView.setOnTouchListener { v: View?, event: MotionEvent? ->
            Toast.makeText(c, "Location marker touched.", Toast.LENGTH_LONG).show()
            false
        }
        return base
    }

    fun checkIsSupportedDeviceOrFinish(activity: MainActivity): Boolean {
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }
}