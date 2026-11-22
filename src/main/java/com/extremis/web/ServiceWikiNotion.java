package com.extremis.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
            pages = toutesLesPages().stream()
                    .filter(page -> !identifiantsExclus().contains(page.id()))
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
     * Identifiants de la page « Jeu des tables aleatoires » et de tout ce
     * qu'elle contient (sous-pages et sous-bases de donnees, recursivement).
     * Si l'interrogation echoue, seule la page racine est exclue.
     */
    private Set<String> identifiantsExclus() {
        Set<String> exclus = new HashSet<>();
        exclus.add(ID_PAGE_JEU_DES_TABLES);
        ArrayDeque<String> aExplorer = new ArrayDeque<>();
        aExplorer.add(ID_PAGE_JEU_DES_TABLES);
        Set<String> vus = new HashSet<>(exclus);
        try {
            while (!aExplorer.isEmpty()) {
                String id = aExplorer.poll();
                for (String enfant : enfantsDe(id)) {
                    if (vus.add(enfant)) {
                        exclus.add(enfant);
                        aExplorer.add(enfant);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de lister le contenu de la page exclue : {}", e.getMessage());
        }
        return exclus;
    }

    private List<String> enfantsDe(String idBloc) {
        List<String> enfants = new ArrayList<>();
        String curseur = null;
        boolean encore = true;
        while (encore) {
            StringBuilder uri = new StringBuilder("/blocks/").append(idBloc).append("/children?page_size=100");
            if (curseur != null) {
                uri.append("&start_cursor=").append(curseur);
            }
            Map<?, ?> reponse = client.get()
                    .uri(uri.toString())
                    .header("Authorization", "Bearer " + jeton)
                    .header("Notion-Version", NOTION_VERSION)
                    .retrieve()
                    .body(Map.class);
            if (reponse == null) {
                break;
            }
            if (reponse.get("results") instanceof List<?> resultats) {
                for (Object resultat : resultats) {
                    if (resultat instanceof Map<?, ?> bloc
                            && ("child_page".equals(bloc.get("type"))
                                || "child_database".equals(bloc.get("type")))) {
                        enfants.add(bloc.get("id").toString());
                    }
                }
            }
            encore = Boolean.TRUE.equals(reponse.get("has_more")) && reponse.get("next_cursor") instanceof String;
            if (reponse.get("next_cursor") instanceof String suivant) {
                curseur = suivant;
            }
        }
        return enfants;
    }

    private List<PageWiki> toutesLesPages() {
        if (jeton.isBlank()) {
            log.debug("Aucun jeton Notion configure : aucun lien wiki renvoye.");
            return List.of();
        }
        List<PageWiki> pages = new ArrayList<>();
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
                        pages.add(versPage(page));
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
