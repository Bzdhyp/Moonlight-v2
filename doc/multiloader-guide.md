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
| idea-ext | 1.2 | `stonecutter.gradle` |
| DevAuth（仅开发环境） | 1.2.2 | `stonecutter.properties.toml` |
| imgui-java | 1.92.7.1 | `stonecutter.properties.toml` |

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
│  ├─ MoonlightClient.java       客户端入口：init()，创建绑定和命令系统
│  ├─ platform/Platform.java     加载器差异的唯一出口
│  ├─ fabric/                    Fabric 入口类（只在 fabric 节点编译）
│  ├─ neoforge/                  NeoForge 入口类（只在 neoforge 节点编译）
│  ├─ event/                     事件系统（见 5.3）
│  ├─ command/                   聊天命令（见 5.4）
│  ├─ bind/                      按键绑定（见 5.5）
│  ├─ utils/                     工具类（见 5.6）
│  ├─ module/                    模块系统，模块在 impl.<分类> 下（见 5.7）
│  ├─ config/                    配置：手动 / 自动保存（见 5.8）
│  ├─ value/                     设置项（见 5.9）
│  ├─ rotation/                  旋转：发给服务器的视角、平滑、移动修复（见 5.11）
│  ├─ ui/                        界面：imgui（ImGui 接入）、clickgui（ClickGUI 绘制，见 5.10）
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

在原版方法里用 Mixin 发出自己的事件，所有功能只订阅这些事件。Mixin 两边通用，所以事件也是两边通用的。项目里已经有完整的事件系统，用法和事件列表见 5.3 节，例如 `MinecraftMixin`：

```java
@Inject(method = "tick", at = @At("HEAD"))
private void moonlight$preTick(CallbackInfo ci) {
    Moonlight.EVENT_BUS.post(TickEvent.PRE);
}
```

tick、渲染、收发包、按键、聊天这些都可以这样做。功能代码完全不依赖加载器时，`fabric.mod.json` 里对 `fabric-api` 的依赖也可以去掉，玩家装 Fabric 时就不用另外装 Fabric API。

**② 必须用加载器 API 的地方，统一加到 `Platform`**

例如配置目录：通用代码只写 `Platform.INSTANCE.configDir()`，两份实现在 `Platform.java` 里各占几行。新增能力时：

1. 在接口里加方法；
2. 在 `FabricPlatform` 里实现；
3. 切到 neoforge 节点，在 `NeoForgePlatform` 里实现，编译通过后切回。

**③ 很小的差异直接用条件注释**

一两行的差异，在原文件里用 `//? if fabric` / `//? if neoforge` 写就行，不用为它单独建类。

**④ 改访问权限用 `@Accessor` / `@Invoker`，不用 AW / AT**

Fabric 用 access widener（`moonlight.ct`），NeoForge 用 access transformer，格式不同，要维护两份。Mixin 的访问器两边通用，例如项目里的 `MinecraftAccessor`（旋转系统用它立刻刷新准星目标，见 5.11）：

```java
@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Invoker("pick")
    void moonlight$pick(float partialTicks);
}
```

使用时：`((MinecraftAccessor) Minecraft.getInstance()).moonlight$pick(1.0F);`。字段用 `@Accessor`，写法相同。访问器接口也要登记到 `moonlight.mixins.json`。

### 5.2 必须用加载器 API 时放在哪

- 只在 `platform/` 或者 `//? if` 条件块里写。
- 不要在通用代码里 `import net.fabricmc.*` 或 `net.neoforged.*`：激活 Fabric 时能编译，NeoForge 节点构建时就会报错。

### 5.3 事件系统

支持优先级、可取消事件、Pre/Post 阶段和 `hasListeners` 快速判断，调用器参考 Meteor Orbit 按方法缓存 `LambdaMetafactory` 工厂。代码在 `wtf.moonlight.event`，事件类在 `wtf.moonlight.event.impl.*`，触发事件的 Mixin 在 `wtf.moonlight.mixin`。全部只用原版类，两个加载器共用。

按代码规范"不留零用法"，事件类里**只有 Mixin 和现有系统实际读写的成员**。以后写模块需要读某个值、改某个值（例如改 `MotionEvent` 的坐标）时，再给事件加对应的 getter / setter，并让 Mixin 用上它。

**核心类**

| 类 | 作用 |
|---|---|
| `Moonlight.EVENT_BUS` | 全局事件总线：`register(obj)`、`unregister(obj)`、`post(event)`、`hasListeners(type)` |
| `Event` / `CancellableEvent` | 事件基类 / 可取消事件（`cancel()`、`isCancelled()`） |
| `EventPhase` | `PRE` / `POST` |
| `Priority` | `HIGHEST=0`、`HIGH=5`、`MEDIUM=10`（默认）、`LOW=15`、`LOWEST=20`，数值小的先执行 |
| `@Subscribe(priority = ...)` | 标在监听方法上 |

**订阅**

```java
public final class ChatFilter {
    @Subscribe(priority = Priority.HIGH)
    private void onChat(ChatSendEvent event) {
        if (event.getMessage().isBlank()) event.cancel();
    }
}

Moonlight.EVENT_BUS.register(new ChatFilter());
```

- `register(obj)` 会扫描这个类和它所有父类里的 `@Subscribe` 方法，私有方法也可以。方法必须是非静态、返回 `void`、只有一个参数，参数类型必须是具体的事件类，否则直接抛 `IllegalArgumentException`。同一个对象重复注册会被忽略。
- `unregister(obj)` 移除这个对象的全部监听方法。派发过程中被注销的监听器，在当前这次派发里也会立即被跳过（例如按键关掉了一个模块，这个模块就收不到同一次按键事件）。模块开关就是靠 `register` / `unregister` 实现的，见 5.7。
- 目前没有 lambda 订阅，需要时再加。

**派发规则**

- 按事件的**确切类型**派发，不走继承：订阅 `Event` 或者父类收不到子类事件。
- 优先级数值小的先执行。同优先级下：不同对象按注册顺序；同一个类里按方法名排序（反射返回的方法顺序不固定，所以统一按名字排）。
- 事件被取消后**后面的监听器照样会收到**，需要时自己判断 `event.isCancelled()`。
- 某个监听器抛异常只会记一条错误日志（`moonlight/events`），不影响其他监听器和游戏。
- `post(event)` 返回传入的事件对象，可以链式写 `EVENT_BUS.post(e).isCancelled()`。
- 派发时不加锁（写时复制数组）。
- 调用器用 `LambdaMetafactory` 生成，每次调用约 8 ns（200 万次派发 × 5 个监听器约 80 ms）。

**在 Mixin 里触发事件**

- 频繁调用的位置先判断 `Moonlight.EVENT_BUS.hasListeners(XxxEvent.class)`，没人订阅时不创建事件对象。
- 只关心本地玩家的实体事件，先判断 `this == Minecraft.getInstance().player`：这些 Mixin 在单人游戏里也会作用到内置服务端的实体上。
- 不带数据、也不能取消的事件用静态单例（`INSTANCE`，`TickEvent` 是 `PRE` / `POST`），不每次新建。可取消的事件每次新建，因为取消状态属于这一次触发。

**事件列表**

