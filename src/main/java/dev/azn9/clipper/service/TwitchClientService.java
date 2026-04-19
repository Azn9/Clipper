package dev.azn9.clipper.service;

import club.minnced.discord.webhook.receive.ReadonlyMessage;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.enums.CommandPermission;
import com.github.twitch4j.helix.domain.CreateClip;
import com.github.twitch4j.helix.domain.CreateClipList;
import dev.azn9.clipper.data.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class TwitchClientService {

    private static final Logger LOGGER = LogManager.getLogger(TwitchClientService.class);

    private final Configuration configuration;
    private final WebhookService webhookService;
    private TwitchClient twitchClient;

    @Autowired
    public TwitchClientService(Configuration configuration, WebhookService webhookService) {
        this.configuration = configuration;
        this.webhookService = webhookService;
    }

    private void startBot() {
        TwitchClientService.LOGGER.info("Starting twitch client...");

        TwitchClientBuilder clientBuilder = TwitchClientBuilder.builder();

        OAuth2Credential credential = new OAuth2Credential(
                "twitch",
                this.configuration.getAccessToken()
        );

        this.twitchClient = clientBuilder
                .withClientId(this.configuration.getAccessToken())
                .withClientSecret(this.configuration.getRefreshToken())
                .withEnableHelix(true)
                .withChatAccount(credential)
                .withEnableChat(true)
                .build();

        SimpleEventHandler eventHandler = this.twitchClient.getEventManager().getEventHandler(SimpleEventHandler.class);
        eventHandler.onEvent(ChannelMessageEvent.class, event -> {
            String message = event.getMessage();
            if (message == null) {
                return;
            }

            if (message.startsWith("!clip")) {
                boolean canEveryoneUse = this.configuration.getCanEveryoneUse();
                boolean canModUse = this.configuration.getCanModeratorUse();
                boolean isMod = event.getPermissions().contains(CommandPermission.MODERATOR);
                boolean canVipUse = this.configuration.getCanVipUse();
                boolean isVip = event.getPermissions().contains(CommandPermission.VIP);
                boolean isBroadcaster = event.getPermissions().contains(CommandPermission.BROADCASTER);
                boolean isAllowedUser = this.configuration.getAllowedUsers().stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).equals(event.getUser().getName().toLowerCase(Locale.ROOT)));

                boolean canBeUsed = canEveryoneUse
                        || (canModUse && isMod)
                        || (canVipUse && isVip)
                        || isAllowedUser
                        || isBroadcaster;

                TwitchClientService.LOGGER.debug("""
                        Received clip command from {}. Permissions check:
                        - IsMod: {} / CanModUse: {}
                        - IsVip: {} / CanVipUse: {}
                        - IsBroadcaster: {}
                        - IsAllowedUser: {}
                        Result: {}
                        """,
                        event.getUser().getName(),
                        isMod, canModUse,
                        isVip, canVipUse,
                        isBroadcaster,
                        isAllowedUser,
                        canBeUsed
                );

                if (!canBeUsed) {
                    return;
                }

                this.createClip()
                        .thenCompose(createClips -> {
                            List<CompletableFuture<ReadonlyMessage>> futures = new ArrayList<>();

                            for (CreateClip clip : createClips) {
                                TwitchClientService.LOGGER.info("Created clip {}", clip.getEditUrl());
                                futures.add(this.webhookService.sendNewClip(clip.getId(), clip.getEditUrl()));
                            }

                            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                    .thenRunAsync(() -> {
                                        event.reply(event.getTwitchChat(), "Clip créé et enregistré sur discord !");
                                    });
                        })
                        .exceptionallyCompose(throwable -> {
                            return CompletableFuture.runAsync(() -> {
                                TwitchClientService.LOGGER.error(throwable);
                            });
                        });
            } else if (message.startsWith("!addclipper")) {
                if (!event.getPermissions().contains(CommandPermission.MODERATOR)) {
                    return;
                }

                String[] split = message.split(" ");
                if (split.length != 2) {
                    event.reply(event.getTwitchChat(), "Erreur de syntaxe : !addclipper <username>");
                    return;
                }

                Set<String> list = new HashSet<>(this.configuration.getAllowedUsers());
                list.add(split[1]);
                this.configuration.setAllowedUsers(list);

                try {
                    this.configuration.save();
                } catch (Exception e) {
                    TwitchClientService.LOGGER.error(e);
                    event.reply(event.getTwitchChat(), "Une erreur est survenue");
                    return;
                }

                event.reply(event.getTwitchChat(), split[1] + " a été ajouté à la liste des utilisateurs autorisés");
            } else if (message.startsWith("!removeclipper")) {
                if (!event.getPermissions().contains(CommandPermission.MODERATOR)) {
                    return;
                }

                String[] split = message.split(" ");
                if (split.length != 2) {
                    event.reply(event.getTwitchChat(), "Erreur de syntaxe : !removeclipper <username>");
                    return;
                }

                Set<String> list = new HashSet<>(this.configuration.getAllowedUsers());
                list.remove(split[1]);
                this.configuration.setAllowedUsers(list);

                try {
                    this.configuration.save();
                } catch (Exception e) {
                    TwitchClientService.LOGGER.error(e);
                    event.reply(event.getTwitchChat(), "Une erreur est survenue");
                    return;
                }

                event.reply(event.getTwitchChat(), split[1] + " a été retiré de la liste des utilisateurs autorisés");
            }
        });

        this.twitchClient.getChat().joinChannel(this.configuration.getStreamerUsername());
    }

    private void stopBot() {
        if (this.twitchClient != null) {
            this.twitchClient.close();
        }
    }

    public void restartClient() {
        this.stopBot();
        this.startBot();
    }

    public CompletableFuture<List<CreateClip>> createClip() {
        if (this.twitchClient == null) {
            TwitchClientService.LOGGER.error("Clip creation failed: Twitch client is null!");
            return CompletableFuture.supplyAsync(List::of);
        }

        return CompletableFuture.supplyAsync(() -> {
            return this.twitchClient.getHelix()
                    .createClip(this.configuration.getAccessToken(), this.configuration.getStreamerId(), true)
                    .execute();
        }).thenApply(CreateClipList::getData);
    }
}
