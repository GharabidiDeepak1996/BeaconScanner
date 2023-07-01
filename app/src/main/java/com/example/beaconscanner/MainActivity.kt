package com.example.beaconscanner

import android.Manifest
import android.R.attr.phoneNumber
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private var btManager: BluetoothManager? = null
    private var btAdapter: BluetoothAdapter? = null
    private var btScanner: BluetoothLeScanner? = null

    var beaconSet: HashSet<Beacon> = HashSet()
    var beaconAdapter: BeaconsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enableScanPermissions()
            enableBluetoothConnectPermissions()
        }

        swipeRefreshLayout = findViewById(R.id.swipeToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            // on below line we are setting is refreshing to false.
            swipeRefreshLayout.isRefreshing = false

            btScanner!!.startScan(leScanCallback)


        }

        initViews()
        setUpBluetoothManager()


    }

    override fun onStart() {
        super.onStart()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        btScanner!!.startScan(leScanCallback)

    }
    override fun onStop() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        btScanner!!.stopScan(leScanCallback)
        super.onStop()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("TAG", "onRequestPermissionsResult: "+requestCode)
        when (requestCode) {

            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) ===
                                PackageManager.PERMISSION_GRANTED)) {

                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }

            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                            android.Manifest.permission.BLUETOOTH_SCAN) ===
                                PackageManager.PERMISSION_GRANTED)) {

                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }

            3 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                            android.Manifest.permission.BLUETOOTH_CONNECT) ===
                                PackageManager.PERMISSION_GRANTED)) {

                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }

        }
    }

    private fun initViews() {

        recyclerView = findViewById(R.id.recyclerView)

        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager

        beaconAdapter = BeaconsAdapter(beaconSet.toList())
        recyclerView.adapter = beaconAdapter

    }

    private fun setUpBluetoothManager() {

        btManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager!!.adapter
        btScanner = btAdapter?.bluetoothLeScanner
        if (btAdapter != null && !btAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableIntent)

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                requestMultiplePermissions.launch(arrayOf(
//                    android.Manifest.permission.BLUETOOTH_SCAN,
//                    android.Manifest.permission.BLUETOOTH_CONNECT))
//            }
//            else{
//                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                requestBluetooth.launch(enableBtIntent)
//            }

            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }
        locationPermission()
      //  checkForLocationPermission()
    }
    private var bluetoothManager = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data

        }
    }


    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {

        }else{
            //deny
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val scanRecord = result.scanRecord
            val beacon = Beacon(result.device.address)

            beacon.manufacturer = result.device.name
            beacon.rssi = result.rssi

            val rssi = result.rssi.toDouble() //The intensity of the received signal
            val tx = result.txPower.toDouble() //The power of the broadcast (Available on Oreo only)
            val distance12 = calculateDistance(tx,rssi)

            if (scanRecord != null) {
                val iBeaconManufactureData = scanRecord.getManufacturerSpecificData(0X004c)

                if (iBeaconManufactureData != null && iBeaconManufactureData.size >= 23) {
                    val iBeaconUUID = Utils.toHexString(iBeaconManufactureData.copyOfRange(2, 18))
                    val major = Integer.parseInt(
                        Utils.toHexString(
                            iBeaconManufactureData.copyOfRange(
                                18,
                                20
                            )
                        ), 16
                    )
                    val minor = Integer.parseInt(
                        Utils.toHexString(
                            iBeaconManufactureData.copyOfRange(
                                20,
                                22
                            )
                        ), 16
                    )

                    var value:Double=((-60 -(result.rssi) )/(10 * 2.0))
                    val distance:Double= Math.pow(value, 10.0);

                    val df = DecimalFormat("#.##")
                    df.roundingMode = RoundingMode.CEILING

                    beacon.type = Beacon.beaconType.iBeacon
                    beacon.uuid = iBeaconUUID
                    beacon.major = major
                    beacon.minor = minor
                   beacon.distance= df.format(distance12)
                    //beacon.distance= df.format(distance)


                    Log.d("TAG", "onScanResult5446: "+distance+"===="+getDistance(distance))
                    Log.e("DINKAR", "iBeaconUUID:$iBeaconUUID major:$major minor:$minor")
                }
            }
            beaconSet.add(beacon)
            (recyclerView.adapter as BeaconsAdapter).updateData(beaconSet.toList())
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("DINKAR", errorCode.toString())
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")

            }
        }

    fun calculateDistance(txPower: Double, rssi: Double): Double {
        val ratio = rssi / txPower
        if (rssi == 0.0) { // Cannot determine accuracy, return -1.
            return -1.0
        } else if (ratio < 1.0) { //default ratio
            return Math.pow(ratio, 10.0)
        }//rssi is greater than transmission strength
        return (0.89976) * Math.pow(ratio, 7.7095) + 0.111
    }



    private fun locationPermission(){
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1
                )

            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1
                )

            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun enableScanPermissions() {
      try{
          if (ContextCompat.checkSelfPermission(
                  this@MainActivity,
                  android.Manifest.permission.BLUETOOTH_SCAN
              ) !=
              PackageManager.PERMISSION_GRANTED
          ) {
              if (ActivityCompat.shouldShowRequestPermissionRationale(
                      this@MainActivity,
                      android.Manifest.permission.BLUETOOTH_SCAN
                  )
              ) {
                  ActivityCompat.requestPermissions(
                      this@MainActivity,
                      arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 2
                  )
              } else {
                  ActivityCompat.requestPermissions(
                      this@MainActivity,
                      arrayOf(android.Manifest.permission.BLUETOOTH_SCAN), 2
                  )
              }
          }
      }catch (e: Exception){
          Log.d("TAG", "enableScanPermissions: "+e)
      }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun enableBluetoothConnectPermissions() {
        try{
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    )
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 3
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 3
                    )
                }
            }
        }catch (e: Exception){
            Log.d("TAG", "enableScanPermissions: "+e)
        }
    }

    fun  getDistance(accuracy: Double):String{
        if (accuracy == -1.0) {
            return "Unknown";
        } else if (accuracy < 1) {
            return "Immediate";
        } else if (accuracy < 3) {
            return "Near";
        } else {
            return "Far";
        }
    }
}
