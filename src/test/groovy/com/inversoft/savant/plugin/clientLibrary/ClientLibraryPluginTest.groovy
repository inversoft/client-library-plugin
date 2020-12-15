/*
 * Copyright (c) 2014-2015, Inversoft Inc., All Rights Reserved
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

import org.savantbuild.dep.domain.License
import org.savantbuild.dep.domain.Version
import org.savantbuild.domain.Project
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Tests the tomcat plugin.
 *
 * @author Brian Pontarelli
 */
class ClientLibraryPluginTest {
  public static Path projectDir

  Output output

  Project project

  @BeforeSuite
  static void beforeSuite() {
    projectDir = Paths.get("")
  }

  @BeforeMethod
  void beforeMethod() {
    output = new SystemOutOutput(true)
    project = new Project(projectDir.resolve("test-project-tomcat"), output)
    project.group = "com.inversoft.cleanspeak"
    project.name = "cleanspeak-search-engine"
    project.version = new Version("1.0")
    project.licenses.put(License.ApacheV2_0, null)
  }

  @Test
  void buildOpenapiParamsSimple() {
    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    def uri = "abc"
    def params = []
    assert plugin.buildOpenapiUri(uri,params) == ["normal":"abc"]
  }

  @Test
  void buildOpenapiParamsWithParamsNoSegment() {
    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    def uri = "abc"
    def params = [["type": "urlParameter", "name":"foo"]]
    assert plugin.buildOpenapiUri(uri,params) == ["normal":"abc"]
  }

  @Test
  void buildOpenapiParamsWithParamsSegmentRequired() {
    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    def uri = "abc"
    def params = [["type": "urlSegment", "name":"foo","required":true]]
    assert plugin.buildOpenapiUri(uri,params) == ["normal":"abc/{foo}"]
  }

  @Test
  void buildOpenapiParamsWithParamsSegmentOptional() {
    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    def uri = "abc"
    def params = [["type": "urlSegment", "name":"foo","required":false]]
    assert plugin.buildOpenapiUri(uri,params) == ["withOptionalParam": "abc/{foo}", "normal":"abc"]
  }

  @Test
  void buildOpenapiParamsWithMultParamsSegmentOptional() {
    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    def uri = "abc"
    def params = [["type": "urlSegment", "name":"foo","required":false], ["type": "urlParameter", "name":"bar"]]
    assert plugin.buildOpenapiUri(uri,params) == ["withOptionalParam": "abc/{foo}", "normal":"abc"]
  }

  @Test
  void buildClient() {
    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.debug = true
    plugin.settings.jsonDirectory = Paths.get("src/test/api")
    plugin.buildClient(template: "src/test/client/java.client.ftl", outputFile: "build/Test.java")
  }

  @Test
  void buildDomain() {
    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.debug = true
    plugin.settings.jsonDirectory = Paths.get("src/test/api")
    plugin.settings.domainDirectory = Paths.get("src/test/domain")
    plugin.buildDomain(template: "src/test/client/java.domain.ftl", outputDir: "build", extension: "java")
  }
}
