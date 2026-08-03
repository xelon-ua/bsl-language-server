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

import com.github._1c_syntax.bsl.languageserver.reporters.DiagnosticReporter;
import com.github._1c_syntax.bsl.languageserver.reporters.JsonReporter;
import com.github._1c_syntax.bsl.languageserver.reporters.ReportersAggregator;
import com.github._1c_syntax.bsl.languageserver.reporters.data.AnalysisInfo;
import com.github._1c_syntax.bsl.languageserver.reporters.data.FileInfo;
import com.github._1c_syntax.bsl.languageserver.reporters.databind.AnalysisInfoJsonMapper;
import com.github._1c_syntax.bsl.languageserver.util.CleanupContextBeforeClassAndAfterEachTestMethod;
import com.github._1c_syntax.bsl.languageserver.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@CleanupContextBeforeClassAndAfterEachTestMethod
class AnalyzeCommandTest {

  private static final String METADATA_PATH = Path.of(TestUtils.PATH_TO_METADATA).toAbsolutePath().toString();
  private static final String CONFIG_PATH = resolveConfigPath();

  @Autowired
  private AnalyzeCommand analyzeCommand;

  @Autowired
  private ReportersAggregator aggregator;

  @Autowired
  private JsonReporter jsonReporter;

  @TempDir
  Path tempDir;

