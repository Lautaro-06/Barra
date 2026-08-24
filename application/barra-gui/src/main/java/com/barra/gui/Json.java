package com.barra.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Json
 *
 * Parser/writer JSON casero, sin dependencias externas (evita atar el
 * proyecto a Maven Central para algo tan chico). Soporta lo que necesita
 * este proyecto: objetos, arrays, strings, números, booleans y null.
 *
 * Si el proyecto crece y hace falta algo más robusto, se puede reemplazar
 * por org.json o Jackson sumándolo al pom.xml sin tocar el resto del código
 * (ApiClient es el único lugar que usa esta clase).
 */
public final class Json {

    private Json() {}

    // ---------------- Parsing ----------------

    public static Object parse(String text) {
        return new Parser(text).parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        return (Map<String, Object>) parse(text);
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String text) {
        return (List<Object>) parse(text);
    }

    private static class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) { this.s = s; }

        Object parseValue() {
            skipWs();
            char c = s.charAt(i);
            if (c == '{') return parseObj();
            if (c == '[') return parseArr();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') { i += 4; return null; }
            return parseNumber();
        }

        Map<String, Object> parseObj() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            skipWs();
            if (s.charAt(i) == '}') { i++; return map; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                i++; // :
                Object val = parseValue();
                map.put(key, val);
                skipWs();
                if (s.charAt(i) == ',') { i++; continue; }
                if (s.charAt(i) == '}') { i++; break; }
            }
            return map;
        }

        List<Object> parseArr() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skipWs();
            if (s.charAt(i) == ']') { i++; return list; }
            while (true) {
                Object val = parseValue();
                list.add(val);
                skipWs();
                if (s.charAt(i) == ',') { i++; continue; }
                if (s.charAt(i) == ']') { i++; break; }
            }
            return list;
        }

        String parseString() {
            skipWs();
            i++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (s.charAt(i) != '"') {
                char c = s.charAt(i);
                if (c == '\\') {
                    i++;
                    char esc = s.charAt(i);
                    switch (esc) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'u' -> {
                            String hex = s.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
                i++;
            }
            i++; // closing quote
            return sb.toString();
        }

        Boolean parseBool() {
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            i += 5; return Boolean.FALSE;
        }

        Double parseNumber() {
            int start = i;
            while (i < s.length() && "-+.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(start, i));
        }

        void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }

    // ---------------- Writing ----------------

    public static String writeObject(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(quote(e.getKey())).append(":").append(writeValue(e.getValue()));
        }
        return sb.append("}").toString();
    }

    private static String writeValue(Object v) {
        if (v == null) return "null";
        if (v instanceof String str) return quote(str);
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        if (v instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mm = (Map<String, Object>) m;
            return writeObject(mm);
        }
        if (v instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int j = 0; j < list.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append(writeValue(list.get(j)));
            }
            return sb.append("]").toString();
        }
        return quote(v.toString());
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }
}
