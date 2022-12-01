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

    // Renderables for this example
    //    private ModelRenderable andyRenderable;
    private var exampleLayoutRenderable: ViewRenderable? = null
    private var exampleLayoutRenderable2: ViewRenderable? = null
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


        // Build a renderable from a 2D View.
        val exampleLayout: CompletableFuture<ViewRenderable> = ViewRenderable.builder()
            .setView(requireContext(), R.layout.example_layout)
            .build()
        // Build a renderable from a 2D View.
        // Build a renderable from a 2D View.
        val exampleLayout2: CompletableFuture<ViewRenderable> = ViewRenderable.builder()
            .setView(requireContext(), R.layout.example_layout)
            .build()

        CompletableFuture.allOf(
            exampleLayout,
            exampleLayout2
        )
            .handle<Any?> { notUsed: Void?, throwable: Throwable? ->
                // When you build a Renderable, Sceneform loads its resources in the background while
                // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                // before calling get().
                if (throwable != null) {
                    ARUtils().displayError(requireContext(), "Unable to load renderables", throwable)
                    return@handle null
                }
                try {
                    exampleLayoutRenderable = exampleLayout.get()
                    /*exampleLayoutRenderable2 = exampleLayout2.get()*/
                    //    private void initModel() {
//
//        Log.d("ARtag", "Init model");
//                                MaterialFactory.makeTransparentWithColor(this, new Color(android.graphics.Color.RED))
//                                        .thenAccept(
//                                                material -> {
//                                                    Vector3 vector3 = new Vector3(0.05f, 0.01f, 0.01f);
//                                                    andyRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
//                                                    andyRenderable.setShadowCaster(false);
//                                                    andyRenderable.setShadowReceiver(false);
//                                                });
                    hasFinishedLoading = true
                } catch (ex: InterruptedException) {
                    ARUtils().displayError(requireContext(), "Unable to load renderables", ex)
                } catch (ex: ExecutionException) {
                    ARUtils().displayError(requireContext(), "Unable to load renderables", ex)
                }
                null
            }
        Log.d("ARCoreCamera", "Updatelistener")
        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        // Set an update listener on the Scene that will hide the loading message once a Plane is
        // detected.
        arSceneView!!.scene.addOnUpdateListener { frameTime: FrameTime? ->
            if (!hasFinishedLoading) {
                return@addOnUpdateListener
            }
            if (locationScene == null) {
                // If our locationScene object hasn't been setup yet, this is a good time to do it
                // We know that here, the AR components have been initiated.
                locationScene = LocationScene(requireContext(), requireActivity(), arSceneView)

                // Now lets create our location markers.
                // First, a layout
                val layoutLocationMarker =
                    LocationMarker(
                        args.latLng.long, args.latLng.lat, getExampleView(
                            exampleLayoutRenderable!!
                        )
                    )
                /*val layoutLocationMarker2 =
                    LocationMarker(
                        17.0140646,
                        68.5586197,
                        getExampleView(exampleLayoutRenderable2!!)
                    )*/

                // An example "onRender" event, called every frame
                // Updates the layout with the markers distance
                layoutLocationMarker.renderEvent =
                    LocationNodeRender { node ->
                        val eView = exampleLayoutRenderable!!.view
                        val distanceTextView = eView.findViewById<TextView>(R.id.textView2)
                        distanceTextView.text = node.distance.toString() + "M"
                        val nameView = eView.findViewById<TextView>(R.id.textView1)
                        nameView.text = args.poiName
                    }
                /*layoutLocationMarker2.renderEvent =
                    LocationNodeRender { node ->
                        val eView = exampleLayoutRenderable2!!.view
                        val distanceTextView = eView.findViewById<TextView>(R.id.textView2)
                        distanceTextView.text = node.distance.toString() + "M"
                        val nameView = eView.findViewById<TextView>(R.id.textView1)
                        nameView.text = "Narvikfjellet"
                    }*/
                // Adding the marker
                locationScene!!.mLocationMarkers.add(layoutLocationMarker)
                /*locationScene!!.mLocationMarkers.add(layoutLocationMarker2)*/

                // Adding a simple location marker of a 3D model
//                        locationScene.mLocationMarkers.add(new LocationMarker(-0.119677, 51.478494, getAndy()));
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


        Log.d("ARCoreCamera", "Request permission")

        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.

        // Lastly request CAMERA & fine location permission which is required by ARCore-Location.
        ARLocationPermissionHelper.requestPermission(requireActivity())

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (locationScene != null) {
            Log.d("ARCoreCamera", "resume locationscene")
            locationScene!!.resume()
        }
        if (arSceneView!!.session == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                Log.d("ARCoreCamera", "demoutils create session")
                val session: Session? = ARUtils().createArSession((activity as MainActivity), installRequested)
                if (session == null) {
                    Log.d("ARCoreCamera", "session==null")
                    installRequested = ARLocationPermissionHelper.hasPermission((activity as MainActivity))
                    return
                } else {
                    Log.d("ARCoreCamera", "setupSession")
                    arSceneView!!.setupSession(session)
                    Log.d("ARCoreCamera", "setupSession done")
                }
            } catch (e: UnavailableException) {
                Log.d("ARCoreCamera", "exception in demoutils")
                ARUtils().handleSessionException((activity as MainActivity), e)
            }
        }

        try {
            Log.d("ARCoreCamera", "resume arsceneview")
            arSceneView!!.resume()
        } catch (ex: CameraNotAvailableException) {
            Log.d("ARCoreCamera", "cameranotaveilable exception")
            ARUtils().displayError(requireContext(), "Unable to get camera", ex)
            return
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")

        if (locationScene != null) {
            Log.d(TAG, "onPause = null")
            locationScene!!.pause()
        }

        arSceneView!!.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        arSceneView!!.destroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun getExampleView(renderable: ViewRenderable): Node? {
        Log.d("ARCoreCamera", "getExampleView")
        val base = Node()
        base.renderable = renderable
        val c: Context? = context
        // Add  listeners etc here
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
            Log.d(TAG, "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }
}