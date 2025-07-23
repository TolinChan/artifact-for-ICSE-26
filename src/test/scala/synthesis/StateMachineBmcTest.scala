package synthesis

import com.microsoft.z3._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import datalog._

class StateMachineBmcTest extends AnyFunSuite with Matchers {

  test("bmc should return None when property is satisfied") {
    val ctx = new Context()
    val sm = new StateMachine("SafeContract", ctx)
    
    // 创建一个简单的状态机：余额只能增加
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    
    // 设置初始状态：余额为0
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(0)))
    
    // 添加存款操作：只能增加余额
    val depositParams = List(ctx.mkIntConst("amount"))
    val depositGuard = ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
    val depositFunc = ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
    sm.addTr("deposit", depositParams, depositGuard, depositFunc)
    
    sm.addOnce()
    
    // 属性：余额永远不会为负数
    val property = ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0))
    
    // BMC应该找不到反例，因为余额只能增加
    val result = sm.bmc(property)
    result should be(None)
    
    ctx.close()
  }

  test("bmc should find counterexample when property is violated") {
    val ctx = new Context()
    val sm = new StateMachine("UnsafeContract", ctx)
    
    // 创建一个不安全的状态机：可以任意减少余额
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    
    // 设置初始状态：余额为100
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
    
    // 添加提取操作：可以提取任意金额（不安全）
    val withdrawParams = List(ctx.mkIntConst("amount"))
    val withdrawGuard = ctx.mkTrue() // 没有安全检查
    val withdrawFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
    sm.addTr("withdraw", withdrawParams, withdrawGuard, withdrawFunc)
    
    sm.addOnce()
    
    // 属性：余额永远不会为负数
    val property = ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0))
    
    // BMC应该找到反例，因为可以提取超过余额的金额
    val result = sm.bmc(property)
    result should not be None
    result.get should not be empty
    
    println(s"Found counterexample with ${result.get.length} steps")
    
    ctx.close()
  }

  test("bmc should handle complex state transitions") {
    val ctx = new Context()
    val sm = new StateMachine("AuctionContract", ctx)
    
    // 创建拍卖合约状态机
    val (highestBid, highestBidOut) = sm.addState("highestBid", ctx.mkIntSort())
    val (auctionEnded, auctionEndedOut) = sm.addState("auctionEnded", ctx.mkBoolSort())
    
    // 初始状态
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
    
    // 属性：拍卖结束后不能再出价
    val property = ctx.mkImplies(
      auctionEnded.asInstanceOf[Expr[BoolSort]],
      ctx.mkEq(highestBidOut, highestBid) // 如果拍卖结束，最高出价不应该改变
    )
    
    val result = sm.bmc(property)
    
    // 这个属性应该是满足的，因为我们的保护条件防止在拍卖结束后出价
    result should be(None)
    
    ctx.close()
  }

  test("bmc should handle multiple state variables") {
    val ctx = new Context()
    val sm = new StateMachine("MultiStateContract", ctx)
    
    // 多个状态变量
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    val (totalSupply, totalSupplyOut) = sm.addState("totalSupply", ctx.mkIntSort())
    val (paused, pausedOut) = sm.addState("paused", ctx.mkBoolSort())
    
    // 初始状态
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(balance, ctx.mkInt(1000)),
      ctx.mkEq(totalSupply, ctx.mkInt(1000)),
      ctx.mkEq(paused, ctx.mkFalse())
    ))
    
    // 转账操作
    val transferParams = List(ctx.mkIntConst("amount"))
    val transferGuard = ctx.mkAnd(
      ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]]),
      ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount"))
    )
    val transferFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount"))),
      ctx.mkEq(totalSupplyOut, totalSupply),
      ctx.mkEq(pausedOut, paused)
    )
    sm.addTr("transfer", transferParams, transferGuard, transferFunc)
    
    // 暂停操作
    val pauseParams = List()
    val pauseGuard = ctx.mkNot(paused.asInstanceOf[Expr[BoolSort]])
    val pauseFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, balance),
      ctx.mkEq(totalSupplyOut, totalSupply),
      ctx.mkEq(pausedOut, ctx.mkTrue())
    )
    sm.addTr("pause", pauseParams, pauseGuard, pauseFunc)
    
    sm.addOnce()
    
    // 属性：总供应量应该始终等于余额（在这个简化的例子中）
    val property = ctx.mkEq(totalSupply.asInstanceOf[Expr[ArithSort]], ctx.mkAdd(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0)))
    
    val result = sm.bmc(property)
    
    // 这个属性应该是满足的
    result should be(None)
    
    ctx.close()
  }

  test("bmc should find violation in invariant property") {
    val ctx = new Context()
    val sm = new StateMachine("InvariantViolation", ctx)
    
    // 创建一个会违反不变量的状态机
    val (x, xOut) = sm.addState("x", ctx.mkIntSort())
    val (y, yOut) = sm.addState("y", ctx.mkIntSort())
    
    // 初始状态：x = 0, y = 0
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(x, ctx.mkInt(0)),
      ctx.mkEq(y, ctx.mkInt(0))
    ))
    
    // 操作1：增加x
    val incXParams = List()
    val incXGuard = ctx.mkTrue()
    val incXFunc = ctx.mkAnd(
      ctx.mkEq(xOut, ctx.mkAdd(x.asInstanceOf[Expr[ArithSort]], ctx.mkInt(1))),
      ctx.mkEq(yOut, y)
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
    
    // 不变量：y总是x的两倍
    val invariant = ctx.mkEq(y.asInstanceOf[Expr[ArithSort]], ctx.mkMul(x.asInstanceOf[Expr[ArithSort]], ctx.mkInt(2)))
    
    // BMC应该找到反例，因为可以先增加x然后不更新y
    val result = sm.bmc(invariant)
    result should not be None
    
    println(s"Found invariant violation with ${result.get.length} steps")
    
    ctx.close()
  }

  test("bmc should handle empty transitions") {
    val ctx = new Context()
    val sm = new StateMachine("EmptyContract", ctx)
    
    // 只有状态，没有转换
    val (value, valueOut) = sm.addState("value", ctx.mkIntSort())
    sm.setInit(ctx.mkEq(value, ctx.mkInt(42)))
    
    sm.addOnce()
    
    // 属性：值应该始终为42
    val property = ctx.mkEq(value.asInstanceOf[Expr[ArithSort]], ctx.mkInt(42))
    
    // 没有转换，属性应该总是满足
    val result = sm.bmc(property)
    result should be(None)
    
    ctx.close()
  }

  test("bmc should handle boolean state variables") {
    val ctx = new Context()
    val sm = new StateMachine("BooleanContract", ctx)
    
    // 布尔状态变量
    val (flag, flagOut) = sm.addState("flag", ctx.mkBoolSort())
    val (enabled, enabledOut) = sm.addState("enabled", ctx.mkBoolSort())
    
    // 初始状态
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(flag, ctx.mkFalse()),
      ctx.mkEq(enabled, ctx.mkTrue())
    ))
    
    // 切换flag操作
    val toggleParams = List()
    val toggleGuard = enabled.asInstanceOf[Expr[BoolSort]]
    val toggleFunc = ctx.mkAnd(
      ctx.mkEq(flagOut, ctx.mkNot(flag.asInstanceOf[Expr[BoolSort]])),
      ctx.mkEq(enabledOut, enabled)
    )
    sm.addTr("toggle", toggleParams, toggleGuard, toggleFunc)
    
    // 禁用操作
    val disableParams = List()
    val disableGuard = ctx.mkTrue()
    val disableFunc = ctx.mkAnd(
      ctx.mkEq(flagOut, flag),
      ctx.mkEq(enabledOut, ctx.mkFalse())
    )
    sm.addTr("disable", disableParams, disableGuard, disableFunc)
    
    sm.addOnce()
    
    // 属性：如果禁用了，flag不应该改变
    val property = ctx.mkImplies(
      ctx.mkNot(enabled.asInstanceOf[Expr[BoolSort]]),
      ctx.mkEq(flagOut, flag)
    )
    
    val result = sm.bmc(property)
    
    // 这个属性应该是满足的
    result should be(None)
    
    ctx.close()
  }

  test("bmc should handle time-based properties") {
    val ctx = new Context()
    val sm = new StateMachine("TimeContract", ctx)
    
    // 时间相关的状态
    val (lastUpdate, lastUpdateOut) = sm.addState("lastUpdate", ctx.mkIntSort())
    val (value, valueOut) = sm.addState("value", ctx.mkIntSort())
    
    // 初始状态
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(lastUpdate, ctx.mkInt(0)),
      ctx.mkEq(value, ctx.mkInt(100))
    ))
    
    // 更新操作（需要时间推进）
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
    
    val result = sm.bmc(property)
    
    // 这个属性应该是满足的
    result should be(None)
    
    ctx.close()
  }

  test("bmc should handle complex logical properties") {
    val ctx = new Context()
    val sm = new StateMachine("LogicContract", ctx)
    
    // 多个相关的状态变量
    val (a, aOut) = sm.addState("a", ctx.mkBoolSort())
    val (b, bOut) = sm.addState("b", ctx.mkBoolSort())
    val (c, cOut) = sm.addState("c", ctx.mkBoolSort())
    
    // 初始状态：a=true, b=false, c=false
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
    
    // 属性：如果a为真，最终c也应该为真（但这需要两步操作）
    // 我们测试一个会被违反的即时属性：a为真时c立即为真
    val property = ctx.mkImplies(
      a.asInstanceOf[Expr[BoolSort]],
      c.asInstanceOf[Expr[BoolSort]]
    )
    
    // 这个属性应该被违反，因为需要两步操作
    val result = sm.bmc(property)
    result should not be None
    
    println(s"Found logical property violation with ${result.get.length} steps")
    
    ctx.close()
  }
} 