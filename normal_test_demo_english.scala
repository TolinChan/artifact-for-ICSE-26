import synthesis.StateMachine
import com.microsoft.z3.{Context, BoolSort, ArithSort}
import com.microsoft.z3.{Expr => Z3Expr}

object NormalTestDemoEnglish {
  def main(args: Array[String]): Unit = {
    println("=== Normal Test: Simulate and Synthesis ===")
    
    val ctx = new Context()
    val sm = new StateMachine("NormalTest", ctx)
    
    // Setup state machine
    setupStateMachine(sm, ctx)
    
    // Test 1: Simulate function
    println("\n[TEST 1] Simulate Function")
    testSimulate(sm, ctx)
    
    // Test 2: Synthesis function
    println("\n[TEST 2] Synthesis Function")
    testSynthesis(sm, ctx)
    
    ctx.close()
  }
  
  def setupStateMachine(sm: StateMachine, ctx: Context): Unit = {
    // Add state variables
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    val (totalSupply, totalSupplyOut) = sm.addState("totalSupply", ctx.mkIntSort())
    
    // Set initial state
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(balance, ctx.mkInt(1000)),
      ctx.mkEq(totalSupply, ctx.mkInt(1000))
    ))
    
    // Add transfer transition
    val transferParams = List(ctx.mkIntConst("amount"))
    val transferGuard = ctx.mkTrue() // Initial true, will be learned by synthesis
    val transferFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount"))),
      ctx.mkEq(totalSupplyOut, totalSupply)
    )
    sm.addTr("transfer", transferParams, transferGuard, transferFunc)
    
    // Add mint transition
    val mintParams = List(ctx.mkIntConst("mintAmount"))
    val mintGuard = ctx.mkTrue() // Initial true, will be learned by synthesis
    val mintFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("mintAmount"))),
      ctx.mkEq(totalSupplyOut, ctx.mkAdd(totalSupply.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("mintAmount")))
    )
    sm.addTr("mint", mintParams, mintGuard, mintFunc)
    
    sm.addOnce()
  }
  
  def testSimulate(sm: StateMachine, ctx: Context): Unit = {
    // Create normal trace
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
    
    // Create candidate conditions
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
    
    println("[INPUT] Simulate Input:")
    println(s"  Trace: ${trace.map(_.map(_.toString))}")
    println(s"  Candidates: ${candidates.map{case (k,v) => k -> v.map(_.toString)}}")
    
    // Execute simulate
    val result = sm.simulate(trace, candidates)
    
    println("[OUTPUT] Simulate Output:")
    println(s"  Result: ${result.map(_.map(_.toString))}")
    println(s"  Result Length: ${result.length}")
    
    if (result.nonEmpty) {
      println("[SUCCESS] Simulate test passed - generated simulation result")
    } else {
      println("[FAILED] Simulate test failed - no result generated")
    }
  }
  
  def testSynthesis(sm: StateMachine, ctx: Context): Unit = {
    // Create positive traces: normal behaviors
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
    
    // Create negative traces: error behaviors
    val negativeTrace1 = List(
      List(
        ctx.mkString("mint").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("mintAmount"), ctx.mkInt(-50)).asInstanceOf[Z3Expr[BoolSort]], // Negative mint
        ctx.mkLt(ctx.mkIntConst("mintAmount"), ctx.mkInt(0)).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    val negativeTrace2 = List(
      List(
        ctx.mkString("transfer").asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(2000)).asInstanceOf[Z3Expr[BoolSort]], // Over-transfer
        ctx.mkGt(ctx.mkIntConst("amount"), sm.states("balance")._1.asInstanceOf[Z3Expr[ArithSort]]).asInstanceOf[Z3Expr[BoolSort]]
      )
    )
    
    val pos = List(positiveTrace1, positiveTrace2)
    val neg = List(negativeTrace1, negativeTrace2)
    
    // Create candidate conditions
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
    
    // Set candidate condition guards
    sm.candidateConditionGuards("mint") = candidates("mint")
    sm.candidateConditionGuards("transfer") = candidates("transfer")
    
    println("[INPUT] Synthesis Input:")
    println(s"  Positive Traces Count: ${pos.length}")
    pos.zipWithIndex.foreach { case (trace, idx) =>
      println(s"    Positive Trace ${idx + 1}: ${trace.map(_.map(_.toString))}")
    }
    println(s"  Negative Traces Count: ${neg.length}")
    neg.zipWithIndex.foreach { case (trace, idx) =>
      println(s"    Negative Trace ${idx + 1}: ${trace.map(_.map(_.toString))}")
    }
    println(s"  Candidates: ${candidates.map{case (k,v) => k -> v.map(_.toString)}}")
    
    // Execute synthesis
    sm.synthesize(pos, neg, candidates)
    
    println("[OUTPUT] Synthesis Output:")
    println("  Learned Guard Conditions:")
    sm.conditionGuards.foreach { case (trName, guard) =>
      println(s"    $trName: $guard")
    }
    
    // Verify learning results
    val mintGuard = sm.conditionGuards.get("mint")
    val transferGuard = sm.conditionGuards.get("transfer")
    
    if (mintGuard.isDefined && transferGuard.isDefined) {
      println("[SUCCESS] Synthesis test passed - learned guard conditions")
      println(s"  Mint constraint: ${mintGuard.get}")
      println(s"  Transfer constraint: ${transferGuard.get}")
    } else {
      println("[FAILED] Synthesis test failed - no guard conditions learned")
    }
  }
} 