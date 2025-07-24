import synthesis.StateMachine
import com.microsoft.z3.{Context, BoolSort, ArithSort}
import com.microsoft.z3.{Expr => Z3Expr}

object SimpleTestDemo {
  def main(args: Array[String]): Unit = {
    println("=== 简单测试演示：Simulate 和 Synthesis ===")
    
    // 测试1: 简单的Simulate测试
    println("\n🔍 测试1: Simulate - 空轨迹")
    testSimulateEmpty()
    
    // 测试2: 简单的Simulate测试 - 单步轨迹
    println("\n🔍 测试2: Simulate - 单步轨迹")
    testSimulateSingleStep()
    
    // 测试3: 简单的Synthesis测试 - 空轨迹
    println("\n🔍 测试3: Synthesis - 空正负轨迹")
    testSynthesizeEmpty()
    
    // 测试4: 简单的Synthesis测试 - 从正轨迹学习
    println("\n🔍 测试4: Synthesis - 从正轨迹学习")
    testSynthesizePositive()
    
    println("\n✅ 所有测试完成！")
  }
  
  def testSimulateEmpty(): Unit = {
    val ctx = new Context()
    val sm = new StateMachine("EmptyTest", ctx)
    
    // 输入
    val emptyTrace: List[List[Z3Expr[BoolSort]]] = List()
    val emptyCandidates: Map[String, List[Z3Expr[BoolSort]]] = Map()
    
    println("📥 输入:")
    println(s"  轨迹: $emptyTrace")
    println(s"  候选条件: $emptyCandidates")
    
    // 执行simulate
    val result = sm.simulate(emptyTrace, emptyCandidates)
    
    // 输出
    println("📤 输出:")
    println(s"  结果: $result")
    println(s"  结果长度: ${result.length}")
    
    ctx.close()
  }
  
  def testSimulateSingleStep(): Unit = {
    val ctx = new Context()
    val sm = new StateMachine("SingleStepTest", ctx)
    
    // 添加状态变量
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    
    // 设置初始状态
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
    
    // 创建输入轨迹
    val transferAction = ctx.mkString("transfer").asInstanceOf[Z3Expr[BoolSort]]
    val amountConstraint = ctx.mkEq(balance, ctx.mkInt(50)).asInstanceOf[Z3Expr[BoolSort]]
    val timeConstraint = ctx.mkGt(sm.nowOut.asInstanceOf[Z3Expr[ArithSort]], sm.now.asInstanceOf[Z3Expr[ArithSort]]).asInstanceOf[Z3Expr[BoolSort]]
    
    val trace = List(
      List(transferAction, amountConstraint, timeConstraint)
    )
    
    // 创建候选条件
    val candidates = Map(
      "transfer" -> List(
        ctx.mkTrue().asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    println("📥 输入:")
    println(s"  轨迹: ${trace.map(_.map(_.toString))}")
    println(s"  候选条件: ${candidates.map{case (k,v) => k -> v.map(_.toString)}}")
    
    // 执行simulate
    val result = sm.simulate(trace, candidates)
    
    // 输出
    println("📤 输出:")
    println(s"  结果: ${result.map(_.map(_.toString))}")
    println(s"  结果长度: ${result.length}")
    
    ctx.close()
  }
  
  def testSynthesizeEmpty(): Unit = {
    val ctx = new Context()
    val sm = new StateMachine("EmptySynthesisTest", ctx)
    
    // 添加简单的状态和转换
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(0)))
    
    val depositParams = List(ctx.mkIntConst("amount"))
    val depositGuard = ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
    val depositFunc = ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")))
    sm.addTr("deposit", depositParams, depositGuard, depositFunc)
    
    sm.addOnce()
    
    // 输入
    val pos = List[List[List[Z3Expr[BoolSort]]]]()
    val neg = List[List[List[Z3Expr[BoolSort]]]]()
    val candidates = Map[String, List[Z3Expr[BoolSort]]]("deposit" -> List(ctx.mkTrue(), ctx.mkFalse()))
    
    println("📥 输入:")
    println(s"  正轨迹: $pos")
    println(s"  负轨迹: $neg")
    println(s"  候选条件: ${candidates.map{case (k,v) => k -> v.map(_.toString)}}")
    
    // 执行synthesize
    sm.synthesize(pos, neg, candidates)
    
    // 输出
    println("📤 输出:")
    println(s"  保护条件: ${sm.conditionGuards.map{case (k,v) => k -> v.toString}}")
    
    ctx.close()
  }
  
  def testSynthesizePositive(): Unit = {
    val ctx = new Context()
    val sm = new StateMachine("PositiveSynthesisTest", ctx)
    
    // 创建简单的计数器状态机
    val (counter, counterOut) = sm.addState("counter", ctx.mkIntSort())
    sm.setInit(ctx.mkEq(counter, ctx.mkInt(0)))
    
    // 添加increment转换
    val incParams = List()
    val incGuard = ctx.mkTrue()
    val incFunc = ctx.mkEq(counterOut, ctx.mkAdd(counter.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(1)))
    sm.addTr("increment", incParams, incGuard, incFunc)
    
    sm.addOnce()
    
    // 输入
    val posTrace = List(
      List(
        List(
          ctx.mkString("increment").asInstanceOf[Z3Expr[BoolSort]],
          ctx.mkTrue().asInstanceOf[Z3Expr[BoolSort]]
        )
      )
    )
    
    val neg = List[List[List[Z3Expr[BoolSort]]]]()
    val candidates = Map[String, List[Z3Expr[BoolSort]]](
      "increment" -> List(
        ctx.mkTrue(),
        ctx.mkGt(counter.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(-1))
      )
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("increment") = candidates("increment")
    
    println("📥 输入:")
    println(s"  正轨迹: ${posTrace.map(_.map(_.map(_.toString)))}")
    println(s"  负轨迹: $neg")
    println(s"  候选条件: ${candidates.map{case (k,v) => k -> v.map(_.toString)}}")
    
    // 执行synthesize
    sm.synthesize(posTrace, neg, candidates)
    
    // 输出
    println("📤 输出:")
    println(s"  保护条件: ${sm.conditionGuards.map{case (k,v) => k -> v.toString}}")
    
    ctx.close()
  }
} 