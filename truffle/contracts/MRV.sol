contract MRVToken {
    uint256 private _totalSupply;
    uint8 public decimals;
    uint public maxCrowdsaleSupplyInWholeTokens;
    uint public constant wholeTokensReserved = 5000;
    uint public constant wholeTokensPerEth = 5000;
    bool crowdsaleStarted;
    bool crowdsaleEnded;
    uint public openTimer = 0;
    uint public closeTimer = 0;
    function openTimerElapsed() public constant returns (bool) {
        return (openTimer != 0 && now > openTimer);
    }
    function closeTimerElapsed() public constant returns (bool) {
        return (closeTimer != 0 && now > closeTimer);
    }
    function setMaxSupply(uint newMaxInWholeTokens) public {
        require(!crowdsaleStarted);
        maxCrowdsaleSupplyInWholeTokens = newMaxInWholeTokens;
    }
    function openCrowdsale() public {
        require(!crowdsaleStarted);
        crowdsaleStarted = true;
        openTimer = 0;
    }
    function setCrowdsaleOpenTimerFor(uint minutesFromNow) public {
        require(!crowdsaleStarted);
        openTimer = now + minutesFromNow * 1 minutes;
    }
    function clearCrowdsaleOpenTimer() public {
        require(!crowdsaleStarted);
        openTimer = 0;
    }
    function setCrowdsaleCloseTimerFor(uint minutesFromNow) public {
        require(!crowdsaleEnded);
        closeTimer = now + minutesFromNow * 1 minutes;
    }
    function clearCrowdsaleCloseTimer() public {
        require(!crowdsaleEnded);
        closeTimer = 0;
    }
    function createTokens(address recipient) internal {
        require(crowdsaleStarted);
        require(!crowdsaleEnded);
        require(msg.value != 0);
        require(recipient != 0);
        uint tokens = msg.value * wholeTokensPerEth;
        uint256 newTotalSupply = _totalSupply + tokens;
        _totalSupply = _totalSupply + tokens;
        _balances[recipient] = _balances[recipient] + tokens;
    }
    function closeCrowdsale() public {
        require(crowdsaleStarted);
        require(!crowdsaleEnded);
        crowdsaleEnded = true;
        closeTimer = 0;
    }  
    function setDecimals(uint8 newDecimals) public {
        require(crowdsaleEnded);
        decimals = newDecimals;
    }
}
