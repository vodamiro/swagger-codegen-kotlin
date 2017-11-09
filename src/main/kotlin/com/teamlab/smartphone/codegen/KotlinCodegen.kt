package com.teamlab.smartphone.codegen

import com.google.gson.Gson
import io.swagger.codegen.*
import io.swagger.codegen.languages.JavaClientCodegen
import io.swagger.models.Model
import io.swagger.models.Operation
import io.swagger.models.Swagger
import io.swagger.models.properties.*
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private const val STAGE_MODEL = 1
private const val STAGE_API = 2

private const val API_PACKAGE = "cz.synetech.app.data.api"
private const val MODEL_PACKAGE = "cz.synetech.app.data.model.api"

private const val CODEGEN_IGNORE_FILE = ".swagger-codegen-ignore"


val generationStage = AtomicInteger(0)
val gson = Gson()

class KotlinCodegen : JavaClientCodegen(), CodegenConfig {
    override fun getTag() = CodegenType.CLIENT
    override fun getName() = "kotlin"
    override fun getHelp() = "Generate a Kotlin client."
    //override fun toApiName(name: String?) = "Api"
    val searchPostfixInModelName: String
    val replacePostfixInModelName: String
    val defaultPostfixInModelName: String

    val apiModelName = HashSet<String>()

    init {
        // region Settings
        apiPackage = API_PACKAGE
        modelPackage = MODEL_PACKAGE
        /*searchPostfixInModelName = "viewmodel"     // If this postfix is found
          replacePostfixInModelName = "RequestModel" // will be replaced with this string
          defaultPostfixInModelName = "APIModel"   // otherwise there will be added this postfix
  */

        searchPostfixInModelName = ""     // If this postfix is found
        replacePostfixInModelName = "" // will be replaced with this string
        defaultPostfixInModelName = ""   // otherwise there will be added this postfix
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
                "ByteArray",
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
                "String",
                "ByteArray",
                "Byte")
        (typeMapping as MutableMap) += mapOf(
                "integer" to "Int",
                "long" to "Long",
                "float" to "Float",
                "double" to "Double",
                "string" to "String",
                "byte" to "Byte",
                "binary" to "ByteArray",
                "byte[]" to "ByteArray",
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
        return super.fromModel(name, model, allDefinitions).apply {
            imports.remove("ApiModelProperty")
            imports.remove("ApiModel")
            imports.remove("IOException")
            imports.remove("JsonAdapter")
            imports.remove("JsonWriter")
            imports.remove("TypeAdapter")
            imports.remove("JsonReader")
        }
    }

    override fun postProcessOperations(objs: Map<String, Any>): Map<String, Any> {
        val newMap = super.postProcessOperations(objs)
        @Suppress("UNCHECKED_CAST")
        (newMap["operations"] as? Map<String, Any>)?.let {
            (it["operation"] as? List<CodegenOperation>)?.forEach {
                it.path = it.path.removePrefix("/")
            }
        }
        return newMap
    }

    override fun fromOperation(path: String?, httpMethod: String?, operation: Operation?, definitions: MutableMap<String, Model>?, swagger: Swagger?): CodegenOperation {
        val op = super.fromOperation(path, httpMethod, operation, definitions, swagger)

        val returnTypeObject = definitions?.entries?.find {
            it.key.removeIllegalSymbols() == op.returnType
        }?.value

        val modelInfo = returnTypeObject?.properties?.get("content")

        val newImports = HashSet<String>()

        op.imports.forEach {
            val correctName = it.removeIllegalModelStartNames()
            if (!languageSpecificPrimitives.contains(correctName)) {
                newImports.add(it.removeIllegalModelStartNames())
            }
        }

        op.imports = newImports

        if (op.returnType != null) {
            op.returnType = op.returnType.removeIllegalModelStartNames()
        }

        if (modelInfo != null) {
            if (modelInfo.type == "array") {
                val newReturnTypeObject = ((modelInfo as? ArrayProperty)?.items as? RefProperty)?.simpleRef
                if (newReturnTypeObject != null) {
                    op.returnType = "List<$newReturnTypeObject>"
                }
            } else if (modelInfo.type == "ref") {
                val newReturnTypeObject = (modelInfo as? RefProperty)?.simpleRef
                if (newReturnTypeObject != null) {
                    op.returnType = newReturnTypeObject.removeIllegalModelStartNames()
                }
            }
        }

        return op
    }

