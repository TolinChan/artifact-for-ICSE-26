contract MANACrowdsale {
    uint256 public constant TOTAL_SHARE = 100;
    uint256 public constant CROWDSALE_SHARE = 40;
    uint256 public constant FOUNDATION_SHARE = 60;
    uint256 public preferentialRate;
    mapping (address => uint256) public buyerRate;
    uint256 public initialRate;
    uint256 public endRate;
    function setBuyerRate(address buyer, uint256 rate) public {
        require(rate != 0);
        require(whitelist[buyer]);
        require(block.number < startBlock);
        buyerRate[buyer] = rate;
    }
    function setInitialRate(uint256 rate) onlyOwner public {
        require(rate != 0);
        require(block.number < startBlock);
        initialRate = rate;
    }
    function setEndRate(uint256 rate) onlyOwner public {
        require(rate != 0);
        require(block.number < startBlock);
        endRate = rate;
    }
    function getRate() internal returns(uint256) {
        if (buyerRate[msg.sender] != 0) {
            return buyerRate[msg.sender];
        }
        if (whitelist[msg.sender]) {
            return preferentialRate;
        }
        uint256 elapsed = block.number - startBlock;
        uint256 rateRange = initialRate - endRate;
        uint256 blockRange = endBlock - startBlock;
        return initialRate - (rateRange * elapsed / blockRange);
    }
    function buyTokens(address beneficiary) public payable {
        require(beneficiary != 0x0);
        uint256 weiAmount = msg.value;
        uint256 tokens = weiAmount * rate;
        weiRaised = weiRaised + weiAmount;
        mint(beneficiary, tokens);
        transaction = Tx.BuyTokens;
    }
    function setWallet(address _wallet) public {
        require(_wallet != 0x0);
        wallet = _wallet;
    }
    function finalization() internal {
        uint256 totalSupply = token.totalSupply();
        uint256 finalSupply = TOTAL_SHARE * totalSupply / CROWDSALE_SHARE;
    }

}
