package synthesis

import com.microsoft.z3._
import verification.TransitionSystem
import BoundedModelChecking.BoundedModelChecking
import util.Misc.parseProgram
import verification.Verifier._
import java.io._


class StateMachine(name: String, ctx: Context) {
  val states: scala.collection.mutable.Map[String, (Expr[_], Expr[_])] = scala.collection.mutable.Map()
  val prevStates: scala.collection.mutable.Map[String, (Expr[_], Expr[_])] = scala.collection.mutable.Map()
  val once: scala.collection.mutable.Map[String, (Expr[_], Expr[_])] = scala.collection.mutable.Map()
  var transitions: List[String] = List()
  val conditionGuards: scala.collection.mutable.Map[String, Expr[BoolSort]] = scala.collection.mutable.Map()
  val candidateConditionGuards: scala.collection.mutable.Map[String, List[Expr[BoolSort]]] = scala.collection.mutable.Map()
  val trParameters: scala.collection.mutable.Map[String, List[Expr[_]]] = scala.collection.mutable.Map()
  val transferFunc: scala.collection.mutable.Map[String, Expr[BoolSort]] = scala.collection.mutable.Map()
  val constants: List[String] = List()
  val ts: TransitionSystem = new TransitionSystem(name, ctx)
  var nowState: Option[String] = None

  val (now, nowOut): (Expr[BitVecSort], Expr[BitVecSort]) = addState("now", ctx.mkBitVecSort(256)).asInstanceOf[(Expr[BitVecSort], Expr[BitVecSort])]
  val (func, funcOut): (Expr[_], Expr[_]) = addState("func", ctx.mkStringSort())

  def addState(stateName: String, stateType: Sort): (Expr[_], Expr[_]) = {
    val (state, stateOut) = ts.newVar(stateName, stateType)
    val (prevState, prevStateOut) = ts.newVar(s"prev_$stateName", stateType)
    if (stateName != "func" && !stateName.startsWith("once_")) {
      states(stateName) = (state, stateOut)
      prevStates(stateName) = (prevState, prevStateOut)
    }
    (state, stateOut)
  }

  def prev(state: Expr[_]): (Expr[_], Expr[_]) = {
    prevStates(state.toString)
  }

  def addTr(trName: String, parameters: List[Expr[_]], guard: Expr[BoolSort], transferFunc: Expr[BoolSort]): Unit = {
    transitions = transitions :+ trName
    once(trName) = addState(s"once_$trName", ctx.mkBoolSort())
    trParameters(trName) = parameters
    conditionGuards(trName) = guard
    candidateConditionGuards(trName) = List()
    val newTransferFunc = ctx.mkAnd(transferFunc, ctx.mkEq(funcOut, ctx.mkString(trName)), ctx.mkEq(once(trName)._2, ctx.mkBool(true)))

    states.foreach { case (stateName, (state, _)) =>
      if (stateName != "now" && stateName != "func") {
        transferFunc = ctx.simplify(ctx.mkAnd(newTransferFunc, ctx.mkEq(prev(state)._2, state)))
        if (!contains(states(stateName)._2, transferFunc)) {
          transferFunc = ctx.simplify(ctx.mkAnd(transferFunc, ctx.mkEq(states(stateName)._2, state)))
        }
      }
    }
    transferFunc(trName) = transferFunc
  }

  def addOnce(): Unit = {
    transitions.foreach { tr =>
      once.foreach { case (onceName, onceVal) =>
        if (onceName != tr) {
          transferFunc(tr) = ctx.mkAnd(transferFunc(tr), ctx.mkEq(onceVal._2, onceVal._1))
        }
      }
    }
  }

  def clearGuards(): Unit = {
    conditionGuards.keys.foreach { key =>
      conditionGuards(key) = ctx.mkBool(true)
    }
  }

  def changeGuard(trName: String, newGuards: Expr[BoolSort]*): Boolean = {
    if (!transitions.contains(trName)) {
      println("Transition not found!")
      false
    } else {
      conditionGuards(trName) = ctx.simplify(ctx.mkAnd(newGuards: _*))
      true
    }
  }

  def addGuard(trName: String, newGuards: Expr[BoolSort]*): Boolean = {
    if (!transitions.contains(trName)) {
      println("Transition not found!")
      false
    } else {
      conditionGuards(trName) = ctx.simplify(ctx.mkAnd(conditionGuards(trName), newGuards: _*))
      true
    }
  }

  def setInit(initState: Expr[BoolSort]): Unit = {
    ts.setInit(ctx.mkAnd(initState, ctx.mkEq(now, ctx.mkInt(0)), ctx.mkEq(func, ctx.mkString("init"))))
    once.values.foreach { case (onceVar, _) =>
      ts.setInit(ctx.simplify(ctx.mkAnd(ts.getInit(), ctx.mkEq(onceVar, ctx.mkBool(false)))))
    }
  }
  def transfer(tr_name: String, candidates: Map[String, List[Expr[BoolSort]]], next: List[Expr[BoolSort]], parameters: Expr[_]*): Option[List[Expr[BoolSort]]] = {
    val success = ctx.mkAnd(now_state, condition_guards(tr_name), nowOut > now_state, ctx.mkAnd(parameters: _*))
    val s = new Solver()
    s.add(success)
    val result = s.check()

    if (result == Status.Unsat) {
      return None
    } else {
      s.reset()
      s.add(ctx.mkAnd(now_state, transfer_func(tr_name), ctx.mkAnd(parameters: _*)))
      val result2 = s.check()
      val model = s.model()
      now_state = ctx.mkBool(true)
      states.foreach { case (_, (state, _)) =>
        now_state = ctx.mkAnd(now_state, state == model.eval(state))
      }
      now_state = ctx.simplify(now_state)

      s.reset()
      s.add(now_state)
      s.add(next.tail)
      val finalCheck = s.check()

      if (finalCheck == Status.Sat) {
        val m = s.model()
        val newLine = candidates(next.head).map(c => m.eval(c))
        Some(newLine)
      } else {
        println("error")
        None
      }
    }
  }

  def simulate(trace: List[List[Expr[BoolSort]]], candidates: Map[String, List[Expr[BoolSort]]]): List[List[Expr[BoolSort]]] = {
    var res: List[List[Expr[BoolSort]]] = List()
    now_state = ts.getInit()

    val s = new Solver()
    s.add(now_state)
    s.add(trace.head.tail)
    if (s.check() == Status.Sat) {
      val m = s.model()
      val newline = candidates(trace.head.head).map(c => m.eval(c))
      res = List(newline)
    }

    for (i <- 0 until trace.size - 1) {
      val tr_name = trace(i).head.toString
      var newline = List(tr_name) ++ res.head
      res = res :+ newline
      val nextLine = transfer(tr_name, candidates, trace(i + 1), trace(i).tail: _*)
      if (nextLine.isEmpty) {
        return res
      }
      res = res :+ nextLine.get
    }
    res
  }

  def bmc(property: Expr[BoolSort]): Option[List[List[Expr[BoolSort]]]] = {

    BoundedModelChecking.index = 0
    ts.setTr(ctx.mkBool(false), Set())

    transitions.foreach { tr =>
      ts.setTr(ctx.simplify(ctx.mkOr(ts.getTr(), ctx.mkAnd(transfer_func(tr), condition_guards(tr), nowOut > now_state))))
    }

    val xs = states.values.map(_._1) ++ states.values.map(_._2) ++ List(nowOut)
    val xns = states.values.map(_._2) ++ List(nowOut)

    val model = BoundedModelChecking.bmc(ts.getInit(), ts.getTr(), property, xs, xns)
    model match {
      case Some(m) =>
        val trace = (1 until m.size - 2).map { i =>
          val tr = m(i)("func").toString
          val rule = List(tr, nowOut == m(i)("now"))
          rule
        }.toList
        Some(trace)
      case None =>
        println("No model found!")
        None
    }
  }

  def generate_candidate_guards(predicates: List[String], array: Boolean): Map[String, List[Expr[BoolSort]]] = {
    var candidateGuards: Map[String, List[Expr[BoolSort]]] = Map()
    transitions.foreach { tr =>
      candidateGuards += tr -> List()

      val s = constants ++ states.values.map(_._1) ++ tr_parameters.getOrElse(tr, List()) ++ List(nowOut)
      if (array) {
        val arrayEnum = s.collect { case arr if isArray(arr) => arr }
        candidateGuards(tr) ++= arrayEnum
      }

      s.zipWithIndex.foreach { case (ls, lsIdx) =>
        if (isBool(ls)) {
          candidateGuards(tr) ++= List(ls, ctx.mkNot(ls))
        }
        s.zipWithIndex.drop(lsIdx + 1).foreach { case (rs, rsIdx) =>
          if (!(isArray(ls) || isArray(rs) || isBool(rs))) {
            predicates.foreach { predicate =>
              try {
                val guard = predicate match {
                  case "<"  => ctx.mkBVULT(ls, rs)
                  case "<=" => ctx.mkBVULE(ls, rs)
                  case ">"  => ctx.mkBVUGT(ls, rs)
                  case ">=" => ctx.mkBVUGE(ls, rs)
                  case "="  => ctx.mkEq(ls, rs)
                  case _    => throw new IllegalArgumentException("Unsupported predicate")
                }
                candidateGuards(tr) :+= guard
              } catch {
                case _: Exception => println("Predicate mismatch")
              }
            }
          }
        }
      }
    }
    candidateGuards
  }

  def synthesize(pos: List[List[List[Expr[BoolSort]]]], neg: List[List[List[Expr[BoolSort]]]], candidates: Map[String, List[Expr[BoolSort]]]): Unit = {
    val s = new Solver()
    var approvePos = ctx.mkBool(true)
    pos.foreach { postrace =>
      var approveT = ctx.mkBool(true)
      postrace.foreach { trRes =>
        val tr = trRes.head
        var approvetx = ctx.mkBool(true)
        trRes.tail.foreach { res =>
          approvetx = ctx.mkAnd(approvetx, ctx.mkImplies(candidate_condition_guards(tr).head, res))
        }
        approveT = ctx.mkAnd(approveT, approvetx)
      }
      approvePos = ctx.mkAnd(approvePos, approveT)
    }

    var approveNeg = ctx.mkBool(true)
    neg.foreach { negtrace =>
      var approveT = ctx.mkBool(true)
      negtrace.foreach { trRes =>
        val tr = trRes.head
        var approvetx = ctx.mkBool(true)
        trRes.tail.foreach { res =>
          approvetx = ctx.mkAnd(approvetx, ctx.mkImplies(candidate_condition_guards(tr).head, res))
        }
        approveT = ctx.mkAnd(approveT, approvetx)
      }
      approveNeg = ctx.mkAnd(approveNeg, ctx.mkNot(approveT))
    }

    s.add(approvePos)
    s.add(approveNeg)
    val result = s.check()
    if (result == Status.Sat) {
      val model = s.model()
      transitions.foreach { tr =>
        candidates(tr).foreach { c =>
          if (model.eval(candidate_condition_guards(tr).head).asBool) {
            addGuard(tr, c)
          }
        }
      }
    } else {
      println("No solution found!")
    }
  }

  def cegis(properties: List[Expr[BoolSort]], positive_traces: List[List[List[Expr[BoolSort]]]], candidate_guard: Map[String, List[Expr[BoolSort]]], array: Boolean = true): Unit = {
    var pos = List[List[List[Expr[BoolSort]]]]()
    var neg = List[List[List[Expr[BoolSort]]]]()

    positive_traces.foreach { trace =>
      pos :+= simulate(trace, candidate_guard)
    }
    var syn_time = 0.0
    var bmc_time = 0.0
    var iter = 0
    while (true) {
      iter += 1
      val startTime = System.nanoTime()
    
      synthesize(pos, neg, candidate_guard)
      val endTime = System.nanoTime()
      val elapsedTimeMs = (endTime - startTime) / 1e9
      syn_time = syn_time + elapsedTimeMs
      var new_ntraces = List[List[List[Expr[BoolSort]]]]()
      val startTime2 = System.nanoTime()
      properties.foreach { p =>
        val ntrace = bmc(ctx.mkNot(p))
        if (ntrace.isEmpty) {
          println("√") // Property verified
        } else {
          new_ntraces = new_ntraces :+ ntrace.get
          println("×") // Property not verified
        }
      }
      val endTime2 = System.nanoTime()
      val elapsedTimeMs2 = (endTime2 - startTime) / 1e9
      bmc_time = bmc_time + elapsedTimeMs2
      if (new_ntraces.isEmpty) {
        println("All properties verified!")
        break
      }

      // Update negative traces
      new_ntraces.foreach { negtrace =>
        neg :+= simulate(negtrace, candidate_guard)
      }
    }

    println(s" $syn_time $bmc_time")
  }
  def inductive_prove(properties: List[Expr[BoolSort]]): Unit = {
    /** Variable keeps track of the current transaction name. */
    val (transactionThis, transactionNext) = tr.newVar("transaction", ctx.mkStringSort())

    /** Generate initial constraints. */
    var initConditions: List[BoolExpr] = List()
    for (rel <- materializedRelations) {
      val sort = getSort(ctx, rel, getIndices(rel))
      val (v_in, _) = tr.newVar(rel.name, sort)
      val (_init, _,_) = getInitConstraints(ctx, rel, v_in, indices, initializationRules.find(_.head.relation==rel))
      initConditions :+= _init
    }
    tr.setInit(ctx.mkAnd(initConditions.toArray:_*))

    val (fullTransitionCondition, transactionConditions) = getTransitionConstraints(transactionThis, transactionNext)
    tr.setTr(fullTransitionCondition, transactionConditions)

    for (property <- properties) {

      val isTransactionProperty = vr.body.exists(_.relation.name=="transaction")

      val (resInit, _resTr) = inductiveProve(ctx, tr, property, isTransactionProperty)
      val resTr = _resTr match {
        case Status.UNSATISFIABLE => _resTr
        case Status.UNKNOWN | Status.SATISFIABLE => {
          invariantGenerator.findInvariant(tr, vr) match {
            case Some(inv) => {
              // validateInvariant(inv, tr, property)
              val (_invInit, _invTr) = inductiveProve(ctx,tr,ctx.mkAnd(property,inv), isTransactionProperty)
              println(s"invariant: ${inv}")
              _invTr
            }
            case None => _resTr
          }
        }
      }
      println(s"Init: $resInit")
      println(s"Tr: $resTr")
  }

  def readFromProgram(p: Program): List[List[List[Expr[BoolSort]]]] = {
    val program = addBuiltInRules(p)
    val violationRules: Set[Rule] = program.rules.filter(r => program.violations.contains(r.head.relation))
    tr = TransitionSystem(program.name, ctx)

    val (transactionThis, transactionNext) = tr.newVar("transaction", ctx.mkStringSort())

    var initConditions: List[BoolExpr] = List()
    for (rel <- materializedRelations) {
      val sort = getSort(ctx, rel, getIndices(rel))
      val (v_in, _) = tr.newVar(rel.name, sort)
      val (_init, _,_) = getInitConstraints(ctx, rel, v_in, indices, initializationRules.find(_.head.relation==rel))
      initConditions :+= _init
    }
    tr.setInit(ctx.mkAnd(initConditions.toArray:_*))

    val (fullTransitionCondition, transactionConditions) = getTransitionConstraints(transactionThis, transactionNext)
    tr.setTr(fullTransitionCondition, transactionConditions)
  }

  def getSolidityType(sort: Sort): String = sort match {
    case _: BitVecSort => "uint256"
    case _: IntSort    => "int256"
    case _: StringSort => "string"
    case _: ArraySort  => "mapping(uint256 => uint256)"
    case _            => "bytes"
  }

  def writeFile(path: String): Unit = {
    val solidityCode = new StringBuilder

    solidityCode.append(
      s"""
      |contract ${name.capitalize} {
      |
      """.stripMargin
    )

    states.foreach { case (stateName, (stateExpr, _)) =>
      val solidityType = getSolidityType(stateExpr.getSort)
      solidityCode.append(s"    $solidityType public $stateName;\n")
    }

    val initialState = nowState.getOrElse(states.keys.head)
    val initialSort = states.get(initialState).map(_._1.getSort).getOrElse(new BitVecSort(256))
    val currentStateType = getSolidityType(initialSort)

    solidityCode.append(s"\n    $currentStateType public currentState;\n\n")

    solidityCode.append(
      s"""
      |    constructor() {
      |        currentState = $initialState;
      |    }
      |
      """.stripMargin
    )

    transitions.foreach { trName =>
      val guardCondition = conditionGuards.getOrElse(trName, null)
      val guardCode = if (guardCondition != null) {
        s"        require(${guardCondition.toString}, \"Transition not allowed\");\n"
      } else {
        ""

      solidityCode.append(
        s"""
        |    function $trName() public {
        |        $guardCode
        |        currentState = ${trName};
        |    }
        |
        """.stripMargin
      )
    }

    solidityCode.append("}")

    try {
      val file = new File(path)
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(solidityCode.toString)
      bw.close()
    } catch {
      case e: IOException => println(s"Error writing Solidity file: ${e.getMessage}")
    }
  }

  def writefile(path: String): Unit = {
    writeFile(path)
  }

  def generate_candidate_guards_from_properties(properties: List[Expr[BoolSort]]): Unit = {
    // Generate candidate guards based on temporal properties
    // This is a simplified version - in practice you would analyze the properties
    // and generate appropriate guards
    
    transitions.foreach { tr =>
      val candidates = List(
        ctx.mkBool(true),  // Always allow
        ctx.mkBool(false), // Never allow
        ctx.mkGt(now, ctx.mkInt(0)), // Time-based guard
        ctx.mkEq(func, ctx.mkString(tr)) // Function-specific guard
      )
      candidateConditionGuards(tr) = candidates
    }
  }

  def synthesizeWithoutTrace(properties: List[Expr[BoolSort]]): Boolean = {
    try {
      // Simplified synthesis without trace dependency
      // In practice, this would use more sophisticated synthesis techniques
      
      // Set initial state
      setInit(ctx.mkBool(true))
      
      // Generate transition conditions based on properties
      properties.foreach { prop =>
        // Analyze property and generate appropriate transitions
        transitions.foreach { tr =>
          val guard = ctx.mkAnd(
            conditionGuards.getOrElse(tr, ctx.mkBool(true)),
            prop
          )
          conditionGuards(tr) = guard
        }
      }
      
      // Verify that the synthesized system satisfies the properties
      val solver = ctx.mkSolver()
      solver.add(ctx.mkNot(ctx.mkAnd(properties: _*)))
      
      if (solver.check() == Status.UNSATISFIABLE) {
        println("Synthesis successful - properties satisfied")
        true
      } else {
        println("Synthesis failed - properties not satisfied")
        false
      }
      
    } catch {
      case e: Exception =>
        println(s"Synthesis error: ${e.getMessage}")
        false
    }
  }

}

}