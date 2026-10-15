package com.peng.aicommit.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JSON 读写实现，避免依赖平台内部的 gson 版本。
 * 仅支持标准 JSON 子集：对象、数组、字符串、数字、布尔、null。
 */
public final class Json {
    private Json() {
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String) {
            writeString(sb, (String) value);
            return;
        }
        if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
            return;
        }
        if (value instanceof Map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
            return;
        }
        if (value instanceof Iterable) {
            sb.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
            return;
        }
        writeString(sb, String.valueOf(value));
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.eof()) {
            throw new IllegalArgumentException("JSON 解析失败：存在多余内容");
        }
        return value;
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private boolean eof() {
            return index >= text.length();
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private char peek() {
            if (eof()) {
                throw new IllegalArgumentException("JSON 解析失败：内容意外结束");
            }
            return text.charAt(index);
        }

        private void expect(String literal) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("JSON 解析失败：期望 " + literal);
            }
            index += literal.length();
        }

        private Object readValue() {
            skipWhitespace();
            char c = peek();
            switch (c) {
                case '{':
                    return readObject();
                case '[':
                    return readArray();
                case '"':
                    return readString();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    return readNumber();
            }
        }

        private Map<String, Object> readObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            if (peek() == '}') {
                index++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                if (peek() != ':') {
                    throw new IllegalArgumentException("JSON 解析失败：对象缺少冒号");
                }
                index++;
                result.put(key, readValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    index++;
                    continue;
                }
                if (c == '}') {
                    index++;
                    return result;
                }
                throw new IllegalArgumentException("JSON 解析失败：对象分隔符错误");
            }
        }

        private List<Object> readArray() {
            List<Object> result = new ArrayList<>();
            index++;
            skipWhitespace();
            if (peek() == ']') {
                index++;
                return result;
            }
            while (true) {
                result.add(readValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    index++;
                    continue;
                }
                if (c == ']') {
                    index++;
                    return result;
                }
                throw new IllegalArgumentException("JSON 解析失败：数组分隔符错误");
            }
        }

        private String readString() {
            if (peek() != '"') {
                throw new IllegalArgumentException("JSON 解析失败：期望字符串");
            }
            index++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (eof()) {
                    throw new IllegalArgumentException("JSON 解析失败：字符串未闭合");
                }
                char c = text.charAt(index++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (eof()) {
                    throw new IllegalArgumentException("JSON 解析失败：转义未结束");
                }
                char esc = text.charAt(index++);
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException("JSON 解析失败：\\u 转义不完整");
                        }
                        sb.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        index += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("JSON 解析失败：未知转义 \\" + esc);
                }
            }
        }

        private Object readNumber() {
            int start = index;
            if (!eof() && (peek() == '-' || peek() == '+')) {
                index++;
            }
            while (!eof()) {
                char c = text.charAt(index);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                    index++;
                } else {
                    break;
                }
            }
            if (start == index) {
                throw new IllegalArgumentException("JSON 解析失败：非法字符 " + peek());
            }
            return Double.valueOf(text.substring(start, index));
        }
    }
}
