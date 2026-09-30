package nl.totem.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.totem.app.R
import nl.totem.app.location.LocationService
import nl.totem.app.model.Place
import kotlin.math.roundToInt

/**
 * Kies de plek waar een schema op locatie begint: zoek op naam of adres
 * ("Basic-Fit Enschede"), of neem je huidige locatie. De naam vult zich
 * vanzelf.
 *
 * Bewust zonder kaart: een kaart vraagt een API-sleutel of internettoegang
 * voor de app. Het zoeken loopt via de Geocoder van Android zelf.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerSheet(
    initial: Place?,
    onDismiss: () -> Unit,
    onChoose: (Place) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoek by remember { mutableStateOf("") }
    var resultaten by remember { mutableStateOf<List<Address>>(emptyList()) }
    var nietsGevonden by remember { mutableStateOf(false) }
    var bezig by remember { mutableStateOf(false) }
    var gekozen by remember { mutableStateOf(initial) }
    var straal by remember { mutableFloatStateOf((initial?.radius ?: 150.0).toFloat()) }
    val standaardNaam = stringResource(R.string.loc_default_name)

    fun zoeken() {
        if (zoek.isBlank()) return
        bezig = true
        scope.launch {
            resultaten = withContext(Dispatchers.IO) { geocode(context, zoek) }
            nietsGevonden = resultaten.isEmpty()
            bezig = false
        }
    }

    val toegang = rememberLocationAccess()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.loc_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = zoek,
                onValueChange = { zoek = it; nietsGevonden = false },
                placeholder = { Text(stringResource(R.string.loc_search_hint)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { zoeken() }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.loc_search))
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { zoeken() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            OutlinedButton(
                onClick = {
                    if (!toegang.foreground) {
                        toegang.request()
                        return@OutlinedButton
                    }
                    bezig = true
                    huidigeLocatie(context) { plek ->
                        bezig = false
                        if (plek != null) {
                            gekozen = plek.copy(radius = straal.toDouble())
                            resultaten = emptyList()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Text(stringResource(R.string.loc_current), modifier = Modifier.padding(start = 8.dp))
            }

            if (bezig) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp).size(24.dp))
            }
            if (nietsGevonden) {
                Text(
                    stringResource(R.string.loc_no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            resultaten.forEach { adres ->
                val naam = adresNaam(adres, standaardNaam)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            gekozen = Place(naam, adres.latitude, adres.longitude, straal.toDouble())
                            resultaten = emptyList()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(naam, fontWeight = FontWeight.Medium)
                        adres.getAddressLine(0)?.let {
                            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            gekozen?.let { plek ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(plek.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
                }

                Text(
                    stringResource(R.string.loc_radius, straal.roundToInt()),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Slider(
                    value = straal,
                    onValueChange = { straal = (it / 50f).roundToInt() * 50f },
                    valueRange = Place.MIN_RADIUS.toFloat()..Place.MAX_RADIUS.toFloat()
                )
                Text(
                    stringResource(R.string.loc_radius_help),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!toegang.background) {
                LocationPermissionCard(toegang, modifier = Modifier.padding(top = 18.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Button(
                    enabled = gekozen != null,
                    onClick = { gekozen?.let { onChoose(it.copy(radius = straal.toDouble())) } }
                ) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

/** Een herkenbare naam: liefst de plek zelf, anders straat en huisnummer. */
private fun adresNaam(adres: Address, standaard: String): String {
    val plek = adres.featureName?.takeIf { it.isNotBlank() && !it.all { c -> c.isDigit() } }
    val straat = listOfNotNull(adres.thoroughfare, adres.subThoroughfare).joinToString(" ").ifBlank { null }
    return plek ?: straat ?: adres.locality ?: standaard
}

@Suppress("DEPRECATION")
private fun geocode(context: Context, zoek: String): List<Address> = runCatching {
    Geocoder(context, context.resources.configuration.locales[0]).getFromLocationName(zoek, 5)
}.getOrNull().orEmpty()

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
private fun huidigeLocatie(context: Context, klaar: (Place?) -> Unit) {
    runCatching {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) { klaar(null); return@addOnSuccessListener }
                val adres = runCatching {
                    Geocoder(context, context.resources.configuration.locales[0])
                        .getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
                }.getOrNull()
                val naam = adres?.let { adresNaam(it, context.getString(R.string.loc_default_name)) }
                    ?: context.getString(R.string.loc_default_name)
                klaar(Place(naam, loc.latitude, loc.longitude))
            }
            .addOnFailureListener { klaar(null) }
    }.onFailure { klaar(null) }
}

// MARK: - Toestemming

/** Hoe het er met de locatietoestemming voor staat, en een manier om hem te vragen. */
class LocationAccess(
    val foreground: Boolean,
    val background: Boolean,
    val request: () -> Unit
)

/**
 * Eerst "tijdens gebruik", daarna "altijd toestaan". Vanaf Android 11 kan dat
 * tweede niet meer in een venster; dan sturen we de gebruiker naar de
 * instellingen van de app, waar hij "Altijd toestaan" kiest.
 */
@Composable
fun rememberLocationAccess(): LocationAccess {
    val context = LocalContext.current
    val meting = terugkeerTeller()
    var hertel by remember { mutableStateOf(0) }
    val voorgrond = remember(meting, hertel) { LocationService.hasForegroundAccess(context) }
    val achtergrond = remember(meting, hertel) { LocationService.hasBackgroundAccess(context) }

    val vraagVoorgrond = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { hertel++ }
    val vraagAchtergrond = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hertel++ }

    return LocationAccess(voorgrond, achtergrond) {
        when {
            !voorgrond -> vraagVoorgrond.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !achtergrond ->
                runCatching { vraagAchtergrond.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                    .onFailure { openAppSettings(context) }
            else -> openAppSettings(context)
        }
    }
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Melding als Totem niet op de achtergrond bij je locatie mag. */
@Composable
fun LocationPermissionCard(access: LocationAccess, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFF9500).copy(alpha = 0.12f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOff, contentDescription = null, tint = Color(0xFFE08A00))
            Text(
                stringResource(R.string.loc_perm_title),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            stringResource(R.string.loc_perm_body),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        TextButton(onClick = access.request) {
            Text(stringResource(if (access.foreground) R.string.loc_perm_settings else R.string.loc_perm_button))
        }
    }
}
