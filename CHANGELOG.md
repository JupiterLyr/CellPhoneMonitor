# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4.2]
### Changed
- 更换了 App 图标
### Fixed
- 在电流和功率的显示逻辑上做了优化，将原先的电流标量改为了标记“充”“放”，功率改为绝对值
- 修复了部分配置问题
### Removed
- 取消了防熄屏机制，防止长时间开启监视器导致不必要的耗电与发热

## [1.4.0]
### Added
- 新增屏幕刷新率窗口
### Fixed
- 修复了部分 UI
- 修复了电流值、功率值显示错误的 bug

## [1.3.0]
### Added
- 配置了 App 图标，并修复了部分潜在的问题

## [1.2.0]
完成了基本功能，调整了 UI 设计

## [1.0.0 ~ 1.1.0]
继承自项目 `Phone Warmer`，并在此基础上逐步抽离成监测器