    override fun getSwaggerType(p: Property?): String {
        if (p is UUIDProperty) {
            return super.getSwaggerType(StringProperty())
        } else {
            return super.getSwaggerType(p)
        }
    }

    private fun String.removeIllegalSymbols(): String {
        return this.replace("[", "").replace("]", "")
    }

    private fun String.removeIllegalModelStartNames(): String {
        return this.replace(Regex("^MetaResponse"), "").replace(Regex("^Response"), "")
    }
    //endregion
}

fun main(vararg args: String) {
    // Generate
    cleanOutput(getOutputFolder(args))
    setupModelStage(getOutputFolder(args))
    SwaggerCodegen.main(args)
    setupApiStage(getOutputFolder(args))
    SwaggerCodegen.main(args)
    removeAllUnneededFiles(getOutputFolder(args))
    println("Done")

}

fun removeAllUnneededFiles(outputFolder: String) {
    val modelFolder = File(outputFolder + "/src/main/java/" + MODEL_PACKAGE.replace(".", "/") + "/")


    val files = modelFolder.listFiles { it ->
        println(it)
        it.name.startsWith("Response") || it.name.startsWith("Meta")
    }


    files?.forEach {
        if (it.delete()) {
            println("Deleted file \"${it.absolutePath}\"")
        } else {
            println("Cannot delete file \"${it.absolutePath}\" ... :(")
        }
    }
}

fun cleanOutput(outputDir: String) {
    println(" ###   #     #####  ###  #   #")
    println("#   #  #     #     #   # ##  #")
    println("#      #     ####  #   # # # #")
    println("#   #  #     #     ##### #  ##")
    println(" ###   ##### ##### #   # #   #")
    println(" ")
    val outputFolder = File(outputDir)
    outputFolder.deleteRecursively()
    outputFolder.mkdir()
}

fun getOutputFolder(args: Array<out String>): String {
    var parameterIndexed = -1
    args.forEachIndexed { index, it ->
        if (it == "-o") parameterIndexed = index
    }
    return args[parameterIndexed + 1]
}

fun setupModelStage(outputDir: String) {
    println("## ##   ###  ####  #### #    ")
    println("# # #  #   # #   # #    #    ")
    println("#   #  #   # #   # #### #    ")
    println("#   #  #   # #   # #    #    ")
    println("#   #   ###  ####  #### #####")
    println(" ")
    generationStage.set(STAGE_MODEL)
    setCodegenIgnore(outputDir, "**/" + API_PACKAGE.replace(".", "/") + "/*")
}

fun setupApiStage(outputDir: String) {
    println(" ###   ####  #")
    println("#   #  #   # #")
    println("#   #  ####  #")
    println("#####  #     #")
    println("#   #  #     #")
    generationStage.set(STAGE_API)
    setCodegenIgnore(outputDir, "**/" + MODEL_PACKAGE.replace(".", "/") + "/*")
}

fun setCodegenIgnore(outputDir: String, ignoreString: String) {
    val codegenFile = File(outputDir + "/" + CODEGEN_IGNORE_FILE)
    codegenFile.delete()
    val writter = codegenFile.bufferedWriter()
    writter.write(ignoreString)
    writter.flush()
    writter.close()

}

class PrettyPrintingMap<K : Any, V : Any>(map: Map<K, V>) {
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
            sb.append('=')
            sb.append('"')
            sb.append(entry.value)
            sb.append('"')
            sb.append('\n')
            if (iter.hasNext()) {
                sb.append(',').append(' ')
            }
        }
        return sb.toString()

    }
}