| 事件 | 触发位置（26.3） | 可取消 | 现有成员 / 说明 |
|---|---|---|---|
| `client.GameLoadedEvent` | `Minecraft.onGameLoadFinished` 末尾 | — | 单例；资源加载完、进入标题画面时触发一次，适合初始化字体、贴图 |
| `client.TickEvent` | `Minecraft.tick` 开头 / 返回 | — | `PRE` / `POST`，`getPhase()` |
| `client.WorldChangeEvent` | `Minecraft.setLevel` / `clearClientLevel` 末尾 | — | 单例；进入或离开世界 |
| `client.ScreenOpenEvent` | 包住 `Gui.setScreen` | 是 | `getScreen()`；26.3 的界面切换在 `Gui` 里，`Minecraft` 已没有 `setScreen` |
| `input.KeyInputEvent` | `KeyboardHandler.keyPress` 开头 | 是 | `getKey()`（SDL scancode，对应 `InputConstants.KEY_*`）、`isPress()`、`isRepeat()` |
| `input.MouseInputEvent` | `MouseHandler.onButton` 开头 | 是 | `getButton()`（SDL 编号：左 1、中 2、右 3）、`isPress()` |
| `input.MouseScrollEvent` | `MouseHandler.onScroll` 开头 | 是 | 不带数据 |
| `network.PacketSendEvent` | `Connection.send` 里调用 `sendPacket` 处 | 是 | `getPacket()`；只处理客户端自己的连接；连接建立前排队的包不经过这里 |
| `network.PacketReceiveEvent` | `Connection.channelRead0` 里调用 `genericsFtw` 处 | 是 | `getPacket()`；**在网络线程触发**，不要在里面直接改游戏状态，需要时 `Minecraft.getInstance().execute(...)` |
| `network.ChatSendEvent` | 包住 `ClientPacketListener.sendChat` | 是 | `getMessage()`；只有普通聊天，`/` 开头的命令走 `sendCommand`，不经过这里 |
| `player.PlayerTickEvent` | `LocalPlayer.tick` 开头 | — | 单例 |
| `player.MotionEvent` | `LocalPlayer.sendPosition` 开头 | 是 | `getX/Y/Z()`、`getPosition()`、`getYaw/Pitch()` / `setYaw/Pitch()`、`isOnGround()`；取消 = 这一 tick 不发移动包。见下方说明 |
| `player.RespawnEvent` | `LocalPlayer.sendPosition` 开头，玩家 `tickCount <= 1` 时 | — | 单例；新的本地玩家对象刚出现（进世界、重生、换维度） |
| `player.MoveInputEvent` | `KeyboardInput.tick` 里新建 `Input` 处 | — | `getInput()` / `setInput()`：设置的 `Input` 就是这一 tick 的移动输入（`Sprint` 用它把冲刺置为按下，移动修复用它改方向键） |
| `player.JumpEvent` | `LivingEntity.jumpFromGround` 里的 `getYRot()` | — | `getYaw()` / `setYaw()`，结果作为疾跑跳的加速方向；只对本地玩家 |
| `player.StrafeEvent` | `Entity.moveRelative` 里 `getInputVector` 的偏航参数 | — | `getYaw()` / `setYaw()`，结果作为移动方向；只对本地玩家 |
| `player.RaycastEvent` | `Entity.getViewVector` 里的 `calculateViewVector` | — | `getYaw/Pitch()` / `setYaw/Pitch()`，结果作为视线方向，准星选中的方块和实体都由它算出；只对本地玩家 |
| `player.ItemRaycastEvent` | `Item.getPlayerPOVHitResult` 里的 `calculateViewVector` | — | 同上，用于物品自己的视线检测（水桶、船、刷子等）；只对本地玩家 |
| `player.UseItemEvent` | `MultiPlayerGameMode.useItem` 里新建 `ServerboundUseItemPacket` 处（lambda `lambda$useItem$0`） | — | `getYaw/Pitch()` / `setYaw/Pitch()`，结果写进使用物品包发给服务器 |
| `player.FallFlyingEvent` | `LivingEntity.updateFallFlyingMovement` 里的 `getXRot()` | — | `getPitch()` / `setPitch()`，结果作为鞘翅滑翔的俯仰角；只对本地玩家 |
| `player.AttackEvent` | `MultiPlayerGameMode.attack` 开头 | 是 | 不带数据；取消 = 不攻击 |
| `player.AttackYawEvent` | `Player.causeExtraKnockback` 里的 4 处 `getYRot()` | — | `getYaw()` / `setYaw()`，结果作为击退方向；只对本地玩家 |
| `player.SlowdownEvent` | `LocalPlayer.modifyInput` 里的 `isUsingItem()` | 是 | 取消 = 使用物品时不减速；只在正在使用物品时触发 |
| `render.Render2DEvent` | `Hud.extractRenderState` 末尾 | — | 单例 |
| `render.Render3DEvent` | `GameRenderer.render3dHud` 里 `popPush("hand")` 处 | — | 单例；世界画完、手部之前 |
| `render.GammaEvent` | `LightmapRenderStateExtractor.extract` 里读取 `options.gamma()` 的值处 | — | `getGamma()` / `setGamma()`：设置的值代替亮度选项参与光照贴图计算（`FullBright` 用它） |
| `render.RotationAnimationEvent` | `LivingEntity.tickHeadTurn` 里的 `getYRot()`；`LivingEntityRenderer.extractRenderState` 里头部偏航的 `Mth.rotLerp` 和俯仰的 `getXRot(float)` | — | `getYaw/LastYaw/Pitch/LastPitch()` 和对应 setter：本地玩家模型的身体转向、头部偏航、俯仰（第三人称和别人看到的效果一致）。每处只读用到的那一对值 |

**`MotionEvent` 的实现要点**：26.3 的 `sendPosition` 构造移动包时用的是 `position()` 返回的 `Vec3`，`getX/getY/getZ` 只用于计算位移差和记录上次位置。所以 Mixin 同时包住了 `position()`、`getX/Y/Z`、`getYRot/getXRot` 和 `onGround()`，发出去的值全部来自事件。旋转系统（5.11）就是用 `setYaw/setPitch` 改发出去的视角，`yRotLast/xRotLast` 记下的也是改过的值。以后给 `MotionEvent` 加坐标的 setter 时，改动同样会真正发到服务器，只改 `getX/Y/Z` 是不够的。骑乘时 26.3 在 `sendChanges` 里单独发 `Rot` 包、不进 `sendPosition`，所以骑乘期间不会触发 `MotionEvent`。

**新增一个事件**

1. 在 `wtf.moonlight.event.impl.<分组>` 下建事件类，继承 `Event` 或 `CancellableEvent`，声明为 `final`。
2. 写 Mixin 触发它，登记到 `moonlight.mixins.json`（客户端相关的放 `client` 数组）。
3. **两份 jar 都要核对注入点**：原版 `minecraft-merged.jar`（在 Gradle 缓存的 `fabric-loom/26.3/` 下）和 NeoForge 的 `versions/26.3-neoforge/build/moddev/artifacts/minecraft-patched-26.3.0.101.jar`。用 `javap -p` 看方法签名，用 `javap -c` 确认 `@At` 指向的调用确实在方法体里。注入点写错的话编译发现不了，要到游戏启动时才会崩溃（`defaultRequire = 1`）。NeoForge 的 `minecraft-patched-26.3.0.101-sources.jar` 里有反编译源码，看方法逻辑比读字节码方便。

### 5.4 聊天命令

代码在 `wtf.moonlight.command`。在聊天框输入以前缀（默认 `.`）开头的消息时，消息不会发到服务器，而是交给客户端命令执行。命令用 Brigadier 定义，**执行和 Tab 补全共用同一棵命令树**，一个命令只写一次。

| 内置命令 | 用法 |
|---|---|
| `.help` / `.h` | 列出所有命令；`.help <命令>` 显示该命令的全部用法 |
| `.prefix` | 显示当前前缀；`.prefix <新前缀>` 修改前缀（1–3 个字符，不能有空格，不能以 `/` 开头） |
| `.say <消息>` | 原样发送一条聊天，即使它以前缀开头 |
| `.bind` / `.b` | 按键绑定，见 5.5 |
| `.toggle` / `.t` | 按分类列出模块和开关状态；`.toggle <模块>` 开关模块，见 5.7 |
| `.config` / `.cfg` | 列出配置；`save [名字]` 保存，`load <名字>` 加载，见 5.8 |

**工作方式**

- `CommandManager` 以最高优先级订阅 `ChatSendEvent`：消息以前缀开头时取消事件（不发给服务器），把去掉前缀的部分交给 `dispatcher.execute(...)`。消息已经进了聊天历史，上箭头可以翻出来。
- 语法错误按原版样式显示：红色的错误原因，加上一行灰色的出错位置和 `<--[HERE]`，点击这行会把命令填回聊天框。命令名不存在时提示用 `.help`。命令里抛出的其他异常会记日志并在聊天里显示一行错误。
- `CommandSuggestionsMixin` 在 `CommandSuggestions.updateCommandInfo()` 第一次调用 `StringReader.canRead()` 之前注入：输入以前缀开头时改用客户端命令树解析和补全，然后跳过原版逻辑。所以补全列表、参数高亮和用法提示都是原版那一套。只在 `ChatScreen` 里生效，命令方块等界面不受影响。客户端命令不受服务器"禁止聊天 / 禁止命令"的限制。
- 前缀保存在 `config/moonlight/commands.properties`。
- 命令树的源类型是 `ClientSuggestionProvider`（和原版补全用的一样），所以原版的参数类型和补全提供者都可以直接用。

**新增一个命令**

```java
public final class EchoCommand extends Command {
    public EchoCommand() {
        super("echo", "Prints a message back to you", "e");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> root) {
        root.then(argument("text", StringArgumentType.greedyString())
                .executes(context -> {
                    ChatUtils.info(StringArgumentType.getString(context, "text"));
                    return SUCCESS;
                }));
    }
}

MoonlightClient.getCommands().register(new EchoCommand());
```

- `build(root)` 里的 `root` 已经是命令名本身，往下加 `literal(...)` / `argument(...)` 即可。别名会用同一份 `build` 再注册一次，所以别名的补全和执行和正名完全一样。
- 命令描述直接写在构造函数里（英文），`.help` 会显示。
- 需要补全时在参数上加 `.suggests((context, builder) -> SharedSuggestionProvider.suggest(列表, builder))`。

### 5.5 按键绑定

代码在 `wtf.moonlight.bind`。

| 类 | 作用 |
|---|---|
| `Bindable` | 可绑定的东西：`getKey()`、`onBind(pressed)`、`triggersOnRelease()` |
| `Bind` | 现成的实现：有名字、按键、模式（切换 / 按住）、开关状态 |
| `BindManager` | 收到按键 / 鼠标事件后分发给绑定了这个键的对象；按名字查找 `Bind` |
| `BindCapture` | "按下一个键来绑定"：开始后下一次按键就成为绑定；`getTarget()` 是正在等待的 `Bind`，`cancel()` 取消 |

