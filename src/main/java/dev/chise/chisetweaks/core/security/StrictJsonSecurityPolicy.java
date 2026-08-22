package dev.chise.chisetweaks.core.security;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * ローカルJSON設定を厳格に事前検証し、重複キーや不正な構造を本処理へ渡さない。
 */
public final class StrictJsonSecurityPolicy {
    public static final int MAX_OBJECT_MEMBERS = 2_048;
    public static final int MAX_ARRAY_ELEMENTS = 4_096;

    private StrictJsonSecurityPolicy() {
    }

    public static Validation validateObjectDocument(String json) {
        JsonStructureBudgetPolicy.Validation budget = JsonStructureBudgetPolicy.validate(json);
        if (!budget.valid()) return Validation.rejected(budget.reason());

        try {
            Parser parser = new Parser(Objects.requireNonNullElse(json, ""));
            parser.skipWhitespace();
            ValueKind root = parser.parseValue(0);
            if (root != ValueKind.OBJECT) return Validation.rejected("root must be an object");
            parser.skipWhitespace();
            if (!parser.atEnd()) return Validation.rejected("trailing content");
            return Validation.accepted();
        } catch (Rejected rejected) {
            return Validation.rejected(rejected.getMessage());
        }
    }

    public record Validation(boolean valid, String reason) {
        public Validation {
            reason = Objects.requireNonNullElse(reason, "");
        }

        public static Validation accepted() {
            return new Validation(true, "");
        }

        public static Validation rejected(String reason) {
            return new Validation(false, reason);
        }
    }

    private enum ValueKind { OBJECT, ARRAY, SCALAR }

    private static final class Rejected extends Exception {
        private Rejected(String message) {
            super(message);
        }
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private boolean atEnd() {
            return index >= input.length();
        }

        private void skipWhitespace() {
            while (!atEnd()) {
                char value = input.charAt(index);
                if (value == ' ' || value == '\t' || value == '\r' || value == '\n') {
                    index++;
                } else {
                    return;
                }
            }
        }

        private ValueKind parseValue(int depth) throws Rejected {
            if (depth > JsonStructureBudgetPolicy.MAX_NESTING_DEPTH) {
                throw reject("nesting budget exceeded");
            }
            skipWhitespace();
            if (atEnd()) throw reject("missing value");
            return switch (input.charAt(index)) {
                case '{' -> parseObject(depth + 1);
                case '[' -> parseArray(depth + 1);
                case '"' -> {
                    parseString();
                    yield ValueKind.SCALAR;
                }
                case 't' -> {
                    consumeLiteral("true");
                    yield ValueKind.SCALAR;
                }
                case 'f' -> {
                    consumeLiteral("false");
                    yield ValueKind.SCALAR;
                }
                case 'n' -> {
                    consumeLiteral("null");
                    yield ValueKind.SCALAR;
                }
                default -> {
                    parseNumber();
                    yield ValueKind.SCALAR;
                }
            };
        }

        private ValueKind parseObject(int depth) throws Rejected {
            expect('{');
            skipWhitespace();
            if (consumeIf('}')) return ValueKind.OBJECT;

            Set<String> keys = new HashSet<>();
            int members = 0;
            while (true) {
                skipWhitespace();
                if (atEnd() || input.charAt(index) != '"') throw reject("object key must be a string");
                String rawKey = parseString();
                if (!RuntimeSecurityPolicy.isSafeJsonObjectKey(rawKey)) {
                    throw reject("unsafe object key");
                }
                String key = Normalizer.normalize(rawKey, Normalizer.Form.NFC);
                if (!keys.add(key)) throw reject("duplicate object key");
                members++;
                if (members > MAX_OBJECT_MEMBERS) throw reject("object member budget exceeded");

                skipWhitespace();
                expect(':');
                parseValue(depth);
                skipWhitespace();
                if (consumeIf('}')) return ValueKind.OBJECT;
                expect(',');
            }
        }

        private ValueKind parseArray(int depth) throws Rejected {
            expect('[');
            skipWhitespace();
            if (consumeIf(']')) return ValueKind.ARRAY;

            int elements = 0;
            while (true) {
                parseValue(depth);
                elements++;
                if (elements > MAX_ARRAY_ELEMENTS) throw reject("array element budget exceeded");
                skipWhitespace();
                if (consumeIf(']')) return ValueKind.ARRAY;
                expect(',');
            }
        }

        private String parseString() throws Rejected {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (!atEnd()) {
                char character = input.charAt(index++);
                if (character == '"') return value.toString();
                if (character == '\\') {
                    if (atEnd()) throw reject("unterminated escape");
                    char escaped = input.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> value.append(escaped);
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'n' -> value.append('\n');
                        case 'r' -> value.append('\r');
                        case 't' -> value.append('\t');
                        case 'u' -> appendEscapedUnicode(value);
                        default -> throw reject("invalid escape sequence");
                    }
                    continue;
                }
                if (character < 0x20) throw reject("unescaped control character");
                if (Character.isHighSurrogate(character)) {
                    if (atEnd()) throw reject("unpaired high surrogate");
                    char low = input.charAt(index++);
                    if (!Character.isLowSurrogate(low)) throw reject("unpaired high surrogate");
                    value.append(character).append(low);
                } else if (Character.isLowSurrogate(character)) {
                    throw reject("unpaired low surrogate");
                } else {
                    value.append(character);
                }
                if (value.length() > JsonStructureBudgetPolicy.MAX_STRING_CHARS) {
                    throw reject("string budget exceeded");
                }
            }
            throw reject("unterminated string");
        }

        private void appendEscapedUnicode(StringBuilder value) throws Rejected {
            char first = readHexCodeUnit();
            if (Character.isHighSurrogate(first)) {
                if (index + 1 >= input.length() || input.charAt(index) != '\\' || input.charAt(index + 1) != 'u') {
                    throw reject("unpaired escaped high surrogate");
                }
                index += 2;
                char second = readHexCodeUnit();
                if (!Character.isLowSurrogate(second)) throw reject("invalid escaped surrogate pair");
                value.append(first).append(second);
            } else if (Character.isLowSurrogate(first)) {
                throw reject("unpaired escaped low surrogate");
            } else {
                value.append(first);
            }
            if (value.length() > JsonStructureBudgetPolicy.MAX_STRING_CHARS) {
                throw reject("string budget exceeded");
            }
        }

        private char readHexCodeUnit() throws Rejected {
            if (index + 4 > input.length()) throw reject("short Unicode escape");
            int result = 0;
            for (int offset = 0; offset < 4; offset++) {
                int digit = Character.digit(input.charAt(index++), 16);
                if (digit < 0) throw reject("invalid Unicode escape");
                result = (result << 4) | digit;
            }
            return (char) result;
        }

        private void parseNumber() throws Rejected {
            int start = index;
            consumeIf('-');
            if (atEnd()) throw reject("invalid number");

            if (consumeIf('0')) {
                if (!atEnd() && Character.isDigit(input.charAt(index))) {
                    throw reject("leading zero in number");
                }
            } else {
                if (atEnd() || input.charAt(index) < '1' || input.charAt(index) > '9') {
                    throw reject("invalid number");
                }
                while (!atEnd() && Character.isDigit(input.charAt(index))) index++;
            }

            if (consumeIf('.')) {
                int fractionStart = index;
                while (!atEnd() && Character.isDigit(input.charAt(index))) index++;
                if (fractionStart == index) throw reject("invalid number fraction");
            }

            if (!atEnd() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                index++;
                if (!atEnd() && (input.charAt(index) == '+' || input.charAt(index) == '-')) index++;
                int exponentStart = index;
                while (!atEnd() && Character.isDigit(input.charAt(index))) index++;
                if (exponentStart == index) throw reject("invalid number exponent");
            }

            if (start == index) throw reject("invalid value");
        }

        private void consumeLiteral(String literal) throws Rejected {
            if (!input.regionMatches(index, literal, 0, literal.length())) {
                throw reject("invalid literal");
            }
            index += literal.length();
        }

        private void expect(char expected) throws Rejected {
            if (atEnd() || input.charAt(index) != expected) {
                throw reject("expected '" + expected + "'");
            }
            index++;
        }

        private boolean consumeIf(char expected) {
            if (!atEnd() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private Rejected reject(String reason) {
            return new Rejected(reason + " at index " + index);
        }
    }
}
