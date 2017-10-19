package com.teamlab.smartphone.codegen

import io.swagger.codegen.*
import io.swagger.codegen.languages.JavaClientCodegen
import io.swagger.models.Model
import io.swagger.models.Operation
import io.swagger.models.properties.Property
import io.swagger.models.properties.StringProperty
import io.swagger.models.properties.UUIDProperty

class KotlinCodegen : JavaClientCodegen(), CodegenConfig {
    override fun getTag() = CodegenType.CLIENT
    override fun getName() = "kotlin"
    override fun getHelp() = "Generate a Kotlin client."
    //override fun toApiName(name: String?) = "Api"
    val searchPostfixInModelName: String
    val replacePostfixInModelName: String
    val defaultPostfixInModelName: String


    init {
        // region Settings

        apiPackage = "cz.synetech.app.data.api"
        modelPackage = "cz.synetech.app.data.model"
        searchPostfixInModelName = "viewmodel"     // If this postfix is found
        replacePostfixInModelName = "RequestModel" // will be replaced with this string
        defaultPostfixInModelName = "APIModel"   // otherwise there will be added this postfix

        // endregion

        // region Kotlin generating configuration
        supportsInheritance = false
        templateDir = "kotlin"
        embeddedTemplateDir = "kotlin"
        modelTemplateFiles["model.mustache"] = ".kt"
        apiTemplateFiles["api.mustache"] = ".kt"
        apiTestTemplateFiles.clear()
        modelDocTemplateFiles.clear()
        apiDocTemplateFiles.clear()
        // https://github.com/JetBrains/kotlin/blob/master/core/descriptors/src/org/jetbrains/kotlin/renderer/KeywordStringsGenerated.java
        (reservedWords as MutableSet) += setOf(
                "package",
                "as",
                "typealias",
                "class",
                "this",
                "super",
                "val",
                "var",
                "fun",
                "for",
                "null",
                "true",
                "false",
                "is",
                "in",
                "throw",
                "return",
                "break",
                "continue",
                "object",
                "if",
                "try",
                "else",
                "while",
                "do",
                "when",
                "interface",
                "typeof")
        (defaultIncludes as MutableSet) += setOf(
                "integer",
                "array",
                "string",
                "List",
                "Map",
                "UUID")
        (languageSpecificPrimitives as MutableSet) += setOf(
                "Boolean",
                "Double",
                "Float",
                "Long",
                "Int",
                "Short",
                "Byte")
        (typeMapping as MutableMap) += mapOf(
                "integer" to "Int",
                "long" to "Long",
                "float" to "Float",
                "double" to "Double",
                "string" to "String",
                "byte" to "Byte",
                "binary" to "ByteArray",
                "boolean" to "Boolean",
                "date" to "Date",
                "dateTime" to "Date",
                "password" to "String",
                "array" to "List",
                "map" to "Map",
                "uuid" to "UUID")
        (importMapping as MutableMap) += mapOf(
                "Date" to "java.util.Date",
                "UUID" to "java.util.UUID")
        // endregion
    }

    /**
     * Function for generating API request function name
     */
    override fun getOrGenerateOperationId(operation: Operation, path: String, httpMethod: String): String {
        var tmpPath = path.replace("\\{".toRegex(), "")
        tmpPath = tmpPath.replace("\\}".toRegex(), "")
        val parts = (httpMethod + "/" + tmpPath).split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val builder = StringBuilder()
        if ("/" == tmpPath) {
            builder.append("root")
        }

        val args = parts
        val lengths = parts.size

        for (i in 0..lengths - 1) {
            var part = args[i]
            if (part.length > 0) {
                if (builder.toString().length == 0) {
                    part = Character.toLowerCase(part[0]) + part.substring(1)
                } else {
                    part = this.initialCaps(part)
                }

                builder.append(part)
            }
        }
        return this.sanitizeName(builder.toString())
    }

    // region Other overridden function

    override fun processOpts() {
        super.processOpts()
        supportingFiles.clear()
    }

    override fun fromModel(name: String, model: Model, allDefinitions: MutableMap<String, Model>): CodegenModel {
        //println(" | "+name.replace("ViewModel","RequestModel")+" [fromModel]")
        return super.fromModel(name, model, allDefinitions).apply {
            imports.remove("ApiModelProperty")
            imports.remove("ApiModel")
        }
    }

    override fun postProcessOperations(objs: MutableMap<String, Any>): MutableMap<String, Any> {
        //println(PrettyPrintingMap(objs).toString())

        super.postProcessOperations(objs)
        @Suppress("UNCHECKED_CAST")
        (objs["operations"] as? Map<String, Any>)?.let {
            (it["operation"] as? List<CodegenOperation>)?.forEach {
                it.path = it.path.removePrefix("/")
            }
        }
        return objs
    }

    override fun postProcessModelsEnum(objs: MutableMap<String, Any>?): MutableMap<String, Any> {
        println(PrettyPrintingMap(objs ?: emptyMap()).toString())
        return super.postProcessModelsEnum(objs)
    }

    override fun getSwaggerType(p: Property?): String {
        if (p is UUIDProperty) {
            return super.getSwaggerType(StringProperty())
        } else {
            return super.getSwaggerType(p)
        }
    }

    override fun toModelName(name: String): String {
        return this.initialCaps(this.modelNamePrefix + removeViewModelToResponse(name) + this.modelNameSuffix)
    }

    private fun removeViewModelToResponse(name: String): String {
        if (name.endsWith(searchPostfixInModelName, ignoreCase = true)) {
            return name.substring(0, name.length - searchPostfixInModelName.length) + replacePostfixInModelName
        } else {
            return name + defaultPostfixInModelName
        }
    }
    //endregion
}

fun main(vararg args: String) = SwaggerCodegen.main(args)

class PrettyPrintingMap<K, V>(map: Map<K, V>) {
    val map: Map<K, V>

    init {
        this.map = map;
    }

    override fun toString(): String {
        val sb = StringBuilder()
        val iter = map.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            sb.append(entry.key)
            sb.append('=').append('"')
            sb.append(entry.value)
            sb.append('"')
            if (iter.hasNext()) {
                sb.append(',').append(' ')
            }
        }
        return sb.toString()

    }
}
