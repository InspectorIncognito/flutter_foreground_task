package com.pravera.flutter_foreground_task

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.pravera.flutter_foreground_task.service.ForegroundService
import com.pravera.flutter_foreground_task.service.ForegroundServiceManager
import com.pravera.flutter_foreground_task.service.ServiceProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

/** FlutterForegroundTaskPlugin */
class FlutterForegroundTaskPlugin : FlutterPlugin, ActivityAware, ServiceProvider {
    private lateinit var foregroundServiceManager: ForegroundServiceManager

    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null
    private lateinit var methodCallHandler: MethodCallHandlerImpl
    private var foregroundService: ForegroundService? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        foregroundServiceManager = ForegroundServiceManager()

        methodCallHandler = MethodCallHandlerImpl(binding.applicationContext, this)
        methodCallHandler.init(binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        if (::methodCallHandler.isInitialized) {
            methodCallHandler.dispose()
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d("ServiceConnection", "inner: Service connected")
            foregroundService = (service as ForegroundService.LocalBinder).getService()
            foregroundService?.initialize()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d("ServiceConnection", "inner: Service disconnected")
            foregroundService = null
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        methodCallHandler.setActivity(binding.activity)
        binding.addActivityResultListener(methodCallHandler)
        activityBinding = binding

        binding.activity.bindService(
            Intent(
                binding.activity,
                ForegroundService::class.java
            ), serviceConnection, Context.BIND_AUTO_CREATE
        )
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        activityBinding?.removeActivityResultListener(methodCallHandler)
        activityBinding?.activity?.unbindService(serviceConnection)
        activityBinding = null
        methodCallHandler.setActivity(null)
    }

    override fun getForegroundServiceManager() = foregroundServiceManager

    override fun connectToHandler(context: Context, arguments: Any?) {
        Log.d("PLUGIN inner", "connectToHandler")
        foregroundService?.initHandler()
    }
}
