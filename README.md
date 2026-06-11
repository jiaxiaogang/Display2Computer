# Display2Computer

Display2Computer 是一个跨平台的 DLNA/UPnP 投屏接收服务，可以让手机上的 B 站、央视频等 App 在投屏列表里发现电脑，并把视频 URL 投到电脑端播放。

当前版本是 MVP：

- 手机投屏列表中的默认设备名格式为 `Display2(用户名-系统类型)`，例如 `Display2(jiaxiaogang-Win)`
- 支持 DLNA/UPnP MediaRenderer 发现
- 支持 AVTransport / RenderingControl / ConnectionManager 基础控制
- 支持 B 站、央视频等 App 下发播放 URL
- 默认使用 Chrome 打开投屏 URL
- 默认全屏打开投屏 URL
- 提供本地状态页和日志

## 系统要求

### Windows

必需：

- Windows 10/11
- Java JDK 17
- Google Chrome
- 手机和电脑连接同一个局域网/Wi-Fi

当前项目默认使用的 JDK 路径是：

```powershell
C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9
```

默认优先查找 Chrome：

```powershell
C:\Program Files\Google\Chrome\Application\chrome.exe
```

如果没有 Chrome，会继续尝试：

```powershell
C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe
C:\Program Files\Microsoft\Edge\Application\msedge.exe
C:\Program Files\VideoLAN\VLC\vlc.exe
C:\Program Files (x86)\VideoLAN\VLC\vlc.exe
```

推荐安装 Chrome。Windows 自带 Windows Media Player 不推荐作为后端播放器。

### macOS

必需：

- macOS
- Java JDK 17
- Google Chrome、VLC 或 mpv 之一
- 手机和 Mac 连接同一个局域网/Wi-Fi

macOS 默认优先查找：

```bash
/Applications/VLC.app/Contents/MacOS/VLC
/opt/homebrew/bin/mpv
/usr/local/bin/mpv
```

当前代码还没有把 macOS Chrome 放进默认查找列表。如果要用 Chrome，可以启动时手动指定播放器路径。

常见 Chrome 路径：

```bash
/Applications/Google Chrome.app/Contents/MacOS/Google Chrome
```

## Windows 运行

在 PowerShell 中执行：

```powershell
cd D:\repos\Display2Computer
.\run.ps1
```

`run.ps1` 会：

1. 使用本机 JDK 编译 Java 源码
2. 启动 Display2Computer 服务
3. 自动使用默认播放器配置

启动成功后会看到类似日志：

```text
HTTP server listening on port 49152
SSDP joined 192.168.1.x
Display2Computer is running. Open http://localhost:49152/
```

打开状态页：

```text
http://localhost:49152/
```

手机也可以访问状态页，例如：

```text
http://192.168.1.37:49152/
```

如果手机无法打开这个地址，投屏 App 通常也无法发现电脑。请检查 Windows 防火墙、网络类型和路由器 AP 隔离。

### Windows 手动指定播放器

如果 Chrome 不在默认路径，可以手动指定：

```powershell
cd D:\repos\Display2Computer
& "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9\bin\java.exe" `
  "-Dshow2pc.player=C:\Program Files\Google\Chrome\Application\chrome.exe" `
  -cp "target\classes;src\main\resources" `
  show2pc.Main
```

也可以指定 Edge、VLC 或 mpv。

默认会尽量全屏打开播放器。需要关闭全屏时，可以加：

```powershell
"-Dshow2pc.fullscreen=false"
```

例如：

```powershell
cd D:\repos\Display2Computer
& "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9\bin\java.exe" `
  "-Dshow2pc.fullscreen=false" `
  -cp "target\classes;src\main\resources" `
  show2pc.Main
```

## Windows 打包运行

生成 jar：

```powershell
cd D:\repos\Display2Computer
.\package.ps1
```

生成文件：

```text
target\display2computer.jar
```

运行 jar：

```powershell
.\run-jar.ps1
```

或者：

```powershell
& "C:\Users\jiaxiaogang\service\javaSDK\jdk-17.0.9\bin\java.exe" -jar target\display2computer.jar
```

如果提示端口占用：

```text
Address already in use: bind
```

说明已有 Display2Computer 实例在运行。请在旧窗口按：

```text
Ctrl + C
```

然后重新启动。

## macOS 运行

macOS 当前没有专门的脚本，可以用 JDK 命令手动编译和运行。

进入项目目录：

```bash
cd /path/to/Display2Computer
```

编译：

```bash
mkdir -p target/classes
javac -encoding UTF-8 -d target/classes $(find src/main/java -name "*.java")
```

使用 VLC 或 mpv 运行：

```bash
java -cp "target/classes:src/main/resources" show2pc.Main
```

如果要指定 Chrome：

```bash
java \
  "-Dshow2pc.player=/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  -cp "target/classes:src/main/resources" \
  show2pc.Main
```

如果要指定 VLC：

```bash
java \
  "-Dshow2pc.player=/Applications/VLC.app/Contents/MacOS/VLC" \
  -cp "target/classes:src/main/resources" \
  show2pc.Main
```

macOS 首次运行时，系统可能会提示允许网络访问。需要允许局域网访问，否则手机无法发现 `Display2(用户名-Mac)`。

## 手机投屏使用方式

1. 启动 Display2Computer。
2. 确认电脑和手机在同一个 Wi-Fi。
3. 打开 B 站、央视频等 App。
4. 播放一个视频。
5. 点击投屏按钮。
6. 选择类似 `Display2(jiaxiaogang-Win)` 的设备名。
7. 电脑端会用 Chrome/指定播放器打开投屏 URL。

成功时日志中会出现：

```text
SOAP /upnp/control/AVTransport#SetAVTransportURI
Set URI http://...
SOAP /upnp/control/AVTransport#Play
Started external player: [...]
```

## 状态页和日志

服务启动后，本地状态页：

```text
http://localhost:49152/
```

状态页会显示：

- 设备名
- HTTP 端口
- 局域网访问地址
- 当前播放器路径
- 是否全屏播放
- 当前播放 URL
- 最近投屏/控制日志

常见日志含义：

```text
SSDP M-SEARCH ...
```

手机或路由器正在搜索投屏设备。

```text
SSDP response sent ...
```

Display2Computer 已经回复设备发现请求。

```text
HTTP GET /device.xml
```

手机已经发现 Display2Computer，并读取设备描述。

```text
HTTP GET /upnp/avtransport-scpd.xml
```

手机正在检查播放控制能力。

```text
SOAP /upnp/control/AVTransport#SetAVTransportURI
```

手机已经把视频 URL 发给电脑。

```text
SOAP /upnp/control/AVTransport#Play
```

手机要求电脑开始播放。

## 常见问题

### 手机投屏列表里看不到 Display2(用户名-系统类型)

检查：

1. 手机和电脑是否在同一个 Wi-Fi。
2. 手机能否打开电脑状态页，例如：

   ```text
   http://192.168.1.37:49152/
   ```

3. Windows 防火墙是否允许 Java 入站。
4. 当前网络是否是“专用网络”，不是“公用网络”。
5. 路由器是否开启了访客网络或 AP 隔离。

### 能发现 Display2(用户名-系统类型)，但点投屏没反应

看日志里有没有：

```text
SetAVTransportURI
Play
```

如果没有，说明 App 还没有下发播放 URL，可能是设备能力校验或该视频不支持 DLNA 投屏。

如果有 `Set URI` 和 `Play`，但没播放，通常是播放器路径或 URL 兼容问题。

### 提示找不到 vlc

早期版本默认使用 `vlc`。现在 Windows 默认优先使用 Chrome。

如果仍然提示：

```text
Cannot run program "vlc"
```

请重新编译/运行最新代码，或手动指定播放器：

```powershell
"-Dshow2pc.player=C:\Program Files\Google\Chrome\Application\chrome.exe"
```

### 端口被占用

如果看到：

```text
Address already in use: bind
```

说明已有 Display2Computer 在运行。关闭旧窗口，或按：

```text
Ctrl + C
```

也可以临时换端口：

```powershell
"-Dshow2pc.httpPort=49153"
```

### 为什么不用 Windows 自带播放器

Windows Media Player 对这类移动 App 投过来的长 URL、HLS/MP4、特殊参数兼容性不如 Chrome/VLC/mpv。当前 MVP 默认用 Chrome 是为了优先保证能打开 B 站等 App 下发的视频 URL。

### Chrome 播放时不能响应暂停/进度/音量

当前版本只是把 URL 打开到 Chrome。DLNA 控制命令已经能收到，但 Chrome 不提供简单稳定的外部控制接口。

后续如果需要完整遥控能力，建议改用 VLC 或 mpv，并通过它们的 IPC/HTTP 控制接口实现暂停、停止、Seek 和音量。

## 当前限制

- 不支持 Miracast 屏幕镜像。
- 不实现 Chromecast 协议。
- DRM、会员专属、需要 Cookie 或特殊鉴权的视频可能无法播放。
- Chrome 后端主要用于打开 URL，不适合精细控制播放。
- 当前没有图形界面和系统托盘。

## 开发说明

项目是纯 Java 实现，当前不依赖第三方库。

主要模块：

```text
src/main/java/show2pc/ssdp        SSDP 发现
src/main/java/show2pc/upnp        HTTP、SOAP、设备描述
src/main/java/show2pc/services    UPnP 服务实现
src/main/java/show2pc/player      播放器封装
src/main/resources/upnp           SCPD XML
```

构建产物目录：

```text
target/
```

该目录已在 `.gitignore` 中排除。
