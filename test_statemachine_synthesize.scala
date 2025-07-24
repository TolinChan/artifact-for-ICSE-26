import synthesis.StateMachine
import com.microsoft.z3.{Context, Status, BoolExpr, ArithSort, BoolSort, BitVecSort}
import com.microsoft.z3.{Expr => Z3Expr}

object TestStateMachineSynthesize {
  def main(args: Array[String]): Unit = {
    println("Testing StateMachine synthesize functionality...")
    
    // 测试1: 基本合成功能
    println("\n=== Test 1: Basic Synthesis Functionality ===")
    testBasicSynthesis()
    
    // 测试2: 从正轨迹学习
    println("\n=== Test 2: Learning from Positive Traces ===")
    testPositiveTraceLearning()
    
    // 测试3: 从负轨迹学习
    println("\n=== Test 3: Learning from Negative Traces ===")
    testNegativeTraceLearning()
    
    // 测试4: 多转换合�?
    println("\n=== Test 4: Multi-Transition Synthesis ===")
    testMultiTransitionSynthesis()
    
    // 测试5: 复杂约束合成
    println("\n=== Test 5: Complex Constraint Synthesis ===")
    testComplexConstraintSynthesis()
    
    // 测试6: 错误处理
    println("\n=== Test 6: Error Handling ===")
    testErrorHandling()
    
    println("\nAll synthesis tests completed!")
  }
  
