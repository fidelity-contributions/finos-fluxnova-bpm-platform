package org.finos.flowave.spin.groovy.json.tree

jsonNode = S(input, "application/json");

stringValue = jsonNode.jsonPath('$.order').stringValue();