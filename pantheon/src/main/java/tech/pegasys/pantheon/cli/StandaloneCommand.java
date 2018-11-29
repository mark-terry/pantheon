/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.cli;

import static tech.pegasys.pantheon.cli.DefaultValues.CONFIG_FILE_OPTION_NAME;
import static tech.pegasys.pantheon.cli.DefaultValues.MANDATORY_PATH_FORMAT_HELP;
import static tech.pegasys.pantheon.cli.DefaultValues.getDefaultPantheonDataDir;

import java.io.File;
import java.nio.file.Path;

import picocli.CommandLine;

class StandaloneCommand {

  @CommandLine.Option(
    names = {CONFIG_FILE_OPTION_NAME},
    paramLabel = MANDATORY_PATH_FORMAT_HELP,
    description = "TOML config file (default: none)"
  )
  private final File configFile = null;

  @CommandLine.Option(
    names = {"--datadir"},
    paramLabel = MANDATORY_PATH_FORMAT_HELP,
    description = "the path to Pantheon data directory (default: ${DEFAULT-VALUE})"
  )
  final Path dataDir = getDefaultPantheonDataDir(this);

  // Genesis file path with null default option if the option
  // is not defined on command line as this default is handled by Runner
  // to use mainnet json file from resources
  // NOTE: we have no control over default value here.
  @CommandLine.Option(
    names = {"--genesis"},
    paramLabel = MANDATORY_PATH_FORMAT_HELP,
    description = "The path to genesis file (default: Pantheon embedded mainnet genesis file)"
  )
  final File genesisFile = null;
}
