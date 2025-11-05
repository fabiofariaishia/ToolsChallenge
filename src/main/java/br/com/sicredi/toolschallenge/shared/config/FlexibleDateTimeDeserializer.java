package br.com.sicredi.toolschallenge.shared.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Deserializador customizado Jackson para converter String em OffsetDateTime.
 * 
 * Aceita múltiplos formatos:
 * - "dd/MM/yyyy HH:mm:ss" (ex: "01/05/2021 18:30:00")
 * - ISO 8601 (ex: "2021-05-01T18:30:00-03:00")
 * 
 * Facilita integração com diferentes clientes que podem enviar datas em formatos variados.
 */
public class FlexibleDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    private static final DateTimeFormatter FORMATTER_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText();
        
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        try {
            // Tentar formato brasileiro "dd/MM/yyyy HH:mm:ss"
            if (value.contains("/")) {
                LocalDateTime localDateTime = LocalDateTime.parse(value, FORMATTER_BR);
                // Usar timezone padrão do sistema ou América/São_Paulo
                return localDateTime.atZone(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime();
            }
            
            // Tentar formato ISO 8601
            return OffsetDateTime.parse(value, FORMATTER_ISO);
            
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Formato de data inválido: " + value + ". " +
                "Use 'dd/MM/yyyy HH:mm:ss' ou ISO 8601 (ex: '2021-05-01T18:30:00-03:00')", e
            );
        }
    }
}
