package com.nsc9012.bluetooth.devices

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import com.nsc9012.bluetooth.R
import com.nsc9012.bluetooth.extension.hasPermission
import com.nsc9012.bluetooth.extension.invisible
import com.nsc9012.bluetooth.extension.toast
import com.nsc9012.bluetooth.extension.visible
import kotlinx.android.synthetic.main.activity_devices.*


class DevicesActivity : AppCompatActivity() {
    var strUui: String? =null

    companion object {
        const val ENABLE_BLUETOOTH = 1
        const val REQUEST_ENABLE_DISCOVERY = 2
        const val REQUEST_ACCESS_COARSE_LOCATION = 3
    }

    /* Broadcast receiver to listen for discovery results. */
    private val bluetoothDiscoveryResult = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("TAG", "AgainSignalDetected: "+intent!!.action)

            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt() //dBm
                val pairedDevices = bluetoothAdapter.bondedDevices

                if(device.name != null && device.name!!.contains("POC")){

                    val d =  BluetoothDevice
                    for (device in pairedDevices) {
                        for (uuid in device.uuids) {
                            val uuid_string = uuid.toString()
                            Log.d("TAG", "uuid : $uuid_string")
                        }
                    }

                    //10 ^ ((-60 -(-86))/(10 * 2)) && device.name!!.contains("POC")
                    var value:Double=((-60 -(rssi) )/(10 * 2.0))
                    val distance:Double= Math.pow(value, 10.0);


                    deviceListAdapter.addDevice(Bluetooth(device.getName(),device.getAddress(),rssi,distance))

                }
            }
        }
    }

    /* Broadcast receiver to listen for discovery updates. */
    private val bluetoothDiscoveryMonitor = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    progress_bar.visible()
                    toast("Scan started...")

                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    progress_bar.invisible()
                    Log.d("TAG", "onReceive989: "+deviceListAdapter.itemCount)
                    if(deviceListAdapter.itemCount == 0 || deviceListAdapter.itemCount>0 ){
                        Log.d("TAG", "onReceive989: "+"in side")

                        initBluetooth()
                        //registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
                    }
                    toast("Scan complete. Found ${deviceListAdapter.itemCount} devices.")
                }
            }
        }
    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val deviceListAdapter = DevicesAdapter()
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    var handler=Handler(Looper.getMainLooper())
    var runnable= Runnable {
        Toast.makeText(baseContext,"Handler Running ",Toast.LENGTH_SHORT).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        swipeRefreshLayout = findViewById(R.id.swipeToRefresh)
        initBluetooth()


        swipeRefreshLayout.setOnRefreshListener {
            // on below line we are setting is refreshing to false.
            swipeRefreshLayout.isRefreshing = false
            initBluetooth()
            registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))

        }
        initUI()

        val handlerThread = HandlerThread("background-thread")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        handler.postDelayed({
            //LOG.d("notify!")
            // call some methods here
            Log.d("TAG", "onCreate54565: 12")
            // make sure to finish the thread to avoid leaking memory
            handlerThread.quitSafely()
        }, 2000)
        //handler.postDelayed(runnable,1000);
//       val timer = object : CountDownTimer(5000, 5000) {
//            override fun onTick(millisUntilFinished: Long) {                Log.d("TAG", "onCreate54565: 36")
//            }
//            override fun onFinish() {
//                Log.d("TAG", "onCreate54565: 12")
//
//            }
//        }
//        timer.start()
    }

    private fun initUI() {
        title = "Bluetooth Scanner"
        recycler_view_devices.adapter = deviceListAdapter
        recycler_view_devices.layoutManager = LinearLayoutManager(this)
        recycler_view_devices.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        //  button_discover.setOnClickListener { initBluetooth() }
    }

    private fun initBluetooth() {

        // if (bluetoothAdapter.isDiscovering) return

        if (bluetoothAdapter.isEnabled) {
            startDiscovery()
            //enableDiscovery()
        } else {
            Log.d("TAG", "initBluetooth: "+"22222")

            // Bluetooth isn't enabled - prompt user to turn it on
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, ENABLE_BLUETOOTH)
        }
    }

    private fun enableDiscovery() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        startActivityForResult(intent, REQUEST_ENABLE_DISCOVERY)
    }

    private fun monitorDiscovery() {
        registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        registerReceiver(bluetoothDiscoveryMonitor, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
    }

    private fun startDiscovery() {
        if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (bluetoothAdapter.isEnabled && !bluetoothAdapter.isDiscovering) {
                beginDiscovery()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_ACCESS_COARSE_LOCATION
            )
        }
    }

    private fun beginDiscovery() {
        registerReceiver(bluetoothDiscoveryResult, IntentFilter(BluetoothDevice.ACTION_FOUND))
        deviceListAdapter.clearDevices()
        monitorDiscovery()
        bluetoothAdapter.startDiscovery()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ACCESS_COARSE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    beginDiscovery()
                } else {
                    toast("Permission required to scan for devices.")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ENABLE_BLUETOOTH -> if (resultCode == Activity.RESULT_OK) {
                enableDiscovery()
            }
            REQUEST_ENABLE_DISCOVERY -> if (resultCode == Activity.RESULT_CANCELED) {
                toast("Discovery cancelled.")
            } else {
                startDiscovery()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothDiscoveryMonitor)
        unregisterReceiver(bluetoothDiscoveryResult)
    }

}
