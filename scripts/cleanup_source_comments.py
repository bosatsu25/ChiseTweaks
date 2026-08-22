#!/usr/bin/env python3
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

ROOT = Path("src/main/java")
LATIN_WORD = re.compile(r"[A-Za-z]{2,}")
JAPANESE = re.compile(r"[\u3040-\u30ff\u3400-\u9fff]")
COMMENTED_OUT_JAVA = re.compile(
    r"(?s)^\s*(?:public|private|protected|static|final|abstract|class|interface|enum|record|"
    r"if|else|for|while|do|switch|case|return|throw|try|catch|finally|import|package|new)\b.*"
)
LEGAL_MARKERS = ("copyright", "spdx", "licensed under", "apache license", "mit license")

# 実装理由や将来の回帰防止に価値があるコメントだけを日本語で残す。
# 単にクラス名や処理内容を言い換えている英語コメントは削除する。
TRANSLATIONS: tuple[tuple[str, str], ...] = (
    (
        "getChunkNow() is explicitly non-loading",
        "getChunkNow() はチャンクを新規ロードしない。getChunk(..., true) へ置き換えてはならない。",
    ),
    (
        "Treats an unloaded horizontal neighbor as unknown rather than as air/non-source",
        "未ロードの水平方向隣接チャンクは空気や非源泉とみなさず、不明として扱う。これによりチャンク境界でもロード済み情報だけで判定できる。",
    ),
    (
        "Returning from a stationary backoff must never make moving users wait longer than the configured base cadence",
        "静止時バックオフから移動へ戻ったときは、設定された基本間隔より長く待たせず、同時に基本間隔より高頻度にも走査しない。",
    ),
    (
        "Rotate only after retaining the buffer that owns this revision",
        "このリビジョンを保持するバッファを確定してからリングを進める。変化のないフレームは保持済みバッファを再利用する。",
    ),
    (
        "Drops all transient CPU/GPU workspace so a later session never inherits a failed frame",
        "一時的なCPU/GPU作業領域をすべて破棄し、後続セッションへ失敗フレームの状態を持ち越さない。",
    ),
    (
        "Render-path failures may be transient across world/session setup",
        "描画経路の失敗はワールドやセッション初期化中だけの一過性である場合がある。Manager側の隔離状態はプロセス全体で保持するため、ここではリセットしない。",
    ),
    (
        "Unknown blocks simply have no Chise inspection categories and therefore fail open",
        "未知のブロックはChiseの検査カテゴリを持たないものとして扱い、機能全体を停止させずフェイルオープンする。",
    ),
    (
        "A broken/late external registration must not quarantine Chise's entire worksite engine",
        "外部登録の不正や遅延だけでChiseの作業支援エンジン全体を隔離しない。",
    ),
    (
        "Removed and unknown keys are ignored so old config files degrade safely",
        "削除済みまたは未知の設定キーは無視し、古い設定ファイルでも安全に縮退させる。",
    ),
    (
        "Directory fsync is not supported on every Java/filesystem combination",
        "ディレクトリのfsyncはすべてのJavaとファイルシステムの組み合わせで利用できるとは限らない。",
    ),
    (
        "Fixed startup budget prevents a broken optional phase from cascading forever",
        "起動時の処理量を固定上限で制限し、任意機能の故障が後続処理へ連鎖し続けることを防ぐ。",
    ),
    (
        "Strict pre-parser for local JSON configuration files",
        "ローカルJSON設定を厳格に事前検証し、重複キーや不正な構造を本処理へ渡さない。",
    ),
    (
        "Retained targets keep their historical bit positions",
        "既存設定との互換性を守るため、保持対象のビット位置は過去の割り当てを維持する。",
    ),
    (
        "Classification is block-stable for the duration of one resource-model reload",
        "1回のリソースモデル再読み込み中は分類結果を固定し、再読み込みをまたいで古い分類を持ち越さない。",
    ),
    (
        "Registrations performed during client initialization are naturally included in the first model bake",
        "クライアント初期化中の登録は最初のモデルベイクへ自然に含まれるため、起動直後の重複再読み込みを発生させない。",
    ),
    (
        "Requests a chunk-geometry refresh without starting a resource reload or permanent polling",
        "リソース全体の再読み込みや常時ポーリングを開始せず、必要なチャンク形状だけの再構築を要求する。",
    ),
    (
        "Precomputed render-only view so per-frame drawing avoids repeated string/state inspection",
        "毎フレームの描画で文字列や状態を再判定しないよう、描画専用情報を事前計算して保持する。",
    ),
    (
        "Hard CPU/allocation budget for ray based line-of-sight checks in one scan",
        "1回の走査で行う視線判定にはCPU処理量と一時割り当ての上限を設ける。",
    ),
    (
        "Nether-only, client-only analyzer for Ancient Debris already present in loaded client chunks",
        "古代の残骸アナライザーはクライアント専用・ネザー専用とし、すでにロード済みのクライアントチャンクだけを対象にする。",
    ),
    (
        "Allocation-bounded handoff from cached debris positions to the retained renderer",
        "古代の残骸キャッシュから保持型レンダラーへの受け渡しは、割り当て量を固定上限内に抑える。",
    ),
    (
        "Allocation-bounded handoff between the lava scanner and retained renderer",
        "溶岩走査結果から保持型レンダラーへの受け渡しは、割り当て量を固定上限内に抑える。",
    ),
    (
        "Runtime snapshots for Chise-owned block/entity visibility rules",
        "ブロックとエンティティの可視性判定はランタイム用スナップショットとして保持し、描画中の設定再解釈を避ける。",
    ),
    (
        "Replaces filtered entities with an invisible, inert render state",
        "除外対象エンティティは不可視かつ副作用のない描画状態へ置き換える。",
    ),
    (
        "Lowers only Minecraft's first-person fire screen effect",
        "Minecraftの一人称視点に重なる炎エフェクトだけを下げ、ワールド上の炎モデルやテクスチャは変更しない。",
    ),
)


