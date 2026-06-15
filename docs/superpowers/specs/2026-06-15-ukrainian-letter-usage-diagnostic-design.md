# UkrainianLetterUsageDiagnostic — design

## Goal

Add a new diagnostic that forbids Ukrainian-specific letters in source code —
in comments and in identifiers (variable names, procedure/function names,
parameters, labels, etc.). Comments and code are expected to be in Russian or
English; Ukrainian text must not appear.

## Detection

Ukrainian and Russian share most of the Cyrillic alphabet, and Russian must stay
allowed, so "any Cyrillic" cannot be flagged. The only reliable signal is the set
of letters that exist in Ukrainian but not in Russian:

- `і` (U+0456), `ї` (U+0457), `є` (U+0454), `ґ` (U+0491)
- uppercase counterparts `І Ї Є Ґ` are covered automatically via case-insensitive
  matching.

The letter set is exposed as a configurable parameter so a team can extend it
(for example, add the Ukrainian apostrophe) without code changes.

### Parameter

- `letters` (`String`, compiled to `CaseInsensitivePattern`), default value
  `[іїєґ]`. A token is flagged when its text contains any character matched by
  this pattern.

## Scope

Only **comments** and **identifiers** are scanned. String literals are **not**
scanned (they frequently contain legitimate Ukrainian UI text), and there is no
`checkStrings` parameter.

## Approach

Pure token scan, mirroring `YoLetterUsageDiagnostic` (which flags any `IDENTIFIER`
token containing `Ё`). One `check()` pass over two token sources:

1. `documentContext.getTokensFromDefaultChannel()` filtered to
   `type == BSLParser.IDENTIFIER` — covers every identifier kind (declarations and
   usages alike), since all of them are `IDENTIFIER` tokens at the lexer level.
2. `documentContext.getComments()` — the `LINE_COMMENT` tokens (1C has only line
   comments).

Each token whose text matches the `letters` pattern is reported via
`diagnosticStorage.addDiagnostic(token)`, which highlights the whole token (same
behavior as `YoLetterUsageDiagnostic`).

This is simpler and more thorough than walking specific AST rules (as
`LatinAndCyrillicSymbolInWordDiagnostic` does): the AST approach would only catch
declarations and would miss usages, while requiring more code.

Base class: `AbstractDiagnostic`.

## Metadata

- `type = DiagnosticType.CODE_SMELL`
- `severity = DiagnosticSeverity.MINOR`
- `minutesToFix = 5`
- `tags = {DiagnosticTag.SUSPICIOUS}`
- `activatedByDefault = false` — this is a project-specific policy and should not
  fire for every project by default.

## Naming

- Class: `UkrainianLetterUsageDiagnostic`
- Diagnostic code: `UkrainianLetterUsage` (suffix `Diagnostic` stripped, consistent
  with `YoLetterUsage`).

## Files to create / change

- `src/main/java/com/github/_1c_syntax/bsl/languageserver/diagnostics/UkrainianLetterUsageDiagnostic.java`
- `src/main/resources/com/github/_1c_syntax/bsl/languageserver/diagnostics/UkrainianLetterUsageDiagnostic_ru.properties`
- `src/main/resources/com/github/_1c_syntax/bsl/languageserver/diagnostics/UkrainianLetterUsageDiagnostic_en.properties`
  - keys: `diagnosticName`, `diagnosticMessage`, and `letters` (parameter
    description).
- `docs/diagnostics/UkrainianLetterUsage.md` (Russian)
- `docs/en/diagnostics/UkrainianLetterUsage.md` (English)
- `src/test/java/com/github/_1c_syntax/bsl/languageserver/diagnostics/UkrainianLetterUsageDiagnosticTest.java`
  - extends `AbstractDiagnosticTest<UkrainianLetterUsageDiagnostic>`
- `src/test/resources/diagnostics/UkrainianLetterUsageDiagnostic.bsl`
  - fixture with: Ukrainian letters in a comment (flagged), Ukrainian letters in
    an identifier (flagged), Russian-only and English-only comments/identifiers
    (clean). Optionally a configuration test for a custom `letters` value.
- Run `gradlew precommit` to regenerate the diagnostics index and related
  generated files (e.g. `Diagnostics.md`, JSON schema, completeness tests).

## Testing

- Default-config test: assert exact ranges for the flagged comment and identifier,
  and assert that Russian/English lines produce no diagnostics.
- Configuration test: provide a custom `letters` value and assert the changed
  behavior.

## Out of scope

- Scanning string literals.
- Detecting Ukrainian by dictionary/word lists.
- Quick fixes (no automatic transliteration/replacement).
