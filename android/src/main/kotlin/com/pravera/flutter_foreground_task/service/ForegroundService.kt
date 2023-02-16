package com.pravera.flutter_foreground_task.service

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.pravera.flutter_foreground_task.R
import com.pravera.flutter_foreground_task.models.*
import com.pravera.flutter_foreground_task.utils.ForegroundServiceUtils
import com.pravera.location.LocationManager
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation
import kotlinx.coroutines.*
import java.util.*
import kotlin.system.exitProcess

/**
 * A service class for implementing foreground service.
 *
 * @author Dev-hwang
 * @version 1.0
 */
class ForegroundService : Service(), MethodChannel.MethodCallHandler {
	companion object {
        private val TAG = ForegroundService::class.java.simpleName
        private const val ACTION_TASK_EVENT = "onEvent"
        private const val ACTION_TASK_START = "onStart"
        private const val ACTION_TASK_DESTROY = "onDestroy"
        private const val ACTION_BUTTON_PRESSED = "onButtonPressed"
        private const val ACTION_NOTIFICATION_PRESSED = "onNotificationPressed"
        private const val DATA_FIELD_NAME = "data"

        private const val TRIP_CHANNEL_ID = "TRIP_CHANNEL_ID"
        private const val ARRIVAL_CHANNEL_ID = "ARRIVAL_CHANNEL_ID"
		//private const val NOTIFICATION_ID = 5517
		//private const val FOREGROUND_ID = 5518

		/** Returns whether the foreground service is running. */
		var isRunningService = false
			private set
	}

	private var foregroundServiceStatus: ForegroundServiceStatus? = null
	private lateinit var foregroundTaskOptions: ForegroundTaskOptions
	private lateinit var notificationOptions: NotificationOptions

	private var wakeLock: PowerManager.WakeLock? = null
	private var wifiLock: WifiManager.WifiLock? = null
	private var currentPlate: String? = null

	private var currFlutterLoader: FlutterLoader? = null
	private var prevFlutterEngine: FlutterEngine? = null
	private var currFlutterEngine: FlutterEngine? = null
	private var backgroundChannel: MethodChannel? = null
	private var backgroundJob: Job? = null
	private var lastKnownLocation: Location? = null

