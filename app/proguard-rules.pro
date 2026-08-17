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
