# SYNTHESIS_WORKFLOW_STATUS

## 项目简介

本项目实现了一个面向智能合约的**自动化合成与验证平台**，支持从高层Datalog规则和时序属性出发，自动合成多个Solidity合约候选，并通过形式化验证工具链对其进行性质验证和通过率评测。整个流程高度自动化，适合大规模实验、对比和可复现性研究。

---

## 当前功能与流程

### 1. 输入
- **Datalog规则**：`synthesis-benchmark/<benchmark>/<benchmark>.dl`
- **时序属性**：`synthesis-benchmark/<benchmark>/temporal_properties.txt`

### 2. 合约自动合成
- 通过Scala合成器（`SynthesizerWithoutTrace`）自动生成多个Solidity合约候选（如5个），每个候选合约均严格基于输入规则和属性，无需依赖example_trace。
- 所有合成均通过Scala工具链完成，**不允许Python模板或兜底合约**。

### 3. Trace自动生成
- 对每个候选合约，自动调用`tmp.py`脚本，解析合约接口并生成example_traces.txt（合约操作轨迹），用于后续验证。

### 4. 性质验证与通过率评测
- 对每个合约及其trace，调用Scala验证器（`VerifierWrapper`）进行性质验证，统计通过率（PASSING_RATE）。
- 验证失败时直接记为0分，不允许任何Python模拟分数或兜底。

### 5. 报告与统计
- 自动汇总所有候选合约的通过率，输出平均、最好、最差通过率等统计信息。
- 生成详细的JSON/文本报告，便于后续分析和复现实验。

---

## 工具链与关键脚本

- **Scala合成器**：`SynthesizerWithoutTrace.scala`，负责合约自动合成。
- **Scala验证器**：`VerifierWrapper.scala`，负责合约性质验证与通过率统计。
- **Python自动化脚本**：
  - `synthesis_without_trace.py`：全自动合成-生成trace-验证-报告一体化流程，**所有核心环节均依赖Scala工具链**。
  - `test_synthesis_workflow.py`：工作流测试脚本，确保pipeline全流程可用，**无Python兜底**。
  - `tmp.py`：合约trace生成工具。

---

## 可复现性与实验保障

- **所有合约候选和验证结果均由Scala工具链生成**，无任何Python fallback、模板合约或模拟分数。
- 合成失败或验证失败的候选直接丢弃或记为0分，保证实验严谨性。
- 所有实验流程、参数、结果均可通过自动化脚本复现。

---

## 使用方法（示例）

1. 运行全自动合成与验证：
   ```bash
   python synthesis_without_trace.py <benchmark_name>
   ```
2. 或运行工作流测试：
   ```bash
   python test_synthesis_workflow.py <benchmark_name>
   ```
3. 查看`synthesis_output/`或`candidates/`目录下的合约、trace和报告。

---

## 适用场景
- 智能合约自动合成与形式化验证实验
- 大规模基准测试与对比
- 可复现性研究与论文实验

---

## 注意事项
- 需确保Scala环境、Z3依赖、相关jar/class文件齐全。
- 所有核心环节均依赖Scala工具链，**无Python兜底**。
- 合成与验证失败的候选不会被兜底或模拟，保证实验结果真实可靠。

---

如需详细使用说明、参数配置或遇到环境问题，请查阅README或联系开发者。 