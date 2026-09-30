# De teksten van de app, in vijf talen: (nl, en, fr, es, de).
#
# Genereer de XML-bestanden met:  python3 tools/make_strings.py
# De standaardmap `values/` is Engels, zodat elke taal die we niet kennen
# (Italiaans, Pools, …) Engels krijgt. Nederlands staat in `values-nl/`.
#
# Placeholders: %1$s voor tekst, %1$d voor getallen. Apostrofs en
# aanhalingstekens worden door het script zelf ge-escaped.

S = {
    # --- Algemeen -----------------------------------------------------------
    "app_name": ("Totem", "Totem", "Totem", "Totem", "Totem"),
    "widget_label": ("Totem", "Totem", "Totem", "Totem", "Totem"),
    "widget_description": (
        "Laat zien of er een blokkade loopt.",
        "Shows whether a block is running.",
        "Indique si un blocage est en cours.",
        "Muestra si hay un bloqueo en curso.",
        "Zeigt, ob eine Sperre läuft."),
    "accessibility_label": (
        "Totem — apps blokkeren",
        "Totem — block apps",
        "Totem — bloquer des apps",
        "Totem — bloquear apps",
        "Totem — Apps sperren"),
    "accessibility_description": (
        "Totem kijkt welke app je opent en toont een blokkadescherm zolang een focusmodus loopt. Er wordt niets gelezen of bewaard van wat er op je scherm staat.",
        "Totem checks which app you open and shows a block screen while a focus mode is running. Nothing on your screen is read or stored.",
        "Totem regarde quelle app tu ouvres et affiche un écran de blocage tant qu’un mode concentration est actif. Rien de ce qui est à l’écran n’est lu ni enregistré.",
        "Totem comprueba qué app abres y muestra una pantalla de bloqueo mientras hay un modo de concentración activo. No se lee ni se guarda nada de lo que hay en tu pantalla.",
        "Totem prüft, welche App du öffnest, und zeigt einen Sperrbildschirm, solange ein Fokusmodus läuft. Nichts auf deinem Bildschirm wird gelesen oder gespeichert."),

    # --- Blokkadescherm -----------------------------------------------------
    "block_title": (
        "%1$s is geblokkeerd",
        "%1$s is blocked",
        "%1$s est bloqué",
        "%1$s está bloqueada",
        "%1$s ist gesperrt"),
    "block_body": (
        "Je hebt gekozen voor focus. Tik je Totem aan als je %1$s weer wilt gebruiken.",
        "You chose focus. Tap your Totem when you want to use %1$s again.",
        "Tu as choisi la concentration. Tape ton Totem quand tu veux utiliser %1$s à nouveau.",
        "Has elegido concentrarte. Toca tu Totem cuando quieras volver a usar %1$s.",
        "Du hast dich für Fokus entschieden. Tippe auf dein Totem, wenn du %1$s wieder nutzen willst."),
    "block_this_app": ("Deze app", "This app", "Cette app", "Esta app", "Diese App"),
    "block_back_to_life": (
        "Terug naar het leven",
        "Back to real life",
        "Retour à la vraie vie",
        "Vuelve a la vida real",
        "Zurück ins echte Leben"),
}
