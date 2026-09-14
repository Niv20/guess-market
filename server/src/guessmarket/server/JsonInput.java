package guessmarket.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Strict, bounded JSON input: missing primitives never silently become zero or false. */
final class JsonInput {
    private static final int MAX_JSON_BYTES = 65_536;
    private final JsonObject json;

    private JsonInput(JsonObject json) {
        this.json = json;
    }

    static JsonInput read(HttpServletRequest request) throws IOException {
        String type = request.getContentType();
        if (type == null || !type.split(";", 2)[0].strip().equalsIgnoreCase("application/json")) {
            throw new ApiException(415, "Send this request as application/json.");
        }
        if (request.getContentLengthLong() > MAX_JSON_BYTES) {
            throw new ApiException(413, "The request is too large.");
        }
        byte[] bytes = request.getInputStream().readNBytes(MAX_JSON_BYTES + 1);
        if (bytes.length > MAX_JSON_BYTES) {
            throw new ApiException(413, "The request is too large.");
        }
        try (JsonReader reader = new JsonReader(new StringReader(new String(bytes, StandardCharsets.UTF_8)))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement document = JsonParser.parseReader(reader);
            if (!document.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new ApiException(400, "The request must contain one JSON object.");
            }
            return new JsonInput(document.getAsJsonObject());
        } catch (com.google.gson.JsonParseException | IllegalStateException | IOException malformed) {
            throw new ApiException(400, "The request contains invalid JSON.");
        }
    }

    void requireSessionIdentity() {
        if (json.has("username") || json.has("userName") || json.has("marketMakerName")) {
            throw new ApiException(400, "User identity comes from your session and must not be supplied in this request.");
        }
    }

    String string(String field) {
        JsonPrimitive value = primitive(field);
        if (!value.isString()) {
            throw invalid(field, "text");
        }
        return value.getAsString();
    }

    int integer(String field) {
        try {
            return decimal(field).intValueExact();
        } catch (ArithmeticException invalid) {
            throw invalid(field, "a whole number within the integer range");
        }
    }

    long longInteger(String field) {
        try {
            return decimal(field).longValueExact();
        } catch (ArithmeticException invalid) {
            throw invalid(field, "a whole number within the long integer range");
        }
    }

    double number(String field) {
        double value = decimal(field).doubleValue();
        if (!Double.isFinite(value)) {
            throw invalid(field, "a finite number");
        }
        return value;
    }

    boolean bool(String field) {
        JsonPrimitive value = primitive(field);
        if (!value.isBoolean()) {
            throw invalid(field, "true or false");
        }
        return value.getAsBoolean();
    }

    <E extends Enum<E>> E enumeration(String field, Class<E> type) {
        try {
            return Enum.valueOf(type, string(field).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new ApiException(400, "Unsupported value for '" + field + "'.");
        }
    }

    List<String> strings(String field) {
        JsonElement value = required(field);
        if (!value.isJsonArray()) {
            throw invalid(field, "a list of text values");
        }
        List<String> strings = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw invalid(field, "a list of text values");
            }
            strings.add(element.getAsString());
        }
        return List.copyOf(strings);
    }

    private BigDecimal decimal(String field) {
        JsonPrimitive value = primitive(field);
        if (!value.isNumber()) {
            throw invalid(field, "a number");
        }
        try {
            return value.getAsBigDecimal();
        } catch (NumberFormatException invalid) {
            throw invalid(field, "a number");
        }
    }

    private JsonPrimitive primitive(String field) {
        JsonElement value = required(field);
        if (!value.isJsonPrimitive()) {
            throw new ApiException(400, "'" + field + "' must be a simple value.");
        }
        return value.getAsJsonPrimitive();
    }

    private JsonElement required(String field) {
        JsonElement value = json.get(field);
        if (value == null || value.isJsonNull()) {
            throw new ApiException(400, "The request is missing '" + field + "'.");
        }
        return value;
    }

    private ApiException invalid(String field, String expected) {
        return new ApiException(400, "'" + field + "' must be " + expected + ".");
    }
}
