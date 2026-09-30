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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.totem.app.R
import nl.totem.app.data.SharedStore
import nl.totem.app.nfc.NfcService
import nl.totem.app.schedule.SessionEngine
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
        val appName = intent.getStringExtra(EXTRA_APP_NAME)
            ?: getString(R.string.block_this_app)
        setContent { BlockScreen(appName = appName) }
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
        /** De naam van de app die geblokkeerd werd, bijvoorbeeld "Instagram". */
        const val EXTRA_APP_NAME = "appName"

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

/**
 * Zelfde ontwerp als het blokkadescherm op iOS: diep paarsblauw, het witte
 * Totem-beeldmerk, "Instagram is geblokkeerd" en een witte knop.
 */
@Composable
internal fun BlockScreen(appName: String) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2D2470)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.totem_logo_white),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )

            Text(
                text = stringResource(R.string.block_title, appName),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = stringResource(R.string.block_body, appName),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(top = 10.dp)
            )

            Spacer(Modifier.weight(1f))

            // Zonder deze knop zit je vast op dit scherm tot je zelf de
            // thuisknop vindt.
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
                    contentColor = Color.Black
                ),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .height(56.dp)
            ) {
                Text(
                    stringResource(R.string.block_back_to_life),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
