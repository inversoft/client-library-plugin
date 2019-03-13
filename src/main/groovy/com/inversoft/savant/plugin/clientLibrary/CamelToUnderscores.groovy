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

import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModelException

/**
 * @author Brian Pontarelli
 */
class CamelToUnderscores implements TemplateMethodModelEx {
  @Override
  Object exec(List arguments) throws TemplateModelException {
    if (arguments.size() != 1) {
      throw new TemplateModelException("You must pass a single String to the camel_to_underscore function")
    }

    String value = arguments.get(0).toString()
    char[] c = value.toCharArray()
    StringBuilder build = new StringBuilder()
    for (int i = 0; i < c.length; i++) {
      if (Character.isUpperCase(c[i])) {
        if (i == 0) {
          build.append(Character.toLowerCase(c[i]))
        } else {
          build.append("_${Character.toLowerCase(c[i])}")
        }
      } else {
        build.append(c[i])
      }
    }

    return build.toString()
  }
}
