import synthesis.StateMachine
import com.microsoft.z3.{Context, Status, BoolExpr, ArithSort, BoolSort, BitVecSort}
import com.microsoft.z3.{Expr => Z3Expr}

object TestStateMachineBmc {
  def main(args: Array[String]): Unit = {
    println("Testing StateMachine BMC functionality...")
    
    // Test 1: Basic BMC functionality
    println("\n=== Test 1: Basic BMC Functionality ===")
    testBasicBmc()
    
    // Test 2: Balance invariant checking
    println("\n=== Test 2: Balance Invariant Checking ===")
    testBalanceInvariant()
    
    // Test 3: Withdrawal constraint verification
    println("\n=== Test 3: Withdrawal Constraint Verification ===")
    testWithdrawalConstraint()
    
    // Test 4: Auction bidding logic
    println("\n=== Test 4: Auction Bidding Logic ===")
    testAuctionBidding()
    
    // Test 5: State transition invariants
    println("\n=== Test 5: State Transition Invariants ===")
    testStateTransitionInvariants()
    
    // Test 6: Complex multi-variable invariants
    println("\n=== Test 6: Complex Multi-Variable Invariants ===")
    testComplexInvariants()
    
    println("\nAll BMC tests completed!")
  }
  
  def testBasicBmc(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("BasicBmc", ctx)
      
      // Create simple counter state machine
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
      
      // Add deposit transition
      val depositParams = List(ctx.mkIntConst("amount"))
      val depositGuard = ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
      val depositFunc = ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("deposit", depositParams, depositGuard, depositFunc)
      sm.addOnce()
      
      // Test property: balance should always be non-negative
      val property = ctx.mkGe(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0))
      
      println("Before BMC:")
      println(s"  Initial balance: 100")
      println(s"  Property: balance >= 0")
      
      val result = sm.bmc(property)
      
      if (result.isDefined) {
        println("BMC found counterexample:")
        result.get.zipWithIndex.foreach { case (step, index) =>
          println(s"  Step $index: ${step.mkString(", ")}")
        }
      } else {
        println("BMC: No counterexample found - property may be satisfied")
      }
      
      println("✓ Basic BMC completed")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"✗ Basic BMC failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testBalanceInvariant(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("BalanceInvariant", ctx)
      
      // Create balance state machine
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
      
      // Add withdraw transition
      val withdrawParams = List(ctx.mkIntConst("amount"))
      val withdrawGuard = ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
      val withdrawFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("withdraw", withdrawParams, withdrawGuard, withdrawFunc)
      sm.addOnce()
      
