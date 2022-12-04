package com.example.stroll.other

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper

class ARUtils {
    private val TAG = "SceneformDemoUtils"

    fun displayError(
        context: Context, errorMsg: String, problem: Throwable?
    ) {
        val tag = context.javaClass.simpleName
        val toastText: String = if (problem?.message != null) {
            Log.e(tag, errorMsg, problem)
            errorMsg + ": " + problem.message
        } else if (problem != null) {
            Log.e(tag, errorMsg, problem)
            errorMsg
        } else {
            Log.e(tag, errorMsg)
            errorMsg
        }
        Log.d("ARCoreCamera", toastText)
        Handler(Looper.getMainLooper())
            .post {
                val toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            }
    }

    @Throws(UnavailableException::class)
    fun createArSession(activity: Activity?, installRequested: Boolean): Session? {
        var session: Session? = null
        // if we have the camera permission, create the session
        if (ARLocationPermissionHelper.hasPermission(activity)) {
            when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                InstallStatus.INSTALL_REQUESTED -> return null
                InstallStatus.INSTALLED -> {}
            }
            session = Session(activity)
            // IMPORTANT!!!  ArSceneView needs to use the non-blocking update mode.
            val config = Config(session)
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE)
            session.configure(config)
        }
        return session
    }

    fun handleSessionException(
        activity: Activity?, sessionException: UnavailableException
    ) {
        val message: String
        when (sessionException) {
            is UnavailableArcoreNotInstalledException -> {
                message = "Please install ARCore"
            }
            is UnavailableApkTooOldException -> {
                message = "Please update ARCore"
            }
            is UnavailableSdkTooOldException -> {
                message = "Please update this app"
            }
            is UnavailableDeviceNotCompatibleException -> {
                message = "This device does not support AR"
            }
            else -> {
                message = "Failed to create AR session"
                Log.e(TAG, "Exception: $sessionException")
            }
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }
}