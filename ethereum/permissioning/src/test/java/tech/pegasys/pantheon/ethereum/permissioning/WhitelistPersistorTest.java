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
package tech.pegasys.pantheon.ethereum.permissioning;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WhitelistPersistorTest {

  private WhitelistPersistor whitelistPersistor;
  private File tempFile;
  private final String accountsWhitelist =
      String.format("%s=%s", WhitelistPersistor.WHITELIST_TYPE.ACCOUNTS.getTomlKey(), "meow,meow");
  private final String nodesWhitelist =
      String.format("%s=%s", WhitelistPersistor.WHITELIST_TYPE.NODES.getTomlKey(), "woof,woof");

  @Before
  public void setUp() throws IOException {
    List<String> lines = Lists.newArrayList(nodesWhitelist, accountsWhitelist);
    tempFile = File.createTempFile("test", "test");
    Files.write(tempFile.toPath(), lines, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    whitelistPersistor = new WhitelistPersistor(tempFile.getAbsolutePath());
  }

  @Test
  public void lineShouldBeRemoved() throws IOException {
    whitelistPersistor.removeExistingConfigItem(WhitelistPersistor.WHITELIST_TYPE.ACCOUNTS);
  }

  @Test
  public void lineShouldBeAdded() throws IOException {
    Set<String> updatedWhitelist = Collections.singleton("moo");
    whitelistPersistor.removeExistingConfigItem(WhitelistPersistor.WHITELIST_TYPE.NODES);
    whitelistPersistor.addNewConfigItem(WhitelistPersistor.WHITELIST_TYPE.NODES, updatedWhitelist);
  }

  @Test
  public void lineShouldBeReplaced() throws IOException {
    Set<String> updatedWhitelist = Collections.singleton("moo");
    whitelistPersistor.updateConfig(WhitelistPersistor.WHITELIST_TYPE.NODES, updatedWhitelist);
  }

  @After
  public void tearDown() {
    tempFile.delete();
  }
}
