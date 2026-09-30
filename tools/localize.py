"""Vervangt een Nederlandse tekst in de code door een string-resource.

Gebruik (vanuit een ander script):
    from localize import apply
    apply("ui/Scherm.kt", [
        ('"Tekst"', "sleutel", ("nl", "en", "fr", "es", "de"), "c"),
    ])
"c" = in Compose (stringResource), "x" = met een context (context.getString).
De vertalingen worden aan tools/strings.py toegevoegd.
"""
import os
import re

ROOT = os.path.join(os.path.dirname(__file__), "..")
SRC = os.path.join(ROOT, "app/src/main/java/nl/totem/app")
STRINGS = os.path.join(os.path.dirname(__file__), "strings.py")


def _add_strings(entries):
    s = open(STRINGS, encoding="utf-8").read().rstrip()
    assert s.endswith("}")
    bestaand = set(re.findall(r'^\s*"([a-z0-9_]+)":', s, re.M))
    regels = []
    for key, vals in entries:
        # vals = None: de tekst bestaat al; alleen hergebruiken.
        if vals is None or key in bestaand:
            continue
        regels.append(f'    "{key}": ({", ".join(repr(v) for v in vals)}),')
        bestaand.add(key)
    if regels:
        s = s[:-1] + "\n".join(regels) + "\n}"
        open(STRINGS, "w", encoding="utf-8").write(s + "\n")


def apply(pad, items, context_expr="context"):
    p = os.path.join(SRC, pad)
    s = open(p, encoding="utf-8").read()
    nieuw = []
    for old, key, vals, mode, *args in items:
        argtekst = (", " + ", ".join(args[0])) if args else ""
        if mode == "c":
            new = f"stringResource(R.string.{key}{argtekst})"
        else:
            new = f"{context_expr}.getString(R.string.{key}{argtekst})"
        # "titel = \"…\"": de naam van het argument laten staan.
        m = re.match(r'^(\w+ = )"', old)
        if m:
            new = m.group(1) + new
        # "Text(\"…\")": de aanroep zelf laten staan.
        m = re.match(r'^(\w+\()"(?:[^"\\]|\\.)*"\)$', old)
        if m:
            new = m.group(1) + new + ")"
        n = s.count(old)
        assert n >= 1, f"{pad}: niet gevonden: {old[:80]}"
        s = s.replace(old, new)
        nieuw.append((key, vals))
    if "stringResource(" in s and "import androidx.compose.ui.res.stringResource" not in s:
        s = s.replace("\nimport ", "\nimport androidx.compose.ui.res.stringResource\nimport ", 1)
    if "R.string." in s and "import nl.totem.app.R\n" not in s and not pad.startswith("R"):
        s = s.replace("\nimport ", "\nimport nl.totem.app.R\nimport ", 1)
    _add_strings(nieuw)
    open(p, "w", encoding="utf-8").write(s)
    print(f"{pad}: {len(items)} teksten")
