package com.redvirtualcreations.wearov.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.curvedText
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material3.MaterialTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.FirebaseApp
import com.redvirtualcreations.wearov.R
import com.redvirtualcreations.wearov.data.ApiManager
import com.redvirtualcreations.wearov.jsonObjects.VertrektijdenApi
import com.redvirtualcreations.wearov.presentation.theme.WearOVTheme
import com.vmadalin.easypermissions.EasyPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var locationCallback: LocationCallback
    lateinit var locationProvider: FusedLocationProviderClient
    var location: MutableLiveData<LatLon> = MutableLiveData()

    private var isLocationListening = true;
    var apiDataLive = location.switchMap { loc ->
        liveData(context = this.lifecycleScope.coroutineContext + Dispatchers.IO) {
            val result = apiManager.getApiInfo(loc);
            emit(result)
            if(result?.apiError ?: true){
                stopLocationUpdates()
            }
        }
    }
    private val apiManager = ApiManager()
    var isRefreshing = false
    var hasLoaded = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        FirebaseApp.initializeApp(this)
        setContent {
            WearApp(this) { ActivityCompat.finishAffinity(this) }
        }
        lifecycleScope.launch {
            while (apiDataLive.value == null) {
                updateNow()
                delay(3000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNow()
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        locationCallback = object : LocationCallback() {
            @SuppressLint("MissingPermission")
            override fun onLocationResult(p0: LocationResult) {
                locationProvider.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        val lat = loc.latitude
                        val long = loc.longitude
                        location.value = LatLon(lat, long)

                    }
                    isLocationListening = true
                }.addOnFailureListener {
                    Log.e("Location_error", "${it.message}")
                }
            }
        }
        locationCallback.let {
            val locationRequest: LocationRequest =
                LocationRequest.Builder(TimeUnit.SECONDS.toMillis(60))
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(50)).build()
            locationProvider.requestLocationUpdates(locationRequest, it, Looper.getMainLooper())
        }
        updateNow()
    }

    fun stopLocationUpdates() {
        locationProvider.removeLocationUpdates(locationCallback)
        isLocationListening = false
    }

    fun resumeLocationUpdates() {
        if (!isLocationListening) {
            startLocationUpdates()
        }
    }

    private fun locationUpdated(location: LatLon) {
        this.location.value = location
    }

    @SuppressLint("MissingPermission")
    fun updateNow() {
        isRefreshing = true
        if (EasyPermissions.hasPermissions(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            locationProvider.getLastLocation(
                LastLocationRequest.Builder().build()
            ).addOnSuccessListener { loc ->
                loc?.let {
                    locationUpdated(LatLon(loc.latitude, loc.longitude))
                    isRefreshing = false
                }
            }
        }
    }


}


private class HorizontalPagerState(state: PagerState) : PageIndicatorState {
    override val pageCount = state.pageCount
    override val pageOffset = state.currentPageOffsetFraction
    override val selectedPage = state.currentPage
}

