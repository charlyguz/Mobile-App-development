package com.example.osm_practice

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.osm_practice.ui.theme.OSMpracticeTheme
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

data class Place(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OSMpracticeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        OSMMap()
                    }
                }
            }
        }
    }
}

@Composable
fun OSMMap() {
    val context = LocalContext.current
    var selectedPlace by remember { mutableStateOf<Place?>(null) }
    val places = remember { definePOIPlaces() }

    var customMarkerCount by remember { mutableStateOf(1) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = selectedPlace?.let { "${it.name} - ${it.description}" } ?: "No place selected",
            modifier = Modifier.padding(16.dp)
        )
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                buildOSMConfiguration(ctx)
                createMapView(ctx, places, customMarkerCount,
                    onPlaceSelected = { selected -> selectedPlace = selected },
                    onCustomMarkerAdded = { customMarkerCount++ }
                )
            }
        )
    }
}

/**
 * Loads the osmdroid configuration with the app's shared preferences
 * and sets the user agent to avoid being blocked by tile servers.
 */
private fun buildOSMConfiguration(context: Context) {
    Configuration.getInstance().load(
        context,
        PreferenceManager.getDefaultSharedPreferences(context)
    )
    Configuration.getInstance().userAgentValue = context.packageName
}

/**
 * Creates and configures the MapView with tile source, initial position,
 * event overlays, and POI markers.
 *
 * The MapEventsOverlay is added FIRST so that POI markers (added after)
 * receive tap events with higher priority. osmdroid dispatches touch events
 * from the last overlay to the first, so markers on top intercept clicks
 * before the generic tap-to-add-marker overlay.
 */
private fun createMapView(
    context: Context,
    places: List<Place>,
    initialCustomCount: Int,
    onPlaceSelected: (Place) -> Unit,
    onCustomMarkerAdded: () -> Unit
): MapView {
    var customMarkerCount = initialCustomCount

    return MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        controller.setZoom(15.0)
        controller.setCenter(GeoPoint(50.0614, 19.9383))

        // 1) Add MapEventsOverlay FIRST — markers added later will have
        //    higher priority for receiving tap events.
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let {
                    val customPlace = Place(
                        "Custom Marker $customMarkerCount",
                        "Lat: ${"%.4f".format(it.latitude)}, Lon: ${"%.4f".format(it.longitude)}",
                        it.latitude,
                        it.longitude
                    )
                    customMarkerCount++
                    onCustomMarkerAdded()
                    createPOIMarker(customPlace, this@apply, onPlaceSelected)
                    invalidate()
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        overlays.add(MapEventsOverlay(mapEventsReceiver))

        // 2) Add predefined POI markers AFTER the events overlay
        //    so they sit on top and intercept taps first.
        places.forEach { place ->
            createPOIMarker(place, this, onPlaceSelected)
        }
    }
}

/**
 * Creates a single map marker for a [Place] and adds it to the [mapView].
 * Clicking the marker selects the place, shows its info window, and
 * displays a Toast with the place name.
 */
private fun createPOIMarker(place: Place, mapView: MapView, onPlaceSelected: (Place) -> Unit) {
    val marker = Marker(mapView)
    marker.position = GeoPoint(place.latitude, place.longitude)
    marker.title = place.name
    marker.snippet = place.description
    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    marker.setOnMarkerClickListener { _, _ ->
        onPlaceSelected(place)
        marker.showInfoWindow()
        Toast.makeText(mapView.context, place.name, Toast.LENGTH_SHORT).show()
        true
    }
    mapView.overlays.add(marker)
}

/**
 * Returns the list of predefined Points of Interest around Kraków.
 */
private fun definePOIPlaces(): List<Place> {
    return listOf(
        Place("AGH University", "Faculty of Space Technologies", 50.0663, 19.9137),
        Place("Main Square", "Historical city center", 50.0614, 19.9383),
        Place("Wawel Castle", "Royal castle", 50.0540, 19.9354)
    )
}