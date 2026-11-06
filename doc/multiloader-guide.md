# Moonlight 双加载器模组开发文档

Moonlight 是一个 Minecraft 26.3 模组，同一份源码同时构建 **Fabric** 和 **NeoForge** 两个版本。工程结构参考 [stonecutter-template-multiloader](https://github.com/stonecutter-versioning/stonecutter-template-multiloader)，用 [Stonecutter](https://stonecutter.kikugie.dev/) 在一份 `src/` 里管理两个加载器的差异。

本文说明工程怎么组织、代码该写在哪、怎么构建和调试，以及怎样避免同一个功能写两份。

---

## 1. 版本一览

| 组件 | 版本 | 在哪里改 |
|---|---|---|
| Minecraft | 26.3 | `settings.gradle` |
| Java | 25 | `build.*.gradle`、`gradle/gradle-daemon-jvm.properties` |
| Gradle | 9.8.2（腾讯云镜像） | `gradle/wrapper/gradle-wrapper.properties` |
| Stonecutter | 0.9.8 | `settings.gradle` |
| Fabric Loader | 0.20.1 | `stonecutter.properties.toml` |
| Fabric API | 0.165.2+26.3 | `stonecutter.properties.toml` |
| Fabric Loom | 1.19.3 | `build.fabric.gradle` |
| NeoForge | 26.3.0.101 | `stonecutter.properties.toml` |
| ModDevGradle | 2.0.148 | `build.neoforge.gradle` |

26.3 是非混淆版本：Fabric 和 NeoForge 看到的都是 Mojang 原名的 Minecraft 类，不需要映射表，也不需要 refmap。

---

## 2. 目录结构

```
Moonlight-v2-NextGen/
├─ settings.gradle               定义 Stonecutter 节点（26.3-fabric / 26.3-neoforge）
├─ stonecutter.gradle            根项目（控制器）：当前激活节点、条件常量、swap
├─ stonecutter.properties.toml   模组信息和依赖版本，按加载器分表
├─ build.fabric.gradle           Fabric 节点的构建脚本（Loom）
├─ build.neoforge.gradle         NeoForge 节点的构建脚本（ModDevGradle）
├─ gradle.properties             Gradle 自身选项
├─ gradle/                       wrapper 和守护进程 JVM 要求
├─ src/main/java/wtf/moonlight/
│  ├─ Moonlight.java             通用入口：MOD_ID、LOGGER、init()、id()
│  ├─ MoonlightClient.java       客户端入口：init()
│  ├─ platform/Platform.java     加载器差异的唯一出口
│  ├─ fabric/                    Fabric 入口类（只在 fabric 节点编译）
│  ├─ neoforge/                  NeoForge 入口类（只在 neoforge 节点编译）
│  └─ mixin/                     Mixin，两个加载器共用
├─ src/main/resources/
│  ├─ fabric.mod.json            只打进 Fabric 包
│  ├─ META-INF/neoforge.mods.toml 只打进 NeoForge 包
│  ├─ moonlight.mixins.json      两边共用
│  └─ moonlight.ct               Fabric 的 class tweaker（访问扩展），只打进 Fabric 包
├─ versions/26.3-fabric/         Stonecutter 自动创建的子项目目录，产物在 build/ 下
├─ versions/26.3-neoforge/
├─ run/                          两个加载器共用的游戏运行目录（不进 git）
└─ doc/                          本文档
```

---

## 3. 环境准备

1. **JDK 25**。Loom 1.19 要求 Gradle 本身跑在 Java 25 上，不只是编译目标是 25。
2. **IDEA 设置**（Settings → Build, Execution, Deployment → Build Tools → Gradle）：
   - **Gradle JVM** 选 JDK 25，或者选 *Daemon JVM criteria*（项目已写 `toolchainVersion=25`）。
   - **Project Structure → SDK** 也设成 25。
3. **网络**：依赖来自 `maven.fabricmc.net`、`maven.neoforged.net`、`maven.kikugie.dev`，国内直连可能很慢。代理写在自己的 `GRADLE_USER_HOME/gradle.properties` 里（地址和端口按自己的代理改），**不要写进项目**：
   ```properties
   systemProp.https.proxyHost=127.0.0.1
   systemProp.https.proxyPort=7897
   systemProp.http.proxyHost=127.0.0.1
   systemProp.http.proxyPort=7897
   ```
4. **第一次同步比较重**：要下载 Gradle 和两套依赖，NeoForge 还会把 Minecraft 反编译再重新编译一遍（`createMinecraftArtifacts`）。期间电脑会比较卡，属于正常现象。

---

## 4. Stonecutter 是怎么工作的

### 4.1 节点和激活节点

`settings.gradle` 里定义了两个节点：

```groovy
version("26.3-fabric", "26.3").buildscript("build.fabric.gradle")
version("26.3-neoforge", "26.3").buildscript("build.neoforge.gradle")
```

- 每个节点是一个 Gradle 子项目（`:26.3-fabric`、`:26.3-neoforge`），目录在 `versions/` 下，用各自的构建脚本。
- 两个节点共用根目录的 `src/`。
- `src/` 里的代码永远是**激活节点**看到的样子。激活节点写在 `stonecutter.gradle`：
  ```groovy
  stonecutter.active "26.3-fabric"
  ```
  这一行由 Stonecutter 自动改写，**不要改它的格式**。
- 非激活节点构建时，Stonecutter 会把源码按条件注释处理成该节点的版本再编译，不用手动切换。所以 Fabric 激活时也能直接构建 NeoForge 版。

### 4.2 切换激活节点

切换只是为了**编辑**：要写 NeoForge 专用代码时，切过去之后 IDE 才能高亮和检查那部分代码。

| 操作 | Gradle 任务（根项目 → Tasks → stonecutter） |
|---|---|
| 切到 NeoForge | `Set active project to 26.3-neoforge` |
| 切回默认（Fabric） | `Reset active project` |
| 重新处理当前节点 | `Refresh active project` |

命令行写法：`./gradlew "Set active project to 26.3-neoforge"`。

> 提交前切回 Fabric（`Reset active project`）。仓库约定以 `26.3-fabric` 为准（`settings.gradle` 里的 `vcsVersion`），否则 diff 里会混进一大片注释切换。

### 4.3 条件注释语法

可用的条件常量是 `fabric` 和 `neoforge`（在 `stonecutter.gradle` 里用 `constants.match` 定义）。

**代码块**：激活分支是正常代码，非激活分支包在 `/* */` 里。

```java
//? if fabric {
import net.fabricmc.api.ModInitializer;
//?} else {
/*import net.neoforged.fml.common.Mod;
*///?}
```

**单行**：只作用于紧跟的下一条语句。

```java
//? if neoforge
//LOGGER.info("only on NeoForge");
```

**行内表达式**：见 `Platform.java`。

```java
Platform INSTANCE =
    /*? if fabric {*/new FabricPlatform();
    /*?} elif neoforge *///new NeoForgePlatform();
```

**嵌套**：在已注释掉的块里再写条件时，用 `/^ ^/` 代替 `/* */`，避免注释提前结束。

**版本条件**：以后加了其他 Minecraft 版本，可以写 `//? if >=26.4 {`。

**swap**：把一段代码替换成配置里的值。例如 `Moonlight.VERSION`：

```java
public static final String VERSION = /*$ mod_version*/ "0.1.0";
```

构建时会被替换成 `stonecutter.properties.toml` 里的 `mod.version`。swap 在 `stonecutter.gradle` 里定义。

> 非激活分支是注释，IDE 不检查。改 NeoForge 那部分代码时请先切换激活节点，改完能通过编译再切回来。

---

## 5. 代码分层（最重要的规则）

| 层 | 位置 | 能用什么 |
|---|---|---|
| 通用代码 | `wtf.moonlight` 下除 `fabric` / `neoforge` / `platform` 以外的所有包 | 只用原版 Minecraft 类、Mixin、自己的代码 |
| 平台层 | `wtf.moonlight.platform.Platform` | 加载器 API，用条件注释分开 |
| 入口层 | `wtf.moonlight.fabric`、`wtf.moonlight.neoforge` | 只负责把调用转给通用入口 |
| Mixin | `wtf.moonlight.mixin` | 原版类，两边共用 |

启动流程：

| | Fabric | NeoForge |
|---|---|---|
| 通用初始化 | `MoonlightFabric.onInitialize()` → `Moonlight.init()` | `@Mod("moonlight")` 构造器 → `Moonlight.init()` |
| 客户端初始化 | `MoonlightFabricClient.onInitializeClient()` → `MoonlightClient.init()` | `@Mod(value = "moonlight", dist = Dist.CLIENT)` 构造器 → `MoonlightClient.init()` |

两个加载器调用入口的时机都比较早，这时窗口、资源、字体都还没准备好。`init()` 里只做对象创建和注册；用到渲染资源的初始化，放到游戏启动完成之后，由自己的 Mixin 事件触发。

### 5.1 怎样避免同一个功能写两份

原则：**功能代码不直接调用任何加载器 API**。这样真正要写两份的只有入口类和 `Platform` 的两份实现。

**① 用自己的事件系统 + Mixin 触发，不用 Fabric API / NeoForge 的事件**

在原版方法里用 Mixin 发出自己的事件，所有功能只订阅这些事件。Mixin 两边通用，所以事件也是两边通用的：

```java
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void moonlight$tick(CallbackInfo ci) {
        EventBus.post(new TickEvent());
    }
}
```

> `EventBus` / `TickEvent` 是示意，目前项目里还没有，需要自己实现（或者引入一个轻量事件库）。

tick、渲染、收发包、按键、聊天这些都可以这样做。功能代码完全不依赖加载器时，`fabric.mod.json` 里对 `fabric-api` 的依赖也可以去掉，玩家装 Fabric 时就不用另外装 Fabric API。

**② 必须用加载器 API 的地方，统一加到 `Platform`**

例如配置目录：通用代码只写 `Platform.INSTANCE.configDir()`，两份实现在 `Platform.java` 里各占几行。新增能力时：

1. 在接口里加方法；
2. 在 `FabricPlatform` 里实现；
3. 切到 neoforge 节点，在 `NeoForgePlatform` 里实现，编译通过后切回。

**③ 很小的差异直接用条件注释**

一两行的差异，在原文件里用 `//? if fabric` / `//? if neoforge` 写就行，不用为它单独建类。

**④ 改访问权限用 `@Accessor` / `@Invoker`，不用 AW / AT**

Fabric 用 access widener（`moonlight.ct`），NeoForge 用 access transformer，格式不同，要维护两份。Mixin 的访问器两边通用：

```java
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("rightClickDelay")
    void moonlight$setRightClickDelay(int delay);

    @Invoker("startUseItem")
    void moonlight$startUseItem();
}
```

使用时：`((MinecraftAccessor) Minecraft.getInstance()).moonlight$setRightClickDelay(0);`。访问器接口也要登记到 `moonlight.mixins.json`。

### 5.2 必须用加载器 API 时放在哪

- 只在 `platform/` 或者 `//? if` 条件块里写。
- 不要在通用代码里 `import net.fabricmc.*` 或 `net.neoforged.*`：激活 Fabric 时能编译，NeoForge 节点构建时就会报错。

---

## 6. Mixin 规范

- 配置文件是 `src/main/resources/moonlight.mixins.json`，Fabric 通过 `fabric.mod.json`、NeoForge 通过 `neoforge.mods.toml` 的 `[[mixins]]` 加载同一份。
- `mixins` 数组：客户端和服务端都会加载。`client` 数组：只在客户端加载。Moonlight 是客户端，**涉及 `net.minecraft.client.*` 的 Mixin 一律放进 `client`**，否则在专用服务端上会因为找不到类而崩溃。
- 命名：Mixin 类叫 `XxxMixin`，访问器叫 `XxxAccessor`；注入方法和访问器方法加 `moonlight$` 前缀，避免和其他模组冲突。
- `injectors.defaultRequire = 1`：注入点没找到就启动失败，问题能第一时间暴露。
- **NeoForge 改过部分原版方法**：同一个注入点在 Fabric 上正常、在 NeoForge 上可能找不到，渲染和事件相关的类最常见。遇到时切到 neoforge 节点看 NeoForge 打过补丁的源码，再用条件注释给 NeoForge 换一个注入点：
  ```java
  //? if fabric {
  @Inject(method = "xxx", at = @At("HEAD"))
  //?} else {
  /*@Inject(method = "xxx", at = @At(value = "INVOKE", target = "..."))
  *///?}
  ```
- 调试：Fabric 的运行配置带了 `-Dmixin.debug.export=true`，Mixin 处理后的类会导出到 `run/.mixin.out/`。
- 新增的 Mixin 必须在**两个加载器上都实际进游戏验证**，不能只测 Fabric。

---

## 7. 资源文件和元数据

| 文件 | Fabric 包 | NeoForge 包 |
|---|---|---|
| `fabric.mod.json` | ✅ | ❌（在 `build.neoforge.gradle` 里排除） |
| `META-INF/neoforge.mods.toml` | ❌（在 `build.fabric.gradle` 里排除） | ✅ |
| `moonlight.ct` | ✅ | ❌ |
| `moonlight.mixins.json` | ✅ | ✅ |
| `assets/moonlight/**` | ✅ | ✅ |

- 元数据里的 `${id}`、`${name}`、`${version}`、`${minecraft}`、`${loader}` / `${neoforge}`，以及 mixin 配置里的 `${java}`，都在 `processResources` 里从 `stonecutter.properties.toml` 填入。
- 这几个文件走 Groovy 模板展开，正文里如果要写字面量 `$`，需要写成 `\$`。
- 新增只给某个加载器用的资源时，记得在另一个加载器的构建脚本里加 `exclude`。

---

## 8. 构建和运行

### 8.1 IDEA 的 Gradle 面板

```
Moonlight                  ← 根项目（控制器）
├─ Tasks → stonecutter     ← 切换激活节点
├─ 26.3-fabric
│   └─ Tasks → build       → build、buildAndCollect
│            → fabric      → runClient、runServer
└─ 26.3-neoforge
    └─ Tasks → build       → build、buildAndCollect
```

- 构建 Fabric 版：双击 `26.3-fabric → Tasks → build → build`
- 构建 NeoForge 版：双击 `26.3-neoforge → Tasks → build → build`
- 根项目的 Tasks 里没有 `build`，要去两个子项目下面找。

### 8.2 命令行

| 目的 | 命令 |
|---|---|
| 只构建 Fabric | `./gradlew :26.3-fabric:build` |
| 只构建 NeoForge | `./gradlew :26.3-neoforge:build` |
| 两个都构建 | `./gradlew build` |
| 构建并把 jar 汇总到 `build/libs/<版本>/` | `./gradlew buildAndCollect` |
| 启动 Fabric 客户端 | `./gradlew :26.3-fabric:runClient` |
| 启动 NeoForge 客户端 | `./gradlew :26.3-neoforge:runClient` |

产物位置：

- `versions/26.3-fabric/build/libs/moonlight-fabric-<版本>+26.3.jar`
- `versions/26.3-neoforge/build/libs/moonlight-neoforge-<版本>+26.3.jar`
- 运行 `buildAndCollect` 后，两边的 jar 和源码 jar 都会复制到 `build/libs/<版本>/`

两个加载器共用 `run/` 作为游戏目录（存档、配置、日志都在里面）。

### 8.3 gradle.properties 说明

- `org.gradle.parallel=false`：两个节点不并行构建。NeoForge 的 Minecraft 重编译很吃内存，并行时容易把电脑跑卡。
- `org.gradle.configuration-cache=true`：开启配置缓存。修改构建脚本时注意第 10 节的写法要求。

---

## 9. 改版本号、加依赖

### 9.1 模组信息

`stonecutter.properties.toml` 顶部的 `mod.id`、`mod.name`、`mod.version`。

如果要**改模组 id**，下面这些地方要一起改：

- `stonecutter.properties.toml` 的 `mod.id`
- `Moonlight.MOD_ID`（`@Mod` 注解也引用它）
- `moonlight.mixins.json` 的文件名，以及 `fabric.mod.json`、`neoforge.mods.toml` 里对它的引用
- `moonlight.ct` 的文件名，以及 `fabric.mod.json` 和 `build.fabric.gradle` 里对它的引用
- `assets/<id>/` 目录名

### 9.2 升级加载器和工具

| 要升级的 | 改哪里 |
|---|---|
| Fabric Loader / Fabric API / NeoForge | `stonecutter.properties.toml` |
| Loom | `build.fabric.gradle` 的 `plugins` |
| ModDevGradle | `build.neoforge.gradle` 的 `plugins` |
| Stonecutter | `settings.gradle` 的 `plugins` |
| Gradle | `gradle/wrapper/gradle-wrapper.properties` |

最新版本可以查 maven 元数据，例如 `https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml`。

### 9.3 加依赖

- **只给一个加载器用的模组 / 库**：只加到对应的 `build.fabric.gradle` 或 `build.neoforge.gradle` 的 `dependencies` 里。
- **通用代码要用的第三方库**：两个构建脚本都要加。打包进 jar 时，Fabric 用 `include`，NeoForge 用 `jarJar`。
- 来自 Modrinth / CurseForge 的模组，先在对应构建脚本里加仓库，例如：
  ```groovy
  repositories {
      maven { url = "https://api.modrinth.com/maven" }
  }
  ```

### 9.4 以后支持更多 Minecraft 版本

1. `settings.gradle` 里为新版本加一对 `version("26.4-fabric", "26.4")` / `version("26.4-neoforge", "26.4")`；
2. `stonecutter.properties.toml` 里加 `[fabric."26.4"]`、`[neoforge."26.4"]` 两张表；
3. 代码里的版本差异用 `//? if >=26.4 {` 区分。

---

## 10. Groovy 构建脚本的写法要求

构建脚本是 Groovy（`.gradle`）。有几个坑要注意：

1. **读配置用 `property("mod.id")`**。Stonecutter 会把 `stonecutter.properties.toml` 的值注入成项目属性。模板 Kotlin 版里的 `sc.properties.get(...)` 是 Kotlin 的 reified 内联函数，在 Groovy 里调用会直接抛异常。
2. **先在脚本顶部把值取成变量**，再在任务块里使用。在 `processResources { }`、`tasks.register(...) { }` 这些块里直接写 `property("...")`，会解析成任务对象自己的属性，读不到项目属性。
3. **给 `Property<String>` 赋值时用普通 String**（`.toString()` 或字符串拼接），不要直接塞 GString。
4. 开了配置缓存，任务执行阶段的闭包里不要访问 `project`。

---

## 11. 常见问题

| 现象 | 原因 / 处理 |
|---|---|
| 同步时 Loom 报需要 Java 25 | Gradle JVM 没选 25，见第 3 节 |
| NeoForge 的代码在 IDE 里全是注释、没有高亮 | 当前激活的是 fabric 节点，切到 neoforge 再改 |
| 切换节点后 git 里出现大量改动 | 正常；提交前执行 `Reset active project` 切回 fabric |
| 通用代码在 NeoForge 构建时报找不到 `net.fabricmc...` | 通用代码引用了加载器 API，挪到 `Platform` 或包进 `//? if fabric` |
| Mixin 只在 NeoForge 上注入失败 | NeoForge 改过那个方法，见第 6 节 |
| 依赖下载超时 | 配置代理，见第 3 节 |
| 第一次同步 / 构建特别慢 | NeoForge 在重编译 Minecraft，只有第一次这样 |

---

## 12. 参考

- Stonecutter 文档：<https://stonecutter.kikugie.dev/>
- 参考模板：<https://github.com/stonecutter-versioning/stonecutter-template-multiloader>
- Fabric 文档：<https://docs.fabricmc.net/>
- NeoForge 文档：<https://docs.neoforged.net/>
- Fabric 官方示例（26.3 分支）：<https://github.com/FabricMC/fabric-example-mod/tree/26.3>
- NeoForge 26.3 MDK：<https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle>