      // Test property: balance should never go negative
      val property = ctx.mkGe(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0))
      
      println("Testing balance invariant:")
      println(s"  Initial balance: 100")
      println(s"  Withdraw transition allows any positive amount")
      println(s"  Property: balance >= 0")
      
      val result = sm.bmc(property)
      
      if (result.isDefined) {
        println("BMC found counterexample (expected):")
        val trace = result.get
        trace.zipWithIndex.foreach { case (step, index) =>
          println(s"  Step $index: ${step.take(3).mkString(", ")}")
        }
      } else {
        println("BMC: No counterexample found")
      }
      
      println("✓ Balance invariant test completed")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"✗ Balance invariant test failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testWithdrawalConstraint(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("WithdrawalConstraint", ctx)
      
      // Create balance state machine with proper withdrawal guard
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
      
      // Add withdraw transition with proper guard
      val withdrawParams = List(ctx.mkIntConst("amount"))
      val withdrawGuard = ctx.mkAnd(
        ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0)),
        ctx.mkGe(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount"))
      )
      val withdrawFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("withdraw", withdrawParams, withdrawGuard, withdrawFunc)
      sm.addOnce()
      
      // Test property: balance should never go negative
      val property = ctx.mkGe(balance.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(0))
      
      println("Testing withdrawal constraint:")
      println(s"  Initial balance: 100")
      println(s"  Withdraw guard: amount > 0 && balance >= amount")
      println(s"  Property: balance >= 0")
      
      val result = sm.bmc(property)
      
      if (result.isDefined) {
        println("BMC found counterexample (unexpected):")
        result.get.zipWithIndex.foreach { case (step, index) =>
          println(s"  Step $index: ${step.take(3).mkString(", ")}")
        }
      } else {
        println("BMC: No counterexample found - property satisfied")
      }
      
      println("✓ Withdrawal constraint test completed")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"✗ Withdrawal constraint test failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testAuctionBidding(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("AuctionBidding", ctx)
      
      // Create auction state machine
      val (highestBid, highestBidOut) = sm.addState("highestBid", ctx.mkIntSort())
      val (auctionEnded, auctionEndedOut) = sm.addState("auctionEnded", ctx.mkBoolSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(highestBid, ctx.mkInt(0)),
        ctx.mkEq(auctionEnded, ctx.mkFalse())
      ))
      
      // Add bid transition
      val bidParams = List(ctx.mkIntConst("bidAmount"))
      val bidGuard = ctx.mkAnd(
        ctx.mkNot(auctionEnded.asInstanceOf[Z3Expr[BoolSort]]),
        ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Z3Expr[ArithSort]])
      )
      val bidFunc = ctx.mkAnd(
        ctx.mkEq(highestBidOut, ctx.mkIntConst("bidAmount")),
        ctx.mkEq(auctionEndedOut, auctionEnded)
      )
      sm.addTr("bid", bidParams, bidGuard, bidFunc)
      
      // Add end auction transition
      val endParams = List()
      val endGuard = ctx.mkNot(auctionEnded.asInstanceOf[Z3Expr[BoolSort]])
      val endFunc = ctx.mkAnd(
        ctx.mkEq(highestBidOut, highestBid),
        ctx.mkEq(auctionEndedOut, ctx.mkTrue())
      )
      sm.addTr("endAuction", endParams, endGuard, endFunc)
      sm.addOnce()
      
      // Test property: once auction ends, no more bids allowed
      val property = ctx.mkImplies(
        auctionEnded.asInstanceOf[Z3Expr[BoolSort]],
        ctx.mkEq(highestBidOut, highestBid.asInstanceOf[Z3Expr[ArithSort]])
      )
      
      println("Testing auction bidding logic:")
      println(s"  Initial: highestBid=0, auctionEnded=false")
      println(s"  Bid guard: !auctionEnded && bidAmount > highestBid")
      println(s"  Property: auctionEnded => highestBidOut == highestBid")
      
      val result = sm.bmc(property)
      
      if (result.isDefined) {
        println("BMC found counterexample:")
        result.get.zipWithIndex.foreach { case (step, index) =>
          println(s"  Step $index: ${step.take(4).mkString(", ")}")
        }
      } else {
        println("BMC: No counterexample found - property satisfied")
      }
      
      println("✓ Auction bidding test completed")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"✗ Auction bidding test failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testStateTransitionInvariants(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("StateTransition", ctx)
      
      // Create simple state machine with two integer variables
      val (x, xOut) = sm.addState("x", ctx.mkIntSort())
      val (y, yOut) = sm.addState("y", ctx.mkIntSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(x, ctx.mkInt(0)),
        ctx.mkEq(y, ctx.mkInt(0))
      ))
      
      // Add transition that increments x and sets y = 2*x
      val incParams = List()
      val incGuard = ctx.mkTrue()
      val incFunc = ctx.mkAnd(
        ctx.mkEq(xOut, ctx.mkAdd(x.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(1))),
        ctx.mkEq(yOut, ctx.mkMul(xOut.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(2)))
      )
      sm.addTr("increment", incParams, incGuard, incFunc)
      sm.addOnce()
      
      // Test invariant: y should always equal 2*x
      val invariant = ctx.mkEq(y.asInstanceOf[Z3Expr[ArithSort]], ctx.mkMul(x.asInstanceOf[Z3Expr[ArithSort]], ctx.mkInt(2)))
      
      println("Testing state transition invariants:")
      println(s"  Initial: x=0, y=0")
      println(s"  Transition: x' = x+1, y' = 2*x'")
      println(s"  Invariant: y == 2*x")
      
      val result = sm.bmc(invariant)
      
      if (result.isDefined) {
        println("BMC found counterexample:")
        result.get.zipWithIndex.foreach { case (step, index) =>
          println(s"  Step $index: ${step.take(3).mkString(", ")}")
        }
      } else {
        println("BMC: No counterexample found - invariant holds")
      }
      
      println("✓ State transition invariants test completed")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"✗ State transition invariants test failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testComplexInvariants(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("ComplexInvariants", ctx)
      
      // Create state machine with timestamp tracking
      val (lastUpdate, lastUpdateOut) = sm.addState("lastUpdate", ctx.mkIntSort())
      
      sm.setInit(ctx.mkEq(lastUpdate, ctx.mkInt(0)))
      
      // Add update transition that should only increase timestamp
      val updateParams = List()
      val updateGuard = ctx.mkGt(sm.nowOut.asInstanceOf[Z3Expr[ArithSort]], lastUpdate.asInstanceOf[Z3Expr[ArithSort]])
      val updateFunc = ctx.mkEq(lastUpdateOut, sm.nowOut)
      sm.addTr("update", updateParams, updateGuard, updateFunc)
      sm.addOnce()
      
      // Test property: lastUpdate should never be greater than current time
      val property = ctx.mkLe(lastUpdate.asInstanceOf[Z3Expr[ArithSort]], sm.now.asInstanceOf[Z3Expr[ArithSort]])
      
      println("Testing complex invariants:")
      println(s"  Initial: lastUpdate=0")
      println(s"  Update guard: now > lastUpdate")
      println(s"  Update func: lastUpdate' = now")
      println(s"  Property: lastUpdate <= now")
      
      val result = sm.bmc(property)
      
      if (result.isDefined) {
        println("BMC found counterexample:")
        result.get.zipWithIndex.foreach { case (step, index) =>
          println(s"  Step $index: ${step.take(3).mkString(", ")}")
        }
      } else {
        println("BMC: No counterexample found - property satisfied")
      }
      
      println("✓ Complex invariants test completed")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"✗ Complex invariants test failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testBooleanStateInvariants(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("BooleanState", ctx)
      
      // Create state machine with boolean flags
      val (a, aOut) = sm.addState("a", ctx.mkBoolSort())
      val (b, bOut) = sm.addState("b", ctx.mkBoolSort())
      val (c, cOut) = sm.addState("c", ctx.mkBoolSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(a, ctx.mkFalse()),
        ctx.mkEq(b, ctx.mkFalse()),
        ctx.mkEq(c, ctx.mkFalse())
      ))
      
      // Add operation 1: set a to true
      val op1Params = List()
      val op1Guard = a.asInstanceOf[Z3Expr[BoolSort]]
      val op1Func = ctx.mkAnd(
        ctx.mkEq(aOut, ctx.mkTrue()),
        ctx.mkEq(bOut, b),
        ctx.mkEq(cOut, c)
      )
      sm.addTr("op1", op1Params, op1Guard, op1Func)
      
      // Add operation 2: set b to true if a is true
      val op2Params = List()
      val op2Guard = b.asInstanceOf[Z3Expr[BoolSort]]
      val op2Func = ctx.mkAnd(
        ctx.mkEq(aOut, a),
        ctx.mkEq(bOut, ctx.mkTrue()),
        ctx.mkEq(cOut, c)
      )
      sm.addTr("op2", op2Params, op2Guard, op2Func)
      sm.addOnce()
      
      // Test mutual exclusion: a and c cannot both be true
      val property = ctx.mkNot(ctx.mkAnd(
        a.asInstanceOf[Z3Expr[BoolSort]],
        c.asInstanceOf[Z3Expr[BoolSort]]
      ))
      
      println("Testing boolean state invariants:")
      println(s"  Initial: a=false, b=false, c=false")
      println(s"  Property: !(a && c)")
      
      val result = sm.bmc(property)
      
      if (result.isDefined) {
        println("BMC found counterexample:")
        result.get.zipWithIndex.foreach { case (step, index) =>
          println(s"  Step $index: ${step.take(4).mkString(", ")}")
        }
      } else {
        println("BMC: No counterexample found - property satisfied")
      }
      
      println("✓ Boolean state invariants test completed")
      ctx.close()
    } catch {
      case e: Exception =>
        println(s"✗ Boolean state invariants test failed: ${e.getMessage}")
        e.printStackTrace()
    }
  }
} 