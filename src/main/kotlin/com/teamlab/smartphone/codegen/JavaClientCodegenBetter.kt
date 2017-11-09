package com.teamlab.smartphone.codegen

import com.google.common.collect.LinkedListMultimap
import io.swagger.codegen.*
import io.swagger.codegen.languages.AbstractJavaCodegen
import io.swagger.codegen.languages.features.BeanValidationFeatures
import io.swagger.codegen.languages.features.GzipFeatures
import io.swagger.codegen.languages.features.PerformBeanValidationFeatures
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.util.*
import java.util.regex.Pattern

import java.util.Collections.sort

open class JavaClientCodegenBetter : AbstractJavaCodegen(), BeanValidationFeatures, PerformBeanValidationFeatures, GzipFeatures {

    var gradleWrapperPackage = "gradle.wrapper"
    private var useRxJava = false
    private var useRxJava2 = false
    private var doNotUseRx = true // backwards compatibility for swagger configs that specify neither rx1 nor rx2 (mustache does not allow for boolean operators so we need this extra field)
    private var usePlayWS = false
    private var playVersion = PLAY_25
    private var parcelableModel = false
    private var useBeanValidation = false
    private var performBeanValidation = false
    private var useGzipFeature = false
    private var useRuntimeException = false

    init {
        outputFolder = "generated-code" + File.separator + "java"
        templateDir = "Java"
        embeddedTemplateDir = templateDir
        invokerPackage = "io.swagger.client"
        artifactId = "swagger-java-client"
        apiPackage = "io.swagger.client.api"
        modelPackage = "io.swagger.client.model"

        cliOptions.add(CliOption.newBoolean(USE_RX_JAVA, "Whether to use the RxJava adapter with the retrofit2 library."))
        cliOptions.add(CliOption.newBoolean(USE_RX_JAVA2, "Whether to use the RxJava2 adapter with the retrofit2 library."))
        cliOptions.add(CliOption.newBoolean(PARCELABLE_MODEL, "Whether to generate models for Android that implement Parcelable with the okhttp-gson library."))
        cliOptions.add(CliOption.newBoolean(USE_PLAY_WS, "Use Play! Async HTTP client (Play WS API)"))
        cliOptions.add(CliOption.newString(PLAY_VERSION, "Version of Play! Framework (possible values \"play24\", \"play25\")"))
        cliOptions.add(CliOption.newBoolean(AbstractJavaCodegen.SUPPORT_JAVA6, "Whether to support Java6 with the Jersey1 library."))
        cliOptions.add(CliOption.newBoolean(BeanValidationFeatures.USE_BEANVALIDATION, "Use BeanValidation API annotations"))
        cliOptions.add(CliOption.newBoolean(PerformBeanValidationFeatures.PERFORM_BEANVALIDATION, "Perform BeanValidation"))
        cliOptions.add(CliOption.newBoolean(GzipFeatures.USE_GZIP_FEATURE, "Send gzip-encoded requests"))
        cliOptions.add(CliOption.newBoolean(USE_RUNTIME_EXCEPTION, "Use RuntimeException instead of Exception"))

        supportedLibraries.put("jersey1", "HTTP client: Jersey client 1.19.4. JSON processing: Jackson 2.8.9. Enable Java6 support using '-DsupportJava6=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.")
        supportedLibraries.put("feign", "HTTP client: OpenFeign 9.4.0. JSON processing: Jackson 2.8.9")
        supportedLibraries.put("jersey2", "HTTP client: Jersey client 2.25.1. JSON processing: Jackson 2.8.9")
        supportedLibraries.put("okhttp-gson", "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.8.1. Enable Parcelable models on Android using '-DparcelableModel=true'. Enable gzip request encoding using '-DuseGzipFeature=true'.")
        supportedLibraries.put(RETROFIT_1, "HTTP client: OkHttp 2.7.5. JSON processing: Gson 2.3.1 (Retrofit 1.9.0). IMPORTANT NOTE: retrofit1.x is no longer actively maintained so please upgrade to 'retrofit2' instead.")
        supportedLibraries.put(RETROFIT_2, "HTTP client: OkHttp 3.8.0. JSON processing: Gson 2.6.1 (Retrofit 2.3.0). Enable the RxJava adapter using '-DuseRxJava[2]=true'. (RxJava 1.x or 2.x)")
        supportedLibraries.put("resttemplate", "HTTP client: Spring RestTemplate 4.3.9-RELEASE. JSON processing: Jackson 2.8.9")
        supportedLibraries.put("resteasy", "HTTP client: Resteasy client 3.1.3.Final. JSON processing: Jackson 2.8.9")
        supportedLibraries.put("vertx", "HTTP client: VertX client 3.2.4. JSON processing: Jackson 2.8.9")
        supportedLibraries.put("google-api-client", "HTTP client: Google API client 1.23.0. JSON processing: Jackson 2.8.9")

        val libraryOption = CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use")
        libraryOption.enum = supportedLibraries
        // set okhttp-gson as the default
        libraryOption.default = "okhttp-gson"
        cliOptions.add(libraryOption)
        setLibrary("okhttp-gson")

    }

