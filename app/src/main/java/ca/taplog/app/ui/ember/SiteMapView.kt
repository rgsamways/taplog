package ca.taplog.app.ui.ember

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.Site
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

@Composable
fun SiteMapView(
    sites: List<Site>,
    onSiteSelected: (Site) -> Unit
) {
    val sitesWithCoords = remember(sites) {
        sites.filter { it.latitude != null && it.longitude != null }
    }
    val unmapped = sites.size - sitesWithCoords.size

    val mapViewportState = rememberMapViewportState {
        if (sitesWithCoords.isNotEmpty()) {
            val avgLat = sitesWithCoords.map { it.latitude!! }.average()
            val avgLng = sitesWithCoords.map { it.longitude!! }.average()
            setCameraOptions {
                center(Point.fromLngLat(avgLng, avgLat))
                zoom(10.0)
            }
        } else {
            setCameraOptions {
                center(Point.fromLngLat(-79.3832, 43.6532))
                zoom(7.0)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState
        ) {
            MapEffect(Unit) { mapView ->
                mapView.mapboxMap.loadStyle(Style.DARK)
            }
            sitesWithCoords.forEach { site ->
                ViewAnnotation(
                    options = viewAnnotationOptions {
                        geometry(Point.fromLngLat(site.longitude!!, site.latitude!!))
                        allowOverlap(false)
                    }
                ) {
                    SitePin(site = site, onClick = { onSiteSelected(site) })
                }
            }
        }

        if (unmapped > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "$unmapped site${if (unmapped > 1) "s" else ""} not yet geocoded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SitePin(site: Site, onClick: () -> Unit) {
    val pinColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = pinColor),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = site.name,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Canvas(modifier = Modifier.size(width = 12.dp, height = 6.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path, color = pinColor)
        }
    }
}
