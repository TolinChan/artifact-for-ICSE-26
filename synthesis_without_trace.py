#!/usr/bin/env python3
"""
Synthesis without example_trace dependency
Generate 5 candidate contracts and test their trace passing rate
"""

import os
import subprocess
import shutil
import tempfile
import json
import random
from pathlib import Path
import time

class SynthesisWithoutTrace:
    def __init__(self, benchmark_name):
        self.benchmark_name = benchmark_name
        self.benchmark_dir = f"./synthesis-benchmark/{benchmark_name}"
        self.datalog_file = f"{self.benchmark_dir}/{benchmark_name}.dl"
        self.temporal_properties_file = f"{self.benchmark_dir}/temporal_properties.txt"
        self.synthesized_contracts_dir = f"./synthesized_contracts/{benchmark_name}"
        
        # Create synthesized contracts directory
        os.makedirs(self.synthesized_contracts_dir, exist_ok=True)
        
    def generate_candidate_contracts(self, num_candidates=5):
        """Generate multiple candidate contracts without example_trace dependency"""
        print(f"Generating {num_candidates} candidate contracts for {self.benchmark_name}...")
        
        candidates = []
        
        for i in range(num_candidates):
            print(f"  Generating candidate {i+1}/{num_candidates}...")
            
            # Create temporary directory for this candidate
            temp_dir = tempfile.mkdtemp(prefix=f"candidate_{i}_")
            
            try:
                # Copy datalog and temporal properties to temp directory
                shutil.copy2(self.datalog_file, f"{temp_dir}/benchmark.dl")
                shutil.copy2(self.temporal_properties_file, f"{temp_dir}/temporal_properties.txt")
                
                # Create a modified temporal properties file without trace dependency
                self._create_modified_properties(temp_dir)
                
                # Run synthesis (modified to not require example_trace)
                candidate_sol = self._run_synthesis_without_trace(temp_dir, i)
                
                if candidate_sol:
                    # Copy the generated contract to synthesized contracts directory
                    candidate_dir = f"{self.synthesized_contracts_dir}/candidate_{i}"
                    os.makedirs(candidate_dir, exist_ok=True)
                    candidate_sol_final = f"{candidate_dir}/contract.sol"
                    shutil.copy2(candidate_sol, candidate_sol_final)
                    
                    candidate_info = {
                        'id': i,
                        'sol_file': candidate_sol_final,
                        'temp_dir': temp_dir,
                        'generation_time': time.time()
                    }
                    candidates.append(candidate_info)
                    print(f"    Candidate {i+1} generated successfully")
                else:
                    print(f"    Candidate {i+1} generation failed")
                    shutil.rmtree(temp_dir)
                    
            except Exception as e:
                print(f"    Error generating candidate {i+1}: {e}")
                shutil.rmtree(temp_dir)
                
        return candidates
    
    def _create_modified_properties(self, temp_dir):
        """Create modified temporal properties that don't depend on example_trace"""
        original_props = f"{temp_dir}/temporal_properties.txt"
        
        # For now, we'll use the original properties as-is
        # The Parser has been updated to handle the ASCII format
        print(f"    Using original properties from {original_props}")
    
    def _run_synthesis_without_trace(self, temp_dir, candidate_id):
        """Run synthesis without example_trace dependency"""
        try:
            # Use SBT to run the synthesis
            result = subprocess.run([
                'sbt', f'runMain synthesis.SynthesizerWithoutTrace {temp_dir} {candidate_id}'
            ], capture_output=True, text=True, cwd='.', timeout=300, shell=True, encoding='utf-8', errors='ignore')
            
            print(f"    SBT command executed with return code: {result.returncode}")
            print(f"    Working directory: {os.getcwd()}")
            print(f"    Temp directory: {temp_dir}")
            print(f"    Files in temp dir: {list(Path(temp_dir).glob('*'))}")
            
            # Look for generated Solidity file
            sol_files = list(Path(temp_dir).glob("*.sol"))
            if sol_files:
                return str(sol_files[0])
            
            print(f"    Synthesis output: {result.stdout}")
            if result.stderr:
                print(f"    Synthesis error: {result.stderr}")
            
            return None
            
        except subprocess.TimeoutExpired:
            print(f"    Synthesis timed out")
            return None
        except Exception as e:
            print(f"    Synthesis error: {e}")
            return None
    

    
    def generate_traces_for_candidates(self, candidates):
        """Generate example_traces for each candidate using trace_generator.py"""
        print(f"Generating traces for {len(candidates)} candidates...")
        
        trace_results = []
        
        for candidate in candidates:
            print(f"  Generating trace for candidate {candidate['id']}...")
            
            try:
                # Create a temporary directory for trace generation
                trace_dir = tempfile.mkdtemp(prefix=f"trace_{candidate['id']}_")
                
                # Copy the Solidity file to trace directory
                shutil.copy2(candidate['sol_file'], f"{trace_dir}/contract.sol")
                
                # Generate trace using trace_generator.py
                trace_file = self._generate_trace_with_tmp(trace_dir)
                
                if trace_file:
                    candidate['trace_file'] = trace_file
                    candidate['trace_dir'] = trace_dir
                    trace_results.append(candidate)
                    print(f"    Trace generated successfully")
                else:
                    print(f"    Trace generation failed")
                    
            except Exception as e:
                print(f"    Error generating trace: {e}")
        
        return trace_results
    
    def _generate_trace_with_tmp(self, trace_dir):
        """Generate trace using trace_generator.py"""
        try:
            # Create a modified trace_generator.py that works with our setup
            modified_tmp = self._create_modified_tmp(trace_dir)
            
            # Run the modified trace_generator.py
            result = subprocess.run([
                'python', modified_tmp
            ], capture_output=True, text=True, cwd=trace_dir)
            
            print(f"    Trace generation return code: {result.returncode}")
            print(f"    Trace generation stdout: {result.stdout}")
            if result.stderr:
                print(f"    Trace generation stderr: {result.stderr}")
            
            if result.returncode == 0:
                trace_file = f"{trace_dir}/example_traces.txt"
                if os.path.exists(trace_file):
                    return trace_file
            
            return None
            
        except Exception as e:
            print(f"    Trace generation error: {e}")
            return None
    
    def _create_modified_tmp(self, trace_dir):
        """Create a modified version of trace_generator.py for trace generation"""
        # Read the original trace_generator.py
        with open('trace_generator.py', 'r', encoding='utf-8') as f:
            tmp_content = f.read()
        
        # Modify the batch_generate function to work with our setup
        modified_content = tmp_content.replace(
            'def batch_generate():',
            '''def batch_generate():
    # Modified for synthesis testing
    sol_files = glob.glob("*.sol")
    if not sol_files:
        print("No Solidity files found")
        return
    
    for sol_file in sol_files:
        print(f"Processing {sol_file}")
        functions = parse_solidity_functions(sol_file)
        if not functions:
            print(f"No functions found in {sol_file}")
            continue
        
        # Generate trace
        state = ContractState({})
        trace = generate_trace(functions, TRACE_LENGTH, state)
        
        # Write trace to file
        output_file = "example_traces.txt"
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("# Generated trace for synthesis testing\\n")
            f.write(f"# Contract: {sol_file}\\n")
            f.write(f"# Length: {len(trace)}\\n\\n")
            
            for i, op in enumerate(trace):
                f.write(f"# Operation {i+1}\\n")
                f.write(f"{op}\\n")
        
        print(f"Trace written to {output_file}")'''
        )
        
        # Write modified trace_generator.py
        modified_tmp = f"{trace_dir}/modified_trace_generator.py"
        with open(modified_tmp, 'w', encoding='utf-8') as f:
            f.write(modified_content)
        
        return modified_tmp
    
    def test_candidate_passing_rate(self, candidates_with_traces):
        """Test the passing rate of candidates against their generated traces"""
        print(f"Testing passing rate for {len(candidates_with_traces)} candidates...")
        
        results = []
        
        for candidate in candidates_with_traces:
            print(f"  Testing candidate {candidate['id']}...")
            
            try:
                passing_rate = self._test_single_candidate(candidate)
                candidate['passing_rate'] = passing_rate
                results.append(candidate)
                print(f"    Passing rate: {passing_rate:.2%}")
                
            except Exception as e:
                print(f"    Error testing candidate: {e}")
                candidate['passing_rate'] = 0.0
                results.append(candidate)
        
        return results

    def _test_single_candidate(self, candidate):
        """Test a single candidate against its generated trace (Scala only, no Python fallback)"""
        try:
            # Create a test environment
            test_dir = tempfile.mkdtemp(prefix=f"test_{candidate['id']}_")
            
            # Copy necessary files
            shutil.copy2(candidate['sol_file'], f"{test_dir}/contract.sol")
            shutil.copy2(candidate['trace_file'], f"{test_dir}/example_traces.txt")
            shutil.copy2(self.temporal_properties_file, f"{test_dir}/temporal_properties.txt")
            
            # Run Scala verification (must implement or call existing Scala verifier)
            passing_rate = self._run_scala_verification(test_dir, candidate)
            
            # Cleanup
            shutil.rmtree(test_dir)
            
            return passing_rate
            
        except Exception as e:
            print(f"    Verification error: {e}")
            return 0.0

    def _run_scala_verification(self, test_dir, candidate):
        """Run Scala verification on the candidate contract and trace (Scala only, no Python fallback)"""
        try:
            # Assume VerifierWrapper takes: temp_dir, benchmark_name, candidate_sol_path
            benchmark_name = self.benchmark_name
            candidate_sol_path = os.path.join(test_dir, "contract.sol")
            temp_dir = test_dir
            # Call Scala verifier
            result = subprocess.run([
                'scala', '-cp', 'unmanaged/com.microsoft.z3.jar:target/scala-2.13/classes',
                'verification.VerifierWrapper',
                temp_dir,
                benchmark_name,
                candidate_sol_path
            ], capture_output=True, text=True, cwd='.')
            if result.returncode == 0:
                # Parse verification result
                for line in result.stdout.split('\n'):
                    if line.startswith("VERIFICATION_RESULT:"):
                        result_text = line.replace("VERIFICATION_RESULT:", "").strip()
                        if result_text.startswith("PASSING_RATE:"):
                            rate_part = result_text.split(" ")[1]
                            try:
                                return float(rate_part)
                            except ValueError:
                                print(f"    Could not parse passing rate: {rate_part}")
                                return 0.0
                        elif "UNKNOWN" in result_text:
                            return 0.0
                        else:
                            print(f"    Unknown verification result format: {result_text}")
                            return 0.0
                # Fallback: check for UNSATISFIABLE/SATISFIABLE/UNKNOWN
                for line in result.stdout.split('\n'):
                    if "UNSATISFIABLE" in line:
                        return 1.0
                    elif "SATISFIABLE" in line:
                        return 0.0
                    elif "UNKNOWN" in line:
                        return 0.0
                return 0.0
            else:
                print(f"    Scala verification failed: {result.stderr}")
                return 0.0
        except Exception as e:
            print(f"    Scala verification error: {e}")
            return 0.0
    
    def generate_report(self, results):
        """Generate a comprehensive report"""
        print(f"\n=== Synthesis Report for {self.benchmark_name} ===")
        
        if not results:
            print("No candidates were successfully generated and tested.")
            return
        
        # Calculate statistics
        passing_rates = [r['passing_rate'] for r in results]
        avg_passing_rate = sum(passing_rates) / len(passing_rates)
        max_passing_rate = max(passing_rates)
        min_passing_rate = min(passing_rates)
        
        print(f"Total candidates: {len(results)}")
        print(f"Average passing rate: {avg_passing_rate:.2%}")
        print(f"Best passing rate: {max_passing_rate:.2%}")
        print(f"Worst passing rate: {min_passing_rate:.2%}")
        
        # Sort by passing rate
        sorted_results = sorted(results, key=lambda x: x['passing_rate'], reverse=True)
        
        print(f"\nTop candidates:")
        for i, result in enumerate(sorted_results[:3]):
            print(f"  {i+1}. Candidate {result['id']}: {result['passing_rate']:.2%}")
        
        # Save detailed results
        report_file = f"{self.synthesized_contracts_dir}/synthesis_report.json"
        with open(report_file, 'w') as f:
            json.dump({
                'benchmark': self.benchmark_name,
                'timestamp': time.time(),
                'statistics': {
                    'total_candidates': len(results),
                    'average_passing_rate': avg_passing_rate,
                    'max_passing_rate': max_passing_rate,
                    'min_passing_rate': min_passing_rate
                },
                'candidates': results
            }, f, indent=2)
        
        print(f"\nDetailed report saved to: {report_file}")
    
    def run_full_synthesis(self, num_candidates=5):
        """Run the complete synthesis process"""
        print(f"Starting synthesis process for {self.benchmark_name}")
        print("=" * 50)
        
        # Step 1: Generate candidate contracts
        candidates = self.generate_candidate_contracts(num_candidates)
        
        if not candidates:
            print("No candidates were generated. Exiting.")
            return
        
        # Step 2: Generate traces for candidates
        candidates_with_traces = self.generate_traces_for_candidates(candidates)
        
        if not candidates_with_traces:
            print("No traces were generated. Exiting.")
            return
        
        # Step 3: Test passing rates
        results = self.test_candidate_passing_rate(candidates_with_traces)
        
        # Step 4: Generate report
        self.generate_report(results)
        
        # Cleanup temporary directories
        for candidate in candidates:
            if 'temp_dir' in candidate and os.path.exists(candidate['temp_dir']):
                shutil.rmtree(candidate['temp_dir'])
        
        for candidate in candidates_with_traces:
            if 'trace_dir' in candidate and os.path.exists(candidate['trace_dir']):
                shutil.rmtree(candidate['trace_dir'])

def main():
    """Main function to run synthesis for a specific benchmark"""
    import sys
    
    if len(sys.argv) != 2:
        print("Usage: python synthesis_without_trace.py <benchmark_name>")
        print("Available benchmarks:")
        # List available benchmarks
        benchmark_dir = "./synthesis-benchmark"
        if os.path.exists(benchmark_dir):
            benchmarks = [d for d in os.listdir(benchmark_dir) 
                         if os.path.isdir(os.path.join(benchmark_dir, d))]
            for bm in sorted(benchmarks):
                print(f"  {bm}")
        return
    
    benchmark_name = sys.argv[1]
    benchmark_path = f"./synthesis-benchmark/{benchmark_name}"
    
    if not os.path.exists(benchmark_path):
        print(f"Benchmark '{benchmark_name}' not found.")
        return
    
    # Run synthesis
    synthesizer = SynthesisWithoutTrace(benchmark_name)
    synthesizer.run_full_synthesis(num_candidates=5)

if __name__ == "__main__":
    main() 