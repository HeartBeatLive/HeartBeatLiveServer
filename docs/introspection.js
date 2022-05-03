const graphql = require("graphql");
const loadFiles = require("@graphql-tools/load-files");
const merge = require("@graphql-tools/merge");

const files = loadFiles.loadFilesSync("../src/main/resources/graphql");
const mergeTypeDefs = merge.mergeTypeDefs(files);
const printedTypeDefs = graphql.print(mergeTypeDefs);
const schema = graphql.buildSchema(printedTypeDefs);
module.exports = graphql.introspectionFromSchema(schema);