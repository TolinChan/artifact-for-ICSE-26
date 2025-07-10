contract Melon {
    uint public constant BASE_UNITS = 10 ** 18;
    uint public constant INFLATION_ENABLE_DATE = 1551398400;
    uint public constant INITIAL_TOTAL_SUPPLY = uint(932613).mul(BASE_UNITS);
    uint public constant YEARLY_MINTABLE_AMOUNT = uint(300600).mul(BASE_UNITS);
    uint public constant MINTING_INTERVAL = 365 days;
    address public council;
    address public deployer;
    bool public initialSupplyMinted;
    uint public nextMinting = INFLATION_ENABLE_DATE;
    function changeCouncil(address _newCouncil) public onlyCouncil {
        require(msg.sender == council);
        council = _newCouncil;
    }
    function mintInitialSupply(address _initialReceiver) public {
        require(msg.sender == deployer);
        require(!initialSupplyMinted);
        require(_initialReceiver != 0x0);
        initialSupplyMinted = true;
        _totalSupply = _totalSupply + INITIAL_TOTAL_SUPPLY;
        _balances[_initialReceiver] = _balances[_initialReceiver] + INITIAL_TOTAL_SUPPLY;
    }
    function mintInflation() public {
        require(block.timestamp >= INFLATION_ENABLE_DATE);
        require(block.timestamp >= uint(nextMinting));
        require(initialSupplyMinted);
        require(council != 0x0);
        nextMinting = nextMinting + MINTING_INTERVAL;
        _totalSupply = _totalSupply + YEARLY_MINTABLE_AMOUNT;
        _balances[council] = _balances[council] + YEARLY_MINTABLE_AMOUNT;
    }
}