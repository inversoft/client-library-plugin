/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.inversoft.savant.plugin.clientLibrary

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Settings for the Tomcat plugin.
 *
 * @author Brian Pontarelli
 */
class ClientLibrarySettings {
  Path jsonDirectory = Paths.get("src/main/api")

  Path domainDirectory = Paths.get("src/main/domain")

  boolean debug = false

  boolean includeGettersFromInterfacesAsFields = false
}
