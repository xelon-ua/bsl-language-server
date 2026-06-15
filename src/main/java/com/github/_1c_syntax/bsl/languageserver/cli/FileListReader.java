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
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Чтение списка файлов из текстового файла (опция {@code --file-list}).
 * <p>
 * Каждая непустая строка, не начинающаяся с {@code #}, трактуется как путь к файлу.
 * Относительные пути резолвятся относительно {@code workspaceDir}, абсолютные — берутся как есть.
 * Все пути нормализуются через {@link Absolute#uri(java.io.File)} для последующего сравнения
 * с файлами проекта.
 */
@UtilityClass
public class FileListReader {

  private static final String COMMENT_PREFIX = "#";

  /**
   * Читает файл-список и возвращает множество нормализованных URI запрошенных файлов.
   *
   * @param fileListFile путь к текстовому файлу со списком путей (один путь на строку)
   * @param workspaceDir каталог, относительно которого резолвятся относительные пути
   * @return множество нормализованных URI; порядок сохраняется (LinkedHashSet)
   */
  public static Set<URI> read(Path fileListFile, Path workspaceDir) {
    try {
      var lines = Files.readAllLines(fileListFile, StandardCharsets.UTF_8);
      var uris = new LinkedHashSet<URI>();
      for (var rawLine : lines) {
        var line = rawLine.strip();
        if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
          continue;
        }
        var path = Path.of(line);
        if (!path.isAbsolute()) {
          path = workspaceDir.resolve(path);
        }
        uris.add(Absolute.uri(path.toFile()));
      }
      return uris;
    } catch (IOException e) {
      throw new UncheckedIOException("Can't read file list: " + fileListFile, e);
    }
  }
}
