package com.example.osm_practice

import android.os.Bundle
import android.preference.PreferenceManager
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
                Configuration.getInstance().load(
                    ctx,
                    PreferenceManager.getDefaultSharedPreferences(ctx)
                )
                Configuration.getInstance().userAgentValue = ctx.packageName
                
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(50.0614, 19.9383))

                    // Add markers for predefined POIs
                    places.forEach { place ->
                        createPOIMarker(place, this) { selected ->
                            selectedPlace = selected
                        }
                    }

                    // Add MapEventsOverlay for custom markers on click
                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let {
                                val customPlace = Place(
                                    "Custom Marker $customMarkerCount",
                                    "Lat: ${it.latitude}, Lon: ${it.longitude}",
                                    it.latitude,
                                    it.longitude
                                )
                                customMarkerCount++
                                createPOIMarker(customPlace, this@apply) { selected ->
                                    selectedPlace = selected
                                }
                                invalidate() // Redraw map to show new marker
                            }
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean {
                            return false
                        }
                    }
                    overlays.add(MapEventsOverlay(mapEventsReceiver))
                }
            }
        )
    }
}

private fun definePOIPlaces(): List<Place> {
    return listOf(
        Place("AGH University", "Faculty of Space Technologies", 50.0663, 19.9137),
        Place("Main Square", "Historical city center", 50.0614, 19.9383),
        Place("Wawel Castle", "Royal castle", 50.0540, 19.9354)
    )
}

private fun createPOIMarker(place: Place, mapView: MapView, onPlaceSelected: (Place) -> Unit) {
    val marker = Marker(mapView)
    marker.position = GeoPoint(place.latitude, place.longitude)
    marker.title = place.name
    marker.snippet = place.description
    marker.setOnMarkerClickListener { _, _ ->
        onPlaceSelected(place)
        marker.showInfoWindow()
        android.widget.Toast.makeText(mapView.context, place.name, android.widget.Toast.LENGTH_SHORT).show()
        true
    }
    mapView.overlays.add(marker)
}