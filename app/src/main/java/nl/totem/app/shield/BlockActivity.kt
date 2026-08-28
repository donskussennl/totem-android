package nl.totem.app.shield

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import nl.totem.app.R
import nl.totem.app.data.SharedStore
import nl.totem.app.nfc.NfcService
import nl.totem.app.schedule.SessionEngine
import nl.totem.app.ui.formatElapsed
import java.lang.ref.WeakReference

/**
 * Het scherm dat over een geblokkeerde app heen komt.
 *
 * Dit is de tegenhanger van `ShieldConfigurationExtension` op iOS: dezelfde
 * donkere Totem, dezelfde tekst. Het verschil is dat Apple dat scherm zelf
 * tekende, en wij het hier als gewone activiteit openen.
 *
 * De terugknop doet niets. Wegkomen doe je met de startknop — en probeer je de
 * app opnieuw te openen, dan komt dit scherm er meteen weer overheen.
 */
class BlockActivity : ComponentActivity() {

    private lateinit var nfc: NfcService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        huidige = WeakReference(this)
        SharedStore.init(this)
        nfc = NfcService(this)

        // Ook boven het vergrendelscherm tonen, zodat een blokkade niet is te
        // omzeilen door het scherm even uit en weer aan te zetten.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Bewust leeg: terug zou je terugbrengen in de geblokkeerde app.
            }
        })

        toon(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        toon(intent)
    }

    private fun toon(intent: Intent) {
        val modeName = intent.getStringExtra(EXTRA_MODE_NAME) ?: "Totem"
        val startedAt = intent.getLongExtra(EXTRA_STARTED_AT, 0L)
        setContent { BlockScreen(modeName = modeName, startedAt = startedAt) }
    }

    /** Loopt de blokkade nog wel? Zo niet, dan hoort dit scherm weg te zijn. */
    override fun onResume() {
        super.onResume()
        if (!ShieldService.isBlocking) {
            finish()
            return
        }

        // Ook hier de lezer aanzetten. Dit is juist hét moment waarop je je
        // Totem pakt: je wilde een app openen, je krijgt dit scherm, en dan wil
        // je hem aantikken om eraf te komen. Zonder dit vangt Android de tik
        // zelf op en meldt hij "Lege tag" — terwijl Totem er vlak achter staat
        // te wachten.
        android.util.Log.i(NfcService.TAG, "BlockActivity.onResume — lezer wordt aangezet")
        nfc.start { result ->
            android.util.Log.i(NfcService.TAG, "BlockActivity kreeg een tik binnen")
            val lezing = result.getOrNull() ?: return@start
            if (!hoortBijMijnTotem(lezing.uid)) return@start

            SessionEngine.stop(this, bySchedule = false)

            // Terug naar Totem, niet naar het startscherm. Dit scherm draait in
            // een eigen taak; sluiten we alleen onszelf, dan valt de gebruiker
            // terug op waar hij vandaan kwam — meestal de launcher, omdat de
            // waakhond daar net op "home" heeft gedrukt. Dat voelt alsof de app
            // wegvalt op het moment dat je hem juist ontgrendelt.
            runCatching {
                startActivity(
                    Intent(this, nl.totem.app.MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            }
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        nfc.stop()
    }

    private fun hoortBijMijnTotem(uid: String): Boolean {
        val bekend = SharedStore.totem ?: return false
        return bekend.tagUID.equals(uid, ignoreCase = true)
    }

    override fun onDestroy() {
        if (huidige?.get() === this) huidige = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE_NAME = "modeName"
        const val EXTRA_STARTED_AT = "startedAt"

        /**
         * De activiteit die nu open staat, als die er is. Een zwakke
         * verwijzing, zodat we het systeem niet in de weg zitten bij opruimen.
         */
        private var huidige: WeakReference<BlockActivity>? = null

        /** Sluit het blokkadescherm zodra de sessie voorbij is. */
        fun close(context: android.content.Context) {
            val activity = huidige?.get() ?: return
            activity.runOnUiThread { activity.finish() }
            huidige = null
        }
    }
}

@Composable
private fun BlockScreen(modeName: String, startedAt: Long) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.totem_dark),
                contentDescription = null,
                modifier = Modifier.height(220.dp)
            )

            Text(
                text = modeName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 28.dp)
            )

            Text(
                text = "Deze app is geblokkeerd. Houd je Totem tegen de telefoon " +
                    "als je weer verder wilt.",
                color = Color(0xFF9E9E9E),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )

            if (startedAt > 0) {
                Text(
                    text = formatElapsed(now - startedAt, withSeconds = true),
                    color = Color(0xFF6E6E6E),
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            // Zonder deze knop zit je vast op dit scherm tot je zelf de
            // thuisknop vindt. iOS heeft hem al ("Terug naar het leven").
            //
            // Let op: gewoon finish() werkt niet -- dan val je terug in de
            // geblokkeerde app en verschijnt dit scherm meteen opnieuw. We
            // sturen de gebruiker daarom expliciet naar het startscherm.
            Button(
                onClick = {
                    val thuis = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(thuis)
                    (context as? ComponentActivity)?.finish()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0A0A0A)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Text(
                    "Terug naar het leven",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
