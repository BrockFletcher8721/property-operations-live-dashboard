package com.infrai.property;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.space();
        if (!parser.end()) throw new IllegalArgumentException("Unexpected JSON content");
        return value;
    }

    static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> fields = new ArrayList<>();
            map.forEach((key, item) -> fields.add(quote(String.valueOf(key)) + ":" + stringify(item)));
            return "{" + String.join(",", fields) + "}";
        }
        if (value instanceof Iterable<?> items) {
            List<String> values = new ArrayList<>();
            items.forEach(item -> values.add(stringify(item)));
            return "[" + String.join(",", values) + "]";
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c < 0x20 ? String.format("\\u%04x", (int) c) : c);
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String source;
        private int at;

        Parser(String source) { this.source = source; }
        boolean end() { return at == source.length(); }
        void space() { while (!end() && Character.isWhitespace(source.charAt(at))) at++; }

        Object value() {
            space();
            if (end()) throw new IllegalArgumentException("Expected JSON value");
            return switch (source.charAt(at)) {
                case '{' -> object();
                case '[' -> array();
                case '\"' -> string();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>();
            at++;
            space();
            if (take('}')) return result;
            do {
                space();
                String key = string();
                space();
                expect(':');
                result.put(key, value());
                space();
            } while (take(','));
            expect('}');
            return result;
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<>();
            at++;
            space();
            if (take(']')) return result;
            do { result.add(value()); space(); } while (take(','));
            expect(']');
            return result;
        }

        private String string() {
            expect('\"');
            StringBuilder out = new StringBuilder();
            while (!end()) {
                char c = source.charAt(at++);
                if (c == '\"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                char escaped = source.charAt(at++);
                switch (escaped) {
                    case '\"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> { out.append((char) Integer.parseInt(source.substring(at, at + 4), 16)); at += 4; }
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private Object number() {
            int start = at;
            while (!end() && "-+0123456789.eE".indexOf(source.charAt(at)) >= 0) at++;
            String token = source.substring(start, at);
            try { return token.contains(".") || token.contains("e") || token.contains("E")
                    ? Double.valueOf(token) : Long.valueOf(token); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid JSON number", e); }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, at)) throw new IllegalArgumentException("Invalid JSON literal");
            at += token.length();
            return value;
        }

        private boolean take(char expected) {
            if (!end() && source.charAt(at) == expected) { at++; return true; }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected '" + expected + "'");
        }
    }
}
