package com.extremis.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class ServiceWikiNotion {
    private static final Logger log = LoggerFactory.getLogger(ServiceWikiNotion.class);
    private static final String NOTION_VERSION = "2022-06-28";
    private static final String BASE_URL_PAR_DEFAUT = "https://api.notion.com/v1";

    private final RestClient client;
    private final String jeton;

    public ServiceWikiNotion(@Value("${extremis.wiki.notion-token:}") String jeton,
                             @Value("${extremis.wiki.notion-base-url:https://api.notion.com/v1}") String baseUrl) {
        String url = (baseUrl == null || baseUrl.isBlank()) ? BASE_URL_PAR_DEFAUT : baseUrl;
        this.client = RestClient.builder().baseUrl(url).build();
        this.jeton = jeton == null ? "" : jeton;
    }

    /**
     * Tire au hasard {@code nombre} pages parmi l'integralite des pages
     * du wiki Notion. La liste est re-interrogeen et le tirage refait a
     * chaque appel : chaque affichage de l'onglet renvoie d'autres pages.
     */
    public List<LienWikiView> liensAleatoires(int nombre) {
        List<LienWikiView> pages;
        try {
            pages = toutesLesPages();
        } catch (Exception e) {
            log.warn("Impossible de recuperer les pages du wiki Notion : {}", e.getMessage());
            return List.of();
        }
        if (pages.size() <= nombre) {
            return pages;
        }
        List<LienWikiView> melange = new ArrayList<>(pages);
        Collections.shuffle(melange, new Random());
        return List.copyOf(melange.subList(0, nombre));
    }

    private List<LienWikiView> toutesLesPages() {
        if (jeton.isBlank()) {
            log.debug("Aucun jeton Notion configure : aucun lien wiki renvoye.");
            return List.of();
        }
        List<LienWikiView> pages = new ArrayList<>();
        String curseur = null;
        boolean encore = true;
        while (encore) {
            Map<String, Object> corps = new LinkedHashMap<>();
            corps.put("filter", Map.of("property", "object", "value", "page"));
            corps.put("page_size", 100);
            if (curseur != null) {
                corps.put("start_cursor", curseur);
            }
            Map<?, ?> reponse = client.post()
                    .uri("/search")
                    .header("Authorization", "Bearer " + jeton)
                    .header("Notion-Version", NOTION_VERSION)
                    .body(corps)
                    .retrieve()
                    .body(Map.class);
            if (reponse == null) {
                break;
            }
            if (reponse.get("results") instanceof List<?> resultats) {
                for (Object resultat : resultats) {
                    if (resultat instanceof Map<?, ?> page) {
                        pages.add(versLien(page));
                    }
                }
            }
            encore = Boolean.TRUE.equals(reponse.get("has_more")) && reponse.get("next_cursor") instanceof String;
            if (reponse.get("next_cursor") instanceof String suivant) {
                curseur = suivant;
            }
        }
        return pages;
    }

    private static LienWikiView versLien(Map<?, ?> page) {
        Object url = page.get("url");
        String titre = "Page sans titre";
        if (page.get("properties") instanceof Map<?, ?> proprietes) {
            for (Object valeur : proprietes.values()) {
                if (valeur instanceof Map<?, ?> propriete
                        && "title".equals(propriete.get("type"))
                        && propriete.get("title") instanceof List<?> segments) {
                    StringBuilder constructeur = new StringBuilder();
                    for (Object segment : segments) {
                        if (segment instanceof Map<?, ?> m && m.get("plain_text") != null) {
                            constructeur.append(m.get("plain_text"));
                        }
                    }
                    if (!constructeur.isEmpty()) {
                        titre = constructeur.toString();
                        break;
                    }
                }
            }
        }
        return new LienWikiView(titre, url == null ? "" : url.toString());
    }
}
