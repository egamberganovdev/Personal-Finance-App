package dev.egamberganov.finflow.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.egamberganov.finflow.MainActivity
import dev.egamberganov.finflow.R
import dev.egamberganov.finflow.data.database.AppDatabase
import dev.egamberganov.finflow.domain.model.CurrencyFormatter
import dev.egamberganov.finflow.domain.model.ScheduledPaymentCalculator
import dev.egamberganov.finflow.domain.model.ScheduledPaymentStatus
import java.util.concurrent.TimeUnit

class ScheduledPaymentNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(context)

        // 1. Check user settings
        val settings = database.appSettingsDao().getSettingsDirect()
        if (settings != null && !settings.notificationsEnabled) {
            return Result.success()
        }

        // 2. Check Android 13+ permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
        }

        // 3. Create Notification Channel
        createNotificationChannel()

        // 4. Query active scheduled payments
        val activePayments = database.scheduledPaymentDao().getActiveScheduledPaymentsWithDetailsDirect()
        if (activePayments.isEmpty()) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val notificationManager = NotificationManagerCompat.from(context)

        for (item in activePayments) {
            val payment = item.scheduledPayment
            val statusInfo = ScheduledPaymentCalculator.calculateStatus(payment.nextPaymentDateMillis, now)

            if (statusInfo.status == ScheduledPaymentStatus.DUE_TODAY || statusInfo.status == ScheduledPaymentStatus.OVERDUE) {
                val title = if (statusInfo.status == ScheduledPaymentStatus.DUE_TODAY) {
                    context.getString(R.string.notification_due_today_title, payment.name)
                } else {
                    context.getString(R.string.notification_overdue_title, payment.name)
                }

                val formattedAmount = CurrencyFormatter.formatAmount(payment.amount, payment.currency)
                val accountName = item.account?.name ?: context.getString(R.string.account_name)
                val dueDescription = if (statusInfo.status == ScheduledPaymentStatus.DUE_TODAY) {
                    context.getString(R.string.status_due_today).lowercase()
                } else {
                    val days = Math.abs(statusInfo.daysDifference)
                    if (days <= 1) {
                        context.getString(R.string.overdue_by_one_day).lowercase()
                    } else {
                        context.getString(R.string.overdue_by_days, days.toInt()).lowercase()
                    }
                }

                val body = context.getString(
                    R.string.notification_payment_body,
                    formattedAmount,
                    accountName,
                    dueDescription
                )

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    payment.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                try {
                    notificationManager.notify(NOTIFICATION_BASE_ID + payment.id.toInt(), notification)
                } catch (_: SecurityException) {
                    // Gracefully handle permission revoked during execution
                }
            }
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "scheduled_payments_channel"
        const val WORK_NAME = "scheduled_payment_reminder_work"
        const val NOTIFICATION_BASE_ID = 3000

        fun schedulePeriodicWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ScheduledPaymentNotificationWorker>(
                1, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
