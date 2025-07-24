package synthesis

import com.microsoft.z3._
import util.Misc.parseProgram
import verification.Verifier._
import synthesis.Parser

case class SynthesizerWithoutTrace() {

  def synthesizeWithoutTrace(tempDir: String, candidateId: Int): Boolean = {
    try {
      val datalogpath = s"$tempDir/benchmark.dl"
      val propertypath = s"$tempDir/temporal_properties.txt"
      val solpath = s"$tempDir/candidate_$candidateId.sol"
      
      val dl = parseProgram(datalogpath)
      val ctx = new com.microsoft.z3.Context()
      val stateMachine = new StateMachine(dl.name, ctx)
      stateMachine.readFromProgram(dl)
      
      val property = Parser.parsePropertyWithoutTrace(propertypath, ctx)
      stateMachine.addOnce()
      stateMachine.generate_candidate_guards_from_properties(property)
      
      val success = stateMachine.synthesizeWithoutTrace(property)
      if (success) {
        stateMachine.writefile(solpath)
        println(s"Synthesis successful for candidate $candidateId")
        true
      } else {
        println(s"Synthesis failed for candidate $candidateId")
        false
      }
    } catch {
      case e: Exception =>
        println(s"Synthesis error for candidate $candidateId: ${e.getMessage}")
        false
    }
  }

  def synthesizeMultipleCandidates(benchmarkName: String): Unit = {
    val tempDir = s"./synthesis_output/$benchmarkName/temp"
    val datalogpath = s"./synthesis-benchmark/$benchmarkName/$benchmarkName.dl"
    val propertypath = s"./synthesis-benchmark/$benchmarkName/temporal_properties.txt"
    
    // Copy files to temp directory
    val dl = parseProgram(datalogpath)
    val stateMachine = new StateMachine(dl.name, new com.microsoft.z3.Context())
    stateMachine.readFromProgram(dl)
    
    val property = Parser.parsePropertyWithoutTrace(propertypath)
    
    for (candidateId <- 0 until 5) {
      val solpath = s"./synthesis_output/$benchmarkName/candidate_$candidateId.sol"
      
      try {
        stateMachine.addOnce()
        stateMachine.generate_candidate_guards_from_properties(property)
        
        val success = stateMachine.synthesizeWithoutTrace(property)
        if (success) {
          stateMachine.writefile(solpath)
          println(s"Generated candidate $candidateId for $benchmarkName")
        } else {
          println(s"Failed to generate candidate $candidateId for $benchmarkName")
        }
      } catch {
        case e: Exception =>
          println(s"Error generating candidate $candidateId for $benchmarkName: ${e.getMessage}")
      }
    }
  }

} 

object SynthesizerWithoutTrace {
  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: SynthesizerWithoutTrace <tempDir> <candidateId>")
      System.exit(1)
    }
    
    val tempDir = args(0)
    val candidateId = args(1).toInt
    
    val synthesizer = new SynthesizerWithoutTrace()
    val result = synthesizer.synthesizeWithoutTrace(tempDir, candidateId)
    
    if (result) {
      println("SUCCESS")
      System.exit(0)
    } else {
      println("FAILED")
      System.exit(1)
    }
  }
} 