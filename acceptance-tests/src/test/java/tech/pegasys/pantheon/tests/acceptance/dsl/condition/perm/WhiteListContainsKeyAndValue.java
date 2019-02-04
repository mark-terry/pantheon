/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.tests.acceptance.dsl.condition.perm;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.tests.acceptance.dsl.condition.Condition;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class WhiteListContainsKeyAndValue implements Condition {
  private final String val;
  private final Path tempFile;

  public WhiteListContainsKeyAndValue(final String val, final Path tempFile) {
    this.val = val;
    this.tempFile = tempFile;
  }

  @Override
  public void verify(final Node node) {
    Boolean result;
    try (Stream<String> lines = Files.lines(tempFile)) {
      result = lines.anyMatch(line -> line.equals(val));
    } catch (IOException e) {
      result = false;
    }
    assertThat(result).isTrue();
  }
}