@dataclass(frozen=True)
class Comment:
    start: int
    end: int
    body: str
    kind: str
    raw: str


def normalize(body: str) -> str:
    body = re.sub(r"(?m)^\s*\*\s?", "", body)
    body = re.sub(r"\{@(?:link|code|literal)\s+[^}]+}", "", body)
    body = re.sub(r"(?m)^\s*@param\s+\S+\s*", "", body)
    body = re.sub(r"(?m)^\s*@throws\s+\S+\s*", "", body)
    body = re.sub(r"(?m)^\s*@(return|since|deprecated)\s*", "", body)
    body = re.sub(r"(?m)^\s*@see\b.*$", "", body)
    body = re.sub(r"(?m)^\s*<p>\s*", "", body)
    body = body.replace("</p>", "")
    return re.sub(r"\s+", " ", body).strip()


def comments(source: str) -> list[Comment]:
    result: list[Comment] = []
    i = 0
    state = "NORMAL"
    start = -1
    body_start = -1
    kind = ""
    while i < len(source):
        c = source[i]
        n = source[i + 1] if i + 1 < len(source) else "\0"
        if state == "NORMAL":
            if source.startswith('"""', i):
                state = "TEXT_BLOCK"
                i += 3
            elif c == '"':
                state = "STRING"
                i += 1
            elif c == "'":
                state = "CHAR"
                i += 1
            elif c == "/" and n == "/":
                state = "LINE"
                start = i
                body_start = i + 2
                kind = "line"
                i += 2
            elif c == "/" and n == "*":
                state = "BLOCK"
                start = i
                body_start = i + 2
                kind = "javadoc" if i + 2 < len(source) and source[i + 2] == "*" else "block"
                i += 2
            else:
                i += 1
        elif state == "STRING":
            if c == "\\" and i + 1 < len(source):
                i += 2
            else:
                if c == '"':
                    state = "NORMAL"
                i += 1
        elif state == "CHAR":
            if c == "\\" and i + 1 < len(source):
                i += 2
            else:
                if c == "'":
                    state = "NORMAL"
                i += 1
        elif state == "TEXT_BLOCK":
            if source.startswith('"""', i):
                state = "NORMAL"
                i += 3
            else:
                i += 1
        elif state == "LINE":
            if c == "\n":
                result.append(Comment(start, i, source[body_start:i], kind, source[start:i]))
                state = "NORMAL"
            else:
                i += 1
        elif state == "BLOCK":
            if c == "*" and n == "/":
                end = i + 2
                result.append(Comment(start, end, source[body_start:i], kind, source[start:end]))
                state = "NORMAL"
                i = end
            else:
                i += 1
    if state == "LINE":
        result.append(Comment(start, len(source), source[body_start:], kind, source[start:]))
    return result


