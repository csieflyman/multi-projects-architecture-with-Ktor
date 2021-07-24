# Multi-Projects Architecture with Ktor

### Multi-Projects Architecture
This project contain two subprojects base on common infrastructure and library but each subproject can be deployed optionally with ktor module configuration like microservice. Subproject also has its own
* routes and openapi document
* authentication methods
* user types and roles
* notification types

### Technique Stack
* Kotlin 1.5.21
* Ktor 1.6.1
* Gradle 7.1.1
* PostgreSQL 13.2
* Redis 6.2.1
* Kotlinx Serialization, Kotlinx Coroutine
* Exposed ORM, HikariCP, Flyway database migration tool
* Koin DI

### Ktor Enhancement
* Ktor Feature (Plugin)
    * integrate with Koin DI
    * All features can be configured with DSL or external config file 
* i18n Support
    * specify the language supported by application in config file
    * retrieving supported languages from an HTTP request cookie and Accept-Language header as ktor ApplicationCall extension function
    * support HOCON and Java Properties message provider
* OpenAPI Generator
    * support multiple openapi documents
    * support basic authentication to protect your openapi document
    * integrate gradle git plugin to add git version information into openapi document
* Authentication and Role-Based Authoriation, like Spring Security
    * You can specify authentication providers, user types and user roles in ktor routing DSL
