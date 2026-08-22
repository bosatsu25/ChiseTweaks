package dev.chise.chisetweaks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SourceCommentPolicyContractTest {
    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Pattern LATIN_WORD = Pattern.compile("[A-Za-z]{2,}");
    private static final Pattern JAPANESE = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    private static final Pattern COMMENTED_OUT_JAVA = Pattern.compile(
            "(?s)^\\s*(?:public|private|protected|static|final|abstract|class|interface|enum|record|"
                    + "if|else|for|while|do|switch|case|return|throw|try|catch|finally|import|package|new)\\b.*");

    @Test
    void productionCommentsAreJapaneseAndContainNoCommentedOutJava() throws IOException {
        List<String> violations = new ArrayList<>();

        try (var paths = Files.walk(MAIN_JAVA)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.toString().endsWith(".java"))
                    .sorted()
                    .toList()) {
                String source = Files.readString(path);
                for (Comment comment : comments(source)) {
                    String body = comment.body().trim();
                    if (body.isEmpty()) continue;

                    String normalized = normalizeJavadoc(body);
                    if (COMMENTED_OUT_JAVA.matcher(normalized).matches()) {
                        violations.add(path + ":" + comment.line()
                                + " コメントアウトされたJavaコード: " + oneLine(normalized));
                        continue;
                    }
                    if (LATIN_WORD.matcher(normalized).find()
                            && !JAPANESE.matcher(normalized).find()) {
                        violations.add(path + ":" + comment.line()
                                + " 日本語を含まないコメント: " + oneLine(normalized));
                    }
                }
            }
        }

        if (!violations.isEmpty()) {
            System.err.println("本番ソースのコメント規約違反:");
            violations.forEach(System.err::println);
        }
        assertTrue(violations.isEmpty(), () -> "本番ソースのコメント規約違反: " + violations.size() + "件");
    }

    private static String normalizeJavadoc(String body) {
        return body.replaceAll("(?m)^\\s*\\*\\s?", "")
                .replaceAll("\\{@(?:link|code|literal)\\s+[^}]+}", "技術参照")
                .replaceAll("(?m)^\\s*@(?:param|return|throws|see|since|deprecated)\\b.*$", "")
                .trim();
    }

    private static String oneLine(String text) {
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 160 ? compact : compact.substring(0, 157) + "...";
    }

    private static List<Comment> comments(String source) {
        List<Comment> comments = new ArrayList<>();
        int line = 1;
        int index = 0;
        State state = State.NORMAL;
        int commentStart = -1;
        int commentLine = -1;
        StringBuilder body = new StringBuilder();

        while (index < source.length()) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';

            switch (state) {
                case NORMAL -> {
                    if (current == '\n') line++;
                    if (current == '"' && index + 2 < source.length()
                            && source.charAt(index + 1) == '"'
                            && source.charAt(index + 2) == '"') {
                        state = State.TEXT_BLOCK;
                        index += 3;
                        continue;
                    }
                    if (current == '"') {
                        state = State.STRING;
                        index++;
                        continue;
                    }
                    if (current == '\'') {
                        state = State.CHARACTER;
                        index++;
                        continue;
                    }
                    if (current == '/' && next == '/') {
                        state = State.LINE_COMMENT;
                        commentStart = index;
                        commentLine = line;
                        body.setLength(0);
                        index += 2;
                        continue;
                    }
                    if (current == '/' && next == '*') {
                        state = State.BLOCK_COMMENT;
                        commentStart = index;
                        commentLine = line;
                        body.setLength(0);
                        index += 2;
                        continue;
                    }
                    index++;
                }
                case STRING -> {
                    if (current == '\\' && index + 1 < source.length()) {
                        index += 2;
                    } else {
                        if (current == '"') state = State.NORMAL;
                        if (current == '\n') line++;
                        index++;
                    }
                }
                case CHARACTER -> {
                    if (current == '\\' && index + 1 < source.length()) {
                        index += 2;
                    } else {
                        if (current == '\'') state = State.NORMAL;
                        if (current == '\n') line++;
                        index++;
                    }
                }
                case TEXT_BLOCK -> {
                    if (current == '"' && index + 2 < source.length()
                            && source.charAt(index + 1) == '"'
                            && source.charAt(index + 2) == '"') {
                        state = State.NORMAL;
                        index += 3;
                    } else {
                        if (current == '\n') line++;
                        index++;
                    }
                }
                case LINE_COMMENT -> {
                    if (current == '\n') {
                        comments.add(new Comment(commentStart, commentLine, body.toString()));
                        state = State.NORMAL;
                        line++;
                        index++;
                    } else {
                        body.append(current);
                        index++;
                    }
                }
                case BLOCK_COMMENT -> {
                    if (current == '*' && next == '/') {
                        comments.add(new Comment(commentStart, commentLine, body.toString()));
                        state = State.NORMAL;
                        index += 2;
                    } else {
                        body.append(current);
                        if (current == '\n') line++;
                        index++;
                    }
                }
            }
        }

        if (state == State.LINE_COMMENT) {
            comments.add(new Comment(commentStart, commentLine, body.toString()));
        }
        return comments;
    }

    private enum State {
        NORMAL,
        STRING,
        CHARACTER,
        TEXT_BLOCK,
        LINE_COMMENT,
        BLOCK_COMMENT
    }

    private record Comment(int offset, int line, String body) {
    }
}
