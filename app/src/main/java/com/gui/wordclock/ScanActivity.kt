package com.gui.wordclock

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_scan.*
import java.io.IOException
import java.util.*


class ScanActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WordClock::ScanActivity"
        private const val REQUEST_ENABLE_BT = 200
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        check()

        /*val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
        }*/

        scan_btn.setOnClickListener {
            bluetoothAdapter?.startDiscovery()
            indeterminateBar.visibility = View.VISIBLE
            scan_btn.isEnabled = false
        }


        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        //98:D3:61:F9:2B:FD
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    scan_btn.isEnabled = true
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                    Log.d("device_add", "$deviceHardwareAddress")
                    Log.d("device", "$deviceName")
                    //device?.address.equals("98:D3:61:F9:2B:FD")

                    deviceName?.let {
                        if(it.toLowerCase().contains("clock")) {
                            indeterminateBar.visibility = View.GONE
                            wordclock.visibility = View.VISIBLE
                            wordclock.text = "wordclock founded at the following address: $device"
                            if(thread != null) {
                                thread!!.cancel()
                                thread = null
                            }
                            thread = ConnectThread(device)
                            thread?.start()
                        }
                    }
                }
            }
        }
    }

//    private var thread: AcceptThread? = null
    private var thread: ConnectThread? = null


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun check() {
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(device.uuids[0].uuid)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()



            try { // This is a blocking call and will only return on a
                    // successful connection or an exception
                Log.d("try", "to connect")
                mmSocket!!.connect()
                manageMyConnectedSocket(mmSocket!!)
            } catch (e: IOException) { // Close the socket
                e.printStackTrace()
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(
                        TAG, "unable to close() socket during connection failure", e2
                    )
                }
                return
            }


        }

        private fun manageMyConnectedSocket(socket: BluetoothSocket) {
            Log.d("manageMyConnectedSocket", "$socket")
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}
