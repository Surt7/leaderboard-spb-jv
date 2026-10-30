package com.worldpinball.leaderboard.infrastructure.security.cryptography;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Implements deterministic JSON canonicalization (aligned with RFC 8785 - JSON Canonicalization Scheme).
 * Recursively sorts all JSON object keys in ordinal order to produce a reproducible canonical string
 * for ECDSA/SHA-256 signature verification.
 * Mirrors C# {@code WorldPinball.Infrastructure.Security.Cryptography.CanonicalJson}.
 */
public final class CanonicalJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CanonicalJson() {
    }

    public static String canonicalize(String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) {
            throw new IllegalArgumentException("rawJson cannot be null or empty.");
        }

        JsonNode node;
        try {
            node = MAPPER.readTree(rawJson);
            if (node == null || node.isMissingNode()) {
                throw new IllegalArgumentException("The provided text is invalid JSON.");
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("The provided text is invalid JSON.", ex);
        }

        if (node.isNull()) {
            return "null";
        }

        JsonNode sorted = sort(node);
        try {
            return MAPPER.writeValueAsString(sorted);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize canonical JSON", ex);
        }
    }

    private static JsonNode sort(JsonNode token) {
        if (token.isObject()) {
            ObjectNode sortedObj = JsonNodeFactory.instance.objectNode();
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> it = token.fields();
            while (it.hasNext()) {
                fields.add(it.next());
            }
            fields.sort(Comparator.comparing(Map.Entry::getKey));
            for (Map.Entry<String, JsonNode> entry : fields) {
                JsonNode val = entry.getValue();
                sortedObj.set(entry.getKey(), (val == null || val.isNull()) ? sortedObj.nullNode() : sort(val));
            }
            return sortedObj;
        }
        if (token.isArray()) {
            ArrayNode sortedArr = JsonNodeFactory.instance.arrayNode();
            for (JsonNode item : token) {
                sortedArr.add((item == null || item.isNull()) ? sortedArr.nullNode() : sort(item));
            }
            return sortedArr;
        }
        return token.deepCopy();
    }
}
