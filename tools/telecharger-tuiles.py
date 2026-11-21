#!/usr/bin/env python3
"""Télécharge les tuiles de la carte du jeu depuis OpenStreetMap et les stocke
dans le projet, sous src/main/resources/static/carte/tuiles/{z}/{x}/{y}.png.

Les tuiles suivent le schéma standard OSM « XYZ » (256×256 pixels, projection
Web Mercator). Une fois téléchargées elles sont servies localement par
l'application et peuvent être modifiées manuellement, image par image, dans
n'importe quel éditeur.

Zones couvertes (définies ci-dessous, modifiables) :
  - le monde entier, zooms 0 à 5 (vue terrestre complète) ;
  - la France, zooms 6 à 9 ;
  - l'Île-de-France : tous les lieux du jeu hors Ukraine, zooms 10 à 13 ;
  - Kyiv : le lieu « Ukraine », zooms 6 à 13.

Utilisation :
    python3 tools/telecharger-tuiles.py [--zoom-max N] [--delai F]

Le script est idempotent : une tuile déjà présente n'est pas retéléchargée.
Il identifie poliment auprès du serveur de tuiles et marque une pause entre
chaque requête (0,1 s par défaut) pour respecter la politique d'usage
d'OpenStreetMap. L'attribution « © OpenStreetMap contributors » doit être
conservée dans le projet (elle figure dans la page de jeu).
"""

import argparse
import math
import time
import urllib.request
import urllib.error
from pathlib import Path

RACINE = Path(__file__).resolve().parent.parent
DOSSIER_TUILES = RACINE / "src" / "main" / "resources" / "static" / "carte" / "tuiles"
SERVEUR = "https://tile.openstreetmap.org"
USER_AGENT_PROJET = "missionExtremis/1.0 (jeu personnel; https://github.com/gabriellevy/missionExtremis)"

ZOOM_MIN = 0

ZONES = {
    "monde": {"entiere": True, "zoom_min": 0, "zoom_max": 5},
    "france": {"lat_sud": 41.0, "lat_nord": 51.2, "ouest": -5.5, "est": 10.0, "zoom_min": 6, "zoom_max": 9},
    "ile-de-france": {"lat_sud": 48.0, "lat_nord": 49.3, "ouest": 1.8, "est": 3.7, "zoom_min": 10, "zoom_max": 13},
    "kyiv": {"lat_sud": 50.2, "lat_nord": 50.7, "ouest": 30.1, "est": 30.9, "zoom_min": 6, "zoom_max": 13},
}


def indice_x(longitude, zoom):
    return int((longitude + 180.0) / 360.0 * 2 ** zoom)


def indice_y(latitude, zoom):
    radians = math.radians(latitude)
    n = 2 ** zoom
    return int((1 - math.log(math.tan(radians) + 1 / math.cos(radians)) / math.pi) / 2 * n)


def intervalles_zone(zone, zoom):
    if zone.get("entiere"):
        return range(0, 2 ** zoom), range(0, 2 ** zoom)
    x1 = indice_x(zone["ouest"], zoom)
    x2 = indice_x(zone["est"], zoom)
    y1 = indice_y(zone["lat_nord"], zoom)
    y2 = indice_y(zone["lat_sud"], zoom)
    return range(x1, x2 + 1), range(y1, y2 + 1)


def telecharger_tuile(zoom, x, y, delai):
    chemin = DOSSIER_TUILES / str(zoom) / str(x) / f"{y}.png"
    if chemin.exists():
        return "deja-la", None
    repertoire = chemin.parent
    repertoire.mkdir(parents=True, exist_ok=True)
    url = f"{SERVEUR}/{zoom}/{x}/{y}.png"
    requete = urllib.request.Request(url, headers={"User-Agent": USER_AGENT_PROJET})
    try:
        with urllib.request.urlopen(requete, timeout=20) as reponse:
            contenu = reponse.read()
    except urllib.error.HTTPError as erreur:
        return "erreur", f"HTTP {erreur.code} pour {url}"
    except Exception as erreur:
        return "erreur", f"{erreur} pour {url}"
    chemin.write_bytes(contenu)
    time.sleep(delai)
    return "ok", None


def main():
    analyseur = argparse.ArgumentParser(description="Télécharge les tuiles OSM de la carte du jeu.")
    analyseur.add_argument("--zoom-max", type=int, default=13, help="niveau de zoom maximum (défaut : 13)")
    analyseur.add_argument("--delai", type=float, default=0.1, help="pause en secondes entre deux tuiles (défaut : 0.1)")
    arguments = analyseur.parse_args()

    total = 0
    ok = 0
    sautees = 0
    erreurs = 0
    for zone_nom, zone in ZONES.items():
        for zoom in range(max(ZOOM_MIN, zone["zoom_min"]), min(arguments.zoom_max, zone["zoom_max"]) + 1):
            xs, ys = intervalles_zone(zone, zoom)
            for x in xs:
                for y in ys:
                    total += 1
                    statut, message = telecharger_tuile(zoom, x, y, arguments.delai)
                    if statut == "ok":
                        ok += 1
                    elif statut == "deja-la":
                        sautees += 1
                    else:
                        erreurs += 1
                        print(message)
                    if total % 250 == 0:
                        print(f"... {total} tuiles traitées ({ok} téléchargées, {sautees} déjà présentes, {erreurs} erreurs)")
    print(f"Terminé : {total} tuiles traitées, {ok} téléchargées, {sautees} déjà présentes, {erreurs} erreurs.")
    print(f"Emplacement : {DOSSIER_TUILES}")


if __name__ == "__main__":
    main()
