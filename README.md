# Mona Slimefun Essentials (Fabric)

Minecraft 26.1.2 / Java 25向けのクライアントModです。対応サーバープラグインからSlimefun4とアドオンのアイテム・レシピを同期し、JEI（および既存REI互換層）へ登録します。

## 主な機能

- 同じMinecraftベースアイテムを使うSlimefunアイテムをSF IDで区別
- JEI一覧、Rキーの製作方法、Uキーの使用用途
- JEIの「＋」から、開いているドロッパー／ディスペンサーへ3×3レシピを安全に自動配置
- SlimeTinker、FluffyMachines、DankTech2を含むアドオン独自RecipeTypeの同期
- 3×3〜9×9の可変レシピ表示
- v2通信のgzip、SHA-256、サイズ制限、破損拒否
- 接続アドレスとサーバーUUIDごとのキャッシュ分離
- 英語・日本語UI

## 必要環境

- Minecraft 26.1.2
- Fabric Loader 0.19.4以降
- Fabric API 0.155.2+26.1.2
- Java 25
- JEI 29.47.0.111（26.1.2 Fabric版）
- サーバー側MonaSlimefunServerEssentials

Mod Menu、Cloth Config、Jade、REIは任意です。EMIソースは対応版が確定するまでビルド対象外です。

## ビルド

```powershell
.\gradlew.bat clean test build --no-daemon
```

出力: `build/libs/MonaSlimefunEssentials-26.1.2-fabric.jar`

## 状態

クリーンビルドと自動テストは成功しています。Paper/Fabric間の実接続およびJEIの目視操作はまだ未検証です。本番投入前にステージング試験を行ってください。

MITライセンス。フォーク元のLICENSEとGit履歴を保持しています。