    override fun getTag(): CodegenType {
        return CodegenType.CLIENT
    }

    override fun getName(): String {
        return "java"
    }

    override fun getHelp(): String {
        return "Generates a Java client library."
    }

    override fun processOpts() {
        super.processOpts()

        if (additionalProperties.containsKey(USE_RX_JAVA) && additionalProperties.containsKey(USE_RX_JAVA2)) {
            LOGGER.warn("You specified both RxJava versions 1 and 2 but they are mutually exclusive. Defaulting to v2.")
        } else if (additionalProperties.containsKey(USE_RX_JAVA)) {
            this.setUseRxJava(java.lang.Boolean.valueOf(additionalProperties[USE_RX_JAVA].toString())!!)
        }
        if (additionalProperties.containsKey(USE_RX_JAVA2)) {
            this.setUseRxJava2(java.lang.Boolean.valueOf(additionalProperties[USE_RX_JAVA2].toString())!!)
        }
        if (!useRxJava && !useRxJava2) {
            additionalProperties.put(DO_NOT_USE_RX, true)
        }
        if (additionalProperties.containsKey(USE_PLAY_WS)) {
            this.setUsePlayWS(java.lang.Boolean.valueOf(additionalProperties[USE_PLAY_WS].toString())!!)
        }
        additionalProperties.put(USE_PLAY_WS, usePlayWS)

        if (additionalProperties.containsKey(PLAY_VERSION)) {
            this.setPlayVersion(additionalProperties[PLAY_VERSION].toString())
        }
        additionalProperties.put(PLAY_VERSION, playVersion)

        if (additionalProperties.containsKey(PARCELABLE_MODEL)) {
            this.setParcelableModel(java.lang.Boolean.valueOf(additionalProperties[PARCELABLE_MODEL].toString())!!)
        }
        // put the boolean value back to PARCELABLE_MODEL in additionalProperties
        additionalProperties.put(PARCELABLE_MODEL, parcelableModel)

        if (additionalProperties.containsKey(BeanValidationFeatures.USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBooleanAndWriteBack(BeanValidationFeatures.USE_BEANVALIDATION))
        }

        if (additionalProperties.containsKey(PerformBeanValidationFeatures.PERFORM_BEANVALIDATION)) {
            this.setPerformBeanValidation(convertPropertyToBooleanAndWriteBack(PerformBeanValidationFeatures.PERFORM_BEANVALIDATION))
        }

        if (additionalProperties.containsKey(GzipFeatures.USE_GZIP_FEATURE)) {
            this.setUseGzipFeature(convertPropertyToBooleanAndWriteBack(GzipFeatures.USE_GZIP_FEATURE))
        }

        if (additionalProperties.containsKey(USE_RUNTIME_EXCEPTION)) {
            this.setUseRuntimeException(convertPropertyToBooleanAndWriteBack(USE_RUNTIME_EXCEPTION))
        }

        val invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/")
        val authFolder = "$sourceFolder/$invokerPackage.auth".replace(".", "/")
        val apiFolder = (sourceFolder + '/' + apiPackage).replace(".", "/")

        //Common files
        writeOptional(outputFolder, SupportingFile("pom.mustache", "", "pom.xml"))
        writeOptional(outputFolder, SupportingFile("README.mustache", "", "README.md"))
        writeOptional(outputFolder, SupportingFile("build.gradle.mustache", "", "build.gradle"))
        writeOptional(outputFolder, SupportingFile("build.sbt.mustache", "", "build.sbt"))
        writeOptional(outputFolder, SupportingFile("settings.gradle.mustache", "", "settings.gradle"))
        writeOptional(outputFolder, SupportingFile("gradle.properties.mustache", "", "gradle.properties"))
        writeOptional(outputFolder, SupportingFile("manifest.mustache", projectFolder, "AndroidManifest.xml"))
        supportingFiles.add(SupportingFile("travis.mustache", "", ".travis.yml"))
        supportingFiles.add(SupportingFile("ApiClient.mustache", invokerFolder, "ApiClient.java"))
        if ("resttemplate" != getLibrary()) {
            supportingFiles.add(SupportingFile("StringUtil.mustache", invokerFolder, "StringUtil.java"))
        }

        // google-api-client doesn't use the Swagger auth, because it uses Google Credential directly (HttpRequestInitializer)
        if ("google-api-client" != getLibrary()) {
            supportingFiles.add(SupportingFile("auth/HttpBasicAuth.mustache", authFolder, "HttpBasicAuth.java"))
            supportingFiles.add(SupportingFile("auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"))
            supportingFiles.add(SupportingFile("auth/OAuth.mustache", authFolder, "OAuth.java"))
            supportingFiles.add(SupportingFile("auth/OAuthFlow.mustache", authFolder, "OAuthFlow.java"))
        }
        supportingFiles.add(SupportingFile("gradlew.mustache", "", "gradlew"))
        supportingFiles.add(SupportingFile("gradlew.bat.mustache", "", "gradlew.bat"))
        supportingFiles.add(SupportingFile("gradle-wrapper.properties.mustache",
                gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.properties"))
        supportingFiles.add(SupportingFile("gradle-wrapper.jar",
                gradleWrapperPackage.replace(".", File.separator), "gradle-wrapper.jar"))
        supportingFiles.add(SupportingFile("git_push.sh.mustache", "", "git_push.sh"))
        supportingFiles.add(SupportingFile("gitignore.mustache", "", ".gitignore"))

        if (performBeanValidation) {
            supportingFiles.add(SupportingFile("BeanValidationException.mustache", invokerFolder,
                    "BeanValidationException.java"))
        }

        //TODO: add doc to retrofit1 and feign
        if ("feign" == getLibrary() || "retrofit" == getLibrary()) {
            modelDocTemplateFiles.remove("model_doc.mustache")
            apiDocTemplateFiles.remove("api_doc.mustache")
        }

        if (!("feign" == getLibrary() || "resttemplate" == getLibrary() || usesAnyRetrofitLibrary() || "google-api-client" == getLibrary())) {
            supportingFiles.add(SupportingFile("apiException.mustache", invokerFolder, "ApiException.java"))
            supportingFiles.add(SupportingFile("Configuration.mustache", invokerFolder, "Configuration.java"))
            supportingFiles.add(SupportingFile("Pair.mustache", invokerFolder, "Pair.java"))
            supportingFiles.add(SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"))
        }

        if ("feign" == getLibrary()) {
            additionalProperties.put("jackson", "true")
            supportingFiles.add(SupportingFile("ParamExpander.mustache", invokerFolder, "ParamExpander.java"))
            supportingFiles.add(SupportingFile("EncodingUtils.mustache", invokerFolder, "EncodingUtils.java"))
        } else if ("okhttp-gson" == getLibrary() || StringUtils.isEmpty(getLibrary())) {
            // the "okhttp-gson" library template requires "ApiCallback.mustache" for async call
            supportingFiles.add(SupportingFile("ApiCallback.mustache", invokerFolder, "ApiCallback.java"))
            supportingFiles.add(SupportingFile("ApiResponse.mustache", invokerFolder, "ApiResponse.java"))
            supportingFiles.add(SupportingFile("JSON.mustache", invokerFolder, "JSON.java"))
            supportingFiles.add(SupportingFile("ProgressRequestBody.mustache", invokerFolder, "ProgressRequestBody.java"))
            supportingFiles.add(SupportingFile("ProgressResponseBody.mustache", invokerFolder, "ProgressResponseBody.java"))
            supportingFiles.add(SupportingFile("GzipRequestInterceptor.mustache", invokerFolder, "GzipRequestInterceptor.java"))
            additionalProperties.put("gson", "true")
        } else if (usesAnyRetrofitLibrary()) {
            supportingFiles.add(SupportingFile("auth/OAuthOkHttpClient.mustache", authFolder, "OAuthOkHttpClient.java"))
            supportingFiles.add(SupportingFile("CollectionFormats.mustache", invokerFolder, "CollectionFormats.java"))
            additionalProperties.put("gson", "true")
            if ("retrofit2" == getLibrary() && !usePlayWS) {
                supportingFiles.add(SupportingFile("JSON.mustache", invokerFolder, "JSON.java"))
            }
        } else if ("jersey2" == getLibrary() || "resteasy" == getLibrary()) {
            supportingFiles.add(SupportingFile("JSON.mustache", invokerFolder, "JSON.java"))
            additionalProperties.put("jackson", "true")
        } else if ("jersey1" == getLibrary()) {
            additionalProperties.put("jackson", "true")
        } else if ("resttemplate" == getLibrary()) {
            additionalProperties.put("jackson", "true")
            supportingFiles.add(SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"))
        } else if ("vertx" == getLibrary()) {
            typeMapping.put("file", "AsyncFile")
            importMapping.put("AsyncFile", "io.vertx.core.file.AsyncFile")
            // setJava8Mode(true);
            additionalProperties.put("java8", "true")
            additionalProperties.put("jackson", "true")
            apiTemplateFiles.put("apiImpl.mustache", "Impl.java")
            apiTemplateFiles.put("rxApiImpl.mustache", ".java")
            supportingFiles.remove(SupportingFile("manifest.mustache", projectFolder, "AndroidManifest.xml"))
        } else if ("google-api-client" == getLibrary()) {
            additionalProperties.put("jackson", "true")
        } else {
            LOGGER.error("Unknown library option (-l/--library): " + getLibrary())
        }

        if (usePlayWS) {
            // remove unsupported auth
            val iter = supportingFiles.iterator()
            while (iter.hasNext()) {
                val sf = iter.next()
                if (sf.templateFile.startsWith("auth/")) {
                    iter.remove()
                }
            }

            apiTemplateFiles.remove("api.mustache")

            if (PLAY_24 == playVersion) {
                additionalProperties.put(PLAY_24, true)
                apiTemplateFiles.put("play24/api.mustache", ".java")

                supportingFiles.add(SupportingFile("play24/ApiClient.mustache", invokerFolder, "ApiClient.java"))
                supportingFiles.add(SupportingFile("play24/Play24CallFactory.mustache", invokerFolder, "Play24CallFactory.java"))
                supportingFiles.add(SupportingFile("play24/Play24CallAdapterFactory.mustache", invokerFolder,
                        "Play24CallAdapterFactory.java"))
            } else {
                additionalProperties.put(PLAY_25, true)
                apiTemplateFiles.put("play25/api.mustache", ".java")

                supportingFiles.add(SupportingFile("play25/ApiClient.mustache", invokerFolder, "ApiClient.java"))
                supportingFiles.add(SupportingFile("play25/Play25CallFactory.mustache", invokerFolder, "Play25CallFactory.java"))
                supportingFiles.add(SupportingFile("play25/Play25CallAdapterFactory.mustache", invokerFolder,
                        "Play25CallAdapterFactory.java"))
                additionalProperties.put("java8", "true")
            }

            supportingFiles.add(SupportingFile("play-common/auth/ApiKeyAuth.mustache", authFolder, "ApiKeyAuth.java"))
            supportingFiles.add(SupportingFile("auth/Authentication.mustache", authFolder, "Authentication.java"))
            supportingFiles.add(SupportingFile("Pair.mustache", invokerFolder, "Pair.java"))

            additionalProperties.put("jackson", "true")
            additionalProperties.remove("gson")
        }

        if (additionalProperties.containsKey("jackson")) {
            supportingFiles.add(SupportingFile("RFC3339DateFormat.mustache", invokerFolder, "RFC3339DateFormat.java"))
            if ("threetenbp" == dateLibrary && !usePlayWS) {
                supportingFiles.add(SupportingFile("CustomInstantDeserializer.mustache", invokerFolder, "CustomInstantDeserializer.java"))
            }
        }
    }

    private fun usesAnyRetrofitLibrary(): Boolean {
        return getLibrary() != null && getLibrary().contains(RETROFIT_1)
    }

    private fun usesRetrofit2Library(): Boolean {
        return getLibrary() != null && getLibrary().contains(RETROFIT_2)
    }

    override fun postProcessOperations(objs: Map<String, Any>): Map<String, Any> {
        super.postProcessOperations(objs)
        if (usesAnyRetrofitLibrary()) {
            val operations = objs["operations"] as Map<String, Any>
            if (operations != null) {
                val ops = operations["operation"] as List<CodegenOperation>
                for (operation in ops) {
                    if (operation.hasConsumes == java.lang.Boolean.TRUE) {

                        if (isMultipartType(operation.consumes)) {
                            operation.isMultipart = java.lang.Boolean.TRUE
                        } else {
                            operation.prioritizedContentTypes = prioritizeContentTypes(operation.consumes)
                        }
                    }

                    if (operation.returnType == null) {
                        operation.returnType = "Void"
                    }

                    if (usesRetrofit2Library() && StringUtils.isNotEmpty(operation.path) && operation.path.startsWith("/")) {
                        operation.path = operation.path.substring(1)
                    }

                    // sorting operation parameters to make sure path params are parsed before query params
                    if (operation.allParams != null) {
                        sort(operation.allParams, Comparator { one, another ->
                            if (one.isPathParam && another.isQueryParam) {
                                return@Comparator -1
                            }
                            if (one.isQueryParam && another.isPathParam) {
                                1
                            } else 0
                        })
                        val iterator = operation.allParams.iterator()
                        while (iterator.hasNext()) {
                            val param = iterator.next()
                            param.hasMore = iterator.hasNext()
                        }
                    }
                }
            }

        }

        // camelize path variables for Feign client
        if ("feign" == getLibrary()) {
            val operations = objs["operations"] as Map<String, Any>
            val operationList = operations["operation"] as List<CodegenOperation>
            for (op in operationList) {
                val path = op.path
                val items = path.split("/".toRegex()).toTypedArray()

                for (i in items.indices) {
                    if (items[i].matches("^\\{(.*)\\}$".toRegex())) { // wrap in {}
                        // camelize path variable
                        items[i] = "{" + DefaultCodegen.camelize(items[i].substring(1, items[i].length - 1), true) + "}"
                    }
                }
                op.path = StringUtils.join(items, "/")
            }
        }

        return objs
    }

    override fun apiFilename(templateName: String, tag: String): String {
        println("API: $templateName\n$tag\n\n\n")
        if ("vertx" == getLibrary()) {
            val suffix = apiTemplateFiles()[templateName]
            var subFolder = ""
            if (templateName.startsWith("rx")) {
                subFolder = "/rxjava"
            }
            return apiFileFolder() + subFolder + '/' + toApiFilename(tag) + suffix
        } else {
            return super.apiFilename(templateName, tag)
        }
    }

    override fun postProcessModelProperty(model: CodegenModel, property: CodegenProperty) {
        super.postProcessModelProperty(model, property)
        if (!BooleanUtils.toBoolean(model.isEnum)) {
            //final String lib = getLibrary();
            //Needed imports for Jackson based libraries
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonProperty")
                model.imports.add("JsonValue")
            }
            if (additionalProperties.containsKey("gson")) {
                model.imports.add("SerializedName")
                model.imports.add("TypeAdapter")
                model.imports.add("JsonAdapter")
                model.imports.add("JsonReader")
                model.imports.add("JsonWriter")
                model.imports.add("IOException")
            }
        } else { // enum class
            //Needed imports for Jackson's JsonCreator
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonValue")
                model.imports.add("JsonCreator")
            }
        }
    }

    override fun postProcessAllModels(objs: Map<String, Any>): Map<String, Any> {
        val allProcessedModels = super.postProcessAllModels(objs)
        if (!additionalProperties.containsKey("gsonFactoryMethod")) {
            val allModels = ArrayList<Any>()
            for (name in allProcessedModels.keys) {
                val models = allProcessedModels[name] as Map<String, Any>
                try {
                    allModels.add((models["models"] as List<Any>)[0])
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            additionalProperties.put("parent", modelInheritanceSupportInGson(allModels))
        }
        return allProcessedModels
    }

    override fun postProcessModelsEnum(objs: Map<String, Any>): Map<String, Any> {
        var objs = objs
        objs = super.postProcessModelsEnum(objs)
        //Needed import for Gson based libraries
        if (additionalProperties.containsKey("gson")) {
            val imports = objs["imports"] as ArrayList<Map<String, String>>
            val models = objs["models"] as List<Any>
            for (_mo in models) {
                val mo = _mo as Map<String, Any>
                val cm = mo["model"] as CodegenModel
                // for enum model
                if (java.lang.Boolean.TRUE == cm.isEnum && cm.allowableValues != null) {
                    cm.imports.add(importMapping["SerializedName"])
                    val item = HashMap<String, String>()
                    item.put("import", importMapping["SerializedName"]!!)
                    imports.add(item)
                }
            }
        }
        return objs
    }

    private fun modelInheritanceSupportInGson(allModels: List<*>): List<Map<String, Any>> {
        val byParent = LinkedListMultimap.create<CodegenModel, CodegenModel>()
        for (m in allModels) {
            val entry = m as Map<*, *>
            val parent = (entry["model"] as CodegenModel).parentModel
            if (null != parent) {
                byParent.put(parent, entry["model"] as CodegenModel)
            }
        }
        val parentsList = ArrayList<Map<String, Any>>()
        for (parentModel in byParent.keySet()) {
            val childrenList = ArrayList<Map<String, Any>>()
            val parent = HashMap<String, Any>()
            parent.put("classname", parentModel.classname)
            val childrenModels = byParent.get(parentModel)
            for (model in childrenModels) {
                val child = HashMap<String, Any>()
                child.put("name", model.name)
                child.put("classname", model.classname)
                childrenList.add(child)
            }
            parent.put("children", childrenList)
            parent.put("discriminator", parentModel.discriminator)
            parentsList.add(parent)
        }
        return parentsList
    }

    fun setUseRxJava(useRxJava: Boolean) {
        this.useRxJava = useRxJava
        doNotUseRx = false
    }

    fun setUseRxJava2(useRxJava2: Boolean) {
        this.useRxJava2 = useRxJava2
        doNotUseRx = false
    }

    fun setDoNotUseRx(doNotUseRx: Boolean) {
        this.doNotUseRx = doNotUseRx
    }

    fun setUsePlayWS(usePlayWS: Boolean) {
        this.usePlayWS = usePlayWS
    }

    fun setPlayVersion(playVersion: String) {
        this.playVersion = playVersion
    }

    fun setParcelableModel(parcelableModel: Boolean) {
        this.parcelableModel = parcelableModel
    }

    override fun setUseBeanValidation(useBeanValidation: Boolean) {
        this.useBeanValidation = useBeanValidation
    }

    override fun setPerformBeanValidation(performBeanValidation: Boolean) {
        this.performBeanValidation = performBeanValidation
    }

    override fun setUseGzipFeature(useGzipFeature: Boolean) {
        this.useGzipFeature = useGzipFeature
    }

    fun setUseRuntimeException(useRuntimeException: Boolean) {
        this.useRuntimeException = useRuntimeException
    }

    companion object {
        internal val MEDIA_TYPE = "mediaType"

        private val LOGGER = LoggerFactory.getLogger(io.swagger.codegen.languages.JavaClientCodegen::class.java)

        val USE_RX_JAVA = "useRxJava"
        val USE_RX_JAVA2 = "useRxJava2"
        val DO_NOT_USE_RX = "doNotUseRx"
        val USE_PLAY_WS = "usePlayWS"
        val PLAY_VERSION = "playVersion"
        val PARCELABLE_MODEL = "parcelableModel"
        val USE_RUNTIME_EXCEPTION = "useRuntimeException"

        val PLAY_24 = "play24"
        val PLAY_25 = "play25"

        val RETROFIT_1 = "retrofit"
        val RETROFIT_2 = "retrofit2"

        /**
         * Prioritizes consumes mime-type list by moving json-vendor and json mime-types up front, but
         * otherwise preserves original consumes definition order.
         * [application/vnd...+json,... application/json, ..as is..]
         *
         * @param consumes consumes mime-type list
         * @return
         */
        internal fun prioritizeContentTypes(consumes: List<MutableMap<String, String?>>): List<MutableMap<String, String?>> {
            if (consumes.size <= 1)
                return consumes

            val prioritizedContentTypes = ArrayList<MutableMap<String, String?>>(consumes.size)

            val jsonVendorMimeTypes = ArrayList<MutableMap<String, String?>>(consumes.size)
            val jsonMimeTypes = ArrayList<MutableMap<String, String?>>(consumes.size)

            for (consume in consumes) {
                if (isJsonVendorMimeType(consume[MEDIA_TYPE])) {
                    jsonVendorMimeTypes.add(consume)
                } else if (isJsonMimeType(consume[MEDIA_TYPE])) {
                    jsonMimeTypes.add(consume)
                } else
                    prioritizedContentTypes.add(consume)

                consume.put("hasMore", "true")
            }

            prioritizedContentTypes.addAll(0, jsonMimeTypes)
            prioritizedContentTypes.addAll(0, jsonVendorMimeTypes)

            prioritizedContentTypes[prioritizedContentTypes.size - 1].put("hasMore", null)

            return prioritizedContentTypes
        }

        private fun isMultipartType(consumes: List<Map<String, String>>): Boolean {
            val firstType = consumes[0]
            if (firstType != null) {
                if ("multipart/form-data" == firstType[MEDIA_TYPE]) {
                    return true
                }
            }
            return false
        }

        private val JSON_MIME_PATTERN = Pattern.compile("(?i)application\\/json(;.*)?")
        private val JSON_VENDOR_MIME_PATTERN = Pattern.compile("(?i)application\\/vnd.(.*)+json(;.*)?")

        /**
         * Check if the given MIME is a JSON MIME.
         * JSON MIME examples:
         * application/json
         * application/json; charset=UTF8
         * APPLICATION/JSON
         */
        internal fun isJsonMimeType(mime: String?): Boolean {
            return mime != null && JSON_MIME_PATTERN.matcher(mime).matches()
        }

        /**
         * Check if the given MIME is a JSON Vendor MIME.
         * JSON MIME examples:
         * application/vnd.mycompany+json
         * application/vnd.mycompany.resourceA.version1+json
         */
        internal fun isJsonVendorMimeType(mime: String?): Boolean {
            return mime != null && JSON_VENDOR_MIME_PATTERN.matcher(mime).matches()
        }
    }

}
