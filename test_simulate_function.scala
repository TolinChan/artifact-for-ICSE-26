import com.microsoft.z3._
import synthesis.StateMachine
import scala.collection.mutable.ListBuffer

object TestSimulateFunction {
  def main(args: Array[String]): Unit = {
    // 创建Z3上下文
    val ctx = new Context()
    
    // 创建状态机实例
    val sm = new StateMachine("TestContract", ctx)
    
    println("=== 状态机测试初始化 ===")
    
    // 添加状态变量
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    val (owner, ownerOut) = sm.addState("owner", ctx.mkIntSort()) 
    val (totalSupply, totalSupplyOut) = sm.addState("totalSupply", ctx.mkIntSort())
    val (paused, pausedOut) = sm.addState("paused", ctx.mkBoolSort())
    
    println(s"添加状态变量: balance, owner, totalSupply, paused")
    
    // 创建一些参数变量
    val amount = ctx.mkIntConst("amount").asInstanceOf[Expr[ArithSort]]
    val recipient = ctx.mkIntConst("recipient").asInstanceOf[Expr[ArithSort]]
    val sender = ctx.mkIntConst("sender").asInstanceOf[Expr[ArithSort]]
    val value = ctx.mkIntConst("value").asInstanceOf[Expr[ArithSort]]
    
    // 添加转换函数
    println("添加转换函数...")
    
    // mint转换
    sm.addTr("mint", 
      List(recipient, amount), 
      ctx.mkTrue(), 
      ctx.mkAnd(
        ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Expr[ArithSort]], amount)),
        ctx.mkEq(totalSupplyOut, ctx.mkAdd(totalSupply.asInstanceOf[Expr[ArithSort]], amount))
      )
    )
    
    // transfer转换
    sm.addTr("transfer", 
      List(sender, recipient, amount), 
      ctx.mkGt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0)), 
      ctx.mkAnd(
        ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], amount)),
        ctx.mkGt(amount, ctx.mkInt(0))
      )
    )
    
    // pause转换
    sm.addTr("pause", 
      List(), 
      ctx.mkTrue(), 
      ctx.mkEq(pausedOut.asInstanceOf[Expr[_]], ctx.mkTrue())
    )
    
    // unpause转换
    sm.addTr("unpause", 
      List(), 
      ctx.mkTrue(), 
      ctx.mkEq(pausedOut.asInstanceOf[Expr[_]], ctx.mkFalse())
    )
    
    println("转换函数添加完成")
    
    // 设置初始状态
    val initState = ctx.mkAnd(
      ctx.mkEq(balance, ctx.mkInt(0)),
      ctx.mkEq(owner, ctx.mkInt(1)),
      ctx.mkEq(totalSupply, ctx.mkInt(0)),
      ctx.mkEq(paused.asInstanceOf[Expr[BoolSort]], ctx.mkFalse())
    )
    sm.setInit(initState)
    
    println("初始状态设置完成")
    
    // 创建丰富的候选条件
    println("\n=== 创建候选条件 ===")
    val candidates = createRichCandidates(ctx, sm, amount, recipient, sender, value, balance, owner, totalSupply, paused)
    
    // 创建测试轨迹
    println("\n=== 创建测试轨迹 ===")
    val testTraces = createTestTraces(ctx, amount, recipient, sender, value)
    
    // 运行测试
    println("\n=== 运行simulate函数测试 ===")
    runSimulateTests(sm, testTraces, candidates)
    
    // 清理资源
    ctx.close()
  }
  
  def createRichCandidates(ctx: Context, sm: StateMachine, amount: Expr[ArithSort], recipient: Expr[ArithSort], 
                          sender: Expr[ArithSort], value: Expr[ArithSort], balance: Expr[_], owner: Expr[_], 
                          totalSupply: Expr[_], paused: Expr[_]): Map[String, List[Expr[BoolSort]]] = {
    
    val candidatesMap = scala.collection.mutable.Map[String, List[Expr[BoolSort]]]()
    
    // mint函数的候选条件
    val mintCandidates = List(
      ctx.mkTrue(),  // 总是允许
      ctx.mkFalse(), // 总是拒绝
      ctx.mkGt(amount, ctx.mkInt(0)),  // 金额必须大于0
      ctx.mkLt(amount, ctx.mkInt(1000000)),  // 金额必须小于1000000
      ctx.mkEq(recipient, ctx.mkInt(1)),  // 只能给owner mint
      ctx.mkGe(amount, ctx.mkInt(1)),  // 最小mint金额
      ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]]),  // 未暂停状态
      ctx.mkAnd(ctx.mkGt(amount, ctx.mkInt(0)), ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]])) // 组合条件
    )
    candidatesMap("mint") = mintCandidates
    
    // transfer函数的候选条件  
    val transferCandidates = List(
      ctx.mkTrue(),
      ctx.mkFalse(),
      ctx.mkGt(amount, ctx.mkInt(0)),  // 转账金额大于0
      ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], amount),  // 余额足够
      ctx.mkNot(ctx.mkEq(sender, recipient)),  // 发送者和接收者不同
      ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]]),  // 未暂停
      ctx.mkLe(amount, ctx.mkInt(10000)),  // 单次转账限额
      ctx.mkAnd(ctx.mkGt(amount, ctx.mkInt(0)), ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], amount)), // 基本转账条件
      ctx.mkAnd(ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]]), ctx.mkNot(ctx.mkEq(sender, recipient))), // 组合业务规则
      ctx.mkGt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(100))  // 最小余额要求
    )
    candidatesMap("transfer") = transferCandidates
    
    // pause函数的候选条件
    val pauseCandidates = List(
      ctx.mkTrue(),
      ctx.mkFalse(),
      ctx.mkEq(sender, owner.asInstanceOf[Expr[ArithSort]]),  // 只有owner可以暂停
      ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]]),  // 当前未暂停
      ctx.mkAnd(ctx.mkEq(sender, owner.asInstanceOf[Expr[ArithSort]]), ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]])) // 组合条件
    )
    candidatesMap("pause") = pauseCandidates
    
    // unpause函数的候选条件
    val unpauseCandidates = List(
      ctx.mkTrue(),
      ctx.mkFalse(), 
      ctx.mkEq(sender, owner.asInstanceOf[Expr[ArithSort]]),  // 只有owner可以解除暂停
      paused.asInstanceOf[Expr[BoolSort]],  // 当前已暂停
      ctx.mkAnd(ctx.mkEq(sender, owner.asInstanceOf[Expr[ArithSort]]), paused.asInstanceOf[Expr[BoolSort]]) // 组合条件
    )
    candidatesMap("unpause") = unpauseCandidates
    
    println(s"为mint创建了${mintCandidates.length}个候选条件")
    println(s"为transfer创建了${transferCandidates.length}个候选条件")
    println(s"为pause创建了${pauseCandidates.length}个候选条件")
    println(s"为unpause创建了${unpauseCandidates.length}个候选条件")
    
    candidatesMap.toMap
  }
  
  def createTestTraces(ctx: Context, amount: Expr[ArithSort], recipient: Expr[ArithSort], 
                      sender: Expr[ArithSort], value: Expr[ArithSort]): List[(String, List[List[Expr[BoolSort]]])] = {
    
    val traces = ListBuffer[(String, List[List[Expr[BoolSort]]])]()
    
    // 正面轨迹1: 简单的mint操作
    val positiveTrace1 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(100)).asInstanceOf[Expr[BoolSort]]
      )
    )
    traces += (("正面轨迹1: 简单mint", positiveTrace1))
    
    // 正面轨迹2: mint后transfer
    val positiveTrace2 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(1000)).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("transfer").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(2)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(500)).asInstanceOf[Expr[BoolSort]]
      )
    )
    traces += (("正面轨迹2: mint后transfer", positiveTrace2))
    
    // 正面轨迹3: 复杂的pause/unpause操作
    val positiveTrace3 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(2000)).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("pause").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("unpause").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("transfer").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(3)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(300)).asInstanceOf[Expr[BoolSort]]
      )
    )
    traces += (("正面轨迹3: pause/unpause序列", positiveTrace3))
    
    // 负面轨迹1: 金额为0的mint
    val negativeTrace1 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(0)).asInstanceOf[Expr[BoolSort]]
      )
    )
    traces += (("负面轨迹1: 金额为0的mint", negativeTrace1))
    
    // 负面轨迹2: 余额不足的transfer
    val negativeTrace2 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(100)).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("transfer").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(2)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(500)).asInstanceOf[Expr[BoolSort]]  // 尝试转账500，但只有100
      )
    )
    traces += (("负面轨迹2: 余额不足的transfer", negativeTrace2))
    
    // 负面轨迹3: 在暂停状态下尝试transfer
    val negativeTrace3 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(1000)).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("pause").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("transfer").asInstanceOf[Expr[BoolSort]],  // 在暂停状态下尝试转账
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(recipient, ctx.mkInt(2)).asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(amount, ctx.mkInt(200)).asInstanceOf[Expr[BoolSort]]
      )
    )
    traces += (("负面轨迹3: 暂停状态下transfer", negativeTrace3))
    
    // 边界测试轨迹: 空轨迹
    val emptyTrace = List[List[Expr[BoolSort]]]()
    traces += (("边界测试: 空轨迹", emptyTrace))
    
    // 边界测试轨迹: 单步轨迹
    val singleStepTrace = List(
      List(
        ctx.mkString("pause").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(sender, ctx.mkInt(1)).asInstanceOf[Expr[BoolSort]]
      )
    )
    traces += (("边界测试: 单步轨迹", singleStepTrace))
    
    println(s"创建了${traces.length}个测试轨迹")
    traces.toList
  }
  
  def runSimulateTests(sm: StateMachine, testTraces: List[(String, List[List[Expr[BoolSort]]])], 
                      candidates: Map[String, List[Expr[BoolSort]]]): Unit = {
    
    var testCount = 0
    var passedCount = 0
    
    testTraces.foreach { case (traceName, trace) =>
      testCount += 1
      println(s"\n--- 测试 $testCount: $traceName ---")
      
      try {
        println(s"轨迹长度: ${trace.length}")
        trace.zipWithIndex.foreach { case (step, index) =>
          println(s"  步骤 $index: ${step.head.toString} (${step.length - 1} 个参数)")
        }
        
        // 调用simulate函数
        val startTime = System.nanoTime()
        val result = sm.simulate(trace, candidates)
        val endTime = System.nanoTime()
        val duration = (endTime - startTime) / 1e6 // 转换为毫秒
        
        println(s"执行时间: ${duration}ms")
        println(s"结果长度: ${result.length}")
        
        // 分析结果
        if (result.nonEmpty) {
          println("结果详情:")
          result.zipWithIndex.foreach { case (step, index) =>
            println(s"  结果步骤 $index: 函数=${step.head.toString}, 候选条件数=${step.length - 1}")
            if (step.length > 1) {
              step.tail.take(3).zipWithIndex.foreach { case (candidate, candIndex) =>
                println(s"    候选条件 $candIndex: ${candidate.toString}")
              }
              if (step.length > 4) {
                println(s"    ... 还有${step.length - 4}个候选条件")
              }
            }
          }
        } else {
          println("结果为空")
        }
        
        // 基本验证
        val expectedLength = if (trace.isEmpty) 0 else trace.length
        if (result.length == expectedLength) {
          println("✅ 长度验证通过")
          passedCount += 1
        } else {
          println(s"❌ 长度验证失败: 期望 $expectedLength, 实际 ${result.length}")
        }
        
        // 验证结果结构
        if (result.nonEmpty) {
          val allStepsValid = result.zipWithIndex.forall { case (step, index) =>
            if (step.nonEmpty) {
              val functionName = step.head.toString.replaceAll("\"", "")
              val expectedFunctionName = trace(index).head.toString.replaceAll("\"", "")
              if (functionName == expectedFunctionName) {
                println(s"✅ 步骤 $index 函数名匹配: $functionName")
                true
              } else {
                println(s"❌ 步骤 $index 函数名不匹配: 期望 '$expectedFunctionName', 实际 '$functionName'")
                false
              }
            } else {
              println(s"❌ 步骤 $index 结果为空")
              false
            }
          }
          
          if (allStepsValid) {
            println("✅ 结构验证通过")
          } else {
            println("❌ 结构验证失败")
          }
        }
        
      } catch {
        case e: Exception =>
          println(s"❌ 测试异常: ${e.getMessage}")
          e.printStackTrace()
      }
      
      println(s"--- 测试 $testCount 完成 ---")
    }
    
    println(s"\n=== 测试总结 ===")
    println(s"总测试数: $testCount")
    println(s"通过测试数: $passedCount")
    println(s"失败测试数: ${testCount - passedCount}")
    println(s"成功率: ${if (testCount > 0) f"${passedCount * 100.0 / testCount}%.1f" else "0.0"}%")
    
    if (passedCount == testCount) {
      println("🎉 所有测试通过!")
    } else {
      println("⚠️  部分测试失败，需要进一步调试")
    }
  }
} 