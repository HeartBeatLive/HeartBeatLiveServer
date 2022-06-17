package com.munoon.heartbeatlive.server.common

import org.bson.Document
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationExpression
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext

object MongodbUtils {
    inline fun <reified T> Document.getList(key: String): List<T> = getList(key, T::class.java)
}

class LookupAggregation(
    private val from: String,
    private val asField: String,
    private val localField: String? = null,
    private val foreignField: String? = null,
    private val let: Map<String, String>? = null,
    private val pipeline: Aggregation? = null
) : AggregationOperation {
    override fun toDocument(context: AggregationOperationContext): Document {
        val value = mutableMapOf<String, Any>("from" to from, "as" to asField)
        if (localField != null) value["localField"] = localField
        if (foreignField != null) value["foreignField"] = foreignField
        if (let != null) value["let"] = let
        if (pipeline != null) value["pipeline"] = pipeline.toPipeline(context)

        return context.getMappedObject(Document("\$lookup", value))
    }
}

class ProjectAggregation(private val fields: Map<String, Any>) : AggregationOperation {
    override fun toDocument(context: AggregationOperationContext): Document {
        val value = fields.mapValues { (_, value) -> when (value) {
            is String -> value
            is AggregationExpression -> value.toDocument(context)
            else -> throw IllegalArgumentException("Field value must be string or AggregationExpression!")
        } }
        return context.getMappedObject(Document("\$project", value))
    }

    constructor(vararg pairs: Pair<String, Any>) : this(pairs.toMap())
}

class GetFieldExpression(
    private val field: String,
    private val input: AggregationExpression
) : AggregationExpression {
    override fun toDocument(context: AggregationOperationContext): Document {
        return context.getMappedObject(Document("\$getField", mapOf(
            "field" to field,
            "input" to input.toDocument(context)
        )))
    }
}

class FilterExpression(
    private val input: AggregationExpression,
    private val asField: String,
    private val cond: AggregationExpression
) : AggregationExpression {
    override fun toDocument(context: AggregationOperationContext): Document {
        return context.getMappedObject(Document("\$filter", mapOf(
            "input" to input.toDocument(context),
            "as" to asField,
            "cond" to cond.toDocument(context)
        )))
    }
}

class MapExpression(
    val input: AggregationExpression,
    val asField: String,
    val expr: String
) : AggregationExpression {
    override fun toDocument(context: AggregationOperationContext): Document {
        return context.getMappedObject(Document("\$map", mapOf(
            "input" to input.toDocument(context),
            "as" to asField,
            "in" to expr
        )))
    }
}