//region Compose

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WearApp(activity: MainActivity, onDismissed: () -> Unit = {}) {
    val apiData = activity.apiDataLive.observeAsState()
    val pagerState =
        rememberPagerState(pageCount = { if (apiData.value == null) 1 else if (apiData.value!!.TRAIN.size > 0) 2 else 1 })
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    val swipeDismissState = rememberSwipeToDismissBoxState()
    val listState = rememberScalingLazyListState()
    val focusRequester = remember {
        FocusRequester()
    }
    val coroutineScope = rememberCoroutineScope()


    WearOVTheme {
        /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
         * version of LazyColumn for wear devices with some added features. For more information,
         * see d.android.com/wear/compose.
         */
        val leadingTextStyle =
            TimeTextDefaults.timeTextStyle(color = MaterialTheme.colorScheme.primary)
        SwipeToDismissBox(state = swipeDismissState) { bg ->
            if (!bg) {
                if (locationPermissionsState.allPermissionsGranted) {
                    DisposableEffect(key1 = activity.locationProvider) {
                        activity.startLocationUpdates()
                        onDispose {
                            activity.stopLocationUpdates()
                        }
                    }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        timeText = {
                            val textString =
                                getScaffoldLabel(pagerState = pagerState, api = apiData.value)
                            TimeText(
                                startLinearContent = {
                                    Text(
                                        text = textString,
                                        style = leadingTextStyle
                                    )
                                },
                                startCurvedContent = {

                                    curvedText(
                                        text = textString,
                                        style = CurvedTextStyle(leadingTextStyle)
                                    )
                                })
                        },
                        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
                        pageIndicator = {
                            if (pagerState.pageCount > 1) {
                                HorizontalPageIndicator(
                                    pageIndicatorState = HorizontalPagerState(
                                        pagerState
                                    )
                                )
                            }
                        },
                        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.edgeSwipeToDismiss(swipeDismissState)
                        ) { page ->
                            val train: Boolean = (page == 0 && hasTrain(apiData.value))
                            apiData.value?.let {
                                TransitPage(
                                    train = train,
                                    api = it,
                                    listState,
                                    coroutineScope,
                                    focusRequester,
                                    activity
                                )
                            }
                        }
                    }
                } else {
                    @Suppress("unused") val allPermissionsRevoked =
                        locationPermissionsState.permissions.size == locationPermissionsState.revokedPermissions.size
                    Alert(
                        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 52.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_location_on_24),
                                contentDescription = stringResource(R.string.location)
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.permissions_required),
                                textAlign = TextAlign.Center,

                            )
                        },
                        message = {
                            Text(
                                text = stringResource(R.string.location_permission_explanation),
                                textAlign = TextAlign.Center
                            )
                        }) {
                        item {
                            Chip(
                                onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                                colors = ChipDefaults.primaryChipColors(),
                                label = {
                                    Text(
                                        text = stringResource(R.string.request_permissions_button)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        LaunchedEffect(swipeDismissState.currentValue) {
            if (swipeDismissState.currentValue == SwipeToDismissValue.Dismissed) {
                onDismissed()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitPage(
    train: Boolean,
    api: VertrektijdenApi,
    listState: ScalingLazyListState,
    coroutine: CoroutineScope,
    focusRequester: FocusRequester,
    activity: MainActivity
) {
    val dateFormat = DateFormat.getTimeFormat(LocalContext.current)
    PullToRefreshBox(modifier = Modifier.fillMaxSize(), isRefreshing = activity.isRefreshing, onRefresh = {activity.updateNow(); activity.resumeLocationUpdates()}) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .onRotaryScrollEvent {
                    coroutine.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            autoCentering = AutoCenteringParams()
        ) {
            if (api.apiError) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 0.dp, 5.dp)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            painter = painterResource(id = R.drawable.baseline_signal_wifi_connected_no_internet_4_24),
                            contentDescription = stringResource(R.string.network_error),
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        if(isInternetAvailable(activity)){
                            Text(
                                text = stringResource(R.string.server_down),
                                textAlign = TextAlign.Center
                            )
                        }else {
                            Text(
                                text = stringResource(R.string.noInternetError),
                                textAlign = TextAlign.Center
                            )
                        }
                        Button({activity.updateNow(); activity.resumeLocationUpdates()}) { Icon(painter = painterResource(R.drawable.baseline_autorenew_24), contentDescription = "Refresh") }
                    }
                }
                return@ScalingLazyColumn
            }
            if (train && api.TRAIN.size > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 0.dp, 5.dp)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.Center
                    ) {

                        Icon(
                            painter = painterResource(id = R.drawable.baseline_train_24),
                            contentDescription = stringResource(R.string.trains)
                        )

                        Text(
                            modifier = Modifier.basicMarquee(),
                            text = api.TRAIN[0].StationInfo.StopName,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                items(api.TRAIN[0].Departures.size) { index ->
                    val departure = api.TRAIN[0].Departures[index]
                    val departureTime = dateFormat.format(
                        Date.from(
                            LocalDateTime.parse(
                                departure.PlannedDeparture,
                                DateTimeFormatter.ISO_OFFSET_DATE_TIME
                            ).atZone(
                                ZoneId.systemDefault()
                            ).toInstant()
                        )
                    )
                    val departureLabel = StringBuilder().append(departure.Destination)
                    @Suppress("UselessCallOnNotNull")
                    if (!departure.Via.isNullOrEmpty()) {
                        departureLabel.append(stringResource(R.string.via)).append(departure.Via)
                    }
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                modifier = Modifier.basicMarquee(),
                                text = departureLabel.toString()
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_train_24),
                                contentDescription = stringResource(R.string.train)
                            )
                        },
                        secondaryLabel = {
                            Text(text = "$departureTime "); if (departure.Delay > 0) {
                            Text(
                                text = "+${departure.Delay} ", color = Color.Red
                            )
                        }
                            Row(horizontalArrangement = Arrangement.End) {
                                Text(text = buildString {
                                    append(stringResource(R.string.platform))
                                    append(departure.Platform)
                                })
                            }
                        },
                        onClick = {})
                }
            } else if (api.BTMF.size > 0) {
                var foundTimes = false
                for (btmf in api.BTMF) {
                    if (btmf.Departures.size > 0) {
                        foundTimes = true
                        item {
                            Row(
                                modifier = Modifier
                                    .padding(0.dp, 0.dp, 0.dp, 5.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.Center
                            ) {

                                Icon(
                                    painter = painterResource(id = R.drawable.baseline_directions_bus_24),
                                    contentDescription = stringResource(R.string.busses)
                                )

                                Text(
                                    modifier = Modifier.basicMarquee(),
                                    text = btmf.StationInfo.StopName,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        val size = if (btmf.Departures.size > 5) 5 else btmf.Departures.size
                        items(size) { index ->
                            val departure = btmf.Departures[index]
                            val departureTime = dateFormat.format(
                                Date.from(
                                    LocalDateTime.parse(
                                        departure.PlannedDeparture,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                    ).atZone(
                                        ZoneId.systemDefault()
                                    ).toInstant()
                                )
                            )
                            Chip(
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(text = "${departure.LineNumber}: "); Text(
                                    modifier = Modifier.basicMarquee(),
                                    text = departure.LineName
                                )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_directions_bus_24),
                                        contentDescription = stringResource(R.string.bus)
                                    )
                                },
                                secondaryLabel = {
                                    Text(
                                        text = "${
                                            departureTime.format(
                                                DateTimeFormatter.ofPattern("hh:mm")
                                            )
                                        } "
                                    )
                                },
                                onClick = {})
                        }
                    }
                }
                if (!foundTimes) {
                    item {
                        Row(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 0.dp, 5.dp)
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.Center
                        ) {

                            Icon(
                                painter = painterResource(id = R.drawable.baseline_directions_bus_24),
                                contentDescription = stringResource(R.string.bus)
                            )

                            Text(
                                modifier = Modifier.basicMarquee(),
                                text = "No departure times found",
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 0.dp, 5.dp)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.Center
                    ) {

                        Icon(
                            painter = painterResource(id = R.drawable.baseline_location_on_24),
                            contentDescription = stringResource(R.string.location)
                        )

                        Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(id = R.string.no_station),
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
fun getScaffoldLabel(pagerState: PagerState, api: VertrektijdenApi?): String {
    if (api == null) {
        return stringResource(R.string.loading)
    }
    if (api.apiError) {
        return stringResource(R.string.no_connection)
    }
    if (api.TRAIN.size == 0 && api.BTMF.size == 0) {
        return stringResource(R.string.no_data)
    }
    if (hasTrain(api) && pagerState.currentPage == 0) {
        return stringResource(id = R.string.trains)
    }

    return if (hasTrain(api) && pagerState.currentPage == 1) {
        stringResource(R.string.busses)
    } else {
        stringResource(R.string.busses)
    }
}

@Composable
fun hasTrain(api: VertrektijdenApi?): Boolean {
    val len = api?.TRAIN?.size ?: 0
    return len > 0
}

fun isInternetAvailable(context: Context): Boolean{
    var result: Boolean
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.activeNetwork ?: return false
    val actNw  = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
    result = when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
    return result
}

//endregion

data class LatLon(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)