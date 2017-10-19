Swagger Codegen Kotlin
===

```bash
$ ./gradlew run -PappArgs="['generate', '-l', 'kotlin', '-i', 'https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v2.0/yaml/petstore.yaml', '-c', 'path/to/config.json', '-o', 'path/to/output']"
```

Anyway configuration file (`'-c', 'path/to/config.json'`) is currently not used, due lack of time. So any changes should be made in source file located in `src/main/kotlin/com/teamlab/smartphone/codegen/KotlinCodegen.kt`. 
There are variables in init block in region `Setting` which can be tweaked to adjust code generation.

All responses from API returns RxJava Single containing wrapper object `APIResponse` eg. (eg. `Single<APIResponse<LoginResponse>>`).
Default empty responses are represented with kotlin `Any` (eg. `Single<APIResponse<Any>>`).

*APIResponse* must be located in package `cz.synetech.app.data.generalmodels`. However import definition of this file can be changed in file `src/main/resources/kotlin/api.mustache`.

After generation you can add generated files to your project and add API classes to dagger.


For DateTime is used DateTime class from Joda library. **Don't forget to include it in your app _build.grade_.**