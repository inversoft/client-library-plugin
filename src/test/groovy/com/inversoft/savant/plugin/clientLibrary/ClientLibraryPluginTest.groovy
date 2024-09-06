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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.savantbuild.dep.domain.License
import org.savantbuild.domain.Project
import org.savantbuild.domain.Version
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.Assert
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
  }

  @Test
  void buildClient() {
    project = new Project(projectDir.resolve("test-project-tomcat"), output)
    project.group = "com.inversoft.cleanspeak"
    project.name = "cleanspeak-search-engine"
    project.version = new Version("1.0.0")
    project.licenses.add(License.parse("ApacheV2_0", null))

    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.debug = true
    plugin.settings.jsonDirectory = Paths.get("src/test/api")
    plugin.buildClient(template: "src/test/client/java.client.ftl", outputFile: "build/Test.java")
  }

  @Test
  void buildDomain() {
    project = new Project(projectDir.resolve("test-project-tomcat"), output)
    project.group = "com.inversoft.cleanspeak"
    project.name = "cleanspeak-search-engine"
    project.version = new Version("1.0.0")
    project.licenses.add(License.parse("ApacheV2_0", null))

    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.debug = true
    plugin.settings.jsonDirectory = Paths.get("src/test/api")
    plugin.settings.domainDirectory = Paths.get("src/test/domain")
    plugin.buildDomain(template: "src/test/client/java.domain.ftl", outputDir: "build", extension: "java")
  }

  def generateDomainJson() {
    project = new Project(projectDir.resolve("test-project-tomcat"), output)
    project.group = "com.inversoft.cleanspeak"
    project.name = "cleanspeak-search-engine"
    project.version = new Version("1.0.0")
    project.licenses.add(License.parse("ApacheV2_0", null))

    ClientLibraryPlugin plugin = new ClientLibraryPlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.debug = true
    plugin.settings.jsonDirectory = Paths.get("src/test/api")
    plugin.settings.domainDirectory = Paths.get("src/test/domain")
    def outputDir = new File("build/test/domain")
    assert outputDir.deleteDir()
    outputDir.mkdirs()

    // act
    plugin.generateDomainJson(srcDir: "src/test/groovy/com/inversoft/savant/plugin/clientLibrary/jsonGenerate",
        outDir: "build/test/domain")

    return outputDir
  }

  def compare(File outputDir, String expectedIdentifier, Map expectedPayload) {
    def prettyActual = JsonOutput.prettyPrint(JsonOutput.toJson(new JsonSlurper().parse(new File(outputDir, "com.inversoft.savant.plugin.clientLibrary.jsonGenerate.${expectedIdentifier}.json"))))
    def prettyExpected = JsonOutput.prettyPrint(JsonOutput.toJson(expectedPayload))
    Assert.assertEquals(prettyActual,
        prettyExpected)
  }

  @Test
  void generateDomainJson_simple_class_field() {
    // arrange + act
    def outputDir = generateDomainJson()

    // assert
    compare(outputDir, "SimpleClassWithField", [
        packageName: "com.inversoft.savant.plugin.clientLibrary.jsonGenerate",
        type: "SimpleClassWithField",
        fields     : [
            doStuff: [
                type: "String"
            ]
        ]
    ])
  }

  @Test
  void generateDomainJson_interface_no_methods() {
    // arrange + act
    def outputDir = generateDomainJson()

    // assert
    compare(outputDir, "InterfaceNoMethods", [
        packageName: "com.inversoft.savant.plugin.clientLibrary.jsonGenerate",
        type       : "InterfaceNoMethods",
        fields     : [:]
    ])
  }

  @Test
  void generateDomainJson_interface_1_method() {
    // arrange + act
    def outputDir = generateDomainJson()

    // assert
    compare(outputDir, "InterfaceOneMethod", [
        packageName: "com.inversoft.savant.plugin.clientLibrary.jsonGenerate",
        type       : "InterfaceOneMethod",
        fields     : [:]
    ])
  }
}
