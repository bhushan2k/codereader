package com.demo.yelp.Views

import com.demo.yelp.LocationUtils.MESSENGER_INTENT_KEY
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.yelp.R
import com.demo.yelp.LocationUtils.LocationUpdatesService
import com.demo.yelp.Models.ResponseModel
import com.demo.yelp.Network.DEMO_CITY
import com.demo.yelp.ViewModels.RecyclerActivityViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Main Activity is an entry point of application
 */
class MainActivity : AppCompatActivity() {
    lateinit var recyclerViewAdapter: RecyclerViewAdapter
    lateinit var viewModel: RecyclerActivityViewModel
    var radius = 500
    var page = 1
    lateinit var mLayoutManager: LinearLayoutManager

    private val DEXTER_PERMISSION_SETTING_NAVIGATION_CODE = 101
    private val REQUEST_CHECK_SETTINGS = 0x1
    private lateinit var mHandler: IncomingMessageHandler
    var obj: Location? = null

    var loading = true
    var pastVisiblesItems = 0
    var visibleItemCount:Int = 0
    var totalItemCount:Int = 0

    var isDefaultSelected = true

    private val TAG = this::class.java.name

    /**
     * to initialise all views
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mHandler = IncomingMessageHandler()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)



        initRecyclerView()
        createData()
        initSeekBar()
        initSwipeLayout()


    }
    /**
     * this method is to create overflow menu with radio buttons
     * to get hotel results based on user's current location or default "New York City" location
     *
     * By default it is set to default location
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.actionbar_menu, menu)
        menu!!.getItem(0).isChecked = true
        isDefaultSelected = true
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * this is to call actual action when selected any item from radio buttons on overflow menu created above
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_default -> {
                item.isChecked = !item.isChecked
                isDefaultSelected = true
                Log.i(TAG, "default pressed")
                recyclerViewAdapter.clear()
                recyclerViewAdapter.notifyDataSetChanged()
                page = 1
                viewModel.searchHotels(radius, page, swipeLayout, DEMO_CITY, null, null)
            }
            R.id.action_current -> {
                item.isChecked = !item.isChecked
                isDefaultSelected = false
                Log.i(TAG, "current pressed")
                checkLocationSettings()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * initialise swipe to refresh listener and called api based on overflow menu selection
     * after clearing existing recycler view data and resetting page index to default
     */
    private fun initSwipeLayout() {
        swipeLayout.apply {
            setOnRefreshListener {
                recyclerViewAdapter.clear()
                recyclerViewAdapter.notifyDataSetChanged()
                page = 1
                if (isDefaultSelected) {
                    viewModel.searchHotels(radius, page, this, DEMO_CITY, null, null)
                } else {
                    checkLocationSettings()
                }
            }
        }
    }