	// A broadcast receiver that handles intents that occur within the foreground service.
	private var broadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			try {
				val action = intent?.action ?: return
				val data = intent.getStringExtra(DATA_FIELD_NAME)
				backgroundChannel?.invokeMethod(action, data)
			} catch (e: Exception) {
				Log.e(TAG, "onReceive", e)
			}
		}
	}

	private val binder = LocalBinder()

	inner class LocalBinder : Binder() {
		fun getService(): ForegroundService = this@ForegroundService
	}

	override fun onCreate() {
		super.onCreate()
		registerBroadcastReceiver()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		Log.d(TAG, "onStartCommand")
		fetchDataFromPreferences()

		when (foregroundServiceStatus?.action) {
			ForegroundServiceAction.UPDATE -> {
				Log.d(TAG, "onStartCommand UPDATE")
				updateNotification()
			}
			ForegroundServiceAction.RESTART -> {
				Log.d(TAG, "onStartCommand RESTART")
			}
			ForegroundServiceAction.STOP -> {
				Log.d(TAG, "onStartCommand STOP")
				stopForegroundService()
				return START_NOT_STICKY
			}
			ForegroundServiceAction.START -> {
				Log.d(TAG, "onStartCommand START")
				startForegroundService()
				//executeDartCallback(foregroundTaskOptions.callbackHandle)
				startForegroundTask()
			}
		}

		return START_STICKY
	}

	private fun updateNotification() {
		val data = notificationOptions.notificationData
		val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		when (data) {
			is ArrivalNotificationData -> {
				mNotificationManager.notify(data.id, buildArrivalNotification(data.stopCode, data.topMessage, data.bottomMessage).build())
			}
			is NormalNotificationData -> {
				mNotificationManager.notify(data.id, buildNormalNotification(data.title, data.message, data.vibration).build())
			}
			is TravelNotificationData -> {
				mNotificationManager.notify(data.id, buildTravelNotification(data).build())
			}
		}
	}

	private fun buildArrivingNotification(): NotificationCompat.Builder {
		val notificationBuilder = NotificationCompat.Builder(this, ARRIVAL_CHANNEL_ID)
			.setContentTitle("El bus asignado llegó a tu paradero")
			.setContentText("Toca para más detalles")
			.setSmallIcon(getAppIconResourceId(applicationContext.packageManager))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setVibrate(longArrayOf(200, 500, 200, 500, 200, 500))

		val color = getNotificationColor()
		if (color != null) {
			notificationBuilder.color = color
		}

		val pendingIntent = getPendingIntent(applicationContext.packageManager)

		notificationBuilder.setContentIntent(pendingIntent)
		return notificationBuilder
	}

	override fun onBind(intent: Intent?): IBinder {
		return binder
	}

	fun initialize() {
		Log.d(TAG, "initialize")
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val mNotificationManager = this
				.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

			// TRIP CHANNEL
			val tripName = "Notificaciones de estado de viaje"
			val tripImportance = NotificationManager.IMPORTANCE_LOW
			val tripChannel = NotificationChannel(TRIP_CHANNEL_ID, tripName, tripImportance)
			tripChannel.setShowBadge(false)
			tripChannel.vibrationPattern = longArrayOf(0)
			tripChannel.enableVibration(false)
			tripChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
			mNotificationManager.createNotificationChannel(tripChannel)


			// ARRIVAL CHANNEL
			val arrivalName = "Notificaciones de aviso de llegada a destino"
			val arrivalImportance = NotificationManager.IMPORTANCE_HIGH
			val arrivalChannel = NotificationChannel(ARRIVAL_CHANNEL_ID, arrivalName, arrivalImportance)
			arrivalChannel.setShowBadge(false)
			arrivalChannel.vibrationPattern = longArrayOf(200, 500, 200, 500, 200, 500)
			arrivalChannel.enableVibration(true)
			arrivalChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
			mNotificationManager.createNotificationChannel(arrivalChannel)
		}
		//executeDartCallback(foregroundTaskOptions.callbackHandle)
	}

	private fun buildTravelNotification(data: TravelNotificationData): NotificationCompat.Builder {
		val notificationLayout = if (data.destinationCode == "") {
			val layout = RemoteViews(packageName, R.layout.notification_travel_empty)
			layout.setTextViewText(R.id.notification_top, data.topMessage)
			val color = getNotificationColor()
			if (color != null) {
				layout.setTextColor(R.id.notification_top, color)
			}
			layout
		} else {
			val layout = RemoteViews(packageName, R.layout.notification_travel_data)
			layout.setTextViewText(R.id.stop_code, data.destinationCode)
			layout.setTextViewText(R.id.stations_quantity, data.destinationStops)
			layout.setTextViewText(R.id.stop_name, data.destinationName)
			layout.setTextViewText(R.id.station_plural, data.destinationStopsSuffix)
			layout
		}

		val notificationBuilder = NotificationCompat.Builder(this, TRIP_CHANNEL_ID)
			.setStyle(NotificationCompat.DecoratedCustomViewStyle())
			.setCustomContentView(notificationLayout)
			.setSmallIcon(getAppIconResourceId(applicationContext.packageManager))
			.setAutoCancel(true)

		val color = getNotificationColor()
		if (color != null) {
			notificationBuilder.color = color
		}
		val pendingIntent = getPendingIntent(applicationContext.packageManager)

		notificationBuilder.setContentIntent(pendingIntent)
		return notificationBuilder
	}

	private fun buildArrivalNotification(stopCode: String, top: String, bottom: String): NotificationCompat.Builder {
		val notificationLayout = RemoteViews(packageName, R.layout.notification_arrival_data)
		notificationLayout.setTextViewText(R.id.stop_code_text, stopCode)
		notificationLayout.setTextViewText(R.id.data_top_text, top)
		notificationLayout.setTextViewText(R.id.data_bottom_text, bottom)
		notificationLayout.setTextColor(R.id.data_bottom_text, resources.getColor(R.color.grises_generales_mid_gray_2))

		val notificationBuilder = NotificationCompat.Builder(this, TRIP_CHANNEL_ID)
			.setStyle(NotificationCompat.DecoratedCustomViewStyle())
			.setCustomContentView(notificationLayout)
			.setSmallIcon(getAppIconResourceId(applicationContext.packageManager))
			.setAutoCancel(true)

		val color = getNotificationColor()
		if (color != null) {
			notificationBuilder.color = color
		}
		val pendingIntent = getPendingIntent(applicationContext.packageManager)

		notificationBuilder.setContentIntent(pendingIntent)
		return notificationBuilder
	}

	private fun getNotificationColor(): Int? {
		var iconBackgroundColor: Int? = null
		val iconBackgroundColorRgb = notificationOptions.backgroundColorRgb?.split(",")
		if (iconBackgroundColorRgb != null && iconBackgroundColorRgb.size == 3) {
			iconBackgroundColor = Color.rgb(
				iconBackgroundColorRgb[0].toInt(),
				iconBackgroundColorRgb[1].toInt(),
				iconBackgroundColorRgb[2].toInt()
			)
		}
		return iconBackgroundColor
	}

	private fun buildNormalNotification(title: String, message: String, enableVibration:Boolean = false): NotificationCompat.Builder {
		val notificationBuilder = NotificationCompat.Builder(this, ARRIVAL_CHANNEL_ID)
			.setContentTitle(title)
			.setContentText(message)
			.setSmallIcon(getAppIconResourceId(applicationContext.packageManager))
			.setAutoCancel(true)

		if (enableVibration) {
			notificationBuilder.setVibrate(longArrayOf(200, 500, 200, 500, 200, 500))
		}

		val color = getNotificationColor()
		if (color != null) {
			notificationBuilder.color = color
		}
		val pendingIntent = getPendingIntent(applicationContext.packageManager)

		notificationBuilder.setContentIntent(pendingIntent)
		return notificationBuilder
	}

	override fun onDestroy() {
		super.onDestroy()
		releaseLockMode()
		destroyBackgroundChannel()
		unregisterBroadcastReceiver()
		if (foregroundServiceStatus?.action != ForegroundServiceAction.STOP) {
			if (isSetStopWithTaskFlag()) {
				exitProcess(0)
			} else {
				Log.i(TAG, "The foreground service was terminated due to an unexpected problem.")
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					if (!ForegroundServiceUtils.isIgnoringBatteryOptimizations(applicationContext)) {
						Log.i(TAG, "Turn off battery optimization to restart service in the background.")
						return
					}
				}
				setRestartAlarm()
			}
		}
	}

	override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
		when (call.method) {
			"initialize" -> startForegroundTask()
			else -> result.notImplemented()
		}
	}

	private fun fetchDataFromPreferences() {
		foregroundServiceStatus = ForegroundServiceStatus.getData(applicationContext)
		foregroundTaskOptions = ForegroundTaskOptions.getData(applicationContext)
		notificationOptions = NotificationOptions.getData(applicationContext)
	}

	private fun registerBroadcastReceiver() {
		val intentFilter = IntentFilter().apply {
			addAction(ACTION_BUTTON_PRESSED)
			addAction(ACTION_NOTIFICATION_PRESSED)
		}
		registerReceiver(broadcastReceiver, intentFilter)
	}

	private fun unregisterBroadcastReceiver() {
		unregisterReceiver(broadcastReceiver)
	}

	@SuppressLint("WrongConstant")
	private fun startForegroundService() {
		when (val data = notificationOptions.notificationData) {
			is ArrivalNotificationData -> {
				startForeground(data.id, buildArrivalNotification(data.stopCode, data.topMessage, data.bottomMessage).build())
			}
			is NormalNotificationData -> {
				startForeground(data.id, buildNormalNotification(data.title, data.message).build())
			}
			is TravelNotificationData -> {
				startForeground(data.id, buildTravelNotification(data).build())
			}
		}

		acquireLockMode()
		isRunningService = true
	}

	private fun stopForegroundService() {
		Log.d(TAG, ACTION_TASK_DESTROY)
		backgroundChannel?.invokeMethod(ACTION_TASK_DESTROY, null)
		releaseLockMode()
		stopForeground(true)
		stopSelf()
		stopForegroundTask()
//		val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//		mNotificationManager.cancel(NOTIFICATION_ID)
		isRunningService = false
		currentPlate = null
	}

	@SuppressLint("WakelockTimeout")
	private fun acquireLockMode() {
		if (foregroundTaskOptions.allowWakeLock && (wakeLock == null || wakeLock?.isHeld == false)) {
			wakeLock = (applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
				newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ForegroundService:WakeLock").apply {
					setReferenceCounted(false)
					acquire()
				}
			}
		}

		if (foregroundTaskOptions.allowWifiLock && (wifiLock == null || wifiLock?.isHeld == false)) {
			wifiLock = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).run {
				createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ForegroundService:WifiLock").apply {
					setReferenceCounted(false)
					acquire()
				}
			}
		}
	}

	private fun releaseLockMode() {
		wakeLock?.let {
			if (it.isHeld) {
				it.release()
			}
		}

		wifiLock?.let {
			if (it.isHeld) {
				it.release()
			}
		}
	}

	private fun setRestartAlarm() {
		val calendar = Calendar.getInstance().apply {
			timeInMillis = System.currentTimeMillis()
			add(Calendar.SECOND, 1)
		}

		val intent = Intent(this, RestartReceiver::class.java)
		val sender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
		} else {
			PendingIntent.getBroadcast(this, 0, intent, 0)
		}

		val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, sender)
	}

	private fun isSetStopWithTaskFlag(): Boolean {
		val pm = applicationContext.packageManager
		val cName = ComponentName(this, this.javaClass)
		val flags = pm.getServiceInfo(cName, PackageManager.GET_META_DATA).flags

		return flags > 0
	}

	private fun initBackgroundChannel() {
		if (backgroundChannel != null) destroyBackgroundChannel()

		currFlutterEngine = FlutterEngine(this)

		currFlutterLoader = FlutterInjector.instance().flutterLoader()
		currFlutterLoader?.startInitialization(this)
		currFlutterLoader?.ensureInitializationComplete(this, null)

		val messenger = currFlutterEngine?.dartExecutor?.binaryMessenger ?: return
		backgroundChannel = MethodChannel(messenger, "flutter_foreground_task/background")
		backgroundChannel?.setMethodCallHandler(this)
	}

	fun initHandler() {
		Log.d(TAG, "initHandler")
		foregroundServiceStatus = ForegroundServiceStatus.getData(applicationContext)
		foregroundTaskOptions = ForegroundTaskOptions.getData(applicationContext)
		executeDartCallback(foregroundTaskOptions.callbackHandle)
	}

	private fun executeDartCallback(callbackHandle: Long?) {
		// If there is no callback handle, the code below will not be executed.
		if (callbackHandle == null) return
		initBackgroundChannel()
		val bundlePath = currFlutterLoader?.findAppBundlePath() ?: return
		val callbackInfo = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
		val dartCallback = DartExecutor.DartCallback(assets, bundlePath, callbackInfo)
		currFlutterEngine?.dartExecutor?.executeDartCallback(dartCallback)
		Log.d(TAG, "executeDartCallback")
	}

	private fun startForegroundTask() {
		stopForegroundTask()
		Log.d(TAG, "startForegroundTask")

		LocationManager.getInstance(this).startLocationUpdates(object: LocationManager.LocationListener {
			override fun onLocation(location: Location) {
				lastKnownLocation = location
			}
		})

		backgroundJob = CoroutineScope(Dispatchers.Default).launch {
			do {
				withContext(Dispatchers.Main) {
					try {
						var data = ""
						if (lastKnownLocation != null) {
							data = "${lastKnownLocation!!.latitude}|${lastKnownLocation!!.longitude}"
						}
						backgroundChannel?.invokeMethod(ACTION_TASK_EVENT, data)
					} catch (e: Exception) {
						Log.e(TAG, "invokeMethod", e)
					}
				}

				delay(foregroundTaskOptions.interval)
			} while (!foregroundTaskOptions.isOnceEvent)
		}
		backgroundChannel?.invokeMethod(ACTION_TASK_START, null)
	}

	private fun stopForegroundTask() {
		Log.d(TAG, "stopForegroundTask")
		if (backgroundJob != null) {
			LocationManager.getInstance(this).stopLocationUpdates()
			backgroundJob?.cancel()
			backgroundJob = null
		}
	}

	private fun destroyBackgroundChannel() {
		Log.d(TAG, "destroyBackgroundChannel")
		stopForegroundTask()

		currFlutterLoader = null
		prevFlutterEngine = currFlutterEngine
		currFlutterEngine = null

		val callback = object : MethodChannel.Result {
			override fun success(result: Any?) {
				prevFlutterEngine?.destroy()
				prevFlutterEngine = null
			}

			override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
				prevFlutterEngine?.destroy()
				prevFlutterEngine = null
			}

			override fun notImplemented() {
				prevFlutterEngine?.destroy()
				prevFlutterEngine = null
			}
		}
		backgroundChannel?.setMethodCallHandler(null)
		backgroundChannel = null
	}

	private fun getDrawableResourceId(resType: String, resPrefix: String, name: String): Int {
		val resName = if (resPrefix.contains("ic")) {
			String.format("ic_%s", name)
		} else {
			String.format("img_%s", name)
		}

		return applicationContext.resources.getIdentifier(resName, resType, applicationContext.packageName)
	}

	private fun getAppIconResourceId(pm: PackageManager): Int {
		return try {
			val appInfo = pm.getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
			appInfo.icon
		} catch (e: PackageManager.NameNotFoundException) {
			Log.e(TAG, "getAppIconResourceId", e)
			0
		}
	}

	private fun getPendingIntent(pm: PackageManager): PendingIntent {
		return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            || ForegroundServiceUtils.canDrawOverlays(applicationContext)) {
			val pressedIntent = Intent(ACTION_NOTIFICATION_PRESSED)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				PendingIntent.getBroadcast(
					this, 20000, pressedIntent, PendingIntent.FLAG_IMMUTABLE)
			} else {
				PendingIntent.getBroadcast(this, 20000, pressedIntent, 0)
			}
		} else {
			val launchIntent = pm.getLaunchIntentForPackage(applicationContext.packageName)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				PendingIntent.getActivity(
					this, 20000, launchIntent, PendingIntent.FLAG_IMMUTABLE)
			} else {
				PendingIntent.getActivity(this, 20000, launchIntent, 0)
			}
		}
	}

	fun createNotification() {
		updateNotification()
	}
}