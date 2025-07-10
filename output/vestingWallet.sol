contract VestingWallet {
    uint256 private _released;
    address private immutable _beneficiary;
    uint64 private immutable _start;
    uint64 private immutable _duration;
    address private _owner;
    mapping(address => int) private _balanceOf;
    int private _totalSupply;
    int totalBalance;
    function mint(address account,int amount) public {
        require(msg.sender == _owner);
        require(account != 0x0);
        require(amount > 0);
        _totalSupply += amount;
        totalBalance += amount;
        _balanceOf[account] += amount;
    }
    function burn(address account,int amount) public {
        require(msg.sender == _owner);
        require(account != 0x0);
        require(amount > 0);
        require(_balanceOf[account]>=amount);
        _totalSupply -= amount;
        totalBalance -= amount;
        _balanceOf[account] -= amount;
    }
    function transfer(address from, address to, int amount) public {
        require(_balanceOf[from] >= amount);
        require(amount > 0);
        _balanceOf[from] -= amount;
        _balanceOf[to] += amount;
    }
    function totalSupply() public view returns(int) {
        return _totalSupply;
    }
    function balanceOf(address account) public view returns(int) {
        assert(_balanceOf[account]>=0);
        return _balanceOf[account];
    }
    function release() public virtual {
        uint256 releasable = vestedAmount(uint64(block.timestamp)) - released();
        _released += releasable;
        payable(beneficiary()).send(releasable);
    }
}