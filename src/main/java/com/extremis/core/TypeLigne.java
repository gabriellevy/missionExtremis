package com.extremis.core;

/**
 * Type d'une ligne du journal de mission, pour son rendu formate :
 * TITRE (titre d'evenement affiche en titre de taille moyenne,
 * sans les "==="), AMBIANCE (phrase d'ambiance affichee en
 * italique) ou TEXTE (ligne ordinaire).
 */
public enum TypeLigne {
    TEXTE,
    TITRE,
    AMBIANCE
}
