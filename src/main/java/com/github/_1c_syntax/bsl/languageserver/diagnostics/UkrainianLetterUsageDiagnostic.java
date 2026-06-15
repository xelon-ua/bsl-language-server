/*
 * This file is a part of BSL Language Server.
 *
 * Copyright (c) 2018-2026
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Fedkin <nixel2007@gmail.com> and contributors
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * BSL Language Server is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * BSL Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BSL Language Server.
 */
package com.github._1c_syntax.bsl.languageserver.diagnostics;

import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticMetadata;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticParameter;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticTag;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.parser.BSLParser;
import com.github._1c_syntax.utils.CaseInsensitivePattern;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@DiagnosticMetadata(
  type = DiagnosticType.CODE_SMELL,
  severity = DiagnosticSeverity.MINOR,
  minutesToFix = 5,
  activatedByDefault = false,
  tags = {DiagnosticTag.SUSPICIOUS}
)
public class UkrainianLetterUsageDiagnostic extends AbstractDiagnostic {

  private static final String DEFAULT_LETTERS = "[іїєґ]";

  @DiagnosticParameter(
    type = String.class,
    defaultValue = DEFAULT_LETTERS
  )
  private Pattern letters = CaseInsensitivePattern.compile(DEFAULT_LETTERS);

  @Override
  public void configure(Map<String, Object> configuration) {
    this.letters = CaseInsensitivePattern.compile(
      (String) configuration.getOrDefault("letters", DEFAULT_LETTERS));
  }

  @Override
  protected void check() {
    Stream.concat(
        documentContext.getTokensFromDefaultChannel().stream()
          .filter(token -> token.getType() == BSLParser.IDENTIFIER),
        documentContext.getComments().stream()
      )
      .filter(token -> letters.matcher(token.getText()).find())
      .forEach(diagnosticStorage::addDiagnostic);
  }
}
