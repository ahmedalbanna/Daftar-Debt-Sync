package com.example.localsync.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.localsync.model.PeerDevice
import com.example.localsync.model.PeerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.InetAddress

/**
 * Discovers and registers P2P sync services over the local network using NSD.
 * Configurable service type allows different apps to use distinct discovery namespaces.
 */
class NsdDiscovery(
    private val context: Context,
    private val deviceId: String,
    private val deviceName: String,
    private val port: Int,
    private val serviceType: String = "_p2p-sync._tcp."
) {
    private val TAG = "NsdDiscovery"

    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val serviceName = "Sync_${deviceName.replace(" ", "_")}_${deviceId.take(4)}"

    fun start() {
        acquireMulticastLock()
        registerService()
        discoverServices()
    }

    private fun acquireMulticastLock() {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("LocalSyncMulticastLock").apply {
                    setReferenceCounted(true)
                    acquire()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire MulticastLock: ${e.message}")
        }
    }

    private fun registerService() {
        if (nsdManager == null) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@NsdDiscovery.serviceName
            serviceType = this@NsdDiscovery.serviceType
            port = this@NsdDiscovery.port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Log.e(TAG, "Service registration failed: $arg1")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered")
            }

            override fun onUnregistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Log.e(TAG, "Service unregistration failed: $arg1")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering NSD service", e)
        }
    }

    private fun discoverServices() {
        if (nsdManager == null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}")
                if (service.serviceType.contains(serviceType.split(".").first()) &&
                    service.serviceName != serviceName
                ) {
                    try {
                        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                                Log.e(TAG, "Resolve failed: $errorCode")
                            }

                            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                                val host: InetAddress = serviceInfo.host ?: return
                                val ip = host.hostAddress ?: return
                                val peerPort = serviceInfo.port

                                val peer = PeerDevice(
                                    id = serviceInfo.serviceName,
                                    name = serviceInfo.serviceName,
                                    ip = ip,
                                    port = peerPort,
                                    status = PeerStatus.DISCOVERED
                                )

                                _discoveredPeers.update { current ->
                                    val filtered = current.filter { it.id != peer.id }
                                    filtered + peer
                                }
                            }
                        })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resolving service", e)
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${service.serviceName}")
                _discoveredPeers.update { current ->
                    current.filter { it.id != service.serviceName }
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting NSD discovery", e)
        }
    }

    fun stop() {
        try {
            if (registrationListener != null) {
                nsdManager?.unregisterService(registrationListener)
                registrationListener = null
            }
            if (discoveryListener != null) {
                nsdManager?.stopServiceDiscovery(discoveryListener)
                discoveryListener = null
            }
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping NSD", e)
        }
    }
}
