# 拾色（ColorPick）

一款轻量级相机调色 App：原生 Android（Kotlin + Jetpack Compose + CameraX），复刻 iOS 原生相机的交互与视觉语言，内置 11×11 点阵调色盘与左右两类滤镜，GPU 实时渲染（OpenGL ES 2.0）。

## 功能

- **相机拍摄**：1:1 / 3:4 / 16:9 画幅切换、变焦条 + 半圆轮盘变焦、双指缩放、点按对焦/曝光补偿、前后摄切换、闪光灯
- **滤镜**：标准区 5-6 种基础风格（玫瑰金、琥珀、金色、冷调玫瑰、中性等）+ 心情区 9 种风格（鲜明、反差色、飘渺、温馨、热烈、浪漫等），支持 0-100 强度调节
- **调色盘**：11×11 点阵，明暗/饱和度 -100 ~ +100 连续可调，点阵吸附档位
- **相册**：App 内置相册，5 列宫格、日期倒序、下滑分页加载，支持多选/全选/滑动连续选择、批量删除
- **照片存储**：保存至 `Pictures/ColorPick`（系统相册可见），拍摄页缩略图仅显示 App 拍摄的照片

## 技术栈

- Kotlin + Jetpack Compose + CameraX
- OpenGL ES 2.0 GPU 渲染（CameraFrameRenderer / PhotoRenderer / LutGenerator）
- MediaStore 相册管理（兼容 Android 10+ RELATIVE_PATH）

## 版本

当前版本：beta 0.2.02（versionCode 201）

## 文档

- [log.md](log.md) — 版本更新日志
- [style.md](style.md) — UI 设计规范
