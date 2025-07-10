# Artifacts for ICSE'26

Synthesizing Smart Contracts via Multi-Modal Specifications

# Install dependencies

Build tool:
1. [SBT](https://www.scala-sbt.org/1.x/docs/Setup.html)
2. [Truffle](https://trufflesuite.com/docs/truffle/getting-started/installation/): ``npm install -g truffle``

# Verification and bounded model checking

## Setup Z3 

1. Download z3 [source](https://github.com/Z3Prover/z3).
2. Build z3 and generate Java binding: 
    ```
    cd z3
    python scripts/mk_make.py --java
    cd build
    make
    ```
3. Copy files from ``z3`` to ``dsc`` project directory:
    * copy ``com.microsoft.z3.jar`` to sub-directory called ``unmanaged``.
    * copy ``libz3.dylib`` and ``libz3java.dylib`` to the project directory.
4. Add the following line to ``build.sbt``:
    ```
    Compile / unmanagedJars += {
      baseDirectory.value / "unmanaged" / "com.microsoft.z3.jar"
    }
    ```
5. In sbt configuration, set working directory as the project directory, so that Java runtime can locate the two dylib file.

# Synthesize single smart contract

```
sbt "run synthesize [contract_name]"
```
The complete list of benchmark smart contracts are in [here](synthesis-benchmark/).
Each one contains three parts: datalog file, example traces and properties.

# Run

1. Synthesize all Solidity programs: ``sbt "run synthesis"``


# Example contracts

Examples of declarative smart contrats are located in [benchmarks](synthesis-benchmark/).

# Gas consumption

The contracts used for gas measurements, the script and instructions can be found [here](truffle/).