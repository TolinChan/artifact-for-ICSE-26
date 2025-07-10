package synthesis

import datalog.Program
import verification.{TransitionSystem, Verifier}
import synthesis.StateMachine
import synthesis.Parser
import util.Misc.parseProgram

case class Synthesizer() {

  def synthesize(name: String) = {
    print(name)
    val startTime = System.nanoTime()
    // val temporalProperties: TemporalProperty = TemporalProperty()
    val datalogpath = "./synthesis-benchmark/" + name + "/" + name + ".dl"
    val propertypath = "./synthesis-benchmark/" + name + "/temporal_properties.txt"
    val tracepath = "./synthesis-benchmark/" + name + "/example_traces.txt"
    val solpath = "./synthesis-benchmark/" + name + name + ".dl"

    val dl = parseProgram(datalogpath)
    stateMachine.readFromProgram(dl)
    /** The CEGIS loop.  */
    val property = Parser.parseProperty(propertypath)
    val postrace = Parser.parseTrace(tracepath)
    statemachine.addOnce()
    statemachine.generate_candidate_guards()
    stateMachine.cegis(property, postrace)
    stateMachine.inductive_prove(property)
    val endTime = System.nanoTime()
    val elapsedTimeMs = (endTime - startTime) / 1e9
    print(s" $elapsedTimeMs")
    statemachine.writefile(solpath)

  }
}
