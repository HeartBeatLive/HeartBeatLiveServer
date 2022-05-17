## Documentation generation
* We use [SpectaQL](https://github.com/anvilco/spectaql) for generating GraphQL API documentation.
* All documentation configs placing in `config.yml` file.
* Run `npm run build` to build documentation (run `npm install` previously to download all dependencies).
* Documentation will be built in `public` folder.
* Run `npm run build-schema` to build GraphQL schema (it will be places at `public/schema.graphqls` file).
