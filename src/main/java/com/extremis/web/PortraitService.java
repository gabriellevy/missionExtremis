package com.extremis.web;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Portraits provisoires generes a la volee (pas encore d'illustrations
 * officielles). Un motif deterministe par personnage : initiales sur fond
 * colore derive du nom.
 */
@Component
public class PortraitService {

    public String dataUri(String name, boolean alive) {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = pickColor(name);
        Color fg = pickForeground(bg);
        g.setColor(bg);
        g.fill(new Ellipse2D.Float(0, 0, size, size));
        g.setColor(fg);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 96));
        var bounds = g.getFontMetrics().getStringBounds(initials(name), g);
        g.drawString(initials(name),
                (float) ((size - bounds.getWidth()) / 2),
                (float) ((size - bounds.getHeight()) / 2 - bounds.getY()));
        if (!alive) {
            g.setColor(new Color(120, 0, 0, 170));
            g.fillRect(0, 0, size, size);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (Exception e) {
            throw new IllegalStateException("Generation du portrait impossible", e);
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private String initials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(words[0].charAt(0));
        if (words.length > 1) {
            sb.append(words[words.length - 1].charAt(0));
        }
        return sb.toString().toUpperCase();
    }

    private Color pickColor(String name) {
        int h = Math.floorMod(name.hashCode(), 360);
        float s = 0.35f;
        float b = 0.55f;
        return Color.getHSBColor(h / 360f, s, b);
    }

    private Color pickForeground(Color bg) {
        int y = (int) ((bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000);
        return y > 140 ? Color.BLACK : Color.WHITE;
    }
}
