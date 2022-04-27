package com.munoon.heartbeatlive.server

import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo

@AutoConfigureDataMongo
abstract class AbstractMongoDBTest : AbstractTest()