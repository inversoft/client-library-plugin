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
