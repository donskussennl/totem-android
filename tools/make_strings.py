"""Schrijft strings.xml voor elke taal uit tools/strings.py.

Draai vanuit de projectmap:  python3 tools/make_strings.py
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from strings import S  # noqa: E402

RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
# Volgorde in de tuples: nl, en, fr, es, de. Engels is de standaard.
TALEN = [("values-nl", 0), ("values", 1), ("values-fr", 2), ("values-es", 3), ("values-de", 4)]
NIET_VERTALEN = {"app_name", "widget_label"}


def escape(tekst: str) -> str:
    tekst = tekst.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    tekst = tekst.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    tekst = tekst.replace("\n", "\\n")
    # Een @ of ? aan het begin zou Android als verwijzing lezen.
    if tekst[:1] in "@?":
        tekst = "\\" + tekst
    return tekst


def placeholders(tekst: str) -> list:
    return sorted(re.findall(r"%\d+\$[sd]", tekst))


def main() -> None:
    fouten = []
    for sleutel, waarden in S.items():
        if len(waarden) != 5:
            fouten.append(f"{sleutel}: {len(waarden)} talen in plaats van 5")
            continue
        basis = placeholders(waarden[0])
        for i, w in enumerate(waarden):
            if placeholders(w) != basis:
                fouten.append(f"{sleutel} [{i}]: placeholders kloppen niet")
    if fouten:
        sys.exit("\n".join(fouten))

    for map_, index in TALEN:
        pad = os.path.join(RES, map_)
        os.makedirs(pad, exist_ok=True)
        regels = ['<?xml version="1.0" encoding="utf-8"?>',
                  "<!-- Gegenereerd door tools/make_strings.py. Niet met de hand aanpassen. -->",
                  "<resources>"]
        for sleutel, waarden in S.items():
            if map_ != "values" and sleutel in NIET_VERTALEN:
                continue
            extra = ' translatable="false"' if sleutel in NIET_VERTALEN else ""
            regels.append(f'    <string name="{sleutel}"{extra}>{escape(waarden[index])}</string>')
        regels.append("</resources>")
        with open(os.path.join(pad, "strings.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(regels) + "\n")
    print(f"{len(S)} teksten geschreven voor {len(TALEN)} talen")


if __name__ == "__main__":
    main()
