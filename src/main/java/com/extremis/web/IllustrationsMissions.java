package com.extremis.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Illustrations des missions, affichees a cote de leur description dans
 * la liste des missions disponibles. Chaque illustration officielle est
 * rangee dans src/main/resources/static/missions/&lt;id mission&gt;.jpg ;
 * les missions sans illustration n'affichent rien.
 */
@Component
public class IllustrationsMissions {

    private static final String DOSSIER_MISSIONS = "/missions/";

    /**
     * L'illustration de la mission en data URI, ou null si la mission
     * n'en a pas.
     */
    public String dataUri(String missionId) {
        BufferedImage image = chargerIllustration(missionId);
        if (image == null) {
            return null;
        }
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", sortie);
        } catch (Exception e) {
            return null;
        }
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(sortie.toByteArray());
    }

    private BufferedImage chargerIllustration(String missionId) {
        try {
            var ressource = new ClassPathResource("static" + DOSSIER_MISSIONS + missionId + ".jpg");
            if (!ressource.exists()) {
                return null;
            }
            return ImageIO.read(ressource.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }
}
