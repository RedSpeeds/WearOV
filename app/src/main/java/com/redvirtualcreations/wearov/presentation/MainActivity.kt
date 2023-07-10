package com.redvirtualcreations.wearov.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissValue
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.curvedText
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.edgeSwipeToDismiss
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.ColorScheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LastLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.redvirtualcreations.wearov.R
import com.redvirtualcreations.wearov.data.ApiManager
import com.redvirtualcreations.wearov.jsonObjects.VertrektijdenApi
import com.redvirtualcreations.wearov.presentation.theme.WearOVTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    lateinit var locationCallback: LocationCallback
    lateinit var locationProvider: FusedLocationProviderClient
    var location: MutableLiveData<LatLon> = MutableLiveData()
    var apiDataLive = location.switchMap { loc ->
        liveData(context = this.lifecycleScope.coroutineContext + Dispatchers.IO) {
            emit(apiManager.getApiInfo(loc))
        }
    }
    private val apiManager = ApiManager()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            WearApp(this, { ActivityCompat.finishAffinity(this) })
        }
    }

    fun hasTrain(): Boolean {
        if (apiDataLive.value == null) return false
        return apiDataLive.value!!.TRAIN.size > 0
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
                }.addOnFailureListener {
                    Log.e("Location_error", "${it.message}")
                }
            }
        }
        locationCallback.let {
            val locationRequest: LocationRequest =
                LocationRequest.Builder(TimeUnit.SECONDS.toMillis(30))
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(15)).build()
            locationProvider.requestLocationUpdates(locationRequest, it, Looper.getMainLooper())
        }
        updateNow()
    }

    fun stopLocationUpdates() {
        locationProvider.removeLocationUpdates(locationCallback)
    }

    fun locationUpdated(location: LatLon) {
        this.location.value = location
    }

    @SuppressLint("MissingPermission")
    fun updateNow() {
        locationProvider.getLastLocation(
            LastLocationRequest.Builder().build()
        ).addOnSuccessListener { loc ->
            loc?.let {
                locationUpdated(LatLon(loc.latitude, loc.longitude))
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun getTransport(pagerState: PagerState): String {
        if (hasTrain() && pagerState.currentPage == 0) {
            return this.getString(R.string.trains)
        }
        return if (hasTrain() && pagerState.currentPage == 1) {
            return this.getString(R.string.busses)
        } else {
            return this.getString(R.string.busses)
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
private class HorizontalPagerState(state: PagerState) : PageIndicatorState {
    override val pageCount = state.pageCount
    override val pageOffset = state.currentPageOffsetFraction
    override val selectedPage = state.currentPage
}

//region Compose garbage
@OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun WearApp(activity: MainActivity, onDismissed: () -> Unit = {}) {
    val apiData = activity.apiDataLive.observeAsState()
    val pagerState =
        rememberPagerState(pageCount = { if (apiData.value == null) 1 else if (apiData.value!!.TRAIN.size > 0) 2 else 1 })
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
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
            TimeTextDefaults.timeTextStyle(color = androidx.wear.compose.material3.MaterialTheme.colorScheme.primary)
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
                            TimeText(
                                startLinearContent = {
                                    Text(
                                        text = activity.getTransport(pagerState),
                                        style = leadingTextStyle
                                    )
                                },
                                startCurvedContent = {
                                    curvedText(
                                        text = activity.getTransport(pagerState = pagerState),
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
                            var train: Boolean = (page == 0 && activity.hasTrain())
                            apiData.value?.let {
                                TransitPage(
                                    train = train,
                                    api = it,
                                    listState,
                                    coroutineScope,
                                    focusRequester
                                )
                            }
                        }
                    }
                } else {
                    val allPermissionsRevoked =
                        locationPermissionsState.permissions.size == locationPermissionsState.revokedPermissions.size
                    Alert(
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_location_on_24),
                                contentDescription = stringResource(R.string.location)
                            )
                        },
                        title = { Text(stringResource(R.string.permissions_required)) },
                        message = {
                            Text(text = stringResource(R.string.location_permission_explanation))
                        }) {
                        item {
                            Chip(
                                onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                                colors = ChipDefaults.primaryChipColors(),
                                label = {
                                    Text(
                                        text = stringResource(R.string.request_permissions_button)
                                    )
                                })
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransitPage(
    train: Boolean,
    api: VertrektijdenApi,
    listState: ScalingLazyListState,
    coroutine: CoroutineScope,
    focusRequester: FocusRequester
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                    val departureTime = LocalDateTime.parse(
                        departure.PlannedDeparture,
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    )
                    val departureLabel = StringBuilder().append(departure.Destination)
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
                                contentDescription = stringResource(R.string.bus)
                            )
                        },
                        secondaryLabel = {
                            Text(text = "${departureTime.format(DateTimeFormatter.ofPattern("hh:mm"))} "); if (departure.Delay > 0) {
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
                for (btmf in api.BTMF) {
                    if (btmf.StationInfo.Distance < 0.2 && btmf.Departures.size > 0) {
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
                            val departureTime = LocalDateTime.parse(
                                departure.ExpectedDeparture,
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME
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

//endregion

data class LatLon(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)