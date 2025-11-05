package br.com.sicredi.toolschallenge.shared.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Deserializador customizado Jackson para converter String ou Number em Integer.
 * 
 * Permite aceitar tanto:
 * - "1" (String)
 * - 1 (Number)
 * 
 * No JSON de entrada, facilitando integração com diferentes clientes.
 */
public class StringToIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getText();
        
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido para Integer: " + value, e);
        }
    }
}
