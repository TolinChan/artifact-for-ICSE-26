package synthesis

import com.microsoft.z3._
import util.Misc.parseProgram
import verification.Verifier._
import synthesis.Parser

case class Synthesizer() {

  def synthesize(name: String): Unit = {
    val start = System.nanoTime()
    
    val datalogpath = s"./synthesis-benchmark/$name/$name.dl"
    val propertypath = s"./synthesis-benchmark/$name/temporal_properties.txt"
    val tracepath = s"./synthesis-benchmark/$name/example_traces.txt"
    val solpath = s"./output/$name.sol"

    val dl = parseProgram(datalogpath)
    val ctx = new com.microsoft.z3.Context()
    val stateMachine = new StateMachine(dl.name, ctx)
    stateMachine.readFromProgram(dl)
    
    // 使用相同的Z3 Context
    val property = Parser.parsePropertyWithoutTrace(propertypath, ctx)
    val postrace = Parser.parseTrace(tracepath, ctx)
    stateMachine.addOnce()
    val candidates = stateMachine.generate_candidate_guards(List("<", "<=", ">", ">=", "="), array = true)
    stateMachine.cegis(property, List(List(postrace)), candidates)
    stateMachine.inductive_prove(property)
    
    val end = System.nanoTime()
    println(s"Synthesis time: ${(end - start) / 1e9} seconds")
    
    stateMachine.writefile(solpath)
  }

}
