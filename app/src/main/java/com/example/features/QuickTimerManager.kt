package com.example.features

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("timer_channel", "Timer Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, "timer_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timer Finished")
            .setContentText("Your quick timer is up!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 500, 500))
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(1001, notification)
    }
}

class QuickTimerManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun startTimer(minutes: Int) {
        val intent = Intent(context, TimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000)
        try { alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent) } catch(e: SecurityException) {}
        
        // Show ongoing notification (simplified)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("timer_channel", "Timer Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val ongoing = NotificationCompat.Builder(context, "timer_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Timer Running")
            .setContentText("Timer set for $minutes minutes")
            .setOngoing(true)
            .setTimeoutAfter((minutes * 60 * 1000).toLong())
            .build()
        notificationManager.notify(1002, ongoing)
    }
}
