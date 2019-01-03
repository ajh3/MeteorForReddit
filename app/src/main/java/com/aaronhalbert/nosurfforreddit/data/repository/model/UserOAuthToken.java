package com.aaronhalbert.nosurfforreddit.data.repository.model;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("ALL")
public class UserOAuthToken {
    @SerializedName("access_token") private String accessToken;
    @SerializedName("token_type") private String tokenType;
    @SerializedName("expires_in") private int expiresIn;
    @SerializedName("scope") private String scope;
    @SerializedName("refresh_token") private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
