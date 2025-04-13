package dev.azn9.clipper.service;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import dev.azn9.clipper.data.Configuration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class WebhookService {

    private final Configuration configuration;
    private WebhookClient client;

    @Autowired
    public WebhookService(Configuration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    private void init() {
        this.client = WebhookClient.withUrl(this.configuration.getWebhookUrl());
    }

    @PreDestroy
    private void destroy() {
        this.client.close();
    }

    @NotNull
    public CompletableFuture<ReadonlyMessage> sendNewClip(String clipId, String clipEditUrl) {
        return this.client.send(new WebhookMessageBuilder()
                .setContent("""
                        Nouveau clip : https://www.twitch.tv/%s/clip/%s
                        -# Lien d'édition : <%s>
                        """
                        .formatted(
                                this.configuration.getStreamerUsername(),
                                clipId,
                                clipEditUrl
                        ))
                .build());
    }

    @NotNull
    public CompletableFuture<ReadonlyMessage> sendError(String message) {
        return this.client.send(new WebhookMessageBuilder()
                .addEmbeds(new WebhookEmbedBuilder()
                        .setTitle(new WebhookEmbed.EmbedTitle("Une erreur est survenue", null))
                        .setDescription(message)
                        .setColor(10038562)
                        .build())
                .build());
    }

}
