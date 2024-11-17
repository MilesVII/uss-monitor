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

    @SuppressLint("MissingPermission")
    suspend fun scanLeDevice(): Array<BluetoothDevice> {
        if (scanner == null) return emptyArray()

        return suspendCoroutine {
            cont ->

            val devices = mutableListOf<BluetoothDevice>()

            val scannerCB: ScanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    devices.add(result.device)
                }
            }

            if (!scanning) {
                devices.clear()
                runBlocking {
                    launch {
                        delay(SCAN_PERIOD)
                        scanning = false
                        scanner.stopScan(scannerCB)
                        cont.resume(devices.toTypedArray())
                    }
                    scanning = true
                    scanner.startScan(scannerCB)
                }
            }
        }
    }

}