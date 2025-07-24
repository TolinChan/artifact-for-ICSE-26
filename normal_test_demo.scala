import synthesis.StateMachine
import com.microsoft.z3.{Context, BoolSort, ArithSort}
import com.microsoft.z3.{Expr => Z3Expr}

object NormalTestDemo {
  def main(args: Array[String]): Unit = {
    println("=== 正常测试：Simulate 和 Synthesis ===")
    
    val ctx = new Context()
    val sm = new StateMachine("NormalTest", ctx)
    
    // 设置状态机
    setupStateMachine(sm, ctx)
    
    // 测试1: Simulate功能
    println("\n🔍 测试1: Simulate功能")
    testSimulate(sm, ctx)
    
    // 测试2: Synthesis功能
    println("\n🔍 测试2: Synthesis功能")
    testSynthesis(sm, ctx)
    
    ctx.close()
  }
  
  def setupStateMachine(sm: StateMachine, ctx: Context): Unit = {
    // 添加状态变量
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    val (totalSupply, totalSupplyOut) = sm.addState("totalSupply", ctx.mkIntSort())
    
    // 设置初始状态
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(balance, ctx.mkInt(1000)),
      ctx.mkEq(totalSupply, ctx.mkInt(1000))
    ))
    
    // 添加transfer转换
    val transferParams = List(ctx.mkIntConst("amount"))
    val transferGuard = ctx.mkTrue() // 初始为true，将由synthesis学习
    val transferFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount"))),
      ctx.mkEq(totalSupplyOut, totalSupply)
    )
    sm.addTr("transfer", transferParams, transferGuard, transferFunc)
    
    // 添加mint转换
    val mintParams = List(ctx.mkIntConst("mintAmount"))
    val mintGuard = ctx.mkTrue() // 初始为true，将由synthesis学习
    val mintFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("mintAmount"))),
      ctx.mkEq(totalSupplyOut, ctx.mkAdd(totalSupply.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("mintAmount")))
    )
    sm.addTr("mint", mintParams, mintGuard, mintFunc)
    
    sm.addOnce()
  }
  
  def testSimulate(sm: StateMachine, ctx: Context): Unit = {
    // 创建正常的轨迹
    val trace = List(
      List(
        ctx.mkString("mint").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("mintAmount"), ctx.mkInt(100)).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Z3Expr[ArithSort]], sm.now.asInstanceOf[Z3Expr[ArithSort]]).asInstanceOf[Z3Expr[BoolSort]]
      ),
      List(
        ctx.mkString("transfer").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(50)).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Z3Expr[ArithSort]], sm.now.asInstanceOf[Z3Expr[ArithSort]]).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    // 创建候选条件
    val candidates = Map(
      "mint" -> List(
        ctx.mkTrue().asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(ctx.mkIntConst("mintAmount"), ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]]
      ),
      "transfer" -> List(
        ctx.mkTrue().asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(sm.states("balance")._1.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGe(sm.states("balance")._1.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    println("📥 Simulate输入:")
    println(s"  轨迹: ${trace.map(_.map(_.toString))}")
    println(s"  候选条件: ${candidates.map{case (k,v) => k -> v.map(_.toString)}}")
    
    // 执行simulate
    val result = sm.simulate(trace, candidates)
    
    println("📤 Simulate输出:")
    println(s"  结果: ${result.map(_.map(_.toString))}")
    println(s"  结果长度: ${result.length}")
    
    if (result.nonEmpty) {
      println("✅ Simulate测试成功 - 生成了模拟结果")
    } else {
      println("❌ Simulate测试失败 - 没有生成结果")
    }
  }
  
  def testSynthesis(sm: StateMachine, ctx: Context): Unit = {
    // 创建正轨迹：正常的行为
    val positiveTrace1 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("mintAmount"), ctx.mkInt(100)).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(ctx.mkIntConst("mintAmount"), ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    val positiveTrace2 = List(
      List(
        ctx.mkString("transfer").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(50)).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(sm.states("balance")._1.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    // 创建负轨迹：错误的行为
    val negativeTrace1 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("mintAmount"), ctx.mkInt(-50)).asInstanceOf[Z3Expr[BoolSort]], // 负数铸造
        ctx.mkLt(ctx.mkIntConst("mintAmount"), ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    val negativeTrace2 = List(
      List(
        ctx.mkString("transfer").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(2000)).asInstanceOf[Z3Expr[BoolSort]], // 超额转账
        ctx.mkGt(ctx.mkIntConst("amount"), sm.states("balance")._1.asInstanceOf[Z3Expr[ArithSort]]).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    val pos = List(positiveTrace1, positiveTrace2)
    val neg = List(negativeTrace1, negativeTrace2)
    
    // 创建候选条件
    val candidates = Map(
      "mint" -> List(
        ctx.mkTrue().asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(ctx.mkIntConst("mintAmount"), ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkLe(ctx.mkIntConst("mintAmount"), ctx.mkInt(10000)).asInstanceOf[Z3Expr[BoolSort]]
      ),
      "transfer" -> List(
        ctx.mkTrue().asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkGe(sm.states("balance")._1.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")).asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkLe(ctx.mkIntConst("amount"), ctx.mkInt(1000)).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("mint") = candidates("mint")
    sm.candidateConditionGuards("transfer") = candidates("transfer")
    
    println("📥 Synthesis输入:")
    println(s"  正轨迹数量: ${pos.length}")
    pos.zipWithIndex.foreach { case (trace, idx) =>
      println(s"    正轨迹${idx + 1}: ${trace.map(_.map(_.toString))}")
    }
    println(s"  负轨迹数量: ${neg.length}")
    neg.zipWithIndex.foreach { case (trace, idx) =>
      println(s"    负轨迹${idx + 1}: ${trace.map(_.map(_.toString))}")
    }
    println(s"  候选条件: ${candidates.map{case (k,v) => k -> v.map(_.toString)}}")
    
    // 执行synthesis
    sm.synthesize(pos, neg, candidates)
    
    println("📤 Synthesis输出:")
    println(s"  学习到的保护条件:")
    sm.conditionGuards.foreach { case (trName, guard) =>
      println(s"    $trName: $guard")
    }
    
    // 验证学习结果
    val mintGuard = sm.conditionGuards.get("mint")
    val transferGuard = sm.conditionGuards.get("transfer")
    
    if (mintGuard.isDefined && transferGuard.isDefined) {
      println("✅ Synthesis测试成功 - 学习到了保护条件")
      println(s"  mint约束: ${mintGuard.get}")
      println(s"  transfer约束: ${transferGuard.get}")
    } else {
      println("❌ Synthesis测试失败 - 没有学习到保护条件")
    }
  }
} 