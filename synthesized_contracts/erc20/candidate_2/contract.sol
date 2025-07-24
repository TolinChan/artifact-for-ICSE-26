// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract Benchmark {    int256 public owner;
    int256 public burn;
    int256 public transferFrom;
    mapping(int256 => int256) public totalIn;
    int256 public totalSupply;
    bool public constructor;
    int256 public allBurn;
    mapping(int256 => int256) public allowance;
    int256 public mint;
    mapping(int256 => int256) public totalMint;
    int256 public transfer;
    int256 public approve;
    int256 public now;
    mapping(int256 => int256) public balanceOf;
    mapping(int256 => int256) public totalBurn;
    int256 public allMint;
    mapping(int256 => int256) public totalOut;

    int256 public currentState;


    constructor() {
        currentState = owner;
    }

      
    function ansfer() public {
                require(true, "Transition not allowed");

        currentState = ansfer;
    }

        
    function ansfer() public {
                require(true, "Transition not allowed");

        currentState = ansfer;
    }

        
    function ansfer() public {
                require(true, "Transition not allowed");

        currentState = ansfer;
    }

        
    function ansferFrom() public {
                require(true, "Transition not allowed");

        currentState = ansferFrom;
    }

        
    function mint() public {
                require(true, "Transition not allowed");

        currentState = mint;
    }

        
    function burn() public {
                require(true, "Transition not allowed");

        currentState = burn;
    }

        
    function mint() public {
                require(true, "Transition not allowed");

        currentState = mint;
    }

        
    function burn() public {
                require(true, "Transition not allowed");

        currentState = burn;
    }

        
    function approve() public {
                require(true, "Transition not allowed");

        currentState = approve;
    }

        }