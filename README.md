# ktor-example
Kotlin Ktor Web Framework Example

### Technique Stack
* Kotlin 1.5.21
* Ktor 1.6.1
* Gradle 7.1.1
* PostgreSQL 13.2
* Redis 6.2.1
* Kotlinx Serialization, Kotlinx Coroutine, Exposed ORM，Koin DI

### Ktor Enhancement
* i18n support
* OpenAPI Generator
* Authentication and Role-Based Authoriation, like Spring Security

### Infrastructure
* Logging
    * use coroutine channel to write log to different destinations, support file, database, AWS Kinesis stream
    * includes request log, error log, login log, notification log 
* Redis
    * session storage and support session key expired notification by [Redis PubSub Keyspace Notification](https://redis.io/topics/notifications)
    * data cache
    * use [ktorio redis client](https://github.com/ktorio/ktor-clients) based on coroutines. ktorio is a experimental project developed by JetBrains ktor team. I will use [Lettuce coroutine extension](https://lettuce.io/core/release/reference/#kotlin) in the future 
* Notification Service
    * use coroutine channel to send notifications to multiple channels, includes email(AWS SES), push(Firebase), sms(not implemented yet)
    * integrate freemarker template engine
    * support user language preference

### Multi-Projects Architecture
I develop two subprojects base on infrastructure and common library but each subproject can be optional deployed with ktor module configuration and has its own
* routes and openapi document
* authentication methods
* user type and it's roles
* notification types

------------
## 繁體中文 (Traditional Chinese)
Ktor 是 JetBrains 開發的 Web 框架，其特性是
- 100% 純 Kotlin 開發
- 使用 Coroutine 非同步的方式處理請求
- Unopinionated 使得 Ktor 核心更加地輕量、啟動快速

其中 Unopinionated 的特性，Ktor 並沒有預先整合常用的第三方框架及函式庫，例如 Dependency Injection, ORM, Logging...等，而是讓開發者能自由選擇整合至 Ktor。但也因為如此，開發者不僅需要花時間進行整合的工作，對於新手或是習慣 Spring Boot 全家桶的開發者，更是存在不小的門檻。

另一方面，Ktor 的架構設計是讓開發者透過實作 Feature (官方未來將改名為 Plugin)，把 intercepting function 加入至 request pipeline 之中，藉此增加功能或改變行為。這種方式與 Spring Boot 規定開發者實作特定的介面或繼承特定類別，再透過 dependency injection 的方式注入有所不同，再加上如果開發者不熟 Lambda 與 DSL 寫法，就不知該如何動手實作 Feature。

由於 Ktor 是一個很年輕的框架，雖然目前官方不斷地改善使用文件，但開發者在學習過程中，仍然需要有實際的範例，將各個功能整合起來才知道如何建構一個完整的後端服務。但是目前 Ktor 的使用人數不多，網路上的範例也很少，而且大多為展示簡單的單一功能，缺乏 real world 等級的完整後端服務範例，這導致後端經驗不足的開發者會不知道該如何使用 Ktor 建構服務。而且 Ktor 官方也沒有規定或指南，建議開發者應該如何規劃專案的檔案結構及程式寫法，所以開發者必須根據經驗事先思考規劃，才能建構容易維護的專案

綜合以上原因，本專案使用以下技術且包含常見網站後端服務功能的範例，供大家參考學習。截至 2021-07-21，codebase 累計已有 241 個 kt 檔，不含空白行已超過 13000 行

### Technique Stack
- Kotlin 1.5.21
- Ktor 1.6.1
- Gradle 7.1.1
- PostgreSQL 13.2
- Redis 6.2.1
- Kotlinx Serialization, Kotlinx Coroutine, Exposed ORM，Koin DI
------------
### 建置部署
#### 建置步驟
1. 設定 git branch 對應至部署環境  
在 build.gradle.kts 檔案找到以下設定，你也可自行增加 stage, test…等環境對應。Gradle shadow plugin 會根據當下的 git branch 打包程式及 deploy/config/${env} 資料夾裡的檔案，最後壓縮為 zip 檔
```kotlin
    val devBranchName = "dev"
    val releaseBranchName = "main"
    val branchToEnvMap: Map<String, String> = mapOf(devBranchName to "dev", releaseBranchName to "prod")
```
2. 製作部署環境設定檔  
根據 git branch 對應的環境 env 變數值，建立 deploy/config/${env}/application-${env}.conf 檔案。設定檔範例可參考 deploy/config/dev/application-dev.conf
3. 執行 gradle shadowEnvDistZip 打包為 zip

#### 部署步驟
1. 根據 application.conf 設定執行所需要的環境變數
2. 解壓縮 zip 檔(內含部署腳本檔案)
3. 設定 configs.sh
4. 執行 start.sh 啟動服務
