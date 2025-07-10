package verification

import datalog.Program
import imp.{ImperativeTranslator, SolidityTranslator}
import util.Misc.parseProgram
import java.nio.file.Paths

object VerifierWrapper {
  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      println("Usage: VerifierWrapper <temp_dir> <benchmark_name> <candidate_sol_path>")
      System.exit(1)
    }
    
    val tempDir = args(0)
    val benchmarkName = args(1)
    val candidateSolPath = args(2)
    
    try {
      val result = verifyCandidate(tempDir, benchmarkName, candidateSolPath)
      println(s"VERIFICATION_RESULT: $result")
      System.exit(0)
    } catch {
      case e: Exception =>
        println(s"VERIFICATION_ERROR: ${e.getMessage}")
        e.printStackTrace()
        System.exit(1)
    }
  }
  
  def verifyCandidate(tempDir: String, benchmarkName: String, candidateSolPath: String): String = {
    // Parse the datalog program
    val datalogPath = Paths.get(tempDir, s"${benchmarkName}.dl").toString
    val dl = parseProgram(datalogPath)
    
    // Create imperative translator
    val materializedRelations: Set[datalog.Relation] = Set()
    val impTranslator = new ImperativeTranslator(dl, materializedRelations, 
      isInstrument = true, enableProjection = true, 
      monitorViolations = false, arithmeticOptimization = true)
    
    // Translate to imperative program
    val imperative = impTranslator.translate()
    
    // Create verifier
    val verifier = new Verifier(dl, imperative)
    
    // Run verification and capture results
    val verificationResults = captureVerificationResults(verifier)
    
    // Analyze results and return passing rate
    analyzeVerificationResults(verificationResults)
  }
  
  private def captureVerificationResults(verifier: Verifier): List[String] = {
    // Capture console output during verification
    val originalOut = System.out
    val capturedOutput = new java.io.ByteArrayOutputStream()
    val printStream = new java.io.PrintStream(capturedOutput)
    
    try {
      System.setOut(printStream)
      verifier.check()
    } finally {
      System.setOut(originalOut)
      printStream.close()
    }
    
    capturedOutput.toString.split("\n").toList
  }
  
  private def analyzeVerificationResults(results: List[String]): String = {
    var totalProperties = 0
    var satisfiedProperties = 0
    var violatedProperties = 0
    var unknownProperties = 0
    
    for (line <- results) {
      if (line.contains("UNSATISFIABLE")) {
        satisfiedProperties += 1
        totalProperties += 1
      } else if (line.contains("SATISFIABLE")) {
        violatedProperties += 1
        totalProperties += 1
      } else if (line.contains("UNKNOWN")) {
        unknownProperties += 1
        totalProperties += 1
      }
    }
    
    if (totalProperties == 0) {
      "UNKNOWN: No properties found"
    } else {
      val passingRate = satisfiedProperties.toDouble / totalProperties
      s"PASSING_RATE: $passingRate ($satisfiedProperties/$totalProperties properties satisfied)"
    }
  }
} 