/*
 * Copyright (c) 2014-2017, Inversoft Inc., All Rights Reserved
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

import freemarker.cache.FileTemplateLoader
import freemarker.ext.beans.BeansWrapperBuilder
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateException
import groovy.io.FileType
import groovy.json.JsonSlurper
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.BuildFailureException
import org.savantbuild.runtime.RuntimeConfiguration

import java.nio.file.Path

/**
 * Inversoft clientLibrary plugin. This creates the RPM, DEB and ZIP bundles for a front-end.
 *
 * @author Brian Pontarelli
 */
class ClientLibraryPlugin extends BaseGroovyPlugin {
  ClientLibrarySettings settings = new ClientLibrarySettings()

  Configuration config

  ClientLibraryPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)

    BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_26)
    builder.setExposeFields(true)
    builder.setSimpleMapWrapper(true)

    config = new Configuration(Configuration.VERSION_2_3_26)
    config.setDefaultEncoding("UTF-8")
    config.setNumberFormat("computer")
    config.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX)
    config.setObjectWrapper(builder.build())
    config.setNumberFormat("computer")
    try {
      config.setTemplateLoader(new FileTemplateLoader(new File("/")))
    } catch (IOException e) {
      throw new RuntimeException(e)
    }
  }

  /**
   * Creates the client using a FreeMaker template and a set of JSON files.
   * <p>
   * <pre>
   *   clientLibrary.buildClient(template: "foo.ftl", outputFile: "bar.java")
   * </pre>
   */
  void buildClient(parameters) {
    if (!(parameters instanceof Map) || !parameters.containsKey("template") || !parameters.containsKey("outputFile")) {
      throw new BuildFailureException("You must pass in parameters to the buildClient method like this:\n\n   clientLibrary.buildClient(template: \"foo.ftl\", outputFile: \"Bar.java\")")
    }

    def root = [
        'apis'                : [],
        'domain'              : [],
        'camel_to_underscores': new CamelToUnderscores()
    ]
    def jsonSlurper = new JsonSlurper()
    def files = []
    settings.jsonDirectory.eachFile(FileType.FILES) { files << it }
    files.sort().each { f ->
      root['apis'] << jsonSlurper.parse(f.toFile())
    }

    if (settings.domainDirectory.toFile().exists()) {
      settings.domainDirectory.eachFile(FileType.FILES) { f ->
        root['domain'] << jsonSlurper.parse(f.toFile())
      }
    }

    outputFile(FileTools.toPath(parameters['outputFile']), FileTools.toPath(parameters['template']), root, config)
  }

  void outputFile(Path outputFile, Path templateFile, root, Configuration config) {
    Template template = config.getTemplate(templateFile.toAbsolutePath().toString())
    try {

      def writer
      if (settings.debug) {
        writer = new PrintWriter(System.out)
      } else {
        writer = outputFile.newWriter()
      }

      template.process(root, writer)
    } catch (TemplateException e) {
      throw new BuildFailureException("Unable to execute FreeMarker template", e)
    }
  }

  /**
   * Builds the domain on a per file basis.
   */
  void buildDomain(parameters) {
    if (!(parameters instanceof Map) || !parameters.containsKey("template") || !parameters.containsKey("outputDir") || !parameters.containsKey("extension")) {
      throw new BuildFailureException("You must pass in parameters to the buildClient method like this:\n\n   clientLibrary.buildClient(template: \"foo.ftl\", outputDir: \"src\", extension: \"java\")")
    }

    if (!parameters.containsKey("layout")) {
      parameters.put("layout", this.&defaultLayout)
    }

    def root = [
        'domain': {},
        'camel_to_underscores': new CamelToUnderscores()
    ]

    def jsonSlurper = new JsonSlurper()

    if (settings.domainDirectory.toFile().exists()) {
      settings.domainDirectory.eachFile(FileType.FILES) { f ->
        root.domain = jsonSlurper.parse(f.toFile())

        outputFile(
            parameters["layout"](FileTools.toPath(parameters['outputDir']), f.getFileName().toString(), parameters['extension']),
            FileTools.toPath(parameters["template"]),
            root,
            config
        )
      }
    }


  }

  private static defaultLayout(Path root, String name, String extension) {
    def parts = name.split("\\.")
    def packageName = parts[0..<(parts.size()-2)]
    Path ret = root
    for(String part: packageName) {
      ret = ret.resolve(part)
    }
    ret.toFile().mkdirs()
    return ret.resolve(parts[parts.size() - 2] + "." + extension)
  }
}