```kotlin=
authorize(ClubAuth.Admin) {

    put<UUIDEntityIdLocation, UpdateUserForm, Unit>(ClubOpenApi.UpdateUser) { _, form ->
        clubUserService.updateUser(form)
        call.respond(HttpStatusCode.OK)
    }
}
```
* Typesafe Config
    * replace ktor ApplicationConfig with typesafe config and convert config to Kotlin Objects using [config4k](https://github.com/config4k/config4k)
* Request Data Validation
    * validate incoming request body and path, query parameters automatically before pass it to your route dsl function
### Infrastructure
* Logging
    * use coroutine channel to write log to different destinations, support file, database, AWS Kinesis stream
    * includes request log, error log, login log, notification log 
* Authentication Methods
    * Service: API key authentication
    * User: password authentication using bcrypt
* Redis
    * session storage and support session key expired notification by [Redis PubSub Keyspace Notification](https://redis.io/topics/notifications)
    * data cache
    * use [ktorio redis client](https://github.com/ktorio/ktor-clients) based on coroutines. ktorio is a experimental project developed by JetBrains ktor team. I will use [Lettuce coroutine extension](https://lettuce.io/core/release/reference/#kotlin) in the future 
* Notification Service
    * use coroutine channel to send notifications to multiple channels, includes email(AWS SES), push(Firebase), sms(not implemented yet)
    * integrate freemarker template engine
    * support user language preference
* Mobile App Management
    * support multiple apps
    * check app version whether need to upgrade or not 
    * manage user devices and push tokens
* Performance Tunning
    * All Coroutine Channel and Java ExeuctorService threadpool parameters can be configured in the config file. You can also create the config files for each different environment in the deploy folder
------------
## 繁體中文 (Traditional Chinese)
Ktor 是 JetBrains 開發的 Web 框架，其特性是
- 100% 純 Kotlin 開發
- 使用 Coroutine 非同步的方式處理請求
- Unopinionated 使得 Ktor 核心更加地輕量、啟動快速

其中 Unopinionated 的特性，Ktor 並沒有預先整合常用的第三方框架及函式庫，例如 Dependency Injection, ORM, Logging...等，而是讓開發者能自由選擇整合至 Ktor。但也因為如此，開發者不僅需要花時間進行整合的工作，對於新手或是習慣 Spring Boot 全家桶的開發者，更是存在不小的門檻。

另一方面，Ktor 的架構設計是讓開發者透過實作 Feature (官方未來將改名為 Plugin)，把 intercepting function 加入至 request pipeline 之中，藉此增加功能或改變行為。這種方式與 Spring Boot 規定開發者實作特定的介面或繼承特定類別，再透過 dependency injection 的方式注入有所不同，再加上如果開發者不熟 Lambda 與 DSL 寫法，就不知該如何動手實作 Feature。

由於 Ktor 是一個很年輕的框架，所以使用人數不多，網路上的範例也很少，而且大多為展示簡單的單一功能，缺乏將各個功能整合起來的完整後端服務範例。另一方面，Ktor 本身是 unopinionated 的微框架，官方文件也沒有建議的開發指南，所以開發者必須根據自身經驗，事先思考如何規劃專案的檔案結構及程式架構，才能建構容易維護的專案。

綜合以上原因，本專案使用以下技術且包含常見網站後端服務功能的範例，供大家參考學習。截至 2021-07-21，codebase 累計已有 241 個 kt 檔，不含空白行超過 13000 行

### Ktor Enhancement

Ktor 是一個微框架，缺乏與常用第三方框架或函式庫之間的整合，例如 DI, ORM，甚至連 request data validation 及 i18n 也沒有實作。所以如果要直接使用 Ktor 開發專案，就無法像 Spring Boot 設定後立即使用，必須要先花時間自行開發缺少的功能及整合，所以本專案對 Ktor 進行了以下增強改善

* Ktor Feature (官方未來將改名為 Plugin)
    * 整合了 Koin DI 至 Ktor Feature, 協助初始化 Feature
    * Ktor Feature 的開發慣例是使用 DSL 語法進行設定，但實務上，許多參數設定必須由外部設定檔或環境變數提供。所以本專案實作的所有 Feature 都支援以上2種方式，並且以外部設定檔為優先
* i18n Support
    * 在設定檔指定系統支援的語言 `myapp.infra.i18n.langs = ["zh-TW", "en"]`
    * 多國語言訊息檔支援 HOCON 及 Java Properties 2 種格式
    * 可從 cookie 或 Accept-Language header 取得 HTTP 請求的語言，再使用 Ktor ApplicationCall 的 extension function `lang()` 進行操作
* OpenAPI Generator
    * 自行實作文件產生器，所以可根據自身需求調整以最佳化產出的文件 
    * 以編程方式撰寫 OpenAPI Definition，並且集中在專屬的 kt 檔案方便管理，取代一般在 route function 加註大量 annotation 的方式，使得 route function 看起來更簡潔
    * 每個子專案可各自產生文件，避免將所有不同功能的 API 都集中在一份文件
    * 支援 Http Basic Authentication，保護 API 文件不外流
    * 整合 Gradle Git Plugin，將 Git 版本資訊、建置部署時間…等資訊加進文件中 
* Authentication and Role-Based Authoriation
    * Ktor 本身僅實作 authentication，並沒有定義 User 及 User Role。本專案可讓每個子專案定義自己的 User Types 及其 Roles，並整合原有 Ktor authentication 機制，達到類似 Spring Security 的功能
```kotlin=
authorize(ClubAuth.Admin) {

    put<UUIDEntityIdLocation, UpdateUserForm, Unit>(ClubOpenApi.UpdateUser) { _, form ->
        clubUserService.updateUser(form)
        call.respond(HttpStatusCode.OK)
    }
}
```
* Type-safe and Validatable Configuration
    * Ktor 讀取設定檔的方式是透過 ApplicationConfig 物件，但只能使用 `getString()` 函式取值。本專案使用 [Config4k](https://github.com/config4k/config4k) 將設定值轉換至 Kotlin data class，不僅可以達到 type-safe 的效果，直接操作物件的方式也更簡潔。除此之外，本專案也在 config4k 轉換時插入驗證函式`validate()`，類別可實作此函式檢查設定值是否有效
```kotlin=
data class SessionConfig(
    val expireDuration: Duration? = null,
    val extendDuration: Duration? = null
) : ValidateableConfig {

    override fun validate() {
        require(if (expireDuration != null && extendDuration != null) expireDuration > extendDuration else true) {
            "expireDuration $expireDuration should be greater than extendDuration $extendDuration"
        }
    }
}
```
* Request Data Validation
    * Ktor 沒有實作對請求資料進行驗證的功能，本專案透過自定義 route extension fuction 的方式，先將 request body, path parameter, query parameter 轉為 data class 之後，隨即進行資料驗證，最後再傳入 route DSL function 作為參數進行操作。目前本專案使用 [Konform](https://github.com/konform-kt/konform)，以 type-safe DSL 的方式撰寫驗證邏輯，未來再考慮是否支援 JSR-303 annotation

### Multi-Projects Architecture

### Infrastructure

### Technique Stack
* Kotlin 1.5.21
* Ktor 1.6.1
* Gradle 7.1.1
* PostgreSQL 13.2
* Redis 6.2.1
* Kotlinx Serialization, Kotlinx Coroutine
* Exposed ORM, HikariCP, Flyway database migration tool
* Koin DI
------------
### Build & Deploy
#### Build Steps
1. 設定 git branch 對應至部署環境  
在 build.gradle.kts 檔案找到以下設定，你也可自行增加 stage, test…等環境對應。Gradle shadow plugin 會根據當下的 git branch 打包程式及 deploy/config/${env} 資料夾裡的檔案，最後壓縮為 zip 檔
```kotlin
    val devBranchName = "dev"
    val releaseBranchName = "main"
    val branchToEnvMap: Map<String, String> = mapOf(devBranchName to "dev", releaseBranchName to "prod")
```
2. 製作部署環境設定檔  
根據 git branch 對應的環境 env 變數值  
建立`deploy/config/${env}/application-${env}.conf`檔案。  
設定檔範例可參考 `deploy/config/dev/application-dev.conf`
3. 執行 gradle shadowEnvDistZip 打包為 zip

#### Deploy Steps
1. 根據 application.conf 設定執行所需要的環境變數
2. 解壓縮 zip 檔(內含部署腳本檔案)
3. 設定 configs.sh
4. 執行 start.sh 啟動服務
