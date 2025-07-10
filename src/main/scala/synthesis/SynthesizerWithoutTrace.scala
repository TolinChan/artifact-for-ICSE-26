package synthesis

import datalog.Program
import verification.{TransitionSystem, Verifier}
import synthesis.StateMachine
import synthesis.Parser
import util.Misc.parseProgram
import java.nio.file.Paths

case class SynthesizerWithoutTrace() {

  def synthesizeWithoutTrace(tempDir: String, candidateId: Int): Boolean = {
    try {
      val datalogpath = Paths.get(tempDir, "erc20.dl").toString
      val propertypath = Paths.get(tempDir, "temporal_properties.txt").toString
      val solpath = Paths.get(tempDir, s"candidate_${candidateId}.sol").toString

      val dl = parseProgram(datalogpath)
      val stateMachine = new StateMachine(dl.name, new com.microsoft.z3.Context())
      
      // Read program into state machine
      stateMachine.readFromProgram(dl)
      
      // Parse temporal properties (without trace dependency)
      val property = Parser.parsePropertyWithoutTrace(propertypath)
      
      // Add once constraints
      stateMachine.addOnce()
      
      // Generate candidate guards based on temporal properties
      stateMachine.generate_candidate_guards_from_properties(property)
      
      // Run synthesis without trace
      val success = stateMachine.synthesizeWithoutTrace(property)
      
      if (success) {
        // Write the synthesized contract
        stateMachine.writefile(solpath)
        println(s"SUCCESS: Generated candidate ${candidateId} at ${solpath}")
        true
      } else {
        println(s"FAILED: Could not synthesize candidate ${candidateId}")
        false
      }
      
    } catch {
      case e: Exception =>
        println(s"ERROR: Exception during synthesis: ${e.getMessage}")
        e.printStackTrace()
        false
    }
  }

  def synthesizeMultipleCandidates(benchmarkName: String, numCandidates: Int = 5): List[String] = {
    val benchmarkDir = s"./synthesis-benchmark/${benchmarkName}"
    val datalogpath = Paths.get(benchmarkDir, s"${benchmarkName}.dl").toString
    val propertypath = Paths.get(benchmarkDir, "temporal_properties.txt").toString
    
    val dl = parseProgram(datalogpath)
    val property = Parser.parsePropertyWithoutTrace(propertypath)
    
    val candidates = List.newBuilder[String]
    
    for (i <- 0 until numCandidates) {
      println(s"Generating candidate ${i+1}/${numCandidates}...")
      
      val stateMachine = new StateMachine(dl.name, new com.microsoft.z3.Context())
      stateMachine.readFromProgram(dl)
      stateMachine.addOnce()
      stateMachine.generate_candidate_guards_from_properties(property)
      
      val solpath = Paths.get(benchmarkDir, s"candidate_${i}.sol").toString
      
      if (stateMachine.synthesizeWithoutTrace(property)) {
        stateMachine.writefile(solpath)
        candidates += solpath
        println(s"  Candidate ${i+1} generated successfully")
      } else {
        println(s"  Candidate ${i+1} generation failed")
      }
    }
    
    candidates.result()
  }
} 