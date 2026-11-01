package com.extremis.web;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceWikiNotionTest {

    private HttpServer serveur;
    private String baseUrl;
    private volatile String corpsReponse;

    @BeforeEach
    void demarrerFauxNotion() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/v1/search", echange -> {
            String reponse = corpsReponse;
            byte[] octets = reponse.getBytes(StandardCharsets.UTF_8);
            echange.getResponseHeaders().add("Content-Type", "application/json");
            echange.sendResponseHeaders(200, octets.length);
            try (OutputStream corps = echange.getResponseBody()) {
                corps.write(octets);
            }
        });
        serveur.start();
        baseUrl = "http://127.0.0.1:" + serveur.getAddress().getPort() + "/v1";
    }

    @AfterEach
    void arreterFauxNotion() {
        serveur.stop(0);
    }

    private static String pageJson(String id, String titre) {
        return """
                {
                  "object": "page",
                  "id": "%s",
                  "url": "https://www.notion.so/%s",
                  "properties": {
                    "title": {
                      "type": "title",
                      "title": [{"plain_text": "%s"}]
                    }
                  }
                }
                """.formatted(id, titre.replace(' ', '-'), titre);
    }

    private static String reponse(String... pages) {
        return "{\"results\": [" + String.join(",", pages) + "], \"has_more\": false, \"next_cursor\": null}";
    }

    @Test
    void renvoieCinqLiensTiresParmiToutesLesPages() {
        StringBuilder toutes = new StringBuilder();
        for (int i = 1; i <= 8; i++) {
            if (i > 1) {
                toutes.append(',');
            }
            toutes.append(pageJson("page-" + i, "Page " + i));
        }
        corpsReponse = "{\"results\": [" + toutes + "], \"has_more\": false, \"next_cursor\": null}";
        ServiceWikiNotion service = new ServiceWikiNotion("jeton-test", baseUrl);

        List<LienWikiView> liens = service.liensAleatoires(5);

        assertThat(liens).hasSize(5);
        assertThat(liens).allSatisfy(lien -> {
            assertThat(lien.titre()).startsWith("Page ");
            assertThat(lien.url()).startsWith("https://www.notion.so/");
        });
        assertThat(liens.stream().map(LienWikiView::titre).distinct()).hasSize(5);
    }

    @Test
    void deuxAppelsSuccessifsNeRenvoientGeneralementPasLaMemeListe() {
        StringBuilder toutes = new StringBuilder();
        for (int i = 1; i <= 20; i++) {
            if (i > 1) {
                toutes.append(',');
            }
            toutes.append(pageJson("page-" + i, "Page " + i));
        }
        corpsReponse = "{\"results\": [" + toutes + "], \"has_more\": false, \"next_cursor\": null}";
        ServiceWikiNotion service = new ServiceWikiNotion("jeton-test", baseUrl);

        List<LienWikiView> premier = service.liensAleatoires(5);
        List<LienWikiView> second = service.liensAleatoires(5);

        assertThat(premier).hasSize(5);
        assertThat(second).hasSize(5);
        assertThat(premier.stream().map(LienWikiView::titre).toList())
                .isNotEqualTo(second.stream().map(LienWikiView::titre).toList());
    }

    @Test
    void sansJetonAucunLienNEstRenvoye() {
        ServiceWikiNotion service = new ServiceWikiNotion("", baseUrl);
        assertThat(service.liensAleatoires(5)).isEmpty();
    }

    @Test
    void siLeWikiRenvoieMoinsDeCinqPagesToutesSontRenvoyees() {
        corpsReponse = reponse(pageJson("a", "Alpha"), pageJson("b", "Beta"));
        ServiceWikiNotion service = new ServiceWikiNotion("jeton-test", baseUrl);
        assertThat(service.liensAleatoires(5))
                .extracting(LienWikiView::titre)
                .containsExactlyInAnyOrder("Alpha", "Beta");
    }
}