  def testBasicSynthesis(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("BasicSynthesis", ctx)
      
      // 创建简单的计数�?
      val (counter, counterOut) = sm.addState("counter", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(counter, ctx.mkInt(0)))
      
      // 添加increment转换
      val incParams = List()
      val incGuard = ctx.mkTrue()
      val incFunc = ctx.mkEq(counterOut, ctx.mkAdd(counter.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(1)))
      sm.addTr("increment", incParams, incGuard, incFunc)
      
      sm.addOnce()
      
      // 空轨迹测�?
      val pos = List[List[List[Z3Expr[BoolSort]]]]()
      val neg = List[List[List[Z3Expr[BoolSort]]]]()
      val candidates = Map[String, List[Z3Expr[BoolSort]]](
        "increment" -> List(ctx.mkTrue(), ctx.mkFalse())
      )
      
      sm.synthesize(pos, neg, candidates)
      
      println("�?Basic synthesis completed without errors")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"�?Basic synthesis failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testPositiveTraceLearning(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("PositiveLearning", ctx)
      
      // 创建余额状态机
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
      
      // 添加deposit转换
      val depositParams = List(ctx.mkIntConst("amount"))
      val depositGuard = ctx.mkTrue()
      val depositFunc = ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("deposit", depositParams, depositGuard, depositFunc)
      
      sm.addOnce()
      
      // 正轨迹：存款应该成功
      val posTrace = List(
        List(
          List(
            ctx.mkString("deposit").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(50)).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]]
          )
        )
      )
      
      val neg = List[List[List[Z3Expr[BoolSort]]]]()
      val candidates = Map[String, List[Z3Expr[BoolSort]]](
        "deposit" -> List(
          ctx.mkTrue(),
          ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0)),
          ctx.mkLt(ctx.mkIntConst("amount"), ctx.mkInt(1000))
        )
      )
      
      // 设置候选条件保�?
      sm.candidateConditionGuards("deposit") = candidates("deposit")
      
      println("Before synthesis:")
      println(s"  Initial guard: ${sm.conditionGuards("deposit")}")
      
      sm.synthesize(posTrace, neg, candidates)
      
      println("After synthesis:")
      println(s"  Updated guard: ${sm.conditionGuards("deposit")}")
      println("�?Positive trace learning completed")
      
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"�?Positive trace learning failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testNegativeTraceLearning(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("NegativeLearning", ctx)
      
      // 创建余额状态机
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
      
      // 添加withdraw转换
      val withdrawParams = List(ctx.mkIntConst("amount"))
      val withdrawGuard = ctx.mkTrue()
      val withdrawFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("withdraw", withdrawParams, withdrawGuard, withdrawFunc)
      
      sm.addOnce()
      
      // 正轨迹：小额提取成功
      val posTrace = List(
        List(
          List(
            ctx.mkString("withdraw").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(50)).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkGe(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")).asInstanceOf[Z3Expr[BoolSort]]
          )
        )
      )
      
      // 负轨迹：大额提取失败
      val negTrace = List(
        List(
          List(
            ctx.mkString("withdraw").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(200)).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkLt(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")).asInstanceOf[Z3Expr[BoolSort]]
          )
        )
      )
      
      val candidates = Map[String, List[Z3Expr[BoolSort]]](
        "withdraw" -> List(
          ctx.mkTrue(),
          ctx.mkGe(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")),
          ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
        )
      )
      
      // 设置候选条件保�?
      sm.candidateConditionGuards("withdraw") = candidates("withdraw")
      
      println("Before synthesis:")
      println(s"  Initial guard: ${sm.conditionGuards("withdraw")}")
      println(s"  Positive traces: ${posTrace.length}")
      println(s"  Negative traces: ${negTrace.length}")
      
      sm.synthesize(posTrace, negTrace, candidates)
      
      println("After synthesis:")
      println(s"  Updated guard: ${sm.conditionGuards("withdraw")}")
      println("�?Negative trace learning completed")
      
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"�?Negative trace learning failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testMultiTransitionSynthesis(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("MultiTransition", ctx)
      
      // 创建多状态变�?
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      val (locked, lockedOut) = sm.addState("locked", ctx.mkBoolSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(balance, ctx.mkInt(0)),
        ctx.mkEq(locked, ctx.mkFalse())
      ))
      
      // 添加deposit转换
      val depositParams = List(ctx.mkIntConst("amount"))
      val depositGuard = ctx.mkTrue()
      val depositFunc = ctx.mkAnd(
        ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount"))),
        ctx.mkEq(lockedOut, locked)
      )
      sm.addTr("deposit", depositParams, depositGuard, depositFunc)
      
      // 添加lock转换
      val lockParams = List()
      val lockGuard = ctx.mkTrue()
      val lockFunc = ctx.mkAnd(
        ctx.mkEq(balanceOut, balance),
        ctx.mkEq(lockedOut, ctx.mkTrue())
      )
      sm.addTr("lock", lockParams, lockGuard, lockFunc)
      
      sm.addOnce()
      
      // 正轨迹：先存款再锁定
      val posTrace = List(
        List(
          List(
            ctx.mkString("deposit").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(100)).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(locked, ctx.mkFalse()).asInstanceOf[Z3Expr[BoolSort]]
          ),
          List(
            ctx.mkString("lock").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkGt(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(locked, ctx.mkFalse()).asInstanceOf[Z3Expr[BoolSort]]
          )
        )
      )
      
      val candidates = Map[String, List[Z3Expr[BoolSort]]](
        "deposit" -> List(
          ctx.mkTrue(),
          ctx.mkEq(locked, ctx.mkFalse()),
          ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
        ),
        "lock" -> List(
          ctx.mkTrue(),
          ctx.mkEq(locked, ctx.mkFalse()),
          ctx.mkGt(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0))
        )
      )
      
      // 设置候选条件保�?
      sm.candidateConditionGuards("deposit") = candidates("deposit")
      sm.candidateConditionGuards("lock") = candidates("lock")
      
      println("Before synthesis:")
      println(s"  Deposit guard: ${sm.conditionGuards("deposit")}")
      println(s"  Lock guard: ${sm.conditionGuards("lock")}")
      println(s"  Number of transitions: ${sm.transitions.length}")
      
      val neg = List[List[List[Z3Expr[BoolSort]]]]()
      sm.synthesize(posTrace, neg, candidates)
      
      println("After synthesis:")
      println(s"  Deposit guard: ${sm.conditionGuards("deposit")}")
      println(s"  Lock guard: ${sm.conditionGuards("lock")}")
      println("�?Multi-transition synthesis completed")
      
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"�?Multi-transition synthesis failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testComplexConstraintSynthesis(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("ComplexConstraints", ctx)
      
      // 创建拍卖状态机
      val (highestBid, highestBidOut) = sm.addState("highestBid", ctx.mkIntSort())
      val (auctionEnd, auctionEndOut) = sm.addState("auctionEnd", ctx.mkBoolSort())
      val (bidder, bidderOut) = sm.addState("bidder", ctx.mkBitVecSort(256))
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(highestBid, ctx.mkInt(0)),
        ctx.mkEq(auctionEnd, ctx.mkFalse()),
        ctx.mkEq(bidder, ctx.mkBV(0, 256))
      ))
      
      // 添加bid转换
      val bidParams = List(ctx.mkIntConst("bidAmount"), ctx.mkBVConst("newBidder", 256))
      val bidGuard = ctx.mkTrue()
      val bidFunc = ctx.mkAnd(
        ctx.mkEq(highestBidOut, ctx.mkITE(
          ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Z3Expr[ArithSort]]),
          ctx.mkIntConst("bidAmount"),
          highestBid.asInstanceOf[Z3Expr[ArithSort]]
        )),
        ctx.mkEq(bidderOut, ctx.mkITE(
          ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Z3Expr[ArithSort]]),
          ctx.mkBVConst("newBidder", 256),
          bidder.asInstanceOf[Z3Expr[BitVecSort]]
        )),
        ctx.mkEq(auctionEndOut, auctionEnd)
      )
      sm.addTr("bid", bidParams, bidGuard, bidFunc)
      
      // 添加endAuction转换
      val endParams = List()
      val endGuard = ctx.mkTrue()
      val endFunc = ctx.mkAnd(
        ctx.mkEq(highestBidOut, highestBid),
        ctx.mkEq(bidderOut, bidder),
        ctx.mkEq(auctionEndOut, ctx.mkTrue())
      )
      sm.addTr("endAuction", endParams, endGuard, endFunc)
      
      sm.addOnce()
      
      // 正轨迹：正常拍卖流程
      val posTrace = List(
        List(
          List(
            ctx.mkString("bid").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(ctx.mkIntConst("bidAmount"), ctx.mkInt(100)).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(auctionEnd, ctx.mkFalse()).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Z3Expr[ArithSort]]).asInstanceOf[Z3Expr[BoolSort]]
          ),
          List(
            ctx.mkString("endAuction").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkGt(highestBid.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(auctionEnd, ctx.mkFalse()).asInstanceOf[Z3Expr[BoolSort]]
          )
        )
      )
      
      // 负轨迹：拍卖结束后不能出�?
      val negTrace = List(
        List(
          List(
            ctx.mkString("bid").asInstanceOf[Z3Expr[BoolSort]],
            ctx.mkEq(auctionEnd, ctx.mkTrue()).asInstanceOf[Z3Expr[BoolSort]]
          )
        )
      )
      
      val candidates = Map[String, List[Z3Expr[BoolSort]]](
        "bid" -> List(
          ctx.mkTrue(),
          ctx.mkEq(auctionEnd, ctx.mkFalse()),
          ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Z3Expr[ArithSort]]),
          ctx.mkGt(ctx.mkIntConst("bidAmount"), ctx.mkInt(0))
        ),
        "endAuction" -> List(
          ctx.mkTrue(),
          ctx.mkEq(auctionEnd, ctx.mkFalse()),
          ctx.mkGt(highestBid.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0))
        )
      )
      
      // 设置候选条件保�?
      sm.candidateConditionGuards("bid") = candidates("bid")
      sm.candidateConditionGuards("endAuction") = candidates("endAuction")
      
      println("Before synthesis:")
      println(s"  States: ${sm.states.keys.mkString(", ")}")
      println(s"  Transitions: ${sm.transitions.mkString(", ")}")
      println(s"  Bid candidates: ${candidates("bid").length}")
      println(s"  EndAuction candidates: ${candidates("endAuction").length}")
      
      sm.synthesize(posTrace, negTrace, candidates)
      
      println("After synthesis:")
      println(s"  Bid guard: ${sm.conditionGuards("bid")}")
      println(s"  EndAuction guard: ${sm.conditionGuards("endAuction")}")
      println("�?Complex constraint synthesis completed")
      
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"�?Complex constraint synthesis failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testErrorHandling(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("ErrorHandling", ctx)
      
      // 创建状态机但不添加任何状态或转换
      sm.setInit(ctx.mkTrue())
      
      // 测试空候选集
      val pos = List[List[List[Z3Expr[BoolSort]]]]()
      val neg = List[List[List[Z3Expr[BoolSort]]]]()
      val emptyCandidates = Map[String, List[Z3Expr[BoolSort]]]()
      
      println("Testing empty candidates...")
      sm.synthesize(pos, neg, emptyCandidates)
      
      // 添加一个转换但提供矛盾的轨�?
      val (state, stateOut) = sm.addState("state", ctx.mkIntSort())
      val trParams = List()
      val trGuard = ctx.mkTrue()
      val trFunc = ctx.mkEq(stateOut, ctx.mkAdd(state.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(1)))
      sm.addTr("transition", trParams, trGuard, trFunc)
      
      sm.addOnce()
      
      // 矛盾的轨迹：同一个状态既在正轨迹又在负轨迹中
      val contradictoryTrace = List(
        ctx.mkString("transition").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(state, ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]]
      )
      
      val posContra = List(List(contradictoryTrace))
      val negContra = List(List(contradictoryTrace))
      val candidates = Map[String, List[Z3Expr[BoolSort]]](
        "transition" -> List(ctx.mkTrue(), ctx.mkFalse())
      )
      
      // 设置候选条件保�?
      sm.candidateConditionGuards("transition") = candidates("transition")
      
      println("Testing contradictory traces...")
      sm.synthesize(posContra, negContra, candidates)
      
      println("�?Error handling completed - no crashes")
      
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"�?Error handling test failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
} 
 