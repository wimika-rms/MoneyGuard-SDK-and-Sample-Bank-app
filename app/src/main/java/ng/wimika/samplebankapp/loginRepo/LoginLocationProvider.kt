package ng.wimika.samplebankapp.loginRepo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import ng.wimika.moneyguard_sdk_auth.datasource.auth_service.models.LoginLocation
import java.time.Instant

/** Obtains a fresh, one-shot observation specifically for session registration. */
class LoginLocationProvider(private val context: Context) {
    suspend fun current(): LoginLocation? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null

        val location = withTimeoutOrNull(8_000) {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .await()
        } ?: return null

        return LoginLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy.toDouble(),
            observedAtUtc = Instant.ofEpochMilli(location.time).toString(),
            isMocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else location.isFromMockProvider
        )
    }
}
