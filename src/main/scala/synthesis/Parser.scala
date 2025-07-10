import scala.io.Source
import com.microsoft.z3._

object Parser {
  val ctx: Context = new Context()
  
  def parseProperty(propertyPath: String): List[Expr] = {
    val lines = Source.fromFile(propertyPath).getLines().toList
    lines.map(parseExpr)
  }
  
  def parsePropertyWithoutTrace(propertyPath: String): List[Expr] = {
    val lines = Source.fromFile(propertyPath).getLines().toList
    lines.filterNot(_.trim.isEmpty)
         .filterNot(_.trim.startsWith("//"))
         .map(parseExprWithoutTrace)
  }
  
  def parseTrace(tracePath: String): List[Expr] = {
    val lines = Source.fromFile(tracePath).getLines().toList
    lines.map(parseAction)
  }
  
  private def parseExpr(line: String): Expr = {
    val solver = ctx.mkSolver()
    val reOnce = "♦\((.*?)\)".r
    val reExpr = "□\((.*?)\)".r

    val expr = line match {
      case reOnce(inner) => statemachine.once(parseZ3Expr(inner))
      case reExpr(inner) => ctx.mkForall(Array(), parseZ3Expr(inner), 1, null, null, null, null)
      case _ => throw new IllegalArgumentException(s"Invalid property format: $line")
    }
    expr
  }
  
  private def parseExprWithoutTrace(line: String): Expr = {
    val reOnce = "♦\((.*?)\)".r
    val reAlways = "□\((.*?)\)".r
    val reImplies = "(.*?) → (.*?)".r
    val reAnd = "(.*?) ∧ (.*?)".r
    val reOr = "(.*?) ∨ (.*?)".r

    val expr = line match {
      case reOnce(inner) => parseZ3Expr(inner)
      case reAlways(inner) => parseZ3Expr(inner)
      case reImplies(left, right) => 
        val leftExpr = parseZ3Expr(left.trim)
        val rightExpr = parseZ3Expr(right.trim)
        ctx.mkImplies(leftExpr, rightExpr)
      case reAnd(left, right) =>
        val leftExpr = parseZ3Expr(left.trim)
        val rightExpr = parseZ3Expr(right.trim)
        ctx.mkAnd(leftExpr, rightExpr)
      case reOr(left, right) =>
        val leftExpr = parseZ3Expr(left.trim)
        val rightExpr = parseZ3Expr(right.trim)
        ctx.mkOr(leftExpr, rightExpr)
      case _ => parseZ3Expr(line.trim)
    }
    expr
  }
  
  private def parseZ3Expr(expr: String): BoolExpr = {
    val eqPattern = "(\w+)\((.*?)\) → (.*?)".r
    expr match {
      case eqPattern(left, args, right) =>
        val leftExpr = ctx.mkBoolConst(left + "(" + args + ")")
        val rightExpr = ctx.mkBoolConst(right)
        ctx.mkImplies(leftExpr, rightExpr)
      case _ => ctx.mkBoolConst(expr)
    }
  }
  
  private def parseAction(line: String): Expr = {
    val eventPattern = "(\w+)\((.*?)\)@(\d+)".r
    line match {
      case eventPattern(event, params, time) =>
        val args = params.split(",").map(_.trim).filter(_.nonEmpty).map(parseParam)
        ctx.mkApp(ctx.mkFuncDecl(event, args.map(_.getSort), ctx.getBoolSort), args: _*)
      case _ => throw new IllegalArgumentException(s"Invalid trace format: $line")
    }
  }
  
  private def parseParam(param: String): Expr = {
    val kvPattern = "(\w+)=(\w+)".r
    param match {
      case kvPattern(key, value) => ctx.mkConst(key, ctx.getIntSort)
      case _ => ctx.mkConst(param, ctx.getIntSort)
    }
  }
}