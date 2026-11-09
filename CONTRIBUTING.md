# Contributing / 贡献

Report the Minecraft version, loader, mod version, reproduction steps and relevant logs in Issues. Remove personal information from logs.

请在 Issues 提供 Minecraft 版本、加载器、模组版本、复现步骤与相关日志，去掉日志中的私人信息。

Use short role-based class names under `dev.flower.eat`. Keep code shared unless a Minecraft or loader API requires a version bridge. Explain non-obvious behavior with equivalent Chinese and English comments.

使用职责明确的短类名，包名为 `dev.flower.eat`。通用逻辑保持共享，版本或加载器 API 差异放入对应桥接。非显然行为使用中英等义注释。

Build affected loaders, check feeding effects and item counts, and test both configuration settings and client/server behavior when relevant. Keep generated output, private tools and game worlds out of commits.

构建受影响的端，核对喂食效果和物品数量；改动涉及配置或联机时验证对应行为。提交中不包含构建输出、私人工具或游戏存档。