def translation_for(normalized: str) -> str | None:
    lower = normalized.lower()
    if any(marker in lower for marker in LEGAL_MARKERS):
        raise RuntimeError(f"法的表示の可能性がある英語コメントを検出しました: {normalized}")
    for needle, translated in TRANSLATIONS:
        if normalized.startswith(needle):
            return translated
    return None


def line_indent(source: str, index: int) -> str:
    line_start = source.rfind("\n", 0, index) + 1
    prefix = source[line_start:index]
    return prefix if not prefix.strip() else ""


def render_translation(source: str, comment: Comment, text: str) -> str:
    if comment.kind == "line":
        return "// " + text
    indent = line_indent(source, comment.start)
    if comment.kind == "javadoc":
        return "/**\n" + indent + " * " + text + "\n" + indent + " */"
    return "/* " + text + " */"


def blank_replacement(comment: Comment) -> str:
    if comment.kind == "line":
        return ""
    # インラインのブロックコメントでも前後のトークンを連結しない。
    return " "


def normalize_java_whitespace(source: str) -> str:
    had_final_newline = source.endswith("\n")
    lines = [line.rstrip() for line in source.splitlines()]

    # コメント削除後に残る連続空行を1行へ畳む。
    compact: list[str] = []
    previous_blank = False
    for line in lines:
        blank = not line.strip()
        if blank and previous_blank:
            continue
        compact.append(line)
        previous_blank = blank

    # Javadocの中身を開始行と同じインデントへ揃える。
    formatted: list[str] = []
    in_javadoc = False
    javadoc_indent = ""
    for line in compact:
        stripped = line.lstrip()
        if not in_javadoc and stripped == "/**":
            javadoc_indent = line[: len(line) - len(stripped)]
            formatted.append(javadoc_indent + "/**")
            in_javadoc = True
            continue
        if in_javadoc:
            if stripped == "*/":
                formatted.append(javadoc_indent + " */")
                in_javadoc = False
                continue
            if stripped.startswith("*"):
                formatted.append(javadoc_indent + " " + stripped)
                continue
        formatted.append(line)

    result = "\n".join(formatted)
    if had_final_newline:
        result += "\n"
    return result


def process_file(path: Path) -> tuple[bool, int, int]:
    original = path.read_text(encoding="utf-8")
    source = original
    edits: list[tuple[int, int, str]] = []
    translated = 0
    removed = 0
    for comment in comments(source):
        normalized = normalize(comment.body)
        if not normalized:
            continue
        if COMMENTED_OUT_JAVA.match(normalized):
            edits.append((comment.start, comment.end, blank_replacement(comment)))
            removed += 1
            continue
        if LATIN_WORD.search(normalized) and not JAPANESE.search(normalized):
            translated_text = translation_for(normalized)
            if translated_text is None:
                edits.append((comment.start, comment.end, blank_replacement(comment)))
                removed += 1
            else:
                edits.append((comment.start, comment.end, render_translation(source, comment, translated_text)))
                translated += 1
    for start, end, replacement in reversed(edits):
        source = source[:start] + replacement + source[end:]
    source = normalize_java_whitespace(source)
    if source == original:
        return False, translated, removed
    path.write_text(source, encoding="utf-8")
    return True, translated, removed


def main() -> int:
    changed_files = 0
    translated = 0
    removed = 0
    for path in sorted(ROOT.rglob("*.java")):
        changed, translated_count, removed_count = process_file(path)
        if changed:
            changed_files += 1
        translated += translated_count
        removed += removed_count
    print(f"SOURCE COMMENT CLEANUP: changed_files={changed_files} translated={translated} removed={removed}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
