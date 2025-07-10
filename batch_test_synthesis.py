#!/usr/bin/env python3
"""
Batch test script for synthesis workflow
Tests multiple benchmarks and generates a comprehensive report
"""

import os
import json
import time
from pathlib import Path
from test_synthesis_workflow import test_synthesis_workflow

def get_available_benchmarks():
    """Get list of available benchmarks"""
    benchmark_dir = "./synthesis-benchmark"
    if not os.path.exists(benchmark_dir):
        return []
    
    benchmarks = []
    for item in os.listdir(benchmark_dir):
        item_path = os.path.join(benchmark_dir, item)
        if os.path.isdir(item_path):
            # Check if it has required files
            temporal_props = os.path.join(item_path, "temporal_properties.txt")
            if os.path.exists(temporal_props):
                benchmarks.append(item)
    
    return sorted(benchmarks)

def batch_test_benchmarks(benchmarks=None, max_benchmarks=5):
    """Run tests on multiple benchmarks"""
    if benchmarks is None:
        available = get_available_benchmarks()
        benchmarks = available[:max_benchmarks]
    
    print(f"Starting batch test for {len(benchmarks)} benchmarks")
    print("=" * 60)
    
    results = {}
    start_time = time.time()
    
    for i, benchmark in enumerate(benchmarks, 1):
        print(f"\n[{i}/{len(benchmarks)}] Testing {benchmark}...")
        print("-" * 40)
        
        try:
            success = test_synthesis_workflow(benchmark)
            results[benchmark] = {
                'success': success,
                'timestamp': time.time()
            }
            
            if success:
                # Load the individual result file
                result_file = f"./synthesis_output/{benchmark}/reports/test_results.json"
                if os.path.exists(result_file):
                    with open(result_file, 'r') as f:
                        individual_result = json.load(f)
                    results[benchmark]['details'] = individual_result
                    print(f"✅ {benchmark} completed successfully")
                else:
                    print(f"⚠️  {benchmark} completed but no result file found")
            else:
                print(f"❌ {benchmark} failed")
                
        except Exception as e:
            print(f"❌ {benchmark} failed with error: {e}")
            results[benchmark] = {
                'success': False,
                'error': str(e),
                'timestamp': time.time()
            }
    
    # Generate batch report
    generate_batch_report(results, time.time() - start_time)
    
    return results

def generate_batch_report(results, total_time):
    """Generate comprehensive batch report"""
    print(f"\n{'='*60}")
    print("BATCH TEST SUMMARY")
    print(f"{'='*60}")
    
    successful = [k for k, v in results.items() if v.get('success', False)]
    failed = [k for k, v in results.items() if not v.get('success', False)]
    
    print(f"Total benchmarks tested: {len(results)}")
    print(f"Successful: {len(successful)}")
    print(f"Failed: {len(failed)}")
    print(f"Success rate: {len(successful)/len(results)*100:.1f}%")
    print(f"Total time: {total_time:.2f} seconds")
    
    if successful:
        print(f"\nSuccessful benchmarks:")
        for benchmark in successful:
            result = results[benchmark]
            if 'details' in result:
                stats = result['details']['statistics']
                print(f"  {benchmark}: {stats['average_passing_rate']:.1%} avg passing rate")
            else:
                print(f"  {benchmark}: ✅")
    
    if failed:
        print(f"\nFailed benchmarks:")
        for benchmark in failed:
            result = results[benchmark]
            error = result.get('error', 'Unknown error')
            print(f"  {benchmark}: {error}")
    
    # Calculate overall statistics
    overall_stats = calculate_overall_statistics(results)
    if overall_stats:
        print(f"\nOverall Statistics:")
        print(f"  Average passing rate across all candidates: {overall_stats['avg_passing_rate']:.1%}")
        print(f"  Best candidate passing rate: {overall_stats['best_passing_rate']:.1%}")
        print(f"  Total candidates generated: {overall_stats['total_candidates']}")
    
    # Save batch report
    batch_report = {
        'timestamp': time.time(),
        'total_time': total_time,
        'summary': {
            'total_benchmarks': len(results),
            'successful': len(successful),
            'failed': len(failed),
            'success_rate': len(successful)/len(results)*100
        },
        'results': results,
        'overall_statistics': overall_stats
    }
    
    # Create batch reports directory
    batch_report_dir = "./synthesis_output/batch_reports"
    os.makedirs(batch_report_dir, exist_ok=True)
    report_file = f"{batch_report_dir}/batch_test_report_{int(time.time())}.json"
    with open(report_file, 'w') as f:
        json.dump(batch_report, f, indent=2)
    
    print(f"\nDetailed batch report saved to: {report_file}")

def calculate_overall_statistics(results):
    """Calculate overall statistics from all successful tests"""
    all_passing_rates = []
    total_candidates = 0
    
    for benchmark, result in results.items():
        if result.get('success') and 'details' in result:
            details = result['details']
            if 'statistics' in details:
                stats = details['statistics']
                if 'average_passing_rate' in stats:
                    all_passing_rates.append(stats['average_passing_rate'])
                if 'total_candidates' in stats:
                    total_candidates += stats['total_candidates']
    
    if not all_passing_rates:
        return None
    
    return {
        'avg_passing_rate': sum(all_passing_rates) / len(all_passing_rates),
        'best_passing_rate': max(all_passing_rates),
        'worst_passing_rate': min(all_passing_rates),
        'total_candidates': total_candidates,
        'benchmarks_with_results': len(all_passing_rates)
    }

def main():
    """Main function"""
    import sys
    
    # Parse command line arguments
    if len(sys.argv) > 1:
        if sys.argv[1] == '--list':
            benchmarks = get_available_benchmarks()
            print("Available benchmarks:")
            for i, benchmark in enumerate(benchmarks, 1):
                print(f"  {i}. {benchmark}")
            return
        elif sys.argv[1] == '--all':
            benchmarks = get_available_benchmarks()
        else:
            benchmarks = sys.argv[1:]
    else:
        # Default: test first 3 benchmarks
        benchmarks = get_available_benchmarks()[:3]
    
    if not benchmarks:
        print("No benchmarks found!")
        return
    
    print(f"Testing benchmarks: {', '.join(benchmarks)}")
    batch_test_benchmarks(benchmarks)

if __name__ == "__main__":
    main() 