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
package com.github._1c_syntax.bsl.languageserver.cli;

import com.github._1c_syntax.utils.Absolute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileListReaderTest {

  @TempDir
  Path tempDir;

  /** Относительные пути резолвятся относительно workspaceDir, абсолютные — сохраняются. */
  @Test
  void readResolvesRelativeAgainstWorkspaceAndKeepsAbsolute() throws IOException {
    // given
    var workspaceDir = tempDir.resolve("workspace");
    var absoluteEntry = tempDir.resolve("other/Module.bsl").toAbsolutePath();

    var fileList = tempDir.resolve("files.txt");
    Files.writeString(
      fileList,
      "Catalogs/Справочник1/Ext/ObjectModule.bsl\n" + absoluteEntry + "\n",
      StandardCharsets.UTF_8
    );

    // when
    var result = FileListReader.read(fileList, workspaceDir);

    // then
    assertThat(result).containsExactlyInAnyOrder(
      Absolute.uri(workspaceDir.resolve("Catalogs/Справочник1/Ext/ObjectModule.bsl").toFile()),
      Absolute.uri(absoluteEntry.toFile())
    );
  }

  /** Пустые строки и строки-комментарии (начинающиеся с #) игнорируются. */
  @Test
  void readSkipsBlankLinesAndComments() throws IOException {
    // given
    var workspaceDir = tempDir.resolve("workspace");
    var fileList = tempDir.resolve("files.txt");
    Files.writeString(
      fileList,
      "# это комментарий\n\n   \nModule.bsl\n  # ещё комментарий\n",
      StandardCharsets.UTF_8
    );

    // when
    var result = FileListReader.read(fileList, workspaceDir);

    // then
    assertThat(result).containsExactly(
      Absolute.uri(workspaceDir.resolve("Module.bsl").toFile())
    );
  }

  /** Пустой файл-список даёт пустое множество. */
  @Test
  void readReturnsEmptySetForEmptyFile() throws IOException {
    // given
    var fileList = tempDir.resolve("empty.txt");
    Files.writeString(fileList, "", StandardCharsets.UTF_8);

    // when
    var result = FileListReader.read(fileList, tempDir);

    // then
    assertThat(result).isEmpty();
  }

  /** Несуществующий файл-список оборачивается в UncheckedIOException. */
  @Test
  void readWrapsMissingFileInUncheckedIOException() {
    // given
    var missing = tempDir.resolve("does-not-exist.txt");

    // when / then
    assertThatThrownBy(() -> FileListReader.read(missing, tempDir))
      .isInstanceOf(java.io.UncheckedIOException.class);
  }
}