    /**
     * this is to initialise recycler view and pagination listener
     *
     * In pagination, we are fetching next page results from API if user has reached end of the list
     * by default 15 results are shown per page. we are also checking the possibility of getting next data if total number of items is not divisible by 15.
     */
    private fun initRecyclerView() {
        mLayoutManager = LinearLayoutManager(this@MainActivity)
        recyclerView.apply {
            layoutManager = mLayoutManager
            recyclerViewAdapter = RecyclerViewAdapter()
            adapter = recyclerViewAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener(){
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    visibleItemCount = mLayoutManager.childCount
                    totalItemCount = mLayoutManager.itemCount
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition()

                    if (loading) {
                        if ((visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                            loading = false
                            if (totalItemCount % 15 == 0) {
                                page++
                                if (isDefaultSelected) {
                                    viewModel.searchHotels(radius, page, swipeLayout, DEMO_CITY, null, null)
                                } else {
                                    if (obj != null) {
                                        viewModel.searchHotels(radius, page, swipeLayout, null, obj!!.latitude.toString(), obj!!.longitude.toString())
                                    }
                                }
                            }
                        }
                    }
                    super.onScrolled(recyclerView, dx, dy)
                }
            })
        }
    }

    /**
     * this is to initialise initial data and get api results whenever it is called using observer, called live data
     */
    private fun createData() {
    viewModel = ViewModelProvider(this).get(RecyclerActivityViewModel::class.java)
        viewModel.getRecyclerListDataObserver().observe(this, Observer<ResponseModel>{
            loading = true
            if(it != null) {
                recyclerViewAdapter.addList(it.businesses)
                recyclerViewAdapter.notifyDataSetChanged()

            } else {
                Toast.makeText(this@MainActivity, "Error in getting data from api.", Toast.LENGTH_LONG).show()
            }

        })
        viewModel.searchHotels(radius, page, swipeLayout, DEMO_CITY, null, null)
    }

    /**
     * initialise seek bar change listener and api is called based on overflow menu selection
     * after clearing existing recycler view data and resetting page index to default
     */
    private fun initSeekBar() {
        seekBar.apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                }

                override fun onStartTrackingTouch(p0: SeekBar?) {

                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    if (p0 != null) {
                        radius = p0.progress
                        selectedRadius.text = p0.progress.toString() + " M"
                        page = 1;
                        recyclerViewAdapter.clear()
                        recyclerViewAdapter.notifyDataSetChanged()
                        if (isDefaultSelected) {
                            viewModel.searchHotels(radius, page, swipeLayout, DEMO_CITY, null, null)
                        } else {
                            checkLocationSettings()
                        }
                    }
                }
            })
        }
    }

    /**
     * this is to check if location service is enabled from settings before checking respective permissions
     */
    private fun checkLocationSettings() {
        Log.i(TAG, "checkLocationSettings called")
        try {
            val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000).setFastestInterval(1000))
                .setAlwaysShow(true)
                .setNeedBle(true)
            LocationServices
                .getSettingsClient(this@MainActivity)
                .checkLocationSettings(builder.build())
                .addOnSuccessListener {
                    requestPermissionWithDexterForLocation()
                }
                .addOnFailureListener { e ->
                    val statusCode = (e as ApiException).statusCode
                    when (statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                            try {
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                            } catch (sie: IntentSender.SendIntentException) {
                                    Log.i(TAG, "PendingIntent unable to execute request.")
                            }
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                            Log.e(TAG, errorMessage)
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * if location setting is enabled then location permissions are checked here using dexter library which is easy to use
     * we need FINE and COARSE permission to get user location
     */
    fun requestPermissionWithDexterForLocation() {
//        Log.i(TAG, "requestPermissionWithDexterForLocation")
        var permissions = emptyArray<String>()
        permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)
        Dexter.withContext(this@MainActivity)
            .withPermissions(
                *permissions
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                            Log.i(TAG, "requestPermissionWithDexterForLocation all permission accepted")
                        startJobService()
                    } else if (report.isAnyPermissionPermanentlyDenied) {
                            Log.i(TAG, "requestPermissionWithDexterForLocation single permission permanently denied")
                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setCancelable(false)
                        builder.setTitle("Need Permissions")
                        builder.setMessage("Required permissions you previously denied are necessary to use app's core functionality. Please grant them all in app settings and allow us to provide you a best experience. Make sure you toggle them all on.")
                        builder.setPositiveButton("SETTINGS") { dialog, which ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivityForResult(intent, DEXTER_PERMISSION_SETTING_NAVIGATION_CODE)
                        }
                        builder.setNegativeButton("Exit") { dialog, which ->
                            dialog.dismiss()
                            finish()
                        }
                        builder.show()
                    } else {
                            Log.i(TAG, "requestPermissionWithDexterForLocation single permission denied")
                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setCancelable(false)
                        builder.setTitle("Need Permissions")
                        builder.setMessage("App needs certain permissions to be granted to use its core functionality. Please grant them all and allow us to provide you a best experience.")
                        builder.setPositiveButton("RE-TRY") { dialog, which ->
                            requestPermissionWithDexterForLocation()
                        }
                        builder.setNegativeButton("Exit") { dialog, which ->
                            dialog.dismiss()
//                            finish()
                        }
                        builder.show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken) {
                        Log.i(TAG, "requestPermissionWithDexterForLocation onPermissionRationaleShouldBeShown")
                    token.continuePermissionRequest()
                }

            }).check()
    }

    /**
     * if all permissions are enabled then we start service which will get user's location
     */
    private fun startJobService() {

        if (!isServiceRunning(LocationUpdatesService::class.java.name)) {
            val startServiceIntent = Intent(applicationContext, LocationUpdatesService::class.java)
            val messengerIncoming = Messenger(mHandler)
            startServiceIntent.putExtra(MESSENGER_INTENT_KEY, messengerIncoming)
            startService(startServiceIntent)
        }
    }

    /**
     * this method is used to stop location service after usage
     */
    private fun stopJobService() {
        if (isServiceRunning(LocationUpdatesService::class.java.name)) {
            val stopIntentService = Intent(applicationContext, LocationUpdatesService::class.java)
            stopService(stopIntentService)
        }
    }

    /**
     * this method is used to check if location service is already in running
     */
    private fun isServiceRunning(className: String): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (className == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * This is handler which will provide location result in its callback method from location service
     */
    @SuppressLint("HandlerLeak")
    private inner class IncomingMessageHandler: Handler() {
        var TAG: String = "IncomingMessageHandler"
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                LocationUpdatesService.LOCATION_MESSAGE -> {
//                    Log.i(TAG, "msg received")
                    mHandler.removeCallbacks {  }
                    val stopIntentService = Intent(applicationContext, LocationUpdatesService::class.java)
                    stopService(stopIntentService)
                    if (msg.obj != null) {
                        obj = msg.obj as Location
//                        Log.i(TAG, "$obj")
//                        Log.i(TAG, "co-ordinates found")
//                        Log.i(TAG, (msg.obj as Location).toString())
                        recyclerViewAdapter.clear()
                        recyclerViewAdapter.notifyDataSetChanged()
                        page = 1
                        viewModel.searchHotels(radius, page, swipeLayout, null, obj!!.latitude.toString(), obj!!.longitude.toString())
                    } else {
                        obj = null
//                        Log.i(TAG, "incoming handler 0")
                        recyclerViewAdapter.clear()
                        recyclerViewAdapter.notifyDataSetChanged()
                        page = 1
                        Toast.makeText(this@MainActivity, "Could not get your current location!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * this method is to check location permission change and location setting changes from outside app
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DEXTER_PERMISSION_SETTING_NAVIGATION_CODE) {
            requestPermissionWithDexterForLocation()
        } else if (requestCode == REQUEST_CHECK_SETTINGS) {
//            Log.i(TAG, "onActivityResult of DashBoardActivity.. RCS")
            if (resultCode == Activity.RESULT_OK) {
//                Log.i(TAG, "onActivityResult of DashBoardActivity.. RCS OK")
                Toast.makeText(this, "Thank you for allowing us to use location services.", Toast.LENGTH_LONG).show()
                requestPermissionWithDexterForLocation()
            } else if (resultCode == Activity.RESULT_CANCELED) {
//                Log.i(TAG, "onActivityResult of DashBoardActivity.. RCS CN")
                recyclerViewAdapter.clear()
                recyclerViewAdapter.notifyDataSetChanged()
                page = 1
                Toast.makeText(this, "App needs location services. Please turn it on and retry.", Toast.LENGTH_LONG).show()
            } else {
                recyclerViewAdapter.clear()
                recyclerViewAdapter.notifyDataSetChanged()
                page = 1
                Toast.makeText(this, "Some error occurred, please retry.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * this is to stop location service if running and stop initialised handler for getting location
     */
    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        stopJobService()
    }
}