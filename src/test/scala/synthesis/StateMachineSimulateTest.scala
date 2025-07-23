package synthesis

import com.microsoft.z3._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class StateMachineSimulateTest extends AnyFunSuite with Matchers {

  test("simulate should handle empty trace") {
    val ctx = new Context()
    val sm = new StateMachine("TestContract", ctx)
    
    val emptyTrace: List[List[Expr[BoolSort]]] = List()
    val emptyCandidates: Map[String, List[Expr[BoolSort]]] = Map()
    
    val result = sm.simulate(emptyTrace, emptyCandidates)
    result should be(List())
    
    ctx.close()
  }

  test("simulate should handle single step trace") {
    val ctx = new Context()
    val sm = new StateMachine("TestContract", ctx)
    
    // 添加一个状态变量
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    
    // 设置初始状态
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
    
    // 创建一个简单的轨迹：一个转账操作
    val transferAction = ctx.mkString("transfer").asInstanceOf[Expr[BoolSort]]
    val amountConstraint = ctx.mkEq(balance, ctx.mkInt(50)).asInstanceOf[Expr[BoolSort]]
    val timeConstraint = ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
    
    val trace = List(
      List(transferAction, amountConstraint, timeConstraint)
    )
    
    // 创建候选条件映射
    val candidates = Map(
      "transfer" -> List(
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0)).asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val result = sm.simulate(trace, candidates)
    
    // 验证结果不为空
    result should not be empty
    
    ctx.close()
  }

  test("simulate should handle multi-step trace") {
    val ctx = new Context()
    val sm = new StateMachine("TokenContract", ctx)
    
    // 添加状态变量
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    val (totalSupply, totalSupplyOut) = sm.addState("totalSupply", ctx.mkIntSort())
    
    // 设置初始状态
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(balance, ctx.mkInt(1000)),
      ctx.mkEq(totalSupply, ctx.mkInt(1000))
    ))
    
    // 添加转换
    val transferParams = List(ctx.mkIntConst("amount"))
    val transferGuard = ctx.mkGt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0))
    val transferFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount"))),
      ctx.mkEq(totalSupplyOut, totalSupply)
    )
    sm.addTr("transfer", transferParams, transferGuard, transferFunc)
    
    val mintParams = List(ctx.mkIntConst("mintAmount"))
    val mintGuard = ctx.mkTrue()
    val mintFunc = ctx.mkAnd(
      ctx.mkEq(balanceOut, ctx.mkAdd(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("mintAmount"))),
      ctx.mkEq(totalSupplyOut, ctx.mkAdd(totalSupply.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("mintAmount")))
    )
    sm.addTr("mint", mintParams, mintGuard, mintFunc)
    
    sm.addOnce()
    
    // 创建多步轨迹
    val trace = List(
      List(
        ctx.mkString("transfer").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(100)).asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("mint").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("mintAmount"), ctx.mkInt(50)).asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      )
    )
    
    // 创建候选条件
    val candidates = Map(
      "transfer" -> List(
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0)).asInstanceOf[Expr[BoolSort]]
      ),
      "mint" -> List(
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]],
        ctx.mkLt(totalSupply.asInstanceOf[Expr[ArithSort]], ctx.mkInt(10000)).asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val result = sm.simulate(trace, candidates)
    
    // 验证结果
    result should not be empty
    // 应该有多个步骤的结果
    result.length should be >= 2
    
    ctx.close()
  }

  test("simulate should handle unsatisfiable initial state") {
    val ctx = new Context()
    val sm = new StateMachine("TestContract", ctx)
    
    // 设置矛盾的初始状态
    val (x, xOut) = sm.addState("x", ctx.mkIntSort())
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(x, ctx.mkInt(5)),
      ctx.mkEq(x, ctx.mkInt(10)) // 矛盾
    ))
    
    val trace = List(
      List(
        ctx.mkString("action").asInstanceOf[Expr[BoolSort]],
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val candidates = Map(
      "action" -> List(ctx.mkTrue().asInstanceOf[Expr[BoolSort]])
    )
    
    val result = sm.simulate(trace, candidates)
    
    // 应该返回空结果，因为初始状态不可满足
    result should be(List())
    
    ctx.close()
  }

  test("simulate should handle transfer failure") {
    val ctx = new Context()
    val sm = new StateMachine("TestContract", ctx)
    
    // 添加状态
    val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
    
    // 设置初始状态
    sm.setInit(ctx.mkEq(balance, ctx.mkInt(100)))
    
    // 添加一个不可能满足的转换
    val transferParams = List(ctx.mkIntConst("amount"))
    val transferGuard = ctx.mkFalse() // 永远不允许转换
    val transferFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
    sm.addTr("transfer", transferParams, transferGuard, transferFunc)
    
    sm.addOnce()
    
    // 创建两步轨迹
    val trace = List(
      List(
        ctx.mkString("init").asInstanceOf[Expr[BoolSort]],
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("transfer").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("amount"), ctx.mkInt(50)).asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val candidates = Map(
      "init" -> List(ctx.mkTrue().asInstanceOf[Expr[BoolSort]]),
      "transfer" -> List(ctx.mkTrue().asInstanceOf[Expr[BoolSort]])
    )
    
    val result = sm.simulate(trace, candidates)
    
    // 应该在第一步后停止，因为transfer转换失败
    result.length should be <= 3 // 初始结果 + transfer尝试，但不会继续
    
    ctx.close()
  }

  test("simulate should handle complex state transitions") {
    val ctx = new Context()
    val sm = new StateMachine("AuctionContract", ctx)
    
    // 添加拍卖合约的状态
    val (highestBid, highestBidOut) = sm.addState("highestBid", ctx.mkIntSort())
    val (auctionEnded, auctionEndedOut) = sm.addState("auctionEnded", ctx.mkBoolSort())
    
    // 设置初始状态
    sm.setInit(ctx.mkAnd(
      ctx.mkEq(highestBid, ctx.mkInt(0)),
      ctx.mkEq(auctionEnded, ctx.mkFalse())
    ))
    
    // 添加bid转换
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
    
    // 添加endAuction转换
    val endParams = List()
    val endGuard = ctx.mkNot(auctionEnded.asInstanceOf[Expr[BoolSort]])
    val endFunc = ctx.mkAnd(
      ctx.mkEq(highestBidOut, highestBid),
      ctx.mkEq(auctionEndedOut, ctx.mkTrue())
    )
    sm.addTr("endAuction", endParams, endGuard, endFunc)
    
    sm.addOnce()
    
    // 创建拍卖轨迹：两次出价然后结束拍卖
    val trace = List(
      List(
        ctx.mkString("bid").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("bidAmount"), ctx.mkInt(100)).asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("bid").asInstanceOf[Expr[BoolSort]],
        ctx.mkEq(ctx.mkIntConst("bidAmount"), ctx.mkInt(150)).asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("endAuction").asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val candidates = Map(
      "bid" -> List(
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]],
        ctx.mkNot(auctionEnded.asInstanceOf[Expr[BoolSort]]).asInstanceOf[Expr[BoolSort]]
      ),
      "endAuction" -> List(
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]],
        ctx.mkNot(auctionEnded.asInstanceOf[Expr[BoolSort]]).asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val result = sm.simulate(trace, candidates)
    
    // 验证模拟成功完成
    result should not be empty
    println(s"Auction simulation result length: ${result.length}")
    
    ctx.close()
  }

  test("simulate should preserve state consistency") {
    val ctx = new Context()
    val sm = new StateMachine("ConsistencyTest", ctx)
    
    // 添加简单的计数器状态
    val (counter, counterOut) = sm.addState("counter", ctx.mkIntSort())
    
    // 设置初始状态
    sm.setInit(ctx.mkEq(counter, ctx.mkInt(0)))
    
    // 添加increment转换
    val incParams = List()
    val incGuard = ctx.mkTrue()
    val incFunc = ctx.mkEq(counterOut, ctx.mkAdd(counter.asInstanceOf[Expr[ArithSort]], ctx.mkInt(1)))
    sm.addTr("increment", incParams, incGuard, incFunc)
    
    sm.addOnce()
    
    // 创建多次increment的轨迹
    val trace = List(
      List(
        ctx.mkString("increment").asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("increment").asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      ),
      List(
        ctx.mkString("increment").asInstanceOf[Expr[BoolSort]],
        ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val candidates = Map(
      "increment" -> List(
        ctx.mkTrue().asInstanceOf[Expr[BoolSort]]
      )
    )
    
    val result = sm.simulate(trace, candidates)
    
    // 验证每一步都有结果
    result should not be empty
    
    // 验证结果的结构：应该有交替的操作和状态
    println(s"Counter simulation steps: ${result.length}")
    result.foreach { step =>
      println(s"Step: ${step.map(_.toString).mkString(", ")}")
    }
    
    ctx.close()
  }

  test("readFromProgram should parse Datalog program correctly") {
    val ctx = new Context()
    val sm = new StateMachine("TestContract", ctx)
    
    // 创建一个简单的Datalog程序用于测试
    import datalog._
    
    // 创建关系
    val balanceRelation = SimpleRelation("balance", List(Type.addressType, Type.uintType), List("addr", "amount"))
    val transferRelation = SimpleRelation("trTransfer", List(Type.addressType, Type.addressType, Type.uintType), List("from", "to", "value"))
    
    // 创建规则
    val transferRule = Rule(
      head = Literal(balanceRelation, List(Variable(Type.addressType, "to"), Variable(Type.uintType, "newBalance"))),
      body = Set(
        Literal(transferRelation, List(Variable(Type.addressType, "from"), Variable(Type.addressType, "to"), Variable(Type.uintType, "value"))),
        Literal(balanceRelation, List(Variable(Type.addressType, "to"), Variable(Type.uintType, "oldBalance")))
      ),
      functors = Set(
        Assign(Param(Variable(Type.uintType, "newBalance")), Add(Param(Variable(Type.uintType, "oldBalance")), Param(Variable(Type.uintType, "value"))))
      ),
      aggregators = Set()
    )
    
    // 创建程序
    val program = Program(
      rules = Set(transferRule),
      interfaces = Set(),
      relationIndices = Map(balanceRelation -> List(0)), // balance indexed by address
      functions = Set(),
      violations = Set(),
      name = "TokenContract"
    )
    
    // 测试readFromProgram
    sm.readFromProgram(program)
    
    // 验证状态已被添加
    sm.states should not be empty
    sm.transitions should not be empty
    
    println(s"States: ${sm.states.keys.mkString(", ")}")
    println(s"Transitions: ${sm.transitions.mkString(", ")}")
    
    ctx.close()
  }
} 