**键码约定**（`KeyUtils`）：`0` = 未绑定；正数 = SDL scancode（就是 `InputConstants.KEY_*`）；负数 = 鼠标键，`-1` 左键、`-2` 中键、`-3` 右键、`-4`/`-5` 侧键（26.3 的 SDL 鼠标编号取负）。

**`Bind` 的行为**

- 切换模式：按下切换开关，松开不处理。
- 按住模式（`hold`）：按下开启，松开关闭。每 tick 还会用 `KeyUtils.isDown` 查一次物理按键：如果开着但键已经不在按着（比如切出窗口时松开、松开事件被吞了），自动关掉。
- 按下时要满足触发条件，默认是没有打开任何界面；`setGate(...)` 可以换掉（ClickGUI 用它在自己的界面里也能关闭）。**松开不受这个限制**，保证按住模式能正常关闭。
- 构造时传入状态回调 `new Bind(name, state -> ...)`，开关状态变化时调用。外部改了状态（比如用命令开关了模块）用 `sync(state)` 同步，不触发回调。
- 按键和模式随模块一起保存在配置里（见 5.8），`Bind.save()` / `load()` 负责读写 `{"key", "hold"}`。

每个模块自带一个同名的 `Bind`，回调就是 `setEnabled`，由 `ModuleManager` 注册到 `BindManager`（见 5.7）。`BindManager` 和 `ModuleManager` 都在 `MoonlightClient.init()` 里创建。

**输入顺序**：`BindCapture`（最高优先级）→ `BindManager`（高优先级）。捕获中按下的键会取消事件，所以不会同时触发已有的绑定，也不会传给原版。松开事件不吞，避免原版按键卡住。

**`.bind` 命令**

| 写法 | 作用 |
|---|---|
| `.bind` | 列出所有绑定 |
| `.bind <名字>` | 开始捕获：接下来按的键就是绑定，Esc 清除绑定，鼠标左键取消 |
| `.bind <名字> <按键>` | 直接设置按键，`none` 解除绑定 |
| `.bind <名字> <按键> <toggle\|hold>` | 同时设置模式 |

名字就是模块名，不区分大小写、忽略空格（`Kill Aura` 写成 `killaura` 即可），例如 `.bind Sprint R`。按键名见下面的 `KeyUtils`。

### 5.6 utils

工具类放在 `wtf.moonlight.utils`。

| 类 | 作用 |
|---|---|
| `ChatUtils` | 在聊天框显示客户端消息：`info(...)`、`error(...)`（带 `[Moonlight]` 标签），`send(...)`（原样，不带标签）。内部用 `Minecraft.execute` 切回主线程，任何线程都能调用 |
| `KeyUtils` | 键码和名字互转、按键名补全列表、`isDown(code)` 查询物理按键状态 |
| `RotationUtils` | 算朝向某点 / 实体 / 方块面的 `Rotation`（`calculate` 各重载，实体版可以自适应找能射到的点），GCD 修正（`applySensitivityPatch`），限速转动（`move`）和平滑（`smooth`），`relateToPlayerRotation`、`resetRotation`，眼睛到实体碰撞箱的最近距离（`getDistance`）。见 5.11 |
| `RaycastUtils` | 按给定 `Rotation` 做视线检测：`rayCast`（方块 + 实体，可以放大实体碰撞箱），`inView`（实体是否在视野里且看得见），`overBlock`（是否正对某个方块 / 方块面，距离 4.5） |
| `MoveUtils` | `fixMovement`：移动修复，见 5.11；`isMoving()`：这一 tick 有没有移动输入（`moveVector` 非零）；`impulse(正向键, 反向键)`：把一对按键换成 1 / 0 / -1 |

**聊天输出的写法和配色**

`info` / `error` 用模板写：`{}` 处依次填入参数，参数显示为白色，其余文字 `info` 是灰色、`error` 是红色。参数如果本身就是带颜色的 `Component`，保持它自己的颜色。

```java
ChatUtils.info("{} bound to {}.", bind.getName(), "R");
ChatUtils.info("{} {}", module.getName(), Component.literal("enabled").withStyle(ChatFormatting.GREEN));
ChatUtils.error("Unknown module: {}", name);
```

统一配色：

| 部分 | 颜色 |
|---|---|
| 标签 `[Moonlight]` | 灰色方括号 + 青色名字 |
| 普通说明文字 | 灰色 |
| 重点值（模块名、按键、前缀、数量、命令名） | 白色 |
| 开 / 关状态 | `enabled` 绿色，`disabled` 红色；列表里开着的模块绿色、关着的深灰色 |
| 错误 | 红色文字，出错的值白色 |
| 列表里的次要信息（别名、分隔符、未绑定的 `None`） | 深灰色 |

多行输出（命令列表、绑定列表、模块列表）：第一行用 `info` 带标签写标题，比如 `Commands (6):`；下面的条目用 `send`，缩进两格，不带标签。

**`KeyUtils` 的按键名**

- 键盘：由原版的 `key.keyboard.*` 名字转成驼峰，例如 `A`、`0`、`F5`、`LeftShift`、`RightControl`、`Keypad0`、`KeypadEnter`、`PageUp`、`GraveAccent`。没有登记名字的 scancode 显示为 `Key<数字>`。
- 鼠标：`LMB`、`MMB`、`RMB`、`MB4`、`MB5`……；未绑定显示 `None`。
- `parse(...)` 不分大小写，忽略空格、下划线、点等分隔符（`left_shift`、`left.shift`、`LeftShift` 都行），另外认这些简写：`lshift`、`rshift`、`lctrl`、`rctrl`、`lalt`、`ralt`、`esc`、`del`、`ins`、`bs`、`return`、`pgup`、`pgdn`、`caps`、`mouse1`…`mouseN`、`key<scancode>`、`none` / `unbind`。认不出返回 `null`。
- `isDown(code)`：键盘读 `InputConstants.isKeyDown`（SDL 键盘状态），鼠标读 `SDL_GetMouseState` 的按键掩码。

### 5.7 模块

代码在 `wtf.moonlight.module`。

```
module
├─ Module          模块基类：名字、分类、Bind、开关、设置项
├─ Category        分类
├─ ModuleManager   创建模块、按名字查找、注册每个模块的 Bind
└─ impl
   ├─ client / combat / misc / movement / player / render / world   按分类放模块
   ├─ combat/KillAura
   ├─ movement/Sprint
   ├─ player/Scaffold
   └─ render/FullBright、ClickGui
```

- `impl` 下每个分类一个包，目前 `combat`（`KillAura`）、`movement`（`Sprint`）、`player`（`Scaffold`）和 `render`（`FullBright`、`ClickGui`）里有模块，其余是空包（只放了 `package-info.java`，方便 git 保留目录；包里有了第一个类就把它删掉）。
- 模块要用到另一个模块时，由 `ModuleManager` 在构造时传进去，不做全局查找。例如 `KillAura` 构造时拿到 `Scaffold`，`Scaffold` 开着时 `KillAura` 不工作，免得两个模块抢着转头。
- `Category` 枚举目前是 `MOVEMENT`、`RENDER`。在某个分类的包里写下第一个模块时，再给 `Category` 加对应的常量（按"不留零用法"，没有模块的分类不加，`.toggle` 列表里也不会出现空分类）。

**模块怎么工作**

- `setEnabled(true)` 时先调用 `onEnable()`，再把模块注册到事件总线；`setEnabled(false)` 时注销。所以模块里的 `@Subscribe` 方法只在模块开着时收到事件，不用自己判断开关。需要在开启时初始化状态的模块重写 `onEnable()`（例如 `FullBright` 记下当前亮度），关闭时要收尾的重写 `onDisable()`（在注销之后调用，例如 `ClickGui` 关掉界面）。
- 每个模块自带一个同名的 `Bind`：按绑定键切换（或按住）时开关模块；用命令开关模块时，`Bind` 的状态跟着同步。默认打开任何界面时绑定键都不生效，模块可以用 `getBind().setGate(...)` 换成自己的条件（`ClickGui` 允许在自己的界面里按绑定键关闭，正在输入文字时除外）。
- 模块的开关状态和绑定都保存在配置里，重启后自动恢复（见 5.8）。`isPersistent()` 返回 `false` 的模块只保存绑定、不保存开关（`ClickGui`：不会一启动就弹出界面）。
- 设置项用 `add(...)` 声明成字段，按声明顺序登记到模块上：`private final BoolValue swing = add(new BoolValue("Swing", true));`。登记过的设置项会跟着模块存进配置，并显示在 ClickGUI 模块行的齿轮弹窗里（见 5.9）。

**`.toggle` 命令**

| 写法 | 作用 |
|---|---|
| `.toggle` / `.t` | 按分类列出所有模块，开着的绿色、关着的灰色 |
| `.toggle <模块>` | 开关这个模块 |

**新增一个模块**

```java
package wtf.moonlight.module.impl.movement;

public final class Sprint extends Module {
    public Sprint() {
        super("Sprint", Category.MOVEMENT);
    }

    @Subscribe
    private void onMoveInput(MoveInputEvent event) {
        Input input = event.getInput();
        event.setInput(new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.shift(), true));
    }
}
```

1. 在 `module.impl.<分类>` 下建类，继承 `Module`，构造里传名字和分类；分类还没有就先给 `Category` 加上。
2. 在 `ModuleManager` 的模块列表里加上 `new Xxx()`。
3. 需要事件里还没有的数据或修改能力时，给事件类加对应的 getter / setter，并让 Mixin 用上。

**`Sprint` 的做法**：26.3 的 `KeyboardInput.tick` 把按键状态打包成 `Input`，`LocalPlayer.aiStep` 在可以冲刺且 `input.keyPresses.sprint()` 为真时开始冲刺，这个 `Input` 也会发给服务器。`Sprint` 在 `MoveInputEvent` 里把 `sprint` 置为 `true`，效果等于一直按着冲刺键；不改全局按键状态，关掉模块后下一 tick 就恢复，不需要额外清理。

**`FullBright` 的做法**：26.3 没有 `LightTexture` 了，亮度选项在 `LightmapRenderStateExtractor.extract` 里读取（`options.gamma().get()`），减去黑暗效果后写进 `LightmapRenderState.brightness`。Mixin 用 `@Slice` 从 `options.gamma()` 开始、改紧跟它的那次 `OptionInstance.get()` 的返回值（这个方法里还有 `hideLightningFlash`、`darknessEffectScale` 两次 `get()`，不能只按序号找），通过 `GammaEvent` 交给模块。`FullBright` 开启时记下当前亮度，之后每 tick 加 0.1、逐渐升到 15，光照贴图每 tick 重算一次，所以是渐亮效果；关掉后立刻恢复原亮度。不修改玩家的亮度设置，也不加药水效果。模式和目标亮度现在是固定的，以后可以接上设置项（见 5.9）做成可调。

**`Scaffold` 的做法**：普通平搭，脚下是空的就在脚下放方块。每个 `PlayerTickEvent` 做四件事：

1. 选方块：快捷栏里第一格能用的方块。不能用的有：非 `BlockItem`、火把这类 `StandingAndWallBlockItem`、`VegetationBlock`（26.3 的花草、作物、蘑菇、菌类都继承它）、台阶、黑名单里的方块（容器、矿石、沙子、蜘蛛网、栅栏等），以及物品名带 `Click` / `点击` 的菜单物品。关闭模块时切回开启前的格子。
2. 找放置点：脚下这格（玩家 Y 向下取整再减 1）能替换时，从这格开始往下、往四周按曼哈顿距离搜到 6 格深，找离眼睛 4.5 格以内最近的一个可依附面。依附的方块要有碰撞箱、不能是有界面的方块，而且这个面要朝着眼睛。正在移动且没按跳跃时不往方块顶面放。
3. 转头：偏航候选是 8 个固定方向（-135°～180°，按和移动方向反方向的接近程度排序），最后再加上直接对准放置面的偏航。每个偏航先试 75° / 82° / 87° 的俯仰（各加 ±0.3° 随机量），不行再从 -90° 到 89° 逐度扫，第一个能射中目标方块的就用。`45 Degrees` 打开时跳过正方向的偏航。都不行就直接对准放置面。第一次开启时的初始视角是"玩家偏航 -135°、俯仰 82°"。结果交给 `setRotations`，速度是 `Rotation Speed`。
4. 放方块：用当前服务端视角做一次视线检测，确认正对目标方块（`Raycast` 选 `Strict` 时还要求面也对上），然后 `MultiPlayerGameMode.useItemOn`。点击位置在面中心附近随机偏移，侧面再上移 0.08。

移动方向取自 `MoveInputEvent`（最高优先级，在移动修复改按键之前），按 45° 取整。`Swing` 打开时按原版的规则在本地挥手（`SwingSource.PREDICTED` 才挥）。26.3 原版放方块时不发挥手包（`ServerboundPunchPacket` 只在攻击和挖掘时发），服务器收到放置包后自己处理挥手，所以这里也不发。

| 设置项 | 默认 | 说明 |
|---|---|---|
| `45 Degrees` | 关 | 只用斜向偏航 |
| `Swing` | 开 | 放下方块时本地挥手 |
| `Raycast` | `Normal` | `Normal` 只要求射中目标方块，`Strict` 还要求射中的面就是放置面 |
| `Rotation Speed` | 127 | 每 tick 最多转多少度（1～180） |

**`KillAura` 的做法**：

1. 找目标（`TickEvent.PRE`）：遍历 `ClientLevel.entitiesForRendering()`，留下满足目标条件、眼睛到碰撞箱的距离不超过 `Range`、和玩家偏航相差不超过 `FOV` 一半的活体实体，再按 `Priority` 排序。目标条件：活着、不是自己、不是旁观者；隐身的只在 `Invisibles` 打开时算；玩家、村民（`AbstractVillager`）、怪物（`Monster`、`Slime`、`EnderDragon`）、动物（`Animal`、`AmbientCreature`、`WaterAnimal`）各由一个开关决定。
2. 选目标：`Single` 一直打排第一的；`Switch` 每过 `Switch Delay` 毫秒换下一个，到末尾回到第一个。
3. 转头：用 `RotationUtils.calculate(目标, true, Range)` 算朝向（直接对准射不中时，在碰撞箱上找一个能射中的点）。这个方向先撞上方块的话这一 tick 不转；否则交给 `setRotations`，速度是 `Rotation Speed`，检查条件是"3 格内的视线检测正好射中目标"。
4. 攻击：有目标时每帧（`RenderOverlayEvent`）按 CPS 攒攻击次数，间隔是 `1000 / 正态随机(CPS, 5)` 毫秒；到 `PlayerTickEvent` 再把攒下的次数打出去。准星（已经按服务端视角刷新过）指着实体就 `MultiPlayerGameMode.attack`，然后和原版 `startAttack` 一样本地挥手（`getAttackAnimation()`）并发送 `ServerboundPunchPacket`。正在使用物品时不攻击。

   发包顺序必须是"攻击包 → 挥手包"：1.9 以后的客户端每次攻击之间都要有一个挥手包，攻击包之后、挥手包之前也不能插进别的包，否则反作弊的发包顺序检查（`PacketOrderB`）会报 `pre-attack` / `post-attack`。

| 设置项 | 默认 | 说明 |
|---|---|---|
| `Mode` | `Single` | `Single` / `Switch` |
| `Switch Delay` | 100 ms | 只在 `Switch` 时显示 |
| `Priority` | `Range` | `Health`（血量少的先）、`FoV`（偏航差小的先）、`Range`（离得近的先）、`None`（不排序） |
| `CPS` | 12 | 每秒攻击次数的均值（1～20） |
| `Range` | 4.00 | 索敌距离（3～6），从眼睛算到目标碰撞箱 |
| `FOV` | 180 | 视野角度（0～360），目标要在玩家视线左右各一半以内 |
| `Rotation Speed` | 180 | 每 tick 最多转多少度 |
| `Players` / `Mobs` / `Animals` / `Villagers` / `Invisibles` | 开 / 关 / 关 / 关 / 关 | 打哪些实体 |

### 5.8 配置

代码在 `wtf.moonlight.config.ConfigManager`，命令是 `.config`。

**存什么、存在哪**

- 每份配置是 `config/moonlight/configs/<名字>.json`，内容是每个模块的开关状态和绑定，以及 ClickGUI 的 Client 设置（见 5.10）：
  ```json
  {
    "modules": {
      "Sprint": { "enabled": true, "bind": { "key": 21, "hold": false } },
      "Scaffold": {
        "enabled": false,
        "bind": { "key": 0, "hold": false },
        "values": { "45 Degrees": false, "Swing": true, "Raycast": "Normal", "Rotation Speed": 127.0 }
      },
      "ClickGUI": { "bind": { "key": 229, "hold": false } }
    },
    "client": { "Theme": "Dark", "Size": 100.0, "Custom Client Name": false, "Client Name": "" }
  }
  ```
- 模块的设置项（见 5.9）放在各自模块对象的 `values` 里，没有设置项的模块不写这一项。加载时文件里没有的设置项保持现状，先读设置项再恢复开关，所以 `onEnable()` 里看到的已经是加载后的值。
- 命令前缀是全局设置，不跟配置走，单独存在 `config/moonlight/commands.properties`。

**自动保存**

- 自动保存只写 `latest`：每 20 tick（约 1 秒）把当前状态序列化一次，和上次写入的内容不同才写盘。所以按键频繁开关模块不会反复写文件，状态最多晚 1 秒落盘。
- 退出游戏时 JVM 关闭钩子再保存一次（正常退出、Alt+F4、IDE 里点 Stop 都会触发）。
- 游戏加载完成（`GameLoadedEvent`）时自动加载 `latest`；在这之前不会自动保存，避免用默认状态覆盖上次的配置。不在启动更早的时候加载，是因为部分模块开启时要读游戏设置（例如 `FullBright` 读亮度）。

**手动保存 / 加载**

| 写法 | 作用 |
|---|---|
| `.config` / `.cfg` | 列出已保存的配置，`latest` 标 `(auto)` |
| `.config save` | 立即保存 `latest` |
| `.config save <名字>` | 把当前状态另存为一份命名配置 |
| `.config load <名字>` | 加载一份配置（加载后 `latest` 会在 1 秒内跟着更新） |

- 命名配置只在手动 `save` 时写入，不会被自动保存改动；想把改过的状态存回去就再 `save` 一次。
- 配置名只能用英文字母、数字、空格、`_`、`-`、`.`，最长 63 个字符，开头结尾不能是空格或 `.`，不能以 `.json` 结尾。名字是命令的最后一个参数，可以带空格，支持 Tab 补全。
- 加载时，文件里没有的模块保持现状；文件损坏或值的类型不对时加载失败并记一条警告（`moonlight/config`），游戏不受影响。
- 写文件时先写 `<名字>.json.tmp` 再整体替换，中途崩溃也不会留下写了一半的配置。

**当前配置**

- `ConfigManager` 记着"当前配置"（默认 `latest`）：手动保存或加载哪份，当前配置就是哪份。ClickGUI 顶栏显示的就是它，左边的保存按钮保存的也是它。
- ClickGUI 的配置下拉框还能新建、覆盖、重命名、复制（`<名字> copy`、`<名字> copy 2`……）、删除、刷新列表、打开配置文件夹；删除当前配置时自动切到列表里的第一份并加载。每次操作的结果写进 `getStatus()`，下拉框底部显示 3 秒。
- 配置列表有缓存：游戏加载完成、每次保存 / 重命名 / 复制 / 删除、点"Refresh"时重新扫描目录，命令列出或补全前也会扫一次。

### 5.9 设置项

代码在 `wtf.moonlight.value`：基类 `Value<T>`（名字、当前值、可见条件、JSON 读写），实现在 `value.impl`：

| 类 | 值 | 说明 |
|---|---|---|
| `BoolValue` | `Boolean` | 开关 |
| `NumberValue` | `Double` | `min` / `max` / `step`，`set` 时按步长四舍五入再夹到范围内；`format` 是 `String.format` 的格式，例如 `"%.0f%%"`；可以多传一个可见条件 |
| `ModeValue` | `Integer`（下标） | 选项列表；配置里存选项名字，不存下标 |
| `TextValue` | `String` | 文本；构造时传可见条件 |

用在两个地方：ClickGUI 的 Client 设置（`ClientSettings`），以及模块用 `Module.add(...)` 登记的设置项（目前是 `Scaffold`）。模块的设置项显示在模块行齿轮弹窗里、`Bind` 那一行下面，顺序就是声明顺序，`Row.of` 按类型选控件。

### 5.10 ClickGUI

按 **右 Shift**（`ClickGUI` 模块的默认绑定）打开，再按一次、按 Esc 或点模块行里 ClickGUI 自己的开关关闭。界面用 ImGui 绘制，布局、尺寸、配色、字体和动画都照原样移植。

**包结构**

```
ui
├─ imgui            ImGui 接入，和界面内容无关
│  ├─ ImGuiManager  创建上下文、加载字体和样式，每帧 NewFrame → 画界面 → Render
│  ├─ ImGuiRenderer 用游戏的 RenderPearl 接口画 ImGui 的绘制数据，并管理 ImGui 的纹理
│  ├─ Input         把游戏的键盘、鼠标、文字输入转成 ImGui 的输入事件
│  ├─ Fonts         四个字体文件
│  ├─ Natives       直接读写 ImGui 结构体（偏移、校验）
│  ├─ Effects       给 ImGui 形状加模糊 / 阴影的接口（见 5.12）
│  └─ post          后处理的 GPU 部分：Blur、Bloom、Kawase、Area、Target
└─ clickgui         ClickGUI 本身
   ├─ ClickGuiScreen 打开 ClickGUI 时显示的 Screen：转发输入、切换光标和文字输入
   ├─ Panel         主窗口（748×576，侧栏 158，顶栏 56，圆角 14）、缩放和入场动画
   ├─ TopBar        字标、配置下拉框、搜索
   ├─ PageLayout    侧栏导航、两列模块卡片、Client 设置页、左下角账号菜单
   ├─ Row           一行设置（名字 + 控件 + 齿轮弹窗）
   ├─ element       控件：SwitchElement、SliderElement、SelectorElement、TextElement、BindElement
   ├─ Controls      开关、选择器、滑条、卡片、弹窗底板等基础绘制和动画
   ├─ Widgets       字体、缩放、选项行、弹窗的进出动画
   ├─ Popups        弹窗关闭后的淡出快照
   ├─ Windows       记录这一帧画过的窗口，供入场 / 翻页 / 弹窗动画变换顶点
   ├─ Placement     主窗口居中和拖动后的位置
   ├─ Theme / Colors 深色、浅色两套配色和颜色运算
   └─ ClientSettings Theme、Size、Custom Client Name、Client Name
```

**渲染**

- `GameRendererMixin` 在 `GameRenderer.render()` 里 `GuiRenderer.render()` 之后发 `RenderOverlayEvent`，`ImGuiManager` 在这里跑一帧 ImGui，画进主渲染目标。所以 ClickGUI 盖在游戏画面和 HUD 上面，截图（F2）里没有它。
- 不用 imgui-java 自带的 `ImGuiImplGl3`：`ImGuiRenderer` 用 RenderPearl 的 `RenderPass`（复用原版 `position_tex_color` 着色器，三角形、不剔除、ImGui 的混合方式），OpenGL 和 Vulkan 都能画。
- ImGui 1.92 的字体是按需光栅化的（任何字号都清晰），需要后端处理纹理的创建、增量更新和销毁。imgui-java 没有导出 `ImTextureData`，所以 `Natives` 按 1.92.7 的结构体布局直接读写，`ImGuiRenderer` 用 `GpuTexture` 上传。入场、翻页、弹窗动画也是直接改顶点缓冲。
- 这些偏移只对 imgui-java 1.92.7.1 成立。启动时 `Natives` 会把它们和绑定接口的结果比对，对不上就停用 ImGui、记一条错误，不会读写错内存。**升级 imgui-java 时要重新核对偏移。**

**输入**

- 打开 ClickGUI 就是打开 `ClickGuiScreen`：游戏松开鼠标，键盘和鼠标交给界面，其他模块的绑定键暂时不生效。
- 鼠标位置从 `MouseHandler` 的窗口坐标换算成帧缓冲像素；按键用 SDL 键码映射成 ImGui 键（`ImGuiImplSdl3.keyEventToImGuiKey`）；文字来自 `charTyped`。ImGui 要输入文字时 `ClickGuiScreen` 打开 SDL 的文字输入，不需要时关掉。
- 光标形状跟着 ImGui 变（输入框是 I 形）。
- 正在等按键绑定时按 Esc，按 5.5 的规则清除绑定，不关闭界面。
- ImGui 的断言在 imgui-java 里会直接结束进程，所以把可恢复的用户错误（Push / Pop 不成对之类）设成不断言，只显示 ImGui 自带的错误提示。

**字体**

`src/main/resources/assets/moonlight/fonts/`：

| 文件 | 用途 |
|---|---|
| `sst-medium.ttf` | 正文，15 px 基准，3 倍横向过采样 |
| `medium.ttf` | 正文缺字时的回退（中日韩等），合并进正文字体 |
| `fa-solid-900.ttf` | 图标（Font Awesome 5 Solid），13 px |
| `extrabold.ttf` | 左上角字标，28 px，带 1.2 字距和两秒一轮的错位动画 |

ImGui 1.92 要求字体数据在整个图集生命周期里一直有效，imgui-java 直接传 `byte[]` 的接口做不到，所以 `Fonts` 先把字体解到临时目录，用 `addFontFromFileTTF` 让 ImGui 自己读进内存，读完马上删掉临时文件。两个大字体各约 8 MB，模组 jar 因此约 22 MB。

**页面**

- 侧栏 MODULES 下按 `Category` 列出分类（图标在 `PageLayout.icon`，加分类时要补一个），超过 5 个分类时这一栏可以滚动；CLIENT 下是 Settings。
- 模块页：所有模块放在 General 卡片里，按数量均分左右两列，两列各自滚动。模块行右边是开关和齿轮，齿轮弹窗里第一行是 `Bind`：Click / Hold 切换、按键（点了以后按键或鼠标侧键绑定，左键取消）、Delete 清除；下面是这个模块的设置项（见 5.9）。
- Settings 页：Theme（Light / Dark）、Size（60%～150%，拖动滑条或点数值直接输入）、Custom Client Name、Client Name（打开 Custom Client Name 后出现；字标和账号菜单里的名字跟着变，默认 Moonlight）。
- 顶栏：配置下拉框（见 5.8）和搜索（按名字过滤所有模块，关闭搜索或换页时清空）。
- 左下角是当前 Minecraft 账号名，点开后可以进 Client settings、切浅色主题、调界面缩放。
- 窗口居中显示，可以按住空白处拖动；拖动过的位置在改窗口大小后保留。窗口放不下时自动缩小。

### 5.11 旋转

代码在 `wtf.moonlight.rotation`：`Rotation`（record，`yaw` / `pitch`）和 `RotationManager`。计算用的工具方法在 `utils` 的 `RotationUtils`、`RaycastUtils`、`MoveUtils`（见 5.6）。`RotationManager` 全局只有一个，`MoonlightClient` 创建它并注册到事件总线，用 `MoonlightClient.getRotations()` 取。

作用是把**发给服务器的视角**（下面叫服务端视角）和玩家自己看到的视角分开：画面不跟着转，服务器、其他玩家和客户端里所有依赖视角的判定（准星目标、移动方向、使用物品、模型朝向）都按服务端视角来。

**用法**

```java
RotationManager rotations = MoonlightClient.getRotations();
rotations.setRotations(RotationUtils.calculate(target), 30);
rotations.setRotations(RotationUtils.calculate(target, true, 3.0), 30,
        rotation -> RaycastUtils.rayCast(rotation, 3.0) instanceof EntityHitResult hit && hit.getEntity() == target);
```

| 方法 | 作用 |
|---|---|
| `setRotations(目标, 速度)` | 服务端视角每 tick 最多转 `速度` 度（偏航和俯仰按比例分配）。需要保持时每 tick 都调用；不再调用后，服务端视角从下一 tick 开始转回玩家视角，偏航（取模后）和俯仰都差不到 1° 时结束，结束时把玩家偏航整圈平移到和最后发出的偏航同一圈（见下方"结束时的整圈平移"） |
| `setRotations(目标, 速度, 检查)` | 同上。目标和当前服务端视角相差超过 5° 时，在目标附近加随机偏移，用 `检查` 判断偏移后是否还满足条件（例如还能射中目标）；不满足就换个方向偏移，还不行就直接对准目标再加 0～2° 随机量 |
| `getRotation()` | 当前服务端视角；没在转时返回玩家视角 |
| `setRotation(r)` | 直接设定服务端视角，不经过平滑（配合 `setActive(true)` 使用） |
| `getLastRotation()` | 上一 tick 发出去的视角 |
| `isActive()` / `setActive(b)` | 是否正在使用服务端视角 |
| `isSmoothed()` / `setSmoothed(b)` / `smooth()` | 每 tick 只平滑一次；`smooth()` 在这一 tick 还没平滑过时算一次，然后调用 `Minecraft.pick` 刷新准星目标 |

**一个 tick 里的顺序**

| 时机 | 事件（优先级） | 做什么 |
|---|---|---|
| `LocalPlayer.tick` 开头 | `PlayerTickEvent`（LOW） | 没在转时，把服务端视角、上次视角、目标都对齐到玩家视角；在转时调用 `smooth()` |
| `KeyboardInput.tick` | `MoveInputEvent`（HIGH，在 `Sprint` 之前） | 移动修复 |
| 移动、跳跃 | `StrafeEvent`、`JumpEvent` | 移动方向和疾跑跳方向用服务端偏航 |
| 发移动包 | `MotionEvent`（LOWEST，最后执行） | 把服务端视角写进移动包（俯仰限制在 ±90°，NaN 不写）；记下这一 tick 发出的视角；和玩家视角相差不到 1° 时结束旋转并整圈平移玩家偏航；清掉平滑标记和 `检查` |
| 新的玩家对象 | `RespawnEvent` | 清空服务端视角和上次视角 |

其他钩子：

- 准星目标（`RaycastEvent`）、物品视线（`ItemRaycastEvent`）、使用物品包（`UseItemEvent`，HIGHEST）：在转时用服务端视角。`smooth()` 之后马上刷新了准星目标，所以同一 tick 里接着攻击、放方块就是对着服务端视角的。
- 鞘翅俯仰（`FallFlyingEvent`）、击退方向（`AttackYawEvent`）：只要有服务端视角就用它，不在转时它就等于玩家视角。
- 模型（`RotationAnimationEvent`）：在转时，本地玩家模型的身体、头部偏航和俯仰按发给服务器的视角显示，第三人称里看到的就是别人看到的样子。

**平滑**（`RotationUtils.smooth`）

- 先按速度限制这一 tick 的转动量（`move`），速度另加 0～1 的随机量。偏航差按 `wrapDegrees` 取最短方向，所以服务端视角的偏航是连续的（可以超出 ±180°），不会一下跳一整圈。目标和上次视角完全重合时转动量是 0（不能除以 0 得到 NaN，否则视角会跳回玩家上一 tick 的偏航）。

**结束时的整圈平移**

Minecraft 的偏航不取模，转几圈就会累积到 ±720° 之类的值。服务端视角沿最短方向转回玩家视角时，停下来的位置可能和玩家偏航差了整整 360°（例如玩家 170°、服务端视角 -190°）。如果直接交还，下一个移动包就从 -190 跳到 170，反作弊的 `AimModulo360` 检查（一次偏航变化超过 320° 而上一次小于 30°，当前偏航又在 ±360° 内）会马上报。

所以结束时以这一包实际发出的偏航为准，把玩家偏航加减整数个 360°，挪到同一圈里。画面完全不变，只是换了一种表示，下一包的偏航变化就不到 1°。`yRotO`（渲染插值）和 `yBob` / `yBobO`（手臂摆动，第一人称手部按"视角偏航 - 摆动偏航"算偏转）同时平移，否则这一帧会转一圈、手会甩一大下。不做鼠标步长修正：那样会改变玩家看到的画面。
- 再做若干次（约 FPS / 20 + 0～10 次）GCD 修正：转动量取整到当前鼠标灵敏度的最小步长（`applySensitivityPatch`，换算和 `MouseHandler` / `Entity.turn` 一致），在动的时候每次加一点随机抖动。

**移动修复**（`MoveUtils.fixMovement`）

服务端偏航和玩家偏航不同时，如果按原来的键走，服务器算出的移动方向和实际不一致。修复的做法：先算出玩家想走的方向（玩家偏航加上按键组合对应的 0 / ±45 / ±90 / ±135 / 180°），再相对服务端偏航选出最接近的按键组合（每个方向留 ±22.5° 容差，斜向就同时按两个键）。

26.3 的移动输入是 `Input` 里的布尔按键，`KeyboardInput.tick` 再由它算出 `moveVector`，所以修复直接改前后左右四个键，跳跃、潜行、冲刺不变。改过的 `Input` 也会作为 `ServerboundPlayerInputPacket` 发给服务器，和服务器看到的移动方向一致。没按方向键时不处理。

**26.3 上要注意的地方**

- 准星检测在 `Minecraft.pick`（私有），通过 `MinecraftAccessor` 调用。
- 使用物品时视角直接写在 `ServerboundUseItemPacket` 里，没有单独的位置包，所以改的是这个包。注入目标是 lambda `lambda$useItem$0`，原版和 NeoForge 的 jar 里名字相同，升级版本时要重新核对。
- 第三人称模型的头部偏航在 `LivingEntityRenderer.extractRenderState` 的 `Mth.rotLerp`，俯仰是 `entity.getXRot(partialTicks)`。
- 击退方向在 `Player.causeExtraKnockback`；客户端只有 `hurtClient` 返回 `true` 的实体会走到这里。
- 骑乘时不触发 `MotionEvent`（见 5.3），服务端视角不会写进骑乘时发的 `Rot` 包。

目前用到旋转系统的模块（见 5.7）：

- `Scaffold`：`setRotations(目标, 速度)`、`getRotation()`、`RotationUtils.calculate(BlockPos, Direction)`、`RaycastUtils.overBlock` 的两个重载。
- `KillAura`：`setRotations(目标, 速度, 检查)`、`RotationUtils.calculate(Entity, true, 距离)`（连带 `calculate(Entity)`）、`RotationUtils.getDistance(Entity)`、`RaycastUtils.rayCast(Rotation, 距离)`。

### 5.12 后处理（模糊和阴影）

给 ImGui 画的界面加背景模糊和外阴影。开关和参数在 Render 分类的 `PostProcess` 模块（`module.impl.render.PostProcess`），默认关闭：

| 设置项 | 范围 | 默认 | 说明 |
|---|---|---|---|
| Render Blur | 开 / 关 | 开 | 背景模糊 |
| Render Shadow | 开 / 关 | 开 | 外阴影 |
| Blur Amount | 1～8 | 4 | 模糊金字塔层数，每层分辨率减半 |
| Blur Offset | 1～10 | 10 | 模糊采样偏移 |
| Bloom Amount | 1～8 | 2 | 阴影金字塔层数 |
| Bloom Offset | 1～10 | 2 | 阴影扩散距离；超过 2 时自动拆成更多层，见下方"怎么实现的" |

**在界面里用**

```java
ImDrawList draw = ImGui.getWindowDrawList();
draw.pushClipRectFullScreen();
Effects.shadow(draw, minX, minY, maxX, maxY, rounding, 0, Colors.rgb(0, 0, 0));
Effects.blur(draw, minX, minY, maxX, maxY, rounding, 0);
draw.popClipRect();
// 然后画这个元素自己的半透明底色、边框和内容
```

- 两个方法都在元素背景**之前**调用，先阴影后模糊。模块关着、或者对应开关关着时什么都不画，没有开销。
- 参数和 `addRectFilled` 一样（屏幕坐标、圆角、`ImDrawFlags`）。阴影颜色就是遮罩颜色：RGB 决定阴影颜色，alpha 决定阴影浓度。一般用黑色，想要彩色阴影就传主题色。同一组连续的阴影形状只用一种颜色（取 alpha 最大的那个）。
- 阴影要画到元素外面，所以先 `pushClipRectFullScreen()`，否则会被窗口剪裁矩形切掉。
- 效果命令是普通的 ImGui 形状，入场、翻页、弹窗动画会一起缩放和淡入淡出。
- ClickGUI 的主窗口（`Panel`）和所有子窗口都已经接好：子窗口统一走 `Widgets.beginPopup`，它在弹窗玻璃底板之前加阴影和模糊，所以新加的弹窗自动带效果。阴影色统一用 `Controls.SHADOW`（不透明黑，边缘约 50%，往外渐隐）。
- 阴影只出现在形状外面：挖空按形状的覆盖率算，不管遮罩颜色多透明，形状内部都完全挖掉。

**怎么实现的**

- `Effects` 用两个保留的纹理 ID（`-2` 模糊、`-3` 阴影）画普通的 ImGui 圆角形状，`ImGuiRenderer` 按命令顺序执行：普通命令照常画；遇到阴影命令就暂停主绘制，把形状的"颜色 alpha × 覆盖率"写进遮罩，做 Kawase 金字塔，挖掉形状内部后合成回屏幕；遇到模糊命令就从主渲染目标做 Dual Kawase 金字塔，再用 `imgui_blur` 着色器按屏幕位置把结果画进这个形状。
- 挖空：渲染器取这一组阴影形状里最大的顶点 alpha 当作颜色 alpha，挖空时用 `1 - min(遮罩 / 颜色 alpha, 1)`，也就是 `1 - 覆盖率`。
- **阴影的中间结果全部存在 RGB 里，不用贴图的 alpha 通道。** 26.3 里这些离屏贴图的 alpha 读出来始终是 1（清空成 0 也一样），一开始按 alpha 存遮罩，结果遮罩背景变成 1，阴影要么朝内要么完全被挖掉。现在遮罩、金字塔、挖空结果都只用 R 通道；最后合成时由 `shadow` 着色器从 R 算出 alpha，阴影颜色用 uniform 传进去。以后写后处理也一样：要存的数据放 RGB，输出 alpha 只在合成到主画面那一步由着色器算出来。
- Bloom Offset 越大，Kawase 每一遍的采样点隔得越远，超过约 2 个像素就会跳格，出现一圈圈台阶状的重影。所以 Offset 超过 2 时，每翻一倍就多降一层金字塔、步长减半，让每一遍的步长始终不超过 2。扩散范围基本不变，重影消失；Offset ≤ 2 时和原来完全一样。
- 弹窗画在主窗口后面，所以弹窗的模糊会从已经画了主窗口的画面重新取样，阴影也会落在主窗口上。
- 所有离屏 pass 都用剪裁矩形限制在效果区域加外扩的范围里（外扩量按层数和偏移算，离线测试验证过和整屏计算结果一致）。
- 模糊只取 ImGui 之前的画面（游戏画面、原版 HUD 和前面合成的阴影），最多每 1/60 秒重算一次，改参数或窗口大小时立即重算。如果模糊命令前面已经画过别的 ImGui 内容，就从当前画面重新算，不节流。
- 着色器在 `assets/moonlight/shaders/core/`：`blur_down`、`blur_up`（模糊和阴影金字塔共用）、`bloom_cut`（挖空输出）、`shadow`（阴影合成）、`imgui_mask`（把阴影形状写进遮罩）、`imgui_blur`（顶点和片元各一个）。全屏 pass 的顶点着色器复用原版的 `core/screenquad`。
- **着色器里不要用 `gl_FragCoord`。** 26.3 先按 Vulkan 规则用 shaderc 把 GLSL 编成 SPIR-V，再用 spvc 转成 OpenGL 的 GLSL；`gl_FragCoord` 的原点约定和 OpenGL 渲染目标的行序对不上，按它取样会上下颠倒，原版着色器也一处都没用。需要屏幕坐标时在顶点着色器里用 `gl_Position.xy / gl_Position.w * 0.5 + 0.5` 算出来再传下去（`imgui_blur.vsh` 就是这样），它和 `screenquad` 的 `texCoord` 约定一致。
- 离屏目标第一次用到时才创建，模块关掉后释放。后处理出错只记一次日志并在本次运行里停用，ImGui 界面照常显示。

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
├─ Tasks → application     → 26.3-fabric-client、26.3-neoforge-client、buildAllJars
├─ Tasks → stonecutter     ← 切换激活节点
├─ 26.3-fabric
│   └─ Tasks → build       → build、buildAndCollect
│            → fabric      → runClient、runServer
└─ 26.3-neoforge
    └─ Tasks → build       → build、buildAndCollect
```

日常使用只看根项目的 `application` 分组：

| 任务 | 作用 |
|---|---|
| `26.3-fabric-client` | 启动 Fabric 客户端（执行 `:26.3-fabric:runClient`） |
| `26.3-neoforge-client` | 启动 NeoForge 客户端（执行 `:26.3-neoforge:runClient`） |
| `buildAllJars` | 同时构建 Fabric 和 NeoForge，jar 汇总到根目录的 `build/libs/<版本>/` |

- 这三个任务都定义在 `stonecutter.gradle`。`<节点>-client` 按子项目自动生成，`buildAllJars` 依赖所有节点的 `buildAndCollect`，以后新增 Minecraft 版本节点时都会自动包含，不用改。
- 右键这些任务选"调试"，IDEA 会把调试器挂到游戏进程上。
- 只构建 Fabric 版：双击 `26.3-fabric → Tasks → build → build`
- 只构建 NeoForge 版：双击 `26.3-neoforge → Tasks → build → build`

**根项目 Tasks 里为什么有 `fabric`、`mod development` 这些分组**

IDEA 在根项目下列出的是"可以从根项目运行的任务"，也就是根项目自己的任务加上所有子项目的任务（同名的合并成一项）。所以 Loom 的 `fabric` 分组、ModDevGradle 的 `mod development` 分组、两边的 `build` / `jar` 等都会出现在根项目下。这是 Gradle 多项目构建的正常行为，不是配置错误。各分组的来源：

| 分组 | 来源 |
|---|---|
| `fabric` | Loom（只有 26.3-fabric 有） |
| `mod development`、`mod development/internal` | ModDevGradle（只有 26.3-neoforge 有） |
| `stonecutter`、`stonecutter-impl` | Stonecutter（切换节点和它的内部任务） |
| `ide` | `idea` 插件（由 idea-ext 带进来，用于生成运行配置） |
| `build`、`documentation`、`verification`、`help`、`build setup`、`other` | Gradle 自带 |

在根项目下双击一个任务，Gradle 会在**每个有这个任务的子项目里都执行一次**。例如根项目的 `build` 会同时构建两个加载器。`runClient` / `runServer` 也一样，会把 Fabric 和 NeoForge 的游戏依次全部启动，所以 `stonecutter.gradle` 里拦掉了这两个：在根项目直接运行它们（比如双击根项目 `fabric` 分组下的 `runClient`）会报错，并提示改用 `application` 分组里的任务。

建议：根项目下只用 `application` 和 `stonecutter` 两个分组；其他单个加载器的任务去 `26.3-fabric` / `26.3-neoforge` 节点下找。

### 8.2 IDEA 的运行 / 调试配置

每次 Gradle 同步完成后，IDEA 会自动在右上角的运行 / 调试配置里创建这五个，全部是 Gradle 类型：

| 配置 | 执行的 Gradle 任务 | 作用 |
|---|---|---|
| `26.3-fabric-client` | `:26.3-fabric:runClient` | 启动 Fabric 客户端 |
| `26.3-fabric-server` | `:26.3-fabric:runServer` | 启动 Fabric 服务端 |
| `26.3-neoforge-client` | `:26.3-neoforge:runClient` | 启动 NeoForge 客户端 |
| `26.3-neoforge-server` | `:26.3-neoforge:runServer` | 启动 NeoForge 服务端 |
| `buildAllJars` | `buildAllJars` | 同时构建两个加载器的 jar |

实现方式：

- 根项目应用了 JetBrains 的 `idea-ext` 插件（1.2，和 ModDevGradle 内部用的是同一个版本）。这五个配置都在 `stonecutter.gradle` 里注册到 `idea.project.settings.runConfigurations`，IDEA 同步时读取并创建，不需要在仓库里放 `.run` / `.idea` 的 xml 文件。
- `<节点>-client` / `<节点>-server` 按子项目自动生成，以后新增版本节点会自动多出对应的配置。
- 配置直接执行 Loom / ModDevGradle 的 `runClient` / `runServer` 任务，所以构建脚本里给运行加的参数（DevAuth、`-Dmixin.debug.export` 等）、启动前的准备任务（生成 `launch.cfg`、下载资源、NeoForge 的 `prepareClientRun`）都由 Gradle 处理，和命令行运行完全一致。点"调试"时 IDEA 会把调试器挂到 Gradle 启动的游戏进程上。
- 加载器自己生成的 IDE 运行配置都已关闭：Loom 是 `ideConfigGenerated(false)`，ModDevGradle 是两个 `runs` 里的 `disableIdeRun()`。否则同步后会多出它们默认的"应用程序"类型配置。
- 同步不会删除旧的配置。以前留下的 `26.3-fabric`、`26.3-neoforge`、`26.3-neoforge - Client`、`26.3-neoforge - Server`、`Build All Jars`，以及上一版生成的"应用程序"类型的 `26.3-*-client` / `26.3-*-server`，需要在"编辑配置"里手动删一次，再重新同步。`Moonlight-v2-NextGen [xxx]` 这种是双击 Gradle 任务时 IDEA 自动生成的临时配置，可以不管。

### 8.3 命令行

| 目的 | 命令 |
|---|---|
| 只构建 Fabric | `./gradlew :26.3-fabric:build` |
| 只构建 NeoForge | `./gradlew :26.3-neoforge:build` |
| 两个都构建，并把 jar 汇总到 `build/libs/<版本>/` | `./gradlew buildAllJars` |
| 启动 Fabric / NeoForge 客户端（等同下面两行） | `./gradlew 26.3-fabric-client` / `./gradlew 26.3-neoforge-client` |
| 两个都构建（不汇总） | `./gradlew build` |
| 启动 Fabric 客户端 | `./gradlew :26.3-fabric:runClient` |
| 启动 NeoForge 客户端 | `./gradlew :26.3-neoforge:runClient` |

产物位置：

- `versions/26.3-fabric/build/libs/moonlight-fabric-<版本>+26.3.jar`
- `versions/26.3-neoforge/build/libs/moonlight-neoforge-<版本>+26.3.jar`
- 运行 `buildAllJars` 后，两边的 jar 和源码 jar 都会复制到 `build/libs/<版本>/`：
  ```
  build/libs/0.1.0/
  ├─ moonlight-fabric-0.1.0+26.3.jar
  ├─ moonlight-fabric-0.1.0+26.3-sources.jar
  ├─ moonlight-neoforge-0.1.0+26.3.jar
  └─ moonlight-neoforge-0.1.0+26.3-sources.jar
  ```

两个加载器共用 `run/` 作为游戏目录（存档、配置、日志都在里面）。

### 8.4 开发环境登录正版账号（DevAuth）

两个加载器都以 `runtimeOnly` 引入了 [DevAuth](https://github.com/DJtheRedstoner/DevAuth)（版本在 `stonecutter.properties.toml` 的 `deps.devauth`）。它只在开发环境运行时生效，**不会打进发布的 jar**。

- 只有**客户端**运行带了 `-Ddevauth.enabled=true`：Fabric 在 `build.fabric.gradle` 的 `runConfigs`（client）里，NeoForge 在 `build.neoforge.gradle` 的 `runs.client` 里。IDEA 的 `26.3-fabric-client`、`26.3-neoforge-client` 配置都会带上，服务端配置不带。
- 第一次启动客户端时，控制台会打出一个微软登录链接，在浏览器里登录授权后就会用这个账号进游戏。令牌保存在 `C:\Users\<用户名>\.devauth\microsoft_accounts.json`，之后会自动续期。**这个文件不要分享给别人。**
- 多个账号：在 `~/.devauth/config.toml` 里加账号，用 `-Ddevauth.account=<名字>` 选择要登录哪个。
- 想临时用离线账号：把对应的启用参数改成 `false`，或者删掉那一行，然后重新同步 Gradle。
- DevAuth 的兼容表只写到 1.21.11。它的原理是在 `net.minecraft.client.main.Main.main` 开头改写启动参数，26.3 里这个方法没变，所以理论上能用。如果升级 Minecraft 后登录失效，先检查这一点。

### 8.5 gradle.properties 说明

- `org.gradle.parallel=false`：两个节点不并行构建。NeoForge 的 Minecraft 重编译很吃内存，并行时容易把电脑跑卡。
- `org.gradle.configuration-cache=true`：开启配置缓存。修改构建脚本时注意第 10 节的写法要求。
- `org.gradle.configuration-cache.problems=warn`：配置缓存遇到不兼容的写法时只警告、不中断构建。IDEA 设成"使用 Gradle 构建和运行"时，如果运行"应用程序"类型的配置（比如自己在 IDEA 里新建的），IDEA 会临时生成一个初始化脚本交给 Gradle 执行。这个脚本注册了构建监听器，还在执行阶段访问 `Task.project`，两者都不兼容配置缓存；不加这一行就会报 `Invocation of 'Task.project' ... is unsupported with the configuration cache`。平时构建仍然正常使用配置缓存，只是自己写的脚本出了兼容问题也只会显示成警告，要留意构建输出里的 `problems were found`。
- `dev.kikugie.stonecutter.hard_mode=true`：关闭 Stonecutter 每次构建都打印的"推荐使用 Kotlin DSL"提示。项目有意使用 Groovy。

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
| Fabric Loader / Fabric API / NeoForge / DevAuth / imgui-java | `stonecutter.properties.toml` |
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

### 9.5 ImGui（imgui-java）

两个加载器都引入了 [imgui-java](https://github.com/SpaiR/imgui-java)，版本在 `stonecutter.properties.toml` 的 `deps.imgui`，来自 Maven Central。

| 构件 | 用途 | 依赖方式 |
|---|---|---|
| `imgui-java-binding` | ImGui 的 Java 绑定（`imgui.ImGui` 等） | `implementation` |
| `imgui-java-lwjgl3` | 后端：`imgui.sdl3.ImGuiImplSdl3`（SDL3 平台）、`imgui.gl3.ImGuiImplGl3`（OpenGL3 渲染） | `implementation`，排除 `org.lwjgl` |
| `imgui-java-natives-windows` / `-linux` / `-macos` | 三个平台的原生库 | `runtimeOnly` |

- 五个构件都打进了发布的模组 jar：Fabric 用 `include`（放在 `META-INF/jars/`），NeoForge 用 `jarJar`（放在 `META-INF/jarjar/`）。三个平台的原生库约 10 MB，加上 ClickGUI 的字体，模组 jar 约 22 MB。
- ClickGUI 实际怎么接入的见 5.10：渲染用自己的 `ImGuiRenderer`（RenderPearl），输入走 `ClickGuiScreen`，只借用了 `ImGuiImplSdl3.keyEventToImGuiKey` 做按键映射，下面关于两个后端的说明是给以后直接用它们的场合准备的。
- **不要带 imgui 自己的 LWJGL**。`imgui-java-lwjgl3` 默认依赖 LWJGL 3.4.1，还要 `lwjgl-glfw`。26.3 自带的是 LWJGL 3.4.3，并且已经用 SDL3 取代了 GLFW，没有 `lwjgl-glfw`。所以 `org.lwjgl` 整组都排除掉，用游戏自带的。我已经核对过：`ImGuiImplSdl3` 和 `ImGuiImplGl3` 用到的 105 个 LWJGL 方法和字段，在 3.4.3 里都存在，签名也一致。
- **平台后端用 `ImGuiImplSdl3`，不要用 `ImGuiImplGlfw`**：26.3 没有 GLFW 窗口。
- **渲染后端只能在 OpenGL 下用**：26.3 的图形 API 是一个选项（`PreferredGraphicsApi`：`DEFAULT` / `OPENGL` / `VULKAN`），默认由游戏自己选，可能跑在 Vulkan 上。`ImGuiImplGl3` 只在 OpenGL 下有效，接入时要先判断当前用的是哪个后端。
- 原生库由 `imgui.ImGui` 在第一次使用时从类路径的 `io/imgui/java/native-bin/` 解压到系统临时目录的 `imgui-java-natives/<版本>/` 再加载。也可以用系统属性 `imgui.library.path` 指定原生库所在目录。
- NeoForge 下的非模组库直接用 `implementation` / `runtimeOnly` 即可。ModDevGradle 在新版本上不允许往 `additionalRuntimeClasspath` 里加依赖。

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
