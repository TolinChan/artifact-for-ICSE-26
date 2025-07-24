# smart_contract_synthesis

## artifact for ICSE'25

This artifact contains the benchmarks, binaries, and scripts to reproduce the the experimental results of the corresponding paper

## instructions
The results reported in Table 2 in the paper is collected from a server with 125GB memory. Smaller memory size may impact the run time of verification tasks.

1. Download and extract the artifact file
`` cd smart_contract_synthesis ``

2. Install python and z3 package, you may use any python package manager you want, e.g.
`` pip install z3-solver ``

3. Run all experiments:  ./run_all.sh

The results are written to `results.out`.
