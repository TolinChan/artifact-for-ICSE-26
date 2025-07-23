import synthesis.StateMachine
import com.microsoft.z3._
import datalog._

object TestStateMachineBmc {
  def main(args: Array[String]): Unit = {
    println("Testing StateMachine BMC (Bounded Model Checking) functionality...")
    
    // 测试1: 属性满足的情况
    println("\n=== Test 1: Property Satisfied (No Counterexample) ===")
    testPropertySatisfied()
    
    // 测试2: 属性违反的情况
    println("\n=== Test 2: Property Violated (Counterexample Found) ===")
    testPropertyViolated()
    
    // 测试3: 复杂状态转换
    println("\n=== Test 3: Complex State Transitions ===")
    testComplexTransitions()
    
    // 测试4: 不变量违反
    println("\n=== Test 4: Invariant Violation ===")
    testInvariantViolation()
    
    // 测试5: 时间相关属性
    println("\n=== Test 5: Time-based Properties ===")
    testTimeBasedProperties()
    
    // 测试6: 布尔逻辑属性
    println("\n=== Test 6: Boolean Logic Properties ===")
    testBooleanLogic()
    
    println("\nAll BMC tests completed!")
  }
  
  def testPropertySatisfied(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("SafeContract", ctx)
      
      println("Creating safe contract where balance can only increase...")
      
      // 创建只能增加余额的安全合约
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(0)))
      
      // 只允许存款操作
      val depositParams = List(ctx.mkIntConst("amount"))
      val depositGuard = ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
      val depositFunc = ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("deposit", depositParams, depositGuard, depositFunc)
      sm.addOnce()
      
      // 属性：余额永远不为负
      val property = ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0))
      
      println("Running BMC to check property: balance >= 0")
      val result = sm.bmc(property)
      
      if (result.isEmpty) {
        println("✅ SUCCESS: No counterexample found - property is satisfied")
      } else {
        println("❌ UNEXPECTED: Found counterexample when property should be satisfied")
      }
      
      ctx.close()
      
    } catch {
      case e: Exception =>
        println(s"❌ ERROR in property satisfied test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testPropertyViolated(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("UnsafeContract", ctx)
      
      println("Creating unsafe contract that allows unlimited withdrawals...")
      
      // 创建不安全的合约，允许任意提取
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
      
      // 允许任意金额的提取（不安全）
      val withdrawParams = List(ctx.mkIntConst("amount"))
      val withdrawGuard = ctx.mkTrue() // 没有余额检查
      val withdrawFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("withdraw", withdrawParams, withdrawGuard, withdrawFunc)
      sm.addOnce()
      
      // 属性：余额永远不为负
      val property = ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0))
      
      println("Running BMC to check property: balance >= 0")
      val result = sm.bmc(property)
      
      if (result.nonEmpty) {
        println(s"✅ SUCCESS: Found counterexample with ${result.get.length} steps")
        println("Counterexample trace:")
        result.get.zipWithIndex.foreach { case (step, i) =>
          println(s"  Step $i: ${step.map(_.toString).take(3).mkString(", ")}...")
        }
      } else {
        println("❌ UNEXPECTED: No counterexample found when property should be violated")
      }
      
      ctx.close()
      
    } catch {
      case e: Exception =>
        println(s"❌ ERROR in property violated test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testComplexTransitions(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("AuctionContract", ctx)
      
      println("Creating auction contract with complex state transitions...")
      
      // 拍卖合约状态
      val (highestBid, highestBidOut) = sm.addState("highestBid", ctx.mkIntSort())
      val (auctionEnded, auctionEndedOut) = sm.addState("auctionEnded", ctx.mkBoolSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(highestBid, ctx.mkInt(0)),
        ctx.mkEq(auctionEnded, ctx.mkFalse())
      ))
      
      // 出价操作
      val bidParams = List(ctx.mkIntConst("bidAmount"))
      val bidGuard = ctx.mkAnd(
        ctx.mkNot(auctionEnded.asInstanceOf[Expr[BoolSort]]),
        ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Expr[ArithSort]])
      )
      val bidFunc = ctx.mkAnd(
        ctx.mkEq(highestBidOut, ctx.mkIntConst("bidAmount")),
        ctx.mkEq(auctionEndedOut, auctionEnded)
      )
      sm.addTr("bid", bidParams, bidGuard, bidFunc)
      
      // 结束拍卖操作
      val endParams = List()
      val endGuard = ctx.mkNot(auctionEnded.asInstanceOf[Expr[BoolSort]])
      val endFunc = ctx.mkAnd(
        ctx.mkEq(highestBidOut, highestBid),
        ctx.mkEq(auctionEndedOut, ctx.mkTrue())
      )
      sm.addTr("endAuction", endParams, endGuard, endFunc)
      sm.addOnce()
      
      // 属性：拍卖结束后最高出价不再改变
      val property = ctx.mkImplies(
        auctionEnded.asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(highestBidOut, highestBid)
      )
      
      println("Running BMC to check property: auction ended => highest bid unchanged")
      val result = sm.bmc(property)
      
      if (result.isEmpty) {
        println("✅ SUCCESS: Auction property satisfied - no counterexample found")
      } else {
        println(s"❌ UNEXPECTED: Found counterexample with ${result.get.length} steps")
      }
      
      ctx.close()
      
    } catch {
      case e: Exception =>
        println(s"❌ ERROR in complex transitions test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testInvariantViolation(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("InvariantContract", ctx)
      
      println("Creating contract that can violate invariant x*2 = y...")
      
      // 两个相关的状态变量
      val (x, xOut) = sm.addState("x", ctx.mkIntSort())
      val (y, yOut) = sm.addState("y", ctx.mkIntSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(x, ctx.mkInt(0)),
        ctx.mkEq(y, ctx.mkInt(0))
      ))
      
      // 操作1：增加x但不更新y
      val incXParams = List()
      val incXGuard = ctx.mkTrue()
      val incXFunc = ctx.mkAnd(
        ctx.mkEq(xOut, ctx.mkAdd(x.asInstanceOf[Expr[ArithSort]], ctx.mkInt(1))),
        ctx.mkEq(yOut, y) // y保持不变
      )
      sm.addTr("incX", incXParams, incXGuard, incXFunc)
      
      // 操作2：设置y为x的两倍
      val setYParams = List()
      val setYGuard = ctx.mkTrue()
      val setYFunc = ctx.mkAnd(
        ctx.mkEq(xOut, x),
        ctx.mkEq(yOut, ctx.mkMul(x.asInstanceOf[Expr[ArithSort]], ctx.mkInt(2)))
      )
      sm.addTr("setY", setYParams, setYGuard, setYFunc)
      sm.addOnce()
      
      // 不变量：y应该总是x的两倍
      val invariant = ctx.mkEq(y.asInstanceOf[Expr[ArithSort]], ctx.mkMul(x.asInstanceOf[Expr[ArithSort]], ctx.mkInt(2)))
      
      println("Running BMC to check invariant: y = x * 2")
      val result = sm.bmc(invariant)
      
      if (result.nonEmpty) {
        println(s"✅ SUCCESS: Found invariant violation with ${result.get.length} steps")
        println("This is expected - we can increment x without updating y")
      } else {
        println("❌ UNEXPECTED: No invariant violation found")
      }
      
      ctx.close()
      
    } catch {
      case e: Exception =>
        println(s"❌ ERROR in invariant violation test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testTimeBasedProperties(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("TimeContract", ctx)
      
      println("Creating contract with time-based constraints...")
      
      // 时间相关状态
      val (lastUpdate, lastUpdateOut) = sm.addState("lastUpdate", ctx.mkIntSort())
      val (value, valueOut) = sm.addState("value", ctx.mkIntSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(lastUpdate, ctx.mkInt(0)),
        ctx.mkEq(value, ctx.mkInt(100))
      ))
      
      // 更新操作需要时间推进
      val updateParams = List(ctx.mkIntConst("newValue"))
      val updateGuard = ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], lastUpdate.asInstanceOf[Expr[ArithSort]])
      val updateFunc = ctx.mkAnd(
        ctx.mkEq(lastUpdateOut, sm.nowOut),
        ctx.mkEq(valueOut, ctx.mkIntConst("newValue"))
      )
      sm.addTr("update", updateParams, updateGuard, updateFunc)
      sm.addOnce()
      
      // 属性：lastUpdate应该总是小于等于当前时间
      val property = ctx.mkLe(lastUpdate.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]])
      
      println("Running BMC to check property: lastUpdate <= now")
      val result = sm.bmc(property)
      
      if (result.isEmpty) {
        println("✅ SUCCESS: Time-based property satisfied")
      } else {
        println(s"❌ UNEXPECTED: Found counterexample with ${result.get.length} steps")
      }
      
      ctx.close()
      
    } catch {
      case e: Exception =>
        println(s"❌ ERROR in time-based properties test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testBooleanLogic(): Unit = {
    try {
      val ctx = new Context()
      val sm = new StateMachine("LogicContract", ctx)
      
      println("Creating contract with boolean logic properties...")
      
      // 布尔状态变量
      val (a, aOut) = sm.addState("a", ctx.mkBoolSort())
      val (b, bOut) = sm.addState("b", ctx.mkBoolSort())
      val (c, cOut) = sm.addState("c", ctx.mkBoolSort())
      
      sm.setInit(ctx.mkAnd(
        ctx.mkEq(a, ctx.mkTrue()),
        ctx.mkEq(b, ctx.mkFalse()),
        ctx.mkEq(c, ctx.mkFalse())
      ))
      
      // 操作1：如果a为真，设置b为真
      val op1Params = List()
      val op1Guard = a.asInstanceOf[Expr[BoolSort]]
      val op1Func = ctx.mkAnd(
        ctx.mkEq(aOut, a),
        ctx.mkEq(bOut, ctx.mkTrue()),
        ctx.mkEq(cOut, c)
      )
      sm.addTr("op1", op1Params, op1Guard, op1Func)
      
      // 操作2：如果b为真，设置c为真
      val op2Params = List()
      val op2Guard = b.asInstanceOf[Expr[BoolSort]]
      val op2Func = ctx.mkAnd(
        ctx.mkEq(aOut, a),
        ctx.mkEq(bOut, b),
        ctx.mkEq(cOut, ctx.mkTrue())
      )
      sm.addTr("op2", op2Params, op2Guard, op2Func)
      sm.addOnce()
      
      // 属性：如果a为真，c也应该立即为真（这是错误的，需要两步）
      val property = ctx.mkImplies(
        a.asInstanceOf[Expr[BoolSort]],
        c.asInstanceOf[Expr[BoolSort]]
      )
      
      println("Running BMC to check property: a => c (immediate)")
      val result = sm.bmc(property)
      
      if (result.nonEmpty) {
        println(s"✅ SUCCESS: Found logical property violation with ${result.get.length} steps")
        println("This is expected - c requires two operations to become true")
      } else {
        println("❌ UNEXPECTED: No violation found for immediate implication")
      }
      
      ctx.close()
      
    } catch {
      case e: Exception =>
        println(s"❌ ERROR in boolean logic test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
} 