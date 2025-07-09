package org.finos.flowave.spin.groovy.json.tree

node = S(input, "application/json");
currencies = node.prop("orderDetails").prop("currencies");

currencies.insertBefore("euro", null);
