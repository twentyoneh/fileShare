package ru.twentyoneh.service;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenService {
    private final SecureRandom random = new SecureRandom();

    public String newToken(){
        byte[] token = new byte[18];
        random.nextBytes(token);
        return Base64.getEncoder().withoutPadding().encodeToString(token).replaceAll("/", "");
    }
}
