package com.pravera.flutter_foreground_task.service

import android.content.Context

/** ServiceProvider */
interface ServiceProvider {
	fun getForegroundServiceManager(): ForegroundServiceManager
	fun connectToHandler(context: Context, arguments: Any?)
}
