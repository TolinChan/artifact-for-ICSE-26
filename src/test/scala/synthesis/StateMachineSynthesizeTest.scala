package synthesis

import com.microsoft.z3._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import datalog._

class StateMachineSynthesizeTest extends AnyFunSuite with Matchers {

  test("synthesize should handle empty positive and negative traces") {
    val ctx = new Context()
    val sm = new StateMachine("EmptyTest", ctx)
    
    // 添加简单的状态和转换
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(0)))
    
    val depositParams = List(ctx.mkIntConst("amount"))
    val depositGuard = ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
    val depositFunc = ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
    sm.addTr("deposit", depositParams, depositGuard, depositFunc)
    
    sm.addOnce()
    
    // 空的正负轨迹
    val pos = List[List[List[Expr[BoolSort]]]]()
    val neg = List[List[List[Expr[BoolSort]]]]()
    val candidates = Map[String, List[Expr[BoolSort]]]("deposit" -> List(ctx.mkTrue(), ctx.mkFalse()))
    
    // 测试synthesize函数不会崩溃
    noException should be thrownBy {
      sm.synthesize(pos, neg, candidates)
    }
    
    ctx.close()
  }

  test("synthesize should learn from positive traces") {
    val ctx = new Context()
    val sm = new StateMachine("PositiveTest", ctx)
    
    // 创建简单的计数器状态机
    val (counter, counterOut) = sm.addState("counter", ctx.mkIntSort())
    sm.setInit(ctx.mkEq(counter, ctx.mkInt(0)))
    
    // 添加increment转换
    val incParams = List()
    val incGuard = ctx.mkTrue()
    val incFunc = ctx.mkEq(counterOut, ctx.mkAdd(counter.asInstanceOf[Expr[ArithSort]], ctx.mkInt(1)))
    sm.addTr("increment", incParams, incGuard, incFunc)
    
    sm.addOnce()
    
    // 创建正轨迹：increment应该总是允许的
    val posTrace = List(
      List(
        List(
          ctx.mkString("increment").asInstanceOf[Expr[BoolSort]],
          ctx.mkTrue().asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    val neg = List[List[List[Expr[BoolSort]]]]()
    val candidates = Map[String, List[Expr[BoolSort]]](
      "increment" -> List(
        ctx.mkTrue(),
        ctx.mkGt(counter.asInstanceOf[Expr[ArithSort]], ctx.mkInt(-1))
      )
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("increment") = candidates("increment")
    
    sm.synthesize(posTrace, neg, candidates)
    
    // 检查保护条件是否被正确学习
    sm.conditionGuards("increment") should not be null
    
    ctx.close()
  }

  test("synthesize should learn from negative traces") {
    val ctx = new Context()
    val sm = new StateMachine("NegativeTest", ctx)
    
    // 创建余额状态机
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
    
    // 添加withdraw转换
    val withdrawParams = List(ctx.mkIntConst("amount"))
    val withdrawGuard = ctx.mkTrue()
    val withdrawFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
    sm.addTr("withdraw", withdrawParams, withdrawGuard, withdrawFunc)
    
    sm.addOnce()
    
    // 正轨迹：小额提取应该成功
    val posTrace = List(
      List(
        List(
          ctx.mkString("withdraw").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(50)).asInstanceOf[Expr[BoolSort]],
          ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    // 负轨迹：大额提取应该失败
    val negTrace = List(
      List(
        List(
          ctx.mkString("withdraw").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(200)).asInstanceOf[Expr[BoolSort]],
          ctx.mkLt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    val candidates = Map[String, List[Expr[BoolSort]]](
      "withdraw" -> List(
        ctx.mkTrue(),
        ctx.mkGe(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")),
        ctx.mkGt(ctx.mkIntConst("amount"), ctx.mkInt(0))
      )
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("withdraw") = candidates("withdraw")
    
    sm.synthesize(posTrace, negTrace, candidates)
    
    // 检查保护条件是否被正确学习
    sm.conditionGuards("withdraw") should not be null
    
    ctx.close()
  }

  test("synthesize should handle multiple transitions") {
    val ctx = new Context()
    val sm = new StateMachine("MultiTransitionTest", ctx)
    
    // 创建多状态变量
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    val (locked, lockedOut) = sm.addState("locked", ctx.mkBoolSort())
    
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(balance, ctx.mkInt(0)),
      ctx.mkEq(locked, ctx.mkFalse())
    ))
    
    // 添加多个转换
    val depositParams = List(ctx.mkIntConst("amount"))
    val depositGuard = ctx.mkTrue()
    val depositFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount"))),
      ctx.mkEq(lockedOut, locked)
    )
    sm.addTr("deposit", depositParams, depositGuard, depositFunc)
    
    val lockParams = List()
    val lockGuard = ctx.mkTrue()
    val lockFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, balance),
      ctx.mkEq(lockedOut, ctx.mkTrue())
    )
    sm.addTr("lock", lockParams, lockGuard, lockFunc)
    
    sm.addOnce()
    
    // 创建正轨迹
    val posTrace = List(
      List(
        List(
          ctx.mkString("deposit").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(100)).asInstanceOf[Expr[BoolSort]]
        ),
        List(
          ctx.mkString("lock").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(locked, ctx.mkFalse()).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    val candidates = Map[String, List[Expr[BoolSort]]](
      "deposit" -> List(ctx.mkTrue(), ctx.mkEq(locked, ctx.mkFalse())),
      "lock" -> List(ctx.mkTrue(), ctx.mkGt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0)))
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("deposit") = candidates("deposit")
    sm.candidateConditionGuards("lock") = candidates("lock")
    
    val neg = List[List[List[Expr[BoolSort]]]]()
    
    sm.synthesize(posTrace, neg, candidates)
    
    // 检查所有转换的保护条件
    sm.conditionGuards("deposit") should not be null
    sm.conditionGuards("lock") should not be null
    
    ctx.close()
  }

  test("synthesize should handle complex logical constraints") {
    val ctx = new Context()
    val sm = new StateMachine("ComplexTest", ctx)
    
    // 创建拍卖状态机
    val (highestBid, highestBidOut) = sm.addState("highestBid", ctx.mkIntSort())
    val (auctionEnd, auctionEndOut) = sm.addState("auctionEnd", ctx.mkBoolSort())
    
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(highestBid, ctx.mkInt(0)),
      ctx.mkEq(auctionEnd, ctx.mkFalse())
    ))
    
    // 添加bid转换
    val bidParams = List(ctx.mkIntConst("bidAmount"))
    val bidGuard = ctx.mkTrue()
    val bidFunc = ctx.mkAnd(
      ctx.mkEq(highestBidOut, ctx.mkITE(
        ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Expr[ArithSort]]),
        ctx.mkIntConst("bidAmount"),
        highestBid.asInstanceOf[Expr[ArithSort]]
      )),
      ctx.mkEq(auctionEndOut, auctionEnd)
    )
    sm.addTr("bid", bidParams, bidGuard, bidFunc)
    
    // 添加endAuction转换
    val endParams = List()
    val endGuard = ctx.mkTrue()
    val endFunc = ctx.mkAnd(
      ctx.mkEq(highestBidOut, highestBid),
      ctx.mkEq(auctionEndOut, ctx.mkTrue())
    )
    sm.addTr("endAuction", endParams, endGuard, endFunc)
    
    sm.addOnce()
    
    // 正轨迹：拍卖过程
    val posTrace = List(
      List(
        List(
          ctx.mkString("bid").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(ctx.mkIntConst("bidAmount"), ctx.mkInt(100)).asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(auctionEnd, ctx.mkFalse()).asInstanceOf[Expr[BoolSort]]
        ),
        List(
          ctx.mkString("endAuction").asInstanceOf[Expr[BoolSort]],
          ctx.mkGt(highestBid.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0)).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    // 负轨迹：拍卖结束后不能再出价
    val negTrace = List(
      List(
        List(
          ctx.mkString("bid").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(auctionEnd, ctx.mkTrue()).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    val candidates = Map[String, List[Expr[BoolSort]]](
      "bid" -> List(
        ctx.mkTrue(),
        ctx.mkEq(auctionEnd, ctx.mkFalse()),
        ctx.mkGt(ctx.mkIntConst("bidAmount"), highestBid.asInstanceOf[Expr[ArithSort]])
      ),
      "endAuction" -> List(
        ctx.mkTrue(),
        ctx.mkEq(auctionEnd, ctx.mkFalse()),
        ctx.mkGt(highestBid.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0))
      )
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("bid") = candidates("bid")
    sm.candidateConditionGuards("endAuction") = candidates("endAuction")
    
    sm.synthesize(posTrace, negTrace, candidates)
    
    // 验证合成结果
    sm.conditionGuards("bid") should not be null
    sm.conditionGuards("endAuction") should not be null
    
    ctx.close()
  }

  test("synthesize should handle unsatisfiable constraints") {
    val ctx = new Context()
    val sm = new StateMachine("UnsatTest", ctx)
    
    val (state, stateOut) = sm.addState("state", ctx.mkIntSort())
    sm.setInit(ctx.mkEq(state, ctx.mkInt(0)))
    
    val trParams = List()
    val trGuard = ctx.mkTrue()
    val trFunc = ctx.mkEq(stateOut, ctx.mkAdd(state.asInstanceOf[Expr[ArithSort]], ctx.mkInt(1)))
    sm.addTr("transition", trParams, trGuard, trFunc)
    
    sm.addOnce()
    
    // 创建矛盾的约束：要求状态既为0又为1
    val posTrace = List(
      List(
        List(
          ctx.mkString("transition").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(state, ctx.mkInt(0)).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    val negTrace = List(
      List(
        List(
          ctx.mkString("transition").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(state, ctx.mkInt(0)).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    val candidates = Map[String, List[Expr[BoolSort]]](
      "transition" -> List(ctx.mkTrue(), ctx.mkFalse())
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("transition") = candidates("transition")
    
    // 测试不满足约束的情况
    noException should be thrownBy {
      sm.synthesize(posTrace, negTrace, candidates)
    }
    
    ctx.close()
  }

  test("synthesize should work with boolean state variables") {
    val ctx = new Context()
    val sm = new StateMachine("BooleanTest", ctx)
    
    val (flag, flagOut) = sm.addState("flag", ctx.mkBoolSort())
    sm.setInit(ctx.mkEq(flag, ctx.mkFalse()))
    
    val toggleParams = List()
    val toggleGuard = ctx.mkTrue()
    val toggleFunc = ctx.mkEq(flagOut, ctx.mkNot(flag.asInstanceOf[Expr[BoolSort]]))
    sm.addTr("toggle", toggleParams, toggleGuard, toggleFunc)
    
    sm.addOnce()
    
    val posTrace = List(
      List(
        List(
          ctx.mkString("toggle").asInstanceOf[Expr[BoolSort]],
          ctx.mkEq(flag, ctx.mkFalse()).asInstanceOf[Expr[BoolSort]]
        )
      )
    )
    
    val candidates = Map[String, List[Expr[BoolSort]]](
      "toggle" -> List(ctx.mkTrue(), flag.asInstanceOf[Expr[BoolSort]], ctx.mkNot(flag.asInstanceOf[Expr[BoolSort]]))
    )
    
    // 设置候选条件保护
    sm.candidateConditionGuards("toggle") = candidates("toggle")
    
    val neg = List[List[List[Expr[BoolSort]]]]()
    
    sm.synthesize(posTrace, neg, candidates)
    
    sm.conditionGuards("toggle") should not be null
    
    ctx.close()
  }
} 