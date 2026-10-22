package org.librecellar.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.net.Inet6Address
import java.net.InetAddress
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Android 17 (for apps targeting it) only lets apps reach devices on the local network, such as
 * a home server, with the runtime permission ACCESS_LOCAL_NETWORK ("Nearby devices"). Servers on
 * the internet need no permission.
 */
object LocalNetwork {

    /** Android 17, the first version that enforces the permission. */
    private const val ANDROID_17 = 37

    fun permissionRequired(): Boolean = Build.VERSION.SDK_INT >= ANDROID_17

    fun hasPermission(context: Context): Boolean =
        !permissionRequired() ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Whether the server at [url] is on the local network: a private (10/8, 172.16/12, 192.168/16),
     * link-local or IPv6 unique-local address, or a ".local" name. Resolves the host name, so call
     * it off the main thread.
     */
    fun isLocalServer(url: String): Boolean {
        val host = url.trim().toHttpUrlOrNull()?.host ?: return false
        if (host.endsWith(".local", ignoreCase = true)) return true
        return try {
            InetAddress.getAllByName(host).any { address ->
                address.isSiteLocalAddress || address.isLinkLocalAddress ||
                    (address is Inet6Address && (address.address[0].toInt() and 0xFE) == 0xFC)
            }
        } catch (e: Exception) {
            false
        }
    }

    /** True when the server can only be reached once the user allows local network access. */
    fun isBlocked(context: Context, url: String): Boolean =
        !hasPermission(context) && isLocalServer(url)
}
