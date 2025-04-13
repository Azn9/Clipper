package dev.azn9.clipper.service;

import com.google.gson.Gson;
import dev.azn9.clipper.data.Configuration;
import dev.azn9.clipper.data.RefreshTokenResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

@Service
public class TokenRefreshService {

    private static final Logger LOGGER = LogManager.getLogger(TokenRefreshService.class);
    private static final Gson GSON = new Gson();
    private static final URI TOKEN_URI = URI.create("https://id.twitch.tv/oauth2/token");
    private static final URI VALIDATE_URI = URI.create("https://id.twitch.tv/oauth2/validate");

    private final Configuration configuration;
    private final WebhookService webhookService;
    private final TwitchClientService twitchClientService;

    private HttpClient client;
    private Timer timer;
    private TimerTask refreshTask;

    @Autowired
    public TokenRefreshService(Configuration configuration, WebhookService webhookService, TwitchClientService twitchClientService) {
        this.configuration = configuration;
        this.webhookService = webhookService;
        this.twitchClientService = twitchClientService;
    }

    @PostConstruct
    public void init() {
        this.client = HttpClient.newHttpClient();
        this.timer = new Timer();

        this.validateToken();
    }

    public CompletableFuture<Void> validateToken() {
        TokenRefreshService.LOGGER.info("Validating token...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(TokenRefreshService.VALIDATE_URI)
                .GET()
                .header("Authorization", "Bearer " + this.configuration.getAccessToken())
                .build();

        return this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String data = response.body();
                    TokenRefreshService.LOGGER.info("Received data: {}", data);
                    return TokenRefreshService.GSON.fromJson(data, RefreshTokenResponse.class);
                })
                .thenCompose(tokenResponse -> {
                    Long validity = tokenResponse.getExpiresIn();

                    if (tokenResponse.getError() != null || validity == null) {
                        return this.refreshToken();
                    }

                    return this.startRefreshTask(validity);
                });
    }

    @NotNull
    private CompletableFuture<Void> startRefreshTask(Long validity) {
        TokenRefreshService.LOGGER.info("Scheduling token refresh in {}s", validity);

        return CompletableFuture.runAsync(() -> {
            if (this.refreshTask != null) {
                this.refreshTask.cancel();
            }

            this.refreshTask = new TimerTask() {
                @Override
                public void run() {
                    TokenRefreshService.this.refreshToken();
                }
            };

            this.timer.schedule(this.refreshTask, validity * 1000);

            this.twitchClientService.restartClient();
        });
    }

    public CompletableFuture<Void> refreshToken() {
        TokenRefreshService.LOGGER.info("Refreshing token...");

        String body = "client_id=" + this.configuration.getClientId()
                + "&client_secret=" + this.configuration.getClientSecret()
                + "&grant_type=refresh_token"
                + "&refresh_token=" + this.configuration.getRefreshToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(TokenRefreshService.TOKEN_URI)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        return this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String data = response.body();

                    TokenRefreshService.LOGGER.info("Received data: {}", data);

                    return TokenRefreshService.GSON.fromJson(data, RefreshTokenResponse.class);
                })
                .thenCompose(tokenData -> {
                    Runnable emptyRunnable = () -> {
                    };

                    if (tokenData.getError() != null) {
                        TokenRefreshService.LOGGER.error(tokenData.getError());

                        return this.webhookService.sendError("Impossible de raffraichir le token : `" + tokenData.getError() + "`")
                                .thenRunAsync(emptyRunnable);
                    }

                    String newAccessToken = tokenData.getAccessToken();
                    String newRefreshToken = tokenData.getRefreshToken();
                    Long validity = tokenData.getExpiresIn();

                    if (newAccessToken != null && newRefreshToken != null && validity != null) {
                        return CompletableFuture.supplyAsync(() -> {
                            this.configuration.setAccessToken(newAccessToken);
                            this.configuration.setRefreshToken(newRefreshToken);

                            try {
                                Files.writeString(Path.of("config.json"), TokenRefreshService.GSON.toJson(this.configuration));
                            } catch (Exception e) {
                                TokenRefreshService.LOGGER.error(e);
                                return this.webhookService.sendError("Impossible de raffraichir le token : `" + e.getMessage() + "`")
                                        .thenRunAsync(emptyRunnable);
                            }

                            TokenRefreshService.LOGGER.info("Successfully refreshed token");

                            return this.startRefreshTask(validity);
                        }).thenCompose(f -> f);
                    } else {
                        TokenRefreshService.LOGGER.error("Missing data!");
                        return this.webhookService.sendError("Impossible de raffraichir le token : données de retour manquantes (plus d'informations dans les logs)")
                                .thenRunAsync(emptyRunnable);
                    }
                });
    }

    @PreDestroy
    public void destroy() {
        if (this.client != null) {
            this.client.close();
        }
    }

}
