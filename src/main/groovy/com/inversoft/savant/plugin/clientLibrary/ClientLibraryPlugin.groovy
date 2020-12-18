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
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap
import org.inversoft.Java2Json
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.BuildFailureException
import org.savantbuild.runtime.RuntimeConfiguration

import java.nio.file.Path
import java.nio.file.Paths

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
        'apis'                                     : [],
        'endpoints'                                : [:],
        'domain'                                   : [],
        'camel_to_underscores'                     : new CamelToUnderscores()
    ]
    def jsonSlurper = new JsonSlurper()
    def files = []
    settings.jsonDirectory.eachFile(FileType.FILES) { files << it }
    files.sort().each { f ->
      def json = jsonSlurper.parse(f.toFile())
      root['apis'] << json

      // gather up json by endpoint/http method so that we can build openapi file
      // most endpoints only have one option, but some have two, with and without a param
      def endpoints = buildOpenapiUri(json.uri, json.params)
      def normalEndpoint = endpoints["normal"]
      def endpointWithOptionalParam = endpoints["withOptionalParam"]
      def http_method = json.method
      
      if (!root['endpoints'][normalEndpoint]) {
        root['endpoints'][normalEndpoint] = [:]
      }
      def onlyNormal = endpointWithOptionalParam == null
      if (onlyNormal) {
        // handle normal case
        root['endpoints'][normalEndpoint][http_method] = json

      } else {
        // handle case with param
        if (!root['endpoints'][endpointWithOptionalParam]) {
          root['endpoints'][endpointWithOptionalParam] = [:]
        }
        root['endpoints'][endpointWithOptionalParam][http_method] = json
        
        // handle with optional params
  
        def optionalUrlSegment = json.params.find { it.required != null && it.required == false && it.type == "urlSegment"  } 

        // remove the optional param from a copy. the normal endpoint doesn't get the optional segment param
        def modifiable_json = jsonSlurper.parseText(JsonOutput.toJson(json))
        modifiable_json.params = modifiable_json.params - optionalUrlSegment
        modifiable_json.methodName = modifiable_json.methodName + "WithoutId"
        root['endpoints'][normalEndpoint][http_method] = modifiable_json 
      }
    }

    if (settings.domainDirectory.toFile().exists()) {
      settings.domainDirectory.eachFile(FileType.FILES) { f ->
        root['domain'] << jsonSlurper.parse(f.toFile())
      }
    }

    outputFile(FileTools.toPath(parameters['outputFile']), FileTools.toPath(parameters['template']), root, config)
  }

  //look for urlsegments
  Map buildOpenapiUri(uri, params) {
    if (!params || params.size == 0) {  
      return ["normal": uri]
    }

    def uriSuffix = ""
    // TODO this doesn't handle roles which are in the url segment
    def constantParams = params.findAll { it.constant != null && it.constant == true && it.value != null && it.type == "urlParameter" }
    if (constantParams && constantParams.size >= 1) {
      def first = true
      for (Map constantParam : constantParams) {
        if (first) {
          uriSuffix += "?"
        } else {
          uriSuffix += "&"
        }
        uriSuffix = uriSuffix + constantParam.name+"="+constantParam.value
        if (first) { first = false;}
      }
    }

    def urlSegments = params.findAll { it.type == "urlSegment" }
    if (!urlSegments || urlSegments.size == 0) {  
      return ["normal": uri + uriSuffix]
    }
    def optionalUrlSegment = urlSegments.find { it.required != null && it.required == false }
    if (optionalUrlSegment == null) {
      // only the one url segment, it's required 
      return ["normal": uri+"/{"+urlSegments[0].name+"}"+uriSuffix]
    } 
    def toReturn = [:]
    toReturn["withOptionalParam"] = uri+"/{"+optionalUrlSegment.name+"}" + uriSuffix
    toReturn["normal"] = uri + uriSuffix
  
    return toReturn
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
        'domain'              : [],
        'domain_item'         : new LazyMap(),
        'camel_to_underscores': new CamelToUnderscores(),
        'type_to_package'     : new HashMap<String, String>(),
        'types_in_use'        : new HashSet<String>(),
        'packages'            : new HashSet<String>()
    ]

    def jsonSlurper = new JsonSlurper()

    if (settings.domainDirectory.toFile().exists()) {
      settings.domainDirectory.eachFile(FileType.FILES) { f ->
        root['domain'] << jsonSlurper.parse(f.toFile())
      }
    }

    root.domain.each { domain ->
      root.type_to_package.put(domain.type, domain.packageName)
    }

    root.domain.each { LazyMap domain ->
      root.domain_item = domain
      root.types_in_use = collectTypes(domain)
      root.packages = collectPackages(root.types_in_use, root.type_to_package)

      outputFile(
          parameters["layout"](FileTools.toPath(parameters['outputDir']), domain.packageName + "." + domain.type, parameters['extension']),
          FileTools.toPath(parameters["template"]),
          root,
          config
      )
    }
  }

  void generateDomainJson(parameters) {
    if (!parameters.containsKey("srcDir") || !parameters.containsKey("outDir")) {
      throw new BuildFailureException("You must specify a srcDir and an outDir")
    }

    new Java2Json((Path) Paths.get(parameters["srcDir"]), (Path) Paths.get(parameters["outDir"]), parameters["domainExcludeGlobs"] ?: [], parameters["domainIncludeGlobs"] ?: [], settings.debug).run()
  }

  private static Set<String> collectTypes(LazyMap o, Set<String> types = new HashSet<>()) {
    o.each { key, value ->
      if (key == "type") {
        types.add(value.toString())
      } else if (value instanceof LazyMap) {
        collectTypes(value, types)
      } else if (value instanceof Collection) {
        value.each {
          if (it instanceof LazyMap) {
            collectTypes(it, types)
          }
        }
      }
    }

    return types
  }

  private static Set<String> collectPackages(Set<String> types, HashMap<String, String> typesToPackages) {
    Set<String> packages = types.collect { typesToPackages.get(it) }
    packages.removeIf { it == null }
    return packages
  }

  private static defaultLayout(Path root, String name, String extension) {
    def parts = name.split("\\.")
    def packageName = parts[0..<(parts.size() - 1)]
    Path ret = root
    for (String part : packageName) {
      ret = ret.resolve(part)
    }
    ret.toFile().mkdirs()
    return ret.resolve(parts[parts.size() - 1] + "." + extension)
  }
}
