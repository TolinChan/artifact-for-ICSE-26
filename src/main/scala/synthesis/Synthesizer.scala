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
    val stateMachine = new StateMachine(dl.name, new com.microsoft.z3.Context())
    stateMachine.readFromProgram(dl)
    
    val property = Parser.parseProperty(propertypath)
    val postrace = Parser.parseTrace(tracepath)
    stateMachine.addOnce()
    val candidates = stateMachine.generate_candidate_guards(List("<", "<=", ">", ">=", "="), array = true)
    stateMachine.cegis(property, List(List(postrace)), candidates)
    stateMachine.inductive_prove(property)
    
    val end = System.nanoTime()
    println(s"Synthesis time: ${(end - start) / 1e9} seconds")
    
    stateMachine.writefile(solpath)
  }

}
