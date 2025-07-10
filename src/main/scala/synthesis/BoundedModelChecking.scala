package synthesis

import scala.collection.mutable
import com.microsoft.z3.{BoolExpr, BoolSort, Context, Expr, Solver, Sort, Status, Model}
import verification.TransitionSystem

object BoundedModelChecking {
  private var index: Int = 0

  def pureName(name: String): String = {
    // println("pureorigin:"+name)
    var name_ex = ""
    if (name.contains("\\|"))
      name_ex = name.substring(name.indexOf("\\|")+1, name.indexOf("\\|", 1))
    else {
      if (name.contains("|")) name_ex = name.substring(name.indexOf("|")+1, name.indexOf("|", 1)) else name_ex = name
    }
    // if (name_ex.contains(":")) println("pure1:"+name_ex.substring(0, name_ex.indexOf(':'))) else println("pure2:"+name_ex)
    if (name_ex.contains(":")) name_ex.substring(0, name_ex.indexOf(':')) else name_ex
  }

  def fresh(round: Int, ctx: Context, name: String, sort: Sort): Expr[_] = {
    index += 1
    // println("fresh:"+s"${name}:r${round}:i${index}")
    ctx.mkConst(s"${name}:r${round}:i${index}", sort)
  }

  def zipp(xs: Array[Expr[_]], ys: Array[Expr[_]]): Array[(Expr[_], Expr[_])] = {
    xs.zip(ys)
  }

  def bmc(ctx: Context, init: BoolExpr, trans: BoolExpr, goal: BoolExpr,
          fvs: Array[Expr[_]], xs: Array[Expr[_]], xns: Array[Expr[_]]): Array[mutable.Map[String, Expr[_]]] = {
    val solver = ctx.mkSolver()
    // solver.reset("timeout", 2000)
    solver.add(init)
    
    var count = 0
    var currentTrans = trans
    var currentGoal = goal
    var currentXs = xs
    var currentXns = xns
    var currentFvs = fvs

    while (count <= 20) {
      count += 1
      println(count)
      val p = fresh(count, ctx, "P", ctx.getBoolSort).asInstanceOf[BoolExpr]
      solver.add(ctx.mkImplies(p, currentGoal))

      val res = solver.check(p)
      if (res == Status.SATISFIABLE) {
        println("Counterexample found:")
        val extractedValues = extractModel(solver.getModel)
        // println(extractedValues)
        return extractedValues
      }

      solver.add(currentTrans)

      //val ys = currentXs.map(x => println(x.getSort))
      val ys = currentXs.map(x => fresh(count, ctx, pureName(x.toString()), x.getSort().asInstanceOf[Sort]))
      val nfvs = currentFvs.map(f => fresh(count, ctx, pureName(f.toString()), f.getSort().asInstanceOf[Sort]))

      val (fromExprs, toExprs) = zipp(currentXns ++ currentXs ++ currentFvs, ys ++ currentXns ++ nfvs).unzip
      currentTrans = currentTrans.substitute(fromExprs.toArray, toExprs.toArray).asInstanceOf[BoolExpr]

      // currentTrans = currentTrans.substitute(
      //   zipp(currentXns ++ currentXs ++ currentFvs, ys ++ currentXns ++ nfvs): _*
      // ).asInstanceOf[BoolExpr]
      
      val (fromExprsGoal, toExprsGoal) = zipp(currentXs, currentXns).unzip
      currentGoal = currentGoal.substitute(fromExprsGoal.toArray, toExprsGoal.toArray).asInstanceOf[BoolExpr]

      // currentGoal = currentGoal.substitute(zipp(currentXs, currentXns): _*).asInstanceOf[BoolExpr]
      
      currentXs = currentXns
      currentXns = ys
      currentFvs = nfvs
    }
    Array[mutable.Map[String, Expr[_]]]()
  }

def extractModel(model: Model): Array[mutable.Map[String, Expr[_]]] = {
  val pmodel = model.getDecls.map { d =>
    val name = d.getName.toString
    val value = model.getConstInterp(d)
    name -> value
  }.toMap
  categorizePModel(pmodel)
}

def categorizePModel(pmodel: Map[String, Expr[_]]): Array[mutable.Map[String, Expr[_]]] = {
  var maxrd = 0
  pmodel.foreach { case (key, value) =>
    val rd =
      if (key.contains(":r")) {
        key.substring(key.indexOf(":r")+2, key.indexOf(":i")).toInt
      }
      else if (key.contains("'")) 1 
      else 0
    maxrd = math.max(maxrd, rd)
  }
  val categorized = Array.fill(maxrd-1)(mutable.Map[String, Expr[_]]())
  // categorized.foreach(map => println(map.mkString(", ")))
  pmodel.foreach { case (key, value) =>
    if (pureName(key) == "P") {}
      else{
      val round =
        if (key.contains(":r")) {
          key.substring(key.indexOf(":r")+2, key.indexOf(":i")).toInt
        }
        else if (key.contains("_next")) 1
        else 0
      val purename = pureName(key)
      val ppurename = if (purename.contains("_next")) purename.substring(0, purename.indexOf("_next")) else purename
      categorized(round)(ppurename) = value
    }
  }
  // categorized.foreach(map => println(map.mkString(", ")))
  categorized
}



  // def extractModel(model: com.microsoft.z3.Model): ??? = {
  //   //To be implemented
  //   //get the trace from the model
  //   //example: ["transferFrom", n==1]
  // }

  def testbmc(): Unit = {
    val ctx = new Context()
    val bvSize = 4
    val bvSort = ctx.mkBitVecSort(bvSize)
    val x = ctx.mkBVConst("x", bvSize)
    val x1 = ctx.mkBVConst("x_next", bvSize)
    val init = ctx.mkEq(x, ctx.mkBV(0, bvSize))
    val trans = ctx.mkEq(x1, ctx.mkBVAdd(x, ctx.mkBV(3, bvSize)))
    val goal = ctx.mkEq(x, ctx.mkBV(15, bvSize))
    
    val result = bmc(ctx, init, trans, goal, Array(), Array(x), Array(x1))
    var id = 0
    for (elem <- result) {
      print(id+": ")
      println(elem.mkString(", "))
      id = id + 1
    }


    ctx.close()
  }

  def testbmc2(): Unit = {
    val ctx = new Context()
    val ts = TransitionSystem("Crowdsale", ctx)

    val GOAL = 10000
    val CLOSETIME = 10000

    val addrSort = ctx.mkBitVecSort(256)

    val (state, stateOut) = ts.newVar("state", ctx.mkBitVecSort(2))
    val OPEN = ctx.mkBV(0, 2)
    val SUCCESS = ctx.mkBV(1, 2)
    val REFUND = ctx.mkBV(2, 2)

    val (deposits, depositsOut) = ts.newVar("deposits", ctx.mkArraySort(addrSort, ctx.mkBitVecSort(256)))
    val (totalDeposits, totalDepositsOut) = ts.newVar("totalDeposits", ctx.mkBitVecSort(256))
    val (raised, raisedOut) = ts.newVar("raised", ctx.mkBitVecSort(256))
    val (aux_withdraw, aux_withdrawOut) = ts.newVar("aux_withdraw", ctx.mkBoolSort())
    val (aux_refund, aux_refundOut) = ts.newVar("aux_refund", ctx.mkBoolSort())
    val (func, funcOut) = ts.newVar("func", ctx.mkStringSort())
    val (timestamp, timestampOut) = ts.newVar("now", ctx.mkBitVecSort(256))

    val p = ctx.mkConst("p", addrSort)
    val amount = ctx.mkBVConst("amount", 256)

    val init = ctx.mkAnd(
      ctx.mkForall(Array(p), ctx.mkEq(ctx.mkSelect(deposits, p), ctx.mkBV(0, 256)), 1, null, null, ctx.mkSymbol("Q1"), ctx.mkSymbol("skid1")),
      ctx.mkEq(raised, ctx.mkBV(0, 256)),
      ctx.mkEq(totalDeposits, ctx.mkBV(0, 256)),
      ctx.mkEq(aux_withdraw, ctx.mkFalse()),
      ctx.mkEq(aux_refund, ctx.mkFalse()),
      ctx.mkEq(state, OPEN),
      ctx.mkEq(func, ctx.mkString("init")),
      ctx.mkEq(timestamp, ctx.mkBV(0, 256))
    )

    val trInvest = ctx.mkAnd(
      ctx.mkBVULT(raised, ctx.mkBV(GOAL, 256)),
      ctx.mkEq(stateOut, state),
      ctx.mkEq(raisedOut, ctx.mkBVAdd(raised, amount)),
      ctx.mkEq(depositsOut, ctx.mkStore(deposits, p, ctx.mkBVAdd(ctx.mkSelect(deposits, p), amount))),
      ctx.mkEq(totalDepositsOut, ctx.mkBVAdd(totalDeposits, amount)),
      ctx.mkEq(aux_refundOut, aux_refund),
      ctx.mkEq(aux_withdrawOut, aux_withdraw),
      ctx.mkEq(funcOut, ctx.mkString("invest")),
      ctx.mkBVSGT(timestampOut, timestamp)
    )

    val trCloseSuccess = ctx.mkAnd(
      ctx.mkBVUGE(raised, ctx.mkBV(GOAL, 256)),
      ctx.mkEq(stateOut, SUCCESS),
      ctx.mkEq(depositsOut, deposits),
      ctx.mkEq(raisedOut, raised),
      ctx.mkEq(totalDepositsOut, totalDeposits),
      ctx.mkEq(aux_refundOut, aux_refund),
      ctx.mkEq(aux_withdrawOut, aux_withdraw),
      ctx.mkEq(funcOut, ctx.mkString("close_success")),
      ctx.mkBVSGT(timestampOut, timestamp)
    )

    val trCloseRefund = ctx.mkAnd(
      ctx.mkBVSGT(timestamp, ctx.mkBV(CLOSETIME, 256)),
      ctx.mkBVULT(raised, ctx.mkBV(GOAL, 256)),
      ctx.mkEq(stateOut, REFUND),
      ctx.mkEq(depositsOut, deposits),
      ctx.mkEq(raisedOut, raised),
      ctx.mkEq(totalDepositsOut, totalDeposits),
      ctx.mkEq(aux_refundOut, aux_refund),
      ctx.mkEq(aux_withdrawOut, aux_withdraw),
      ctx.mkEq(funcOut, ctx.mkString("close_refund")),
      ctx.mkBVSGT(timestampOut, timestamp)
    )

    val trClaimRefund = ctx.mkAnd(
      ctx.mkEq(state, REFUND),
      ctx.mkBVULT(raised, ctx.mkBV(GOAL, 256)),
      ctx.mkEq(depositsOut, ctx.mkStore(deposits, p, ctx.mkBV(0, 256))),
      ctx.mkEq(totalDepositsOut, ctx.mkBVSub(totalDeposits, ctx.mkSelect(deposits, p))),
      ctx.mkEq(raisedOut, raised),
      ctx.mkEq(stateOut, state),
      ctx.mkEq(aux_refundOut, ctx.mkTrue()),
      ctx.mkEq(aux_withdrawOut, aux_withdraw),
      ctx.mkEq(funcOut, ctx.mkString("claimrefund")),
      ctx.mkBVSGT(timestampOut, timestamp)
    )

    val trWithdraw = ctx.mkAnd(
      ctx.mkEq(state, SUCCESS),
      ctx.mkEq(totalDepositsOut, ctx.mkBV(0, 256)),
      ctx.mkEq(depositsOut, deposits),
      ctx.mkEq(raisedOut, raised),
      ctx.mkEq(stateOut, state),
      ctx.mkEq(aux_withdrawOut, ctx.mkTrue()),
      ctx.mkEq(aux_refundOut, aux_refund),
      ctx.mkEq(funcOut, ctx.mkString("withdraw")),
      ctx.mkBVSGT(timestampOut, timestamp)
    )
    println(funcOut.getClass)
    ts.setInit(init)
    ts.setTr(ctx.mkOr(trInvest, trCloseRefund, trCloseSuccess, trClaimRefund, trWithdraw), 
            Set(trInvest, trCloseRefund, trCloseSuccess, trClaimRefund, trWithdraw))

    val r2 = ctx.mkNot(ctx.mkAnd(aux_withdraw, aux_refund))
    val g = ctx.mkNot(r2)
    val fvs: Array[Expr[_]] = Array(p, amount)
    val xs: Array[Expr[_]] = Array(deposits, totalDeposits, raised, state, aux_withdraw, aux_refund, func, timestamp)
    val xns: Array[Expr[_]] = Array(depositsOut, totalDepositsOut, raisedOut, stateOut, aux_withdrawOut, aux_refundOut, funcOut, timestampOut)
    index = 0
    val model = bmc(ctx, ts.getInit(), ts.getTr(), g, fvs, xs, xns)
    var id = 0
    for (elem <- model) {
      print(id+": ")
      println(elem.mkString(", "))
      id = id + 1
    }
    ctx.close()
  }
}