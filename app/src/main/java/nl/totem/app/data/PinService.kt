package nl.totem.app.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/**
 * Bewaart de pincode van de strikte modus als hash.
 *
 * Op iOS stond dit in de sleutelhanger, die het verwijderen van de app
 * overleeft. Android heeft dat niet: alles van een app gaat weg als de app
 * weggaat. Wat wél kan is de hash versleuteld opslaan met een sleutel uit de
 * hardware-keystore, zodat je hem niet uit een back-up kunt vissen.
 *
 * Wil je dat de pincode een herinstallatie overleeft, dan is Auto Backup naar
 * Google Drive de enige weg. Die staat in `backup_rules.xml` bewust uit voor
 * dit bestand — anders zou een back-up terugzetten ook de blokkade omzeilen.
 */
object PinService {

    private const val FILE = "totem.pin"
    private const val KEY = "strict"

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = runCatching {
            val master = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                FILE,
                master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.onFailure {
            Log.e("Totem", "Versleutelde opslag mislukt, val terug op gewone opslag", it)
        }.getOrElse {
            context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        }
    }

    val isSet: Boolean get() = storedHash() != null

    fun set(pin: String) {
        prefs?.edit()?.putString(KEY, hash(pin))?.apply()
    }

    /** Zonder ingestelde pincode is elke invoer goed, net als op iOS. */
    fun verify(pin: String): Boolean {
        val stored = storedHash() ?: return true
        return stored == hash(pin)
    }

    fun clear() {
        prefs?.edit()?.remove(KEY)?.apply()
    }

    private fun storedHash(): String? = prefs?.getString(KEY, null)

    private fun hash(pin: String): String {
        val salted = "totem.v1.$pin"
        val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
