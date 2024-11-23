package com.milesseventh.ussmonitor

import android.annotation.SuppressLint

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val SCAN_PERIOD: Long = 7000

class BT(btMan: BluetoothManager) {
    private val adapter: BluetoothAdapter? = btMan.adapter
    private val scanner = adapter?.bluetoothLeScanner

    private var scanning = false
    private var knownDevices = mutableMapOf<String, BluetoothDevice>()
    private var connectedDevice: BluetoothDevice? = null

    @SuppressLint("MissingPermission")
    fun scanLeDevice(onFound: (device: BluetoothDevice) -> Unit) {
        if (scanner == null || scanning) return

        val scannerCB: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                onFound(result.device)
                knownDevices[result.device.address] = result.device
            }
        }

        knownDevices.clear()
        runBlocking {
            launch {
                delay(SCAN_PERIOD)
                scanning = false
                scanner.stopScan(scannerCB)
            }
            scanning = true
            scanner.startScan(scannerCB)
        }
    }

    fun connect(address: String): Boolean {
        adapter?.let { adapter ->
            try {
                connectedDevice = adapter.getRemoteDevice(address)
                return true
            } catch (exception: IllegalArgumentException) {
                return false
            }
            // connect to the GATT server on the device
        } ?: run {
            return false
        }

    }

}