[#function methodParameters api]
  [#local result = []]
  [#list api.params as param]
    [#if !param.constant??]
      [#local result = result + [param.javaType + ' ' + param.name]/]
    [/#if]
  [/#list]
  [#return result?join(", ")/]
[/#function]