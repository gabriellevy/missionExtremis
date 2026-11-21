# Carte du jeu et tuiles locales

L'onglet Missions affiche une carte du monde fondée sur les tuiles
[OpenStreetMap](https://www.openstreetmap.org) (schéma « XYZ », 256×256 pixels,
projection Web Mercator), rendue par Leaflet.

## Tuiles stockées dans le projet

Les tuiles sont stockées dans le projet, sous :

```
src/main/resources/static/carte/tuiles/{z}/{x}/{y}.png
```

- `z` : niveau de zoom (0 à 13 : le monde entier jusqu'au zoom 5, la France jusqu'à 9, l'Île-de-France et Kyiv jusqu'à 13) ;
- `x`, `y` : indices de la tuile dans le schéma OSM XYZ.

La page charge d'abord les tuiles locales : dès que les trois tuiles
sentinelles (`carte/tuiles/0/0/0.png`, `carte/tuiles/6/32/22.png` et
`carte/tuiles/10/517/350.png`) sont présentes, les couches locales sont
utilisées — la terre entière est visible jusqu'au zoom 5, la France jusqu'à 9,
les zones de jeu jusqu'à 13 (re amplifié jusqu'au zoom 16) ; sinon, la carte
se rabat sur le serveur `tile.openstreetmap.org` (utile tant que les tuiles
ne sont pas encore téléchargées). Le dézoom est possible jusqu'au niveau 0
(vue terrestre entière).

## Télécharger / regénérer les tuiles

Le script `tools/telecharger-tuiles.py` télécharge les tuiles des zones du
jeu (monde entier 0–5, France 6–9, Île-de-France 10–13 et Kyiv 6–13, soit
environ 5350 tuiles) :

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