  /** Анализ с конфигом, содержащим excludePaths, отрабатывает успешно (исключённые файлы пропускаются). */
  @Test
  void callWithExcludePathsConfigFiltersFiles() {
    // given
    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", tempDir.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", "");
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isZero();
  }

  /** Несуществующий workspaceDir — команда возвращает код 1 (ошибка). */
  @Test
  void callReturnsOneWhenWorkspaceDirDoesNotExist() {
    // given
    var nonexistentWorkspace = tempDir.resolve("nonexistent_workspace").toAbsolutePath().toString();

    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", nonexistentWorkspace);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", tempDir.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", "");
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isOne();
  }

  /** Несуществующий srcDir — команда возвращает код 1 (ошибка). */
  @Test
  void callReturnsOneWhenSrcDirDoesNotExist() {
    // given
    var nonexistentSrc = tempDir.resolve("nonexistent_src").toAbsolutePath().toString();

    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", nonexistentSrc);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", tempDir.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", "");
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isOne();
  }

  /** Без флага {@code silent} анализ выводит прогресс-бар и завершается успешно. */
  @Test
  void callWithoutSilentRunsWithProgressBar() {
    // given
    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", tempDir.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", "");
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", false);

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isZero();
  }

  /** С валидным --file-list, содержащим один существующий файл, анализ завершается успешно. */
  @Test
  void callWithFileListRunsSuccessfully() throws IOException {
    // given
    var objectModule = Path.of(METADATA_PATH,
      "Catalogs", "Справочник1", "Ext", "ObjectModule.bsl").toAbsolutePath();
    var fileList = tempDir.resolve("files.txt");
    Files.writeString(fileList, objectModule + System.lineSeparator(), StandardCharsets.UTF_8);

    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", tempDir.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", fileList.toString());
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isZero();
  }

  /** Несуществующий файл-список --file-list — команда возвращает код 1 (ошибка). */
  @Test
  void callReturnsOneWhenFileListDoesNotExist() {
    // given
    var nonexistentFileList = tempDir.resolve("nonexistent_list.txt").toAbsolutePath().toString();

    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", tempDir.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", nonexistentFileList);
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isOne();
  }

  /** filterByFileList оставляет только перечисленные файлы; непустой список с одним файлом даёт один файл. */
  @Test
  void filterByFileListReturnsOnlyListedFiles() throws IOException {
    // given
    var objectModule = Path.of(METADATA_PATH,
      "Catalogs", "Справочник1", "Ext", "ObjectModule.bsl").toAbsolutePath();
    var managerModule = Path.of(METADATA_PATH,
      "Catalogs", "Справочник1", "Ext", "ManagerModule.bsl").toAbsolutePath();

    var fileList = tempDir.resolve("files.txt");
    // одна валидная запись (objectModule) + одна несовпадающая (warning + skip)
    Files.writeString(
      fileList,
      objectModule + System.lineSeparator() + "does/not/exist.bsl" + System.lineSeparator(),
      StandardCharsets.UTF_8
    );

    var workspaceDir = Path.of(METADATA_PATH).toAbsolutePath();
    var allFiles = new java.util.ArrayList<java.io.File>(
      java.util.List.of(objectModule.toFile(), managerModule.toFile()));

    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", fileList.toString());

    // when
    @SuppressWarnings("unchecked")
    var filtered = (java.util.List<java.io.File>) ReflectionTestUtils.invokeMethod(
      analyzeCommand, "filterByFileList", allFiles, workspaceDir);

    // then
    assertThat(filtered).containsExactly(objectModule.toFile());
  }

  /** Без --file-list filterByFileList возвращает исходный список без изменений. */
  @Test
  void filterByFileListReturnsAllFilesWhenOptionBlank() {
    // given
    var objectModule = Path.of(METADATA_PATH,
      "Catalogs", "Справочник1", "Ext", "ObjectModule.bsl").toAbsolutePath();
    var workspaceDir = Path.of(METADATA_PATH).toAbsolutePath();
    var allFiles = new java.util.ArrayList<java.io.File>(
      java.util.List.of(objectModule.toFile()));

    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", "");

    // when
    @SuppressWarnings("unchecked")
    var filtered = (java.util.List<java.io.File>) ReflectionTestUtils.invokeMethod(
      analyzeCommand, "filterByFileList", allFiles, workspaceDir);

    // then
    assertThat(filtered).isSameAs(allFiles);
  }

  /**
   * End-to-end: {@code --file-list} реально сокращает набор анализируемых файлов.
   * <p>
   * Запускает два полных прохода через {@code call()} с JSON-репортером:
   * <ol>
   *   <li>без {@code --file-list} — ожидаем, что в отчёте окажутся ВСЕ файлы проекта;</li>
   *   <li>с {@code --file-list}, содержащим один файл — ожидаем ровно одну запись в отчёте.</li>
   * </ol>
   * Тест поймает регрессию, при которой diagnostic-цикл будет снова направлен на полный
   * список {@code files} вместо отфильтрованного {@code filesToAnalyze}.
   */
  @Test
  void fileListLimitsAnalyzedFilesEndToEnd() throws IOException {
    // Подменяем filteredReporters напрямую, чтобы не зависеть от @Lazy-инициализации бина,
    // которая могла произойти раньше (в другом тест-методе) с пустым reportersOptions.
    ReflectionTestUtils.setField(aggregator, "filteredReporters", List.of(jsonReporter));

    var mapper = new AnalysisInfoJsonMapper();

    // --- Прогон 1: без --file-list, анализируем весь проект ---
    var outFull = Files.createDirectory(tempDir.resolve("out-full"));

    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", outFull.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", "");
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);

    assertThat(analyzeCommand.call()).isZero();

    var fullReport = mapper.readValue(outFull.resolve("bsl-json.json").toFile(), AnalysisInfo.class);
    var fullCount = fullReport.fileinfos().size();

    // --- Прогон 2: с --file-list, содержащим один файл ---
    var objectModule = Path.of(METADATA_PATH,
      "Catalogs", "Справочник1", "Ext", "ObjectModule.bsl").toAbsolutePath();
    var fileList = tempDir.resolve("files.txt");
    Files.writeString(fileList, objectModule + System.lineSeparator(), StandardCharsets.UTF_8);

    var outFiltered = Files.createDirectory(tempDir.resolve("out-filtered"));

    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", outFiltered.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", fileList.toString());
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);

    assertThat(analyzeCommand.call()).isZero();

    var filteredReport = mapper.readValue(outFiltered.resolve("bsl-json.json").toFile(), AnalysisInfo.class);
    var filteredCount = filteredReport.fileinfos().size();

    // --- Проверки ---
    assertThat(filteredCount)
      .as("--file-list с одним файлом должен дать ровно одну запись в отчёте")
      .isEqualTo(1);
    assertThat(fullCount)
      .as("Полный прогон должен проанализировать больше файлов, чем отфильтрованный")
      .isGreaterThan(filteredCount);
  }

  /** Активен репортер, требующий метрики (json) — метрики вычисляются для каждого файла. */
  @Test
  void metricsComputedWhenActiveReporterRequiresThem() {
    // given: capturing (метрики не нужны) + json (метрики нужны) -> агрегатор требует метрики
    var capturingReporter = new CapturingReporter();
    prepareAnalysis(capturingReporter, new JsonReporter());

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isZero();
    assertThat(capturingReporter.captured())
      .isNotEmpty()
      .allSatisfy(fileInfo -> assertThat(fileInfo.getMetrics()).isNotNull());
  }

  /** Активен только репортер, не требующий метрики — вычисление метрик пропускается. */
  @Test
  void metricsSkippedWhenNoActiveReporterRequiresThem() {
    // given: только capturing (метрики не нужны)
    var capturingReporter = new CapturingReporter();
    prepareAnalysis(capturingReporter);

    // when
    var exitCode = analyzeCommand.call();

    // then
    assertThat(exitCode).isZero();
    assertThat(capturingReporter.captured())
      .isNotEmpty()
      .allSatisfy(fileInfo -> assertThat(fileInfo.getMetrics()).isNull());
  }

  private void prepareAnalysis(DiagnosticReporter... activeReporters) {
    ReflectionTestUtils.setField(analyzeCommand, "srcDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "workspaceDirOption", METADATA_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "outputDirOption", tempDir.toString());
    ReflectionTestUtils.setField(analyzeCommand, "configurationOption", CONFIG_PATH);
    ReflectionTestUtils.setField(analyzeCommand, "fileListOption", "");
    ReflectionTestUtils.setField(analyzeCommand, "silentMode", true);
    // Бин filteredReporters ленивый и в тесте резолвится один раз, поэтому набор активных
    // репортеров задаём агрегатору напрямую — детерминированно для каждого сценария.
    ReflectionTestUtils.setField(aggregator, "filteredReporters", List.of(activeReporters));
  }

  /** Тестовый репортер: не требует метрик и сохраняет полученные {@link FileInfo} для проверок. */
  private static class CapturingReporter implements DiagnosticReporter {

    private final List<FileInfo> captured = new CopyOnWriteArrayList<>();

    @Override
    public String key() {
      return "capturing";
    }

    @Override
    public void report(AnalysisInfo analysisInfo, Path outputDir) {
      captured.clear();
      captured.addAll(analysisInfo.fileinfos());
    }

    List<FileInfo> captured() {
      return captured;
    }
  }

  /** Возвращает абсолютный путь к тестовому конфигу с {@code excludePaths}. */
  private static String resolveConfigPath() {
    var resource = AnalyzeCommandTest.class.getResource("/.bsl-language-server-exclude-paths.json");
    if (resource == null) {
      return Path.of("src/test/resources/.bsl-language-server-exclude-paths.json").toAbsolutePath().toString();
    }
    try {
      return Paths.get(resource.toURI()).toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
