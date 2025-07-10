contract CappedCrowdsale {
    uint256 public startBlock;
    uint256 public endBlock;
    uint256 public rate;
    uint256 public weiRaised;
    uint256 public cap;
    bool public isFinalized = false;
    bool onceFinalized=false;
    Tx transaction;
    enum Tx {
        BuyTokens,Finalize,Mint,Transfer,TransferFrom
    }
    function validPurchase() internal view returns (bool) {
        bool withinCap = weiRaised + msg.value <= cap;
        uint256 current = block.number;
        bool withinPeriod = current >= startBlock && current <= endBlock;
        bool nonZeroPurchase = msg.value != 0;
        return withinPeriod && nonZeroPurchase && withinCap;
    }
    function hasEnded() public view returns (bool) {
        bool capReached = weiRaised >= cap;
        return block.number > endBlock || capReached;
    }
    function finalize() public onlyOwner {
        require(!isFinalized);
        require(hasEnded());
        isFinalized = true;
        finishMinting();
        onceFinalized = true;
        transaction = Tx.Finalize;
    }
    function mint(address _to, uint256 _amount) public returns (bool) {
        totalSupply = totalSupply + _amount;
        balances[_to] = balances[_to] + _amount;
        transaction = Tx.Mint;
        return true;
    }
    function transfer(address _to, uint256 _amount) public returns (bool) {
        balances[msg.sender] = balances[msg.sender] - _value;
        balances[_to] = balances[_to] + _valu;
        transaction = Tx.Transfer;
        return true;
    }
    function buyTokens(address beneficiary) override public payable {
        require(beneficiary != 0x0);
        uint256 weiAmount = msg.value;
        uint256 tokens = weiAmount * rate;
        weiRaised = weiRaised + weiAmount;
        mint(beneficiary, tokens);
        transaction=Tx.BuyTokens;
    }
}
