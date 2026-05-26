package uk.org.cgatechnologies.wideya.common.locationmanager

import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.yayandroid.locationmanager.base.LocationBaseService
import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration
import com.yayandroid.locationmanager.configuration.LocationConfiguration
import com.yayandroid.locationmanager.constants.FailType
import com.yayandroid.locationmanager.constants.ProviderType
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.data.Constants

private const val TAG = "Location_Service"

class LocationService : LocationBaseService() {
    private var isLocationRequested = false
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun getLocationConfiguration(): LocationConfiguration {


        val locationRequest = LocationRequest.create().apply {
            interval = Constants.TIME_INTERVAL
            isWaitForAccurateLocation = true
        }

        return LocationConfiguration.Builder()
            .keepTracking(true)
            .useGooglePlayServices(
                GooglePlayServicesConfiguration.Builder()
                    .locationRequest(locationRequest)
                    .fallbackToDefault(true)
                    .askForGooglePlayServices(false)
                    .askForSettingsApi(false)
                    .failOnSettingsApiSuspended(false)
                    .ignoreLastKnowLocation(false)
                    .setWaitPeriod(20 * 1000)
                    .build()
            )
            .useDefaultProviders(
                DefaultProviderConfiguration.Builder()
                    .requiredTimeInterval(Constants.TIME_INTERVAL)
                    .requiredDistanceInterval(0)
                    .acceptableAccuracy(Constants.DISTANCE_ACCURACY)
                    .acceptableTimePeriod((5 * 60 * 1000).toLong())
                    .gpsMessage(getString(R.string.enable_gps))
                    .setWaitPeriod(ProviderType.GPS, (20 * 1000).toLong())
                    .setWaitPeriod(ProviderType.NETWORK, (20 * 1000).toLong())
                    .build()
            )
            .build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // calling super is required when extending from LocationBaseService
        super.onStartCommand(intent, flags, startId)
        if (!isLocationRequested) {
            isLocationRequested = true
            getLocation()
        }

        // Return type is depends on your requirements
        return START_NOT_STICKY
    }

    override fun onLocationChanged(location: Location?) {
        Log.d(TAG, "Location: ${location?.latitude} - ${location?.longitude}")
        location?.let {
            Utils.getEncSharedPrefs(this).edit()
                .putString(Constants.PREF_LOCATION_LAT, it.latitude.toString())
                .putString(Constants.PREF_LOCATION_LNG, it.longitude.toString())
                .putString(Constants.PREF_LOCATION_DATETIME_ACQUIRED, Utils.getISODateTimeUTC())
                .apply()

            // copy items to SharedPrefs too since its faster than EncSharedPrefs
            Utils.getSharedPrefsState(this).edit()
                .putString(Constants.PREF_LOCATION_LAT, it.latitude.toString())
                .putString(Constants.PREF_LOCATION_LNG, it.longitude.toString())
                .putString(Constants.PREF_LOCATION_DATETIME_ACQUIRED, Utils.getISODateTimeUTC())
                .apply()
        }
    }

    override fun onLocationFailed(@FailType type: Int) {
        Log.d(TAG, "Location: Failed To Fetch - $type")
    }
}