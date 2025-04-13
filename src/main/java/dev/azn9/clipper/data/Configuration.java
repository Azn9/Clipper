package dev.azn9.clipper.data;

public class Configuration {

    private String accessToken;
    private String refreshToken;
    private String clientId;
    private String clientSecret;
    private String streamerUsername;
    private String streamerId;
    private String webhookUrl;

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
}
