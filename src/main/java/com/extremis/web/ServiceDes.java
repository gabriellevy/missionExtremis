package com.extremis.web;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Genere l'image d'un de a dix faces (D10, style jeu de role) sous
 * forme de data URI SVG : un polygone a dix faces vu de dessus avec
 * les points du chiffre dessines au centre. Un jet de D100 est
 * affiche par deux des (dizaines puis unites).
 */
@Component
public class ServiceDes {

    private static final int TAILLE = 64;
    private static final double RAYON = 28.0;
    private static final double CX = TAILLE / 2.0;
    private static final double CY = TAILLE / 2.0 + 2.0;

    public String dataUri(int valeur) {
        int face = Math.floorMod(valeur, 10);
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='" + TAILLE + "' height='" + TAILLE + "'"
                + " viewBox='0 0 " + TAILLE + " " + TAILLE + "'>"
                + "<polygon points='" + pointsPolygone() + "' fill='#e8dcc0' stroke='#5a4a30' stroke-width='3'/>"
                + points(face)
                + "</svg>";
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String pointsPolygone() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / 10;
            double x = CX + RAYON * Math.cos(angle);
            double y = CY + RAYON * Math.sin(angle);
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format(java.util.Locale.ROOT, "%.1f,%.1f", x, y));
        }
        return sb.toString();
    }

    /**
     * Dessine les points (style pips de de) de la face donnee, ou le
     * chiffre pour 0 (aucun pip, le centre reste vide comme sur un
     * vrai D10).
     */
    private String points(int face) {
        double r = 4.5;
        String couleur = "#2a2114";
        StringBuilder sb = new StringBuilder();
        switch (face) {
            case 1 -> pip(sb, CX, CY, r, couleur);
            case 2 -> {
                pip(sb, CX - 12, CY - 12, r, couleur);
                pip(sb, CX + 12, CY + 12, r, couleur);
            }
            case 3 -> {
                pip(sb, CX - 14, CY - 14, r, couleur);
                pip(sb, CX, CY, r, couleur);
                pip(sb, CX + 14, CY + 14, r, couleur);
            }
            case 4 -> {
                pip(sb, CX - 12, CY - 12, r, couleur);
                pip(sb, CX + 12, CY - 12, r, couleur);
                pip(sb, CX - 12, CY + 12, r, couleur);
                pip(sb, CX + 12, CY + 12, r, couleur);
            }
            case 5 -> {
                pip(sb, CX - 12, CY - 12, r, couleur);
                pip(sb, CX + 12, CY - 12, r, couleur);
                pip(sb, CX, CY, r, couleur);
                pip(sb, CX - 12, CY + 12, r, couleur);
                pip(sb, CX + 12, CY + 12, r, couleur);
            }
            case 6 -> {
                pip(sb, CX - 12, CY - 13, r, couleur);
                pip(sb, CX + 12, CY - 13, r, couleur);
                pip(sb, CX - 12, CY, r, couleur);
                pip(sb, CX + 12, CY, r, couleur);
                pip(sb, CX - 12, CY + 13, r, couleur);
                pip(sb, CX + 12, CY + 13, r, couleur);
            }
            case 7 -> {
                pip(sb, CX - 13, CY - 14, r, couleur);
                pip(sb, CX + 13, CY - 14, r, couleur);
                pip(sb, CX - 13, CY, r, couleur);
                pip(sb, CX + 13, CY, r, couleur);
                pip(sb, CX, CY, r, couleur);
                pip(sb, CX - 13, CY + 14, r, couleur);
                pip(sb, CX + 13, CY + 14, r, couleur);
            }
            case 8 -> {
                pip(sb, CX - 12, CY - 14, r, couleur);
                pip(sb, CX + 12, CY - 14, r, couleur);
                pip(sb, CX - 12, CY, r, couleur);
                pip(sb, CX + 12, CY, r, couleur);
                pip(sb, CX, CY - 7, r, couleur);
                pip(sb, CX, CY + 7, r, couleur);
                pip(sb, CX - 12, CY + 14, r, couleur);
                pip(sb, CX + 12, CY + 14, r, couleur);
            }
            case 9 -> {
                pip(sb, CX - 13, CY - 14, r, couleur);
                pip(sb, CX + 13, CY - 14, r, couleur);
                pip(sb, CX - 13, CY - 5, r, couleur);
                pip(sb, CX + 13, CY - 5, r, couleur);
                pip(sb, CX, CY, r, couleur);
                pip(sb, CX - 13, CY + 5, r, couleur);
                pip(sb, CX + 13, CY + 5, r, couleur);
                pip(sb, CX - 13, CY + 14, r, couleur);
                pip(sb, CX + 13, CY + 14, r, couleur);
            }
            default -> { /* 0 : aucun pip, comme sur un D10 reel */ }
        }
        return sb.toString();
    }

    private void pip(StringBuilder sb, double x, double y, double r, String couleur) {
        sb.append("<circle cx='").append(String.format(java.util.Locale.ROOT, "%.1f", x))
                .append("' cy='").append(String.format(java.util.Locale.ROOT, "%.1f", y))
                .append("' r='").append(String.format(java.util.Locale.ROOT, "%.1f", r))
                .append("' fill='").append(couleur).append("'/>");
    }
}
