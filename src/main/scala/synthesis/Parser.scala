package synthesis

import scala.io.Source
import com.microsoft.z3._

object Parser {
  val ctx: Context = new Context()
  
  def parseProperty(propertyPath: String): List[Expr[BoolSort]] = {
    val lines = Source.fromFile(propertyPath).getLines().toList
    lines.map(parseExpr)
  }
  
  def parsePropertyWithoutTrace(propertyPath: String, ctx: Context = ctx): List[Expr[BoolSort]] = {
    val lines = Source.fromFile(propertyPath).getLines().toList
    lines.filterNot(_.trim.isEmpty)
         .filterNot(_.trim.startsWith("//"))
         .map(line => parseExprWithoutTrace(line, ctx))
  }
  
  def parseTrace(tracePath: String, ctx: Context = ctx): List[Expr[BoolSort]] = {
    val lines = Source.fromFile(tracePath).getLines().toList
    lines.filterNot(_.trim.isEmpty)
         .filterNot(_.trim.startsWith("//"))
         .flatMap(line => {
           // 处理可能包含多个动作的行（用分号分隔）
           line.split(";").map(_.trim).filter(_.nonEmpty).map(action => parseAction(action, ctx))
         })
  }
  
  private def parseExpr(line: String): Expr[BoolSort] = {
    val solver = ctx.mkSolver()
    val reOnce = "diamond\\((.*?)\\)".r
    val reExpr = "box\\((.*?)\\)".r

    val expr = line match {
      case reOnce(inner) => parseZ3Expr(inner, ctx)
      case reExpr(inner) => ctx.mkForall(Array(), parseZ3Expr(inner, ctx), 1, null, null, null, null)
      case _ => throw new IllegalArgumentException(s"Invalid property format: $line")
    }
    expr
  }
  
  private def parseExprWithoutTrace(line: String, ctx: Context): Expr[BoolSort] = {
    val reOnce = "diamond\\((.*?)\\)".r
    val reAlways = "box\\((.*?)\\)".r
    val reImplies = "(.*?) -> (.*?)".r
    val reAnd = "(.*?) && (.*?)".r
    val reOr = "(.*?) \\|\\| (.*?)".r

    val expr = line match {
      case reOnce(inner) => parseZ3Expr(inner, ctx)
      case reAlways(inner) => parseZ3Expr(inner, ctx)
      case reImplies(left, right) => 
        val leftExpr = parseZ3Expr(left.trim, ctx)
        val rightExpr = parseZ3Expr(right.trim, ctx)
        ctx.mkImplies(leftExpr, rightExpr)
      case reAnd(left, right) =>
        val leftExpr = parseZ3Expr(left.trim, ctx)
        val rightExpr = parseZ3Expr(right.trim, ctx)
        ctx.mkAnd(leftExpr, rightExpr)
      case reOr(left, right) =>
        val leftExpr = parseZ3Expr(left.trim, ctx)
        val rightExpr = parseZ3Expr(right.trim, ctx)
        ctx.mkOr(leftExpr, rightExpr)
      case _ => parseZ3Expr(line.trim, ctx)
    }
    expr
  }
  
  private def parseZ3Expr(expr: String, ctx: Context): BoolExpr = {
    val eqPattern = "(\\w+)\\((.*?)\\) -> (.*?)".r
    expr match {
      case eqPattern(left, args, right) =>
        val leftExpr = ctx.mkBoolConst(left + "(" + args + ")")
        val rightExpr = ctx.mkBoolConst(right)
        ctx.mkImplies(leftExpr, rightExpr)
      case _ => ctx.mkBoolConst(expr)
    }
  }
  
  private def parseAction(line: String, ctx: Context): Expr[BoolSort] = {
    val eventPattern = "(\\w+)\\((.*?)\\)@(\\d+)".r
    line match {
      case eventPattern(event, params, time) =>
        val args = if (params.trim.nonEmpty) {
          params.split(",").map(_.trim).filter(_.nonEmpty).map(param => parseParam(param, ctx))
        } else {
          Array.empty[Expr[Sort]]
        }
        if (args.nonEmpty) {
          ctx.mkApp(ctx.mkFuncDecl(event, args.map(_.getSort), ctx.getBoolSort), args: _*).asInstanceOf[Expr[BoolSort]]
        } else {
          ctx.mkBoolConst(event)
        }
      case _ => throw new IllegalArgumentException(s"Invalid trace format: $line")
    }
  }
  
  private def parseParam(param: String, ctx: Context): Expr[Sort] = {
    val kvPattern = "(\\w+)=(\\w+)".r
    param match {
      case kvPattern(key, value) => 
        // 对于key=value格式，我们使用value作为常量
        if (value.startsWith("0x")) {
          // 处理十六进制地址
          ctx.mkConst(value, ctx.getIntSort)
        } else {
          // 处理数字
          ctx.mkInt(value.toInt).asInstanceOf[Expr[Sort]]
        }
      case _ => 
        // 对于普通参数，尝试解析为数字
        try {
          ctx.mkInt(param.toInt).asInstanceOf[Expr[Sort]]
        } catch {
          case _: NumberFormatException => ctx.mkConst(param, ctx.getIntSort)
        }
    }
  }
}