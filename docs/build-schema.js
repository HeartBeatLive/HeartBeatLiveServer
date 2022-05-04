const graphql = require("graphql");
const loadFiles = require("@graphql-tools/load-files");
const merge = require("@graphql-tools/merge");
const fs = require("fs");

const schemasDirection = "../src/main/resources/graphql";
const apiVersion = "1.0";
const schemaFileName = "public/schema.graphqls";

const files = loadFiles.loadFilesSync(schemasDirection);
const mergeTypeDefs = merge.mergeTypeDefs(files);
const printedTypeDefs = graphql.print(mergeTypeDefs);

const schemaFileContent = `
# HeartBeatLive GraphQL API
# version = ${apiVersion}
# buildTime = ${new Date().toISOString()}

${printedTypeDefs}
`.trim();

fs.writeFile(schemaFileName, schemaFileContent, e => {
  if (e) {
    console.log(e);
  } else {
    console.log(`Created file: ${schemaFileName}`); 
  }
});
