package br.com.sicredi.toolschallenge.shared.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * Deserializador customizado Jackson para converter String ou Number em BigDecimal.
 * 
 * Permite aceitar tanto:
 * - "500.50" (String)
 * - 500.50 (Number)
 * 
 * No JSON de entrada, facilitando integração com diferentes clientes.
 */
public class StringToBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText();
        
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido para BigDecimal: " + value, e);
        }
    }
}
