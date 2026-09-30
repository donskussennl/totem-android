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
    # --- Algemeen, deel 2 ---------------------------------------------------
    "unknown": ("Onbekend", "Unknown", "Inconnu", "Desconocido", "Unbekannt"),

    # --- Meldingen ----------------------------------------------------------
    "channel_session": ("Lopende blokkade", "Running block", "Blocage en cours", "Bloqueo en curso", "Laufende Sperre"),
    "channel_session_description": (
        "De teller die loopt zolang een blokkade actief is.",
        "The timer that runs while a block is active.",
        "Le compteur qui tourne tant qu’un blocage est actif.",
        "El contador que corre mientras hay un bloqueo activo.",
        "Der Zähler, der läuft, solange eine Sperre aktiv ist."),
    "channel_schedule": ("Schema's", "Schedules", "Programmes", "Horarios", "Zeitpläne"),
    "channel_schedule_description": (
        "Bericht zodra een schema een blokkade start of stopt.",
        "A message as soon as a schedule starts or stops a block.",
        "Un message dès qu’un programme lance ou arrête un blocage.",
        "Un aviso en cuanto un horario inicia o detiene un bloqueo.",
        "Eine Nachricht, sobald ein Zeitplan eine Sperre startet oder beendet."),
    "notify_started_title": ("%1$s is gestart", "%1$s has started", "%1$s a commencé", "%1$s ha empezado", "%1$s hat begonnen"),
    "notify_started_until": ("Je telefoon is rustig tot %1$s.", "Your phone is quiet until %1$s.", "Ton téléphone reste calme jusqu’à %1$s.", "Tu teléfono estará tranquilo hasta %1$s.", "Dein Handy ist ruhig bis %1$s."),
    "notify_started_tap": ("Tik je Totem aan als je weer verder wilt.", "Tap your Totem when you want to carry on.", "Tape ton Totem quand tu veux reprendre.", "Toca tu Totem cuando quieras seguir.", "Tippe auf dein Totem, wenn du weitermachen willst."),
    "notify_ended_title": ("%1$s is afgelopen", "%1$s has ended", "%1$s est terminé", "%1$s ha terminado", "%1$s ist beendet"),
    "notify_ended_body": ("Je apps zijn weer vrij.", "Your apps are free again.", "Tes apps sont de nouveau libres.", "Tus apps vuelven a estar libres.", "Deine Apps sind wieder frei."),
    "notify_refrozen_title": ("%1$s is weer actief", "%1$s is active again", "%1$s est de nouveau actif", "%1$s vuelve a estar activo", "%1$s ist wieder aktiv"),
    "notify_refrozen_body": ("Je pauze is voorbij; je apps zijn weer bevroren.", "Your break is over; your apps are frozen again.", "Ta pause est terminée ; tes apps sont de nouveau gelées.", "Tu pausa ha terminado; tus apps vuelven a estar congeladas.", "Deine Pause ist vorbei; deine Apps sind wieder eingefroren."),
    "notify_cannot_block_title": ("%1$s kon niet starten", "%1$s couldn’t start", "%1$s n’a pas pu démarrer", "%1$s no pudo iniciarse", "%1$s konnte nicht starten"),
    "notify_cannot_block_body": (
        "Totem mist een toestemming. Open de app en controleer de instellingen.",
        "Totem is missing a permission. Open the app and check the settings.",
        "Il manque une autorisation à Totem. Ouvre l’app et vérifie les réglages.",
        "A Totem le falta un permiso. Abre la app y revisa los ajustes.",
        "Totem fehlt eine Berechtigung. Öffne die App und prüfe die Einstellungen."),

    # --- Ontdooien -----------------------------------------------------------
    "detail_paused_label": ("Ontdooid, bevriest weer over", "Unfrozen, freezes again in", "Dégelé, se regèle dans", "Descongelado, se congela de nuevo en", "Aufgetaut, friert wieder ein in"),
    "detail_freeze_now": ("Nu weer bevriezen", "Freeze again now", "Regeler maintenant", "Volver a congelar ya", "Jetzt wieder einfrieren"),
    "detail_unfreeze_minutes": ("%1$d min ontdooien", "Unfreeze for %1$d min", "Dégeler %1$d min", "Descongelar %1$d min", "%1$d Min. auftauen"),
    "detail_deactivate": ("Totem deactiveren", "Deactivate Totem", "Désactiver Totem", "Desactivar Totem", "Totem deaktivieren"),
    "detail_activate": ("Totem activeren", "Activate Totem", "Activer Totem", "Activar Totem", "Totem aktivieren"),
    "detail_block_time": ("Blokkade tijd", "Block time", "Temps de blocage", "Tiempo de bloqueo", "Sperrzeit"),
    "scan_unfreeze": ("Tik je Totem aan om %1$d minuten te ontdooien", "Tap your Totem to unfreeze for %1$d minutes", "Tape ton Totem pour dégeler %1$d minutes", "Toca tu Totem para descongelar %1$d minutos", "Tippe auf dein Totem, um %1$d Minuten aufzutauen"),
    "editor_unfreeze_on_tap": ("Ontdooien bij tikken", "Unfreeze on tap", "Dégeler en tapant", "Descongelar al tocar", "Beim Tippen auftauen"),
    "editor_unfreeze_minutes": ("%1$d min", "%1$d min", "%1$d min", "%1$d min", "%1$d Min."),
    "editor_unfreeze_help": (
        "Tik je tijdens het schema je Totem aan, dan ontdooien je apps even en bevriezen ze daarna weer tot het schema voorbij is.",
        "Tap your Totem during the schedule and your apps unfreeze for a moment, then freeze again until the schedule ends.",
        "Si tu tapes ton Totem pendant le programme, tes apps se dégèlent un moment puis se regèlent jusqu’à la fin du programme.",
        "Si tocas tu Totem durante el horario, tus apps se descongelan un momento y luego se vuelven a congelar hasta que termine el horario.",
        "Tippst du während des Zeitplans auf dein Totem, tauen deine Apps kurz auf und frieren danach wieder ein, bis der Zeitplan vorbei ist."),
    # --- Modusnamen ---------------------------------------------------------
    "mode_work": ("Werk", "Work", "Travail", "Trabajo", "Arbeit"),
    "mode_sport": ("Sport", "Sport", "Sport", "Deporte", "Sport"),
    "mode_relax": ("Relaxen", "Relax", "Détente", "Relax", "Entspannen"),
    "mode_study": ("Studeren", "Study", "Études", "Estudiar", "Lernen"),
    "mode_sleep": ("Slaap", "Sleep", "Sommeil", "Dormir", "Schlafen"),
    "mode_other": ("Anders…", "Other…", "Autre…", "Otro…", "Anderes …"),
    # --- Hoofdscherm ----------------------------------------------------------
    "mode_blocked_count": ("%1$d geblokkeerd", "%1$d blocked", "%1$d bloquées", "%1$d bloqueadas", "%1$d gesperrt"),
    "mode_no_apps": ("Nog geen apps gekozen", "No apps chosen yet", "Aucune app choisie", "Aún no hay apps elegidas", "Noch keine Apps gewählt"),
    "home_add_mode": ("Modus toevoegen", "Add mode", "Ajouter un mode", "Añadir modo", "Modus hinzufügen"),
    "home_swipe_hint": ("Veeg een modus naar links om hem te verwijderen.", "Swipe a mode to the left to delete it.", "Balaie un mode vers la gauche pour le supprimer.", "Desliza un modo hacia la izquierda para borrarlo.", "Wische einen Modus nach links, um ihn zu löschen."),
    "home_activate_mode": ("%1$s activeren", "Activate %1$s", "Activer %1$s", "Activar %1$s", "%1$s aktivieren"),
    "home_activate_hint": ("Houd daarna je telefoon tegen je Totem.", "Then hold your phone against your Totem.", "Ensuite, approche ton téléphone de ton Totem.", "Después, acerca tu teléfono a tu Totem.", "Halte dann dein Handy an dein Totem."),
    "home_activate_hint_empty": ("Kies eerst apps in een modus om te kunnen activeren.", "First choose apps in a mode to be able to activate.", "Choisis d’abord des apps dans un mode pour pouvoir activer.", "Elige primero apps en un modo para poder activarlo.", "Wähle zuerst Apps in einem Modus, um aktivieren zu können."),
    "home_permission_title": ("Totem kan nog niets blokkeren", "Totem can’t block anything yet", "Totem ne peut encore rien bloquer", "Totem aún no puede bloquear nada", "Totem kann noch nichts sperren"),
    "home_permission_body": (
        "Geef toegankelijkheid en ‘over andere apps tekenen’ vrij in de instellingen.",
        "Allow accessibility and ‘display over other apps’ in the settings.",
        "Autorise l’accessibilité et « superposition sur d’autres apps » dans les réglages.",
        "Permite la accesibilidad y «mostrar sobre otras apps» en los ajustes.",
        "Erlaube Bedienungshilfen und „Über anderen Apps einblenden“ in den Einstellungen."),
    "choose_title": ("Welke modus wil je starten?", "Which mode do you want to start?", "Quel mode veux-tu lancer ?", "¿Qué modo quieres iniciar?", "Welchen Modus möchtest du starten?"),
    "delete_title": ("‘%1$s’ verwijderen?", "Delete ‘%1$s’?", "Supprimer « %1$s » ?", "¿Borrar «%1$s»?", "„%1$s“ löschen?"),
    "delete_body": ("De gekozen apps en het schema van deze modus ben je dan kwijt.", "You’ll lose the chosen apps and the schedule of this mode.", "Tu perdras les apps choisies et le programme de ce mode.", "Perderás las apps elegidas y el horario de este modo.", "Die gewählten Apps und der Zeitplan dieses Modus gehen dann verloren."),
    "delete_confirm": ("Verwijderen", "Delete", "Supprimer", "Borrar", "Löschen"),
    "cancel": ("Annuleer", "Cancel", "Annuler", "Cancelar", "Abbrechen"),
    "stats_title": ("Statistieken", "Statistics", "Statistiques", "Estadísticas", "Statistiken"),
    "settings_title": ("Instellingen", "Settings", "Réglages", "Ajustes", "Einstellungen"),
    "error_max_modes": ("Je kunt maximaal %1$d modi maken. Verwijder er eerst een.", "You can create up to %1$d modes. Delete one first.", "Tu peux créer au maximum %1$d modes. Supprimes-en un d’abord.", "Puedes crear un máximo de %1$d modos. Borra uno primero.", "Du kannst höchstens %1$d Modi erstellen. Lösche zuerst einen."),

    # --- Schema-samenvattingen ----------------------------------------------
    "sched_time_summary": ("%1$s  %2$s – %3$s", "%1$s  %2$s – %3$s", "%1$s  %2$s – %3$s", "%1$s  %2$s – %3$s", "%1$s  %2$s – %3$s"),
    "sched_location_summary": ("%1$s  bij %2$s – %3$s", "%1$s  at %2$s – %3$s", "%1$s  à %2$s – %3$s", "%1$s  en %2$s – %3$s", "%1$s  bei %2$s – %3$s"),
    "sched_until_tap": ("tot je tikt", "until you tap", "jusqu’à ce que tu tapes", "hasta que toques", "bis du tippst"),
    "sched_until_leave": ("tot je weggaat", "until you leave", "jusqu’à ce que tu partes", "hasta que te vayas", "bis du gehst"),
    "sched_every_day": ("elke dag", "every day", "tous les jours", "todos los días", "jeden Tag"),
    "sched_weekend": ("weekend", "weekend", "week-end", "fin de semana", "Wochenende"),
    "sched_no_day": ("geen dag", "no days", "aucun jour", "ningún día", "kein Tag"),
    "sched_no_place": ("geen locatie gekozen", "no location chosen", "aucun lieu choisi", "sin ubicación elegida", "kein Ort gewählt"),
}