#!/bin/bash
sed -i 's/alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)/try { alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent) } catch(e: SecurityException) {}/g' /app/applet/app/src/main/java/com/example/features/QuickTimerManager.kt
