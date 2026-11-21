# Carte du jeu et tuiles locales

L'onglet Missions affiche une carte du monde fondée sur les tuiles
[OpenStreetMap](https://www.openstreetmap.org) (schéma « XYZ », 256×256 pixels,
projection Web Mercator), rendue par Leaflet.

## Tuiles stockées dans le projet

Les tuiles sont stockées dans le projet, sous :

```
src/main/resources/static/carte/tuiles/{z}/{x}/{y}.png
```

- `z` : niveau de zoom (10 à 13) ;
- `x`, `y` : indices de la tuile dans le schéma OSM XYZ.

La page charge d'abord les tuiles locales : dès que la tuile sentinelle
`carte/tuiles/10/517/350.png` est présente, la couche locale est utilisée ;
sinon, la carte se rabat sur le serveur de tuiles `tile.openstreetmap.org`
(idéal pour un développement hors connexion pas encore peuplé).

## Télécharger / regénérer les tuiles

Le script `tools/telecharger-tuiles.py` télécharge les tuiles des zones du jeu
(Île-de-France et Kyiv) pour les zooms 10 à 13 :

```
python3 tools/telecharger-tuiles.py
```

Options : `--zoom-max N` (zoom maximum), `--delai F` (pause en secondes entre
deux requêtes, défaut 0,1). Le script est idempotent : relancer ne
retélécharge que les tuiles manquantes. Il s'identifie poliment auprès du
serveur de tuiles ; respecter la politique d'usage d'OpenStreetMap (pas de
téléchargement massif répété inutilement).

## Édition manuelle

Chaque tuile est un simple fichier PNG indépendant :

- éditer une tuile = ouvrir le PNG correspondant dans un éditeur d'images
  (GIMP, Krita, Photoshop…) et le modifier en place ;
- remplacer une zone = retoucher les PNG concernés (par exemple assombrir
  tout un quartier) puis relancer le serveur (les fichiers `static` sont
  servis tels quels) ;
- ajouter un lieu hors zones actuelles = ajouter ses coordonnées dans les
  `ZONES` du script et relancer le téléchargement, ou placer les PNG
  directement au bon chemin `z/x/y.png`.

Les coordonnées de chaque lieu du jeu sont dans `src/main/java/com/extremis/core/Lieu.java`
(`latitude()` / `longitude()` en WGS84) — pour retrouver la tuile d'un lieu :
`https://www.openstreetmap.org/#map=13/<lat>/<lon>` affiche la zone et
l'URL du terrain ; l'indice de tuile se calcule avec `tools/telecharger-tuiles.py`
(`indice_x` / `indice_y`).

## Attribution

Les tuiles proviennent d'OpenStreetMap : conserver l'attribution
« © OpenStreetMap » affichée par la carte (`attribution` de la couche Leaflet)
même après retouche manuelle.
