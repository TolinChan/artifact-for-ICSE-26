contract FinalizableCrowdsale {
    bool public isFinalized = false;
    bool onceFinalized = false;
    address public owner;
    Tx transaction;
    enum Tx {BuyTokens, Finalize, Mint, Transfer, TransferFrom}
    uint256 public startBlock;
    uint256 public endBlock;
    uint256 public rate;
    uint256 public weiRaised;
    function finalize() public {
        require(msg.sender == owner);
        require(!isFinalized);
        require(block.number > endBlock || weiRaised >= cap);
        isFinalized = true;
        mintingFinished = true;
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
    function buyTokens(address beneficiary) public payable {
        require(beneficiary != 0x0);
        uint256 weiAmount = msg.value;
        uint256 tokens = weiAmount * rate;
        weiRaised = weiRaised + weiAmount;
        mint(beneficiary, tokens);
        transaction = Tx.BuyTokens;
    }
}
