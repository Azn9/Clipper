package dev.azn9.clipper.data;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenResponse {

    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("refresh_token")
    private String refreshToken;
    private String error;
    @SerializedName("expires_in")
    private Long expiresIn;

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String getError() {
        return this.error;
    }

    public Long getExpiresIn() {
        return this.expiresIn;
    }
}
