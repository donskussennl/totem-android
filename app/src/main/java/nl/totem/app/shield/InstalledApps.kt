package nl.totem.app.shield

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/**
 * De lijst met apps die je kunt blokkeren.
 *
 * Op iOS deed `FamilyActivityPicker` dit: Apple liet de apps zien en gaf ons
 * alleen versleutelde verwijzingen terug, zonder namen. Op Android mag een app
 * de geïnstalleerde apps gewoon opvragen, dus bouwen we de kiezer zelf — met
 * echte namen en iconen. Dat is voor de gebruiker prettiger.
 *
 * Vanaf Android 11 moet je in het manifest verklaren dat je alle apps wilt
 * zien; dat staat er met `QUERY_ALL_PACKAGES` en het `<queries>`-blok.
 */
object InstalledApps {

    data class Item(
        val packageName: String,
        val label: String,
        val icon: Drawable?
    )

    /**
     * Alle apps met een startpictogram, op naam gesorteerd. Systeem-apps zonder
     * launcher (diensten, providers) laten we weg, en Totem zelf ook.
     */
    fun load(context: Context): List<Item> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolved = pm.queryIntentActivities(intent, 0)
        // De launcher, de instellingen, de telefoon-app en het toetsenbord
        // laten we niet eens zien; die mag je niet kunnen blokkeren.
        val verboden = Onaantastbaar.voor(context)

        return resolved
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName !in verboden }
            .map { info: ApplicationInfo ->
                Item(
                    packageName = info.packageName,
                    label = runCatching { pm.getApplicationLabel(info).toString() }
                        .getOrDefault(info.packageName),
                    icon = runCatching { pm.getApplicationIcon(info) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Naam van één pakket, voor als je alleen de pakketnaam hebt bewaard. */
    fun label(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun icon(context: Context, packageName: String): Drawable? = runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()
}
