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

  ClientLibraryPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
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

    BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_23)
    builder.setExposeFields(true)
    builder.setSimpleMapWrapper(true)

    Configuration config = new Configuration(Configuration.VERSION_2_3_23)
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

    StringWriter writer = new StringWriter()

    def root = [
        'apis': [],
        'camel_to_underscores': new CamelToUnderscores()
    ]
    def jsonSlurper = new JsonSlurper()
    settings.jsonDirectory.eachFile(FileType.FILES) { f ->
      root['apis'] << jsonSlurper.parseText(f.getText())
    }

    if (settings.domainDirectory.toFile().exists()) {
      settings.domainDirectory.eachFile(FileType.FILES) { f ->
        root['domain'] << jsonSlurper.parseText(f.getText())
      }
    }

    Path outputFile = FileTools.toPath(parameters['outputFile'])
    Path templateFile = FileTools.toPath(parameters['template'])
    Template template = config.getTemplate(templateFile.toAbsolutePath().toString())
    try {
      template.process(root, writer)

      if (settings.debug) {
        println writer.toString()
      } else {
        outputFile.text = writer.toString()
      }
    } catch (TemplateException e) {
      throw new BuildFailureException("Unable to execute FreeMarker template", e)
    }
  }
}
