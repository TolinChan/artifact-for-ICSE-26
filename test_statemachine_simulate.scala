import synthesis.StateMachine
import com.microsoft.z3._
import datalog._

object TestStateMachineSimulate {
  def main(args: Array[String]): Unit = {
    println("Testing StateMachine simulate functionality...")
    
    val ctx = new Context()
    val sm = new StateMachine("TestToken", ctx)
    
    // 测试1: 基本状态机功能
    println("\n=== Test 1: Basic StateMachine functionality ===")
    testBasicStateMachine(ctx, sm)
    
    // 测试2: readFromProgram功能
    println("\n=== Test 2: readFromProgram functionality ===")
    testReadFromProgram(ctx)
    
    // 测试3: simulate函数
    println("\n=== Test 3: simulate function ===")
    testSimulateFunction(ctx)
    
    ctx.close()
    println("\nAll tests completed!")
  }
  
  def testBasicStateMachine(ctx: Context, sm: StateMachine): Unit = {
    try {
      // 添加状态
      val (balance, balanceOut) = sm.addState("balance", ctx.mkIntSort())
      println(s"Added state: balance")
      
      // 设置初始状态
      sm.setInit(ctx.mkEq(balance, ctx.mkInt(1000)))
      println("Set initial state: balance = 1000")
      
      // 添加转换
      val transferParams = List(ctx.mkIntConst("amount"))
      val transferGuard = ctx.mkGt(balance.asInstanceOf[Expr[ArithSort]], ctx.mkInt(0))
      val transferFunc = ctx.mkEq(balanceOut, ctx.mkSub(balance.asInstanceOf[Expr[ArithSort]], ctx.mkIntConst("amount")))
      sm.addTr("transfer", transferParams, transferGuard, transferFunc)
      println("Added transition: transfer")
      
      sm.addOnce()
      println("Basic StateMachine setup completed successfully!")
      
    } catch {
      case e: Exception =>
        println(s"Error in basic StateMachine test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testReadFromProgram(ctx: Context): Unit = {
    try {
      val sm = new StateMachine("TestContract", ctx)
      
      // 创建简单的Datalog程序
      val balanceRelation = SimpleRelation("balance", List(Type.addressType, Type.uintType), List("addr", "amount"))
      val transferRelation = SimpleRelation("trTransfer", List(Type.addressType, Type.addressType, Type.uintType), List("from", "to", "value"))
      
      val transferRule = Rule(
        head = Literal(balanceRelation, List(Variable(Type.addressType, "to"), Variable(Type.uintType, "newBalance"))),
        body = Set(
          Literal(transferRelation, List(Variable(Type.addressType, "from"), Variable(Type.addressType, "to"), Variable(Type.uintType, "value")))
        ),
        functors = Set(),
        aggregators = Set()
      )
      
      val program = Program(
        rules = Set(transferRule),
        interfaces = Set(),
        relationIndices = Map(balanceRelation -> List(0)),
        functions = Set(),
        violations = Set(),
        name = "TokenContract"
      )
      
      // 测试readFromProgram
      sm.readFromProgram(program)
      println("readFromProgram completed successfully!")
      println(s"States created: ${sm.states.keys.mkString(", ")}")
      println(s"Transitions created: ${sm.transitions.mkString(", ")}")
      
    } catch {
      case e: Exception =>
        println(s"Error in readFromProgram test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
  
  def testSimulateFunction(ctx: Context): Unit = {
    try {
      val sm = new StateMachine("SimulateTest", ctx)
      
      // 设置简单的状态机
      val (counter, counterOut) = sm.addState("counter", ctx.mkIntSort())
      sm.setInit(ctx.mkEq(counter, ctx.mkInt(0)))
      
      val incParams = List()
      val incGuard = ctx.mkTrue()
      val incFunc = ctx.mkEq(counterOut, ctx.mkAdd(counter.asInstanceOf[Expr[ArithSort]], ctx.mkInt(1)))
      sm.addTr("increment", incParams, incGuard, incFunc)
      sm.addOnce()
      
      // 创建轨迹
      val trace = List(
        List(
          ctx.mkString("increment").asInstanceOf[Expr[BoolSort]],
          ctx.mkGt(sm.nowOut.asInstanceOf[Expr[ArithSort]], sm.now.asInstanceOf[Expr[ArithSort]]).asInstanceOf[Expr[BoolSort]]
        )
      )
      
      val candidates = Map(
        "increment" -> List(ctx.mkTrue().asInstanceOf[Expr[BoolSort]])
      )
      
      // 测试simulate
      val result = sm.simulate(trace, candidates)
      println(s"Simulate result length: ${result.length}")
      println("simulate function test completed successfully!")
      
    } catch {
      case e: Exception =>
        println(s"Error in simulate test: ${e.getMessage}")
        e.printStackTrace()
    }
  }
} 