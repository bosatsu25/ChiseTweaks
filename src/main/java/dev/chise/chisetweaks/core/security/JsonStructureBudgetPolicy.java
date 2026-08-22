package dev.chise.chisetweaks.core.security;

import java.util.Objects;

/**
 * Gsonで解析する前に、敵対的または破損したJSONを拒否する。
 *
 * <p>走査用状態オブジェクト1個だけを確保して入力を1回線形走査し、構造の対応関係に加えて
 * ネスト深度・トークン数・文字列長の明示的な上限を検証する。</p>
 */
public final class JsonStructureBudgetPolicy {
    public static final int MAX_NESTING_DEPTH = 32;
    public static final int MAX_TOKENS = 16_384;
    public static final int MAX_STRING_CHARS = 4_096;

    private JsonStructureBudgetPolicy() {
    }

    public static Validation validate(String json) {
        return new BudgetScanner(Objects.requireNonNullElse(json, "")).scan();
    }

    private static final class BudgetScanner {
        private final String json;
        private int currentIndex;
        private int nestingDepth;
        private int tokenCount;
        private int currentStringChars;
        private long objectNestingMask;
        private boolean insideString;
        private boolean previousCharacterEscaped;

        private BudgetScanner(String json) {
            this.json = json;
        }

        private Validation scan() {
            for (currentIndex = 0; currentIndex < json.length(); currentIndex++) {
                char character = json.charAt(currentIndex);
                Validation characterValidation = scanCharacter(character);
                if (!characterValidation.valid()) {
                    return characterValidation;
                }
            }
            return validateCompletedInput();
        }

        private Validation scanCharacter(char character) {
            if (character == '\u0000') {
                return Validation.rejected("NUL character");
            }
            if (insideString) {
                return scanStringCharacter(character);
            }
            return scanStructuralCharacter(character);
        }

        private Validation scanStringCharacter(char character) {
            if (previousCharacterEscaped) {
                previousCharacterEscaped = false;
                return countStringCharacter();
            }
            if (character == '\\') {
                previousCharacterEscaped = true;
                return Validation.accepted();
            }
            if (character == '"') {
                insideString = false;
                currentStringChars = 0;
                return countToken();
            }
            if (Character.isISOControl(character)) {
                return Validation.rejected("unescaped control character");
            }
            return countStringCharacter();
        }

        private Validation scanStructuralCharacter(char character) {
            return switch (character) {
                case '"' -> beginString();
                case '{' -> openContainer(true);
                case '[' -> openContainer(false);
                case '}' -> closeContainer(true);
                case ']' -> closeContainer(false);
                case ',', ':' -> countToken();
                default -> scanWhitespaceOrLiteral(character);
            };
        }

        private Validation beginString() {
            insideString = true;
            currentStringChars = 0;
            return Validation.accepted();
        }

        private Validation openContainer(boolean opensObject) {
            if (nestingDepth >= MAX_NESTING_DEPTH) {
                return Validation.rejected("nesting budget exceeded");
            }
            if (opensObject) {
                objectNestingMask |= 1L << nestingDepth;
            }
            nestingDepth++;
            return countToken();
        }

        private Validation closeContainer(boolean closesObject) {
            if (nestingDepth == 0) {
                return Validation.rejected("unbalanced closing token");
            }

            nestingDepth--;
            boolean openedObject = (objectNestingMask & (1L << nestingDepth)) != 0L;
            if (closesObject != openedObject) {
                return Validation.rejected("mismatched closing token");
            }
            objectNestingMask &= ~(1L << nestingDepth);
            return countToken();
        }

        private Validation scanWhitespaceOrLiteral(char character) {
            if (Character.isWhitespace(character)) {
                return Validation.accepted();
            }

            skipRemainingLiteralCharacters();
            return countToken();
        }

        private void skipRemainingLiteralCharacters() {
            while (currentIndex + 1 < json.length() && isLiteralContinuation(json.charAt(currentIndex + 1))) {
                currentIndex++;
            }
        }

        private static boolean isLiteralContinuation(char character) {
            return !Character.isWhitespace(character)
                    && character != ','
                    && character != ':'
                    && character != '}'
                    && character != ']';
        }

        private Validation countStringCharacter() {
            currentStringChars++;
            return currentStringChars <= MAX_STRING_CHARS
                    ? Validation.accepted()
                    : Validation.rejected("string budget exceeded");
        }

        private Validation countToken() {
            tokenCount++;
            return tokenCount <= MAX_TOKENS
                    ? Validation.accepted()
                    : Validation.rejected("token budget exceeded");
        }

        private Validation validateCompletedInput() {
            if (insideString || previousCharacterEscaped) {
                return Validation.rejected("unterminated string");
            }
            if (nestingDepth != 0) {
                return Validation.rejected("unbalanced structure");
            }
            return Validation.accepted();
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
}
