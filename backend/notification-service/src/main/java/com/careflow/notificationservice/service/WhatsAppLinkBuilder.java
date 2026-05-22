package com.careflow.notificationservice.service;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class WhatsAppLinkBuilder {

    public String buildLink(String phoneNumber, String message) {
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        // Perú: celular 9 dígitos empezando en 9 → anteponer 51 si falta código de país
        if (digits.length() == 9 && digits.startsWith("9")) {
            digits = "51" + digits;
        }
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        return "https://wa.me/" + digits + "?text=" + encoded;
    }
}
