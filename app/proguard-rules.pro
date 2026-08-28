# kotlinx.serialization heeft de generated serializers nodig.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class nl.totem.app.model.** {
    *** Companion;
}
-keepclasseswithmembers class nl.totem.app.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# De AccessibilityService wordt door het systeem via de naam aangeroepen.
-keep class nl.totem.app.shield.TotemAccessibilityService { *; }

# ---------------------------------------------------------------------------
# Google Tink, meegeleverd via androidx.security.crypto.
#
# Tink verwijst naar annotaties van ErrorProne en JSR-305 die alleen tijdens
# het compileren bestaan en niet in de app terechtkomen. R8 waarschuwt daarover
# en breekt de release-build af. Deze regels komen letterlijk uit het
# missing_rules.txt dat Gradle zelf genereert.
# ---------------------------------------------------------------------------
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

# ---------------------------------------------------------------------------
# Echtheidscontrole van de Totem.
#
# Deze twee worden alleen vanuit eigen code aangeroepen, dus R8 zou ze in
# principe met rust laten. Toch expliciet vastgezet: als de verificatie ooit
# stilletjes wegge-optimaliseerd wordt, accepteert de app elke tag -- en dat
# merk je pas in productie.
# ---------------------------------------------------------------------------
-keep class nl.totem.app.nfc.P192 { *; }
-keep class nl.totem.app.nfc.TotemAuth { *; }
