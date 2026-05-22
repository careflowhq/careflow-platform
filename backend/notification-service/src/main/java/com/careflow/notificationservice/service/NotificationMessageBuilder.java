package com.careflow.notificationservice.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Component
public class NotificationMessageBuilder {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.forLanguageTag("es-PE"))
                    .withZone(ZoneId.of("America/Lima"));

    public String buildMessage(String eventType, Map<String, Object> payload) {
        String patientName = stringValue(payload.get("patientName"), "paciente");
        String type = stringValue(payload.get("type"), "seguimiento");
        String typeLabel = followUpTypeLabel(type);
        String scheduled = formatDate(stringValue(payload.get("scheduledDate"), null));

        return switch (eventType) {
            case "followup.scheduled" -> String.format(
                    "Hola %s, desde tu consultorio te recordamos el seguimiento \"%s\" programado para %s. "
                            + "Si necesitas reprogramar, responde a este mensaje.",
                    patientName, typeLabel, scheduled);
            case "followup.missed" -> String.format(
                    "Hola %s, intentamos contactarte por el seguimiento \"%s\" del %s y no obtuvimos respuesta. "
                            + "¿Podés confirmarnos si todo está bien?",
                    patientName, typeLabel, scheduled);
            default -> "Hola " + patientName + ", tu consultorio tiene un mensaje para vos.";
        };
    }

    private String followUpTypeLabel(String type) {
        return switch (type) {
            case "POST_CONSULTATION" -> "seguimiento post consulta";
            case "APPOINTMENT_REMINDER" -> "recordatorio de cita";
            case "MEDICATION_CHECK" -> "control de medicación";
            case "GENERAL" -> "seguimiento general";
            default -> type;
        };
    }

    private String formatDate(String iso) {
        if (iso == null || iso.isBlank()) {
            return "próximamente";
        }
        return FORMATTER.format(Instant.parse(iso));
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }
}
