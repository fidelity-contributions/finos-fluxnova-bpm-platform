package org.finos.flowave.spin.groovy.json.tree

jsonNode = S(input, "application/json");

jsonNode.jsonPath('$.active').numberValue();