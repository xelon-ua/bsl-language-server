# Using Ukrainian letters in code (UkrainianLetterUsage)

<!-- Блоки выше заполняются автоматически, не трогать -->
## Description

Code (comments and identifiers — variable, procedure, function and parameter names, etc.) must be written in Russian or English. Ukrainian letters are not allowed.

Because Ukrainian and Russian share the Cyrillic alphabet, Ukrainian text can only be reliably distinguished by the letters that do not exist in Russian: `і`, `ї`, `є`, `ґ` (and their uppercase forms `І`, `Ї`, `Є`, `Ґ`). The diagnostic therefore triggers only when these letters are present. The letter set is configured by the `letters` parameter as a regular expression.

String literals are not checked: they often contain interface texts, including in Ukrainian.

## Examples

Triggers (letter `і` in a comment, letter `є`/`і` in an identifier):

```bsl
// Перевірка вхідних даних
Перем Єдність;
```

Does not trigger (Russian and English):

```bsl
// Проверка входных данных
Перем Единство;
Сообщить("Привіт, світ"); // text inside a string is not checked
```

## Sources

<!-- Необходимо указывать ссылки на все источники, из которых почерпнута информация для создания диагностики -->
