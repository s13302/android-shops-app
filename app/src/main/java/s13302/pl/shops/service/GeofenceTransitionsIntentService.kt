package s13302.pl.shops.service

import android.app.IntentService
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import com.google.android.gms.location.*
import s13302.pl.shops.ProjectConstants
import s13302.pl.shops.R

class GeofenceTransitionsIntentService: IntentService("GeofenceTransitionsIntentService") {

    companion object {
        private const val TAG = "GeofenceService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "GeofenceTransitionsIntentService created")
    }

    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence error number: ${geofencingEvent.errorCode}")
            return
        }
        handleGeofenceEvent(geofencingEvent)
    }

    private fun handleGeofenceEvent(geofencingEvent: GeofencingEvent) {
        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            sendNotification(geofencingEvent)
        } else {
            Log.e(TAG, "Transition invalid type: $geofenceTransition")
        }
    }

    private fun sendNotification(geofencingEvent: GeofencingEvent) {
        Log.d(TAG, "Sending the notification about crossing geofence: $geofencingEvent")
        val notification = NotificationCompat.Builder(this, ProjectConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Title")
            .setContentText("Content text - very long one")
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(0, notification)
    }

}