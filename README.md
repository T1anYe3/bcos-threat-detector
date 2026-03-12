# bcos-threat-detector

一个面向 FISCO BCOS 联盟链的**多维安全态势感知与威胁检测系统**，覆盖：

- 智能合约安全
- 交易模型安全
- 区块处理安全
- 共识安全
- 账户安全
- 网络安全

本项目实现了从**链上实时数据采集 → 多阶段安全检测 → 可视化大屏展示**的一体化流程，支持实时检测与历史 CSV 数据离线分析两种模式。

---

## 🌐 功能概览

- **实时链上数据采集**
  - 基于 FISCO BCOS Java SDK，持续拉取区块、交易与回执
  - 将交易写入本地数据库 `blockchain_data.db`
  - 同步链上共识节点白名单（`nodes` 表）

- **语义解码与语义安全检测**
  - 对交易 `input_data` 做语义解码：识别部署 / 原生转账 / 函数调用
  - 提取 `method_id`、`method_name`、`is_sensitive`、`decode_success`、参数摘要等特征
  - 语义规则检测：敏感函数调用失败、未知函数大载荷等
  - 语义异常检测：函数调用在区块维度的突发“放大”行为

- **交易模型安全检测**
  - 规则检测：Gas 超限、输入长度异常、状态码异常、Gas 使用比例异常
  - 统计与机器学习：
    - Isolation Forest（孤立森林）
    - Local Outlier Factor（LOF）
    - Elliptic Envelope（协方差异常）
    - Autoencoder（MLP 重构误差）
  - 多检测器加权融合：统一计算 `ensemble_score` 与风险等级

- **区块处理与共识安全**
  - 区块级行为：
    - 交易量突发、失败风暴、异常区块窗口
  - 共识安全：
    - 从链上动态同步共识节点白名单（Sealer）
    - 识别由非白名单节点打包的可疑区块/交易

- **账户安全检测**
  - 按发送方账户统计 `nonce` 序列
  - 识别 `nonce` 回退、大跳变以及高比例异常序列段
  - 对典型异常样本提升风险得分并输出详细证据链

- **网络安全观测**
  - 在区块采集过程记录：
    - `rpc_latency_ms`（RPC 延迟）
    - `block_interval_ms`（区块时间间隔）
    - `fail_rate`（区块交易失败率）
  - 检测网络层压力与异常波动，并与链上异常联合分析

- **可视化态势前端**
  - 实时模式：展示链上交易流、风险告警、TPS/Gas 曲线、节点拓扑、威胁分布等
  - 历史模式：上传 CSV 文件进行离线分析与回溯

---

## 🧱 目录结构

```text
.
├─ pom.xml                         # Maven 配置（FISCO BCOS SDK 等依赖）
├─ config.toml                     # FISCO BCOS 连接配置
├─ blockchain_data.db              # 本地 SQLite 数据库（运行后生成）
├─ 项目分析与运行文档.md          # 中文项目说明与运行文档
└─ src
   └─ main
      ├─ java
      │  └─ org/example
      │     ├─ RealTimeMonitor.java         # 链上实时采集 + 网络观测
      │     ├─ DBUtil.java                  # DB 初始化与写入（transactions / semantic_calls / network_metrics）
      │     ├─ NodeInfoCollector.java       # 动态同步共识白名单到 nodes 表
      │     ├─ InstructionAnalyzer.java     # 语义解码层（函数/参数解析）
      │     ├─ GenericDecoder.java          # 未知函数盲解辅助
      │     ├─ app_json.py                  # Flask 后端（API + 大屏资源）
      │     ├─ datadetecter.py             # 多阶段检测引擎（规则 + ML + 语义 + 网络）
      │     ├─ dashboard_json.html          # 态势感知大屏前端
      │     └─ static
      │        ├─ vue.min.js
      │        ├─ echarts.min.js
      │        └─ axios.min.js
      └─ resources
            └─ config.toml                     # 另一份链配置（按实际情况放置）
```
---

## Quick Start
**1. 环境准备**
Java 11+ & Maven 已安装
Python 3.10+ 已安装
pip install flask flask-cors pandas numpy scikit-learn
确认链配置 config.toml 正确指向你的 FISCO BCOS 节点。

**2. 启动链上实时采集**
在项目根目录：

mvn -DskipTests exec:java "-Dexec.mainClass=org.example.RealTimeMonitor"
或在 IDE 中运行 RealTimeMonitor.main()。

**3. 启动检测后端与可视化**
python src/main/java/org/example/app_json.py
浏览器访问：

http://127.0.0.1:5000
         
