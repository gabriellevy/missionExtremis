package com.extremis.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class ServiceWikiNotion {
    private static final Logger log = LoggerFactory.getLogger(ServiceWikiNotion.class);
    private static final String NOTION_VERSION = "2022-06-28";
    private static final String BASE_URL_PAR_DEFAUT = "https://api.notion.com/v1";
    private static final String ID_PAGE_JEU_DES_TABLES = "3a77e0b4-d004-804b-9afc-e12e1a83ae56";

    private record PageWiki(String id, LienWikiView lien) {
    }

    private record ResultatRecherche(List<PageWiki> pages, Set<String> identifiantsExclus) {
    }

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
            ResultatRecherche resultat = toutesLesPages();
            pages = resultat.pages().stream()
                    .filter(page -> !resultat.identifiantsExclus().contains(page.id()))
                    .map(PageWiki::lien)
                    .toList();
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

    /**
     * Recherche toutes les pages et bases de donnees du wiki et deduit,
     * depuis les liens de parente renvoyes par l'API, les identifiants de
     * la page « Jeu des tables aleatoires » et de tout ce qu'elle contient
     * a tous les niveaux.
     */
    private ResultatRecherche toutesLesPages() {
        if (jeton.isBlank()) {
            log.debug("Aucun jeton Notion configure : aucun lien wiki renvoye.");
            return new ResultatRecherche(List.of(), Set.of(ID_PAGE_JEU_DES_TABLES));
        }
        List<PageWiki> pages = new ArrayList<>();
        Map<String, String> parents = new HashMap<>();
        String curseur = null;
        boolean encore = true;
        while (encore) {
            Map<String, Object> corps = new LinkedHashMap<>();
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
                    if (resultat instanceof Map<?, ?> objet) {
                        String id = objet.get("id") == null ? "" : objet.get("id").toString();
                        String parent = parentDe(objet);
                        if (parent != null && !id.isBlank()) {
                            parents.put(id, parent);
                        }
                        if ("page".equals(objet.get("object"))) {
                            pages.add(versPage(objet));
                        }
                    }
                }
            }
            encore = Boolean.TRUE.equals(reponse.get("has_more")) && reponse.get("next_cursor") instanceof String;
            if (reponse.get("next_cursor") instanceof String suivant) {
                curseur = suivant;
            }
        }
        return new ResultatRecherche(pages, identifiantsSousLaPageExclue(parents));
    }

    private static String parentDe(Map<?, ?> objet) {
        if (!(objet.get("parent") instanceof Map<?, ?> parent)) {
            return null;
        }
        if (parent.get("page_id") instanceof String idPage) {
            return idPage;
        }
        if (parent.get("database_id") instanceof String idBase) {
            return idBase;
        }
        return null;
    }

    /**
     * Parcourt les liens de parente vers le haut depuis chaque objet et
     * retient tout objet dont un ancetre est la page exclue, ainsi que la
     * page exclue elle-meme.
     */
    private Set<String> identifiantsSousLaPageExclue(Map<String, String> parents) {
        Set<String> exclus = new HashSet<>();
        exclus.add(ID_PAGE_JEU_DES_TABLES);
        for (Map.Entry<String, String> lien : parents.entrySet()) {
            Set<String> dejaVus = new HashSet<>();
            String id = lien.getKey();
            String pere = lien.getValue();
            while (pere != null && dejaVus.add(pere)) {
                if (pere.equals(ID_PAGE_JEU_DES_TABLES)) {
                    exclus.add(id);
                    break;
                }
                pere = parents.get(pere);
            }
        }
        return exclus;
    }

    private static PageWiki versPage(Map<?, ?> page) {
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
        return new PageWiki(page.get("id") == null ? "" : page.get("id").toString(),
                new LienWikiView(titre, url == null ? "" : url.toString()));
    }
}
