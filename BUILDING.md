# Build / 构建

Keep `common` beside the three loader projects. Run the Gradle Wrapper from the desired loader directory. The shared source is compiled together with that loader’s version bridge.

保留 `common` 与三个加载器工程的相邻目录关系，在对应加载器目录运行 Gradle Wrapper。

| Project / 工程 | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| `forge-1.20.1` | 1.20.1 | Forge 47.4.10 | 17 |
| `fabric-1.20.1` | 1.20.1 | Fabric Loader 0.16.14, Fabric API 0.92.6+1.20.1 | 17 |
| `neoforge-1.21.1` | 1.21.1 | NeoForge 21.1.234 | 21 |

Install JDK 17 and 21. Gradle can run with JDK 21 and select the target toolchain.

安装 JDK 17 与 21，Gradle 可使用 JDK 21 运行并选择对应工具链。

```sh
cd forge-1.20.1
./gradlew build
```

On Windows use `gradlew.bat build`. Output: `<loader-project>/build/libs/`.

Windows 使用 `gradlew.bat build`，产物在对应工程的 `build/libs/`。

Client / 客户端：`gradlew runClient`

Server / 服务器：`gradlew runServer`

Development checks / 开发验证：Forge/NeoForge `gradlew runServer -PeatVerify`；Fabric `gradlew runVerification -PeatVerify`。Accept the Minecraft EULA in the test run directory before running a test server. These checks create a flat test world and stop the server after assertions. Inspect `eat-native-result.json` as well as Gradle’s exit status.

验证会在独立目录生成超平坦测试世界并在断言结束后停服。运行测试服前需接受 Minecraft EULA，完成后检查 `eat-native-result.json`，不能仅看 Gradle 的退出码。
