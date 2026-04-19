package dev.azn9.clipper.data;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public class Configuration {

    private static final Gson GSON = new Gson();

    private String accessToken;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String streamerUsername;
    private String streamerId;
    private String webhookUrl;
    private Boolean canEveryoneUse;
    private Boolean canVipUse;
    private Boolean canModeratorUse;
    private Set<String> allowedUsers;

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getStreamerUsername() {
        return this.streamerUsername;
    }

    public String getStreamerId() {
        return this.streamerId;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public boolean getCanEveryoneUse() {
        return !Boolean.FALSE.equals(this.canEveryoneUse);
    }

    public boolean getCanVipUse() {
        return !Boolean.FALSE.equals(this.canVipUse);
    }

    public boolean getCanModeratorUse() {
        return !Boolean.FALSE.equals(this.canModeratorUse);
    }

    public Set<String> getAllowedUsers() {
        return Optional.ofNullable(this.allowedUsers).orElse(Set.of());
    }

    public void setAllowedUsers(Set<String> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    public void save() throws IOException {
        Files.writeString(Path.of("config.json"), Configuration.GSON.toJson(this));
    }
}
