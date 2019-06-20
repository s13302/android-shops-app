package s13302.pl.shops

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = resources.getString(R.string.notification_name)

            val notificationChannel = NotificationChannel(
                ProjectConstants.NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

}