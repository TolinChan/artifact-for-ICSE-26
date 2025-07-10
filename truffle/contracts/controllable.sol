contract Controllable {
    address controller;
    uint256 totalBalance;
    mapping (address => uint256) private _balances;
    mapping (address => mapping (address => uint256)) private _allowances;
    uint256 private _totalSupply;
    function controllerTransfer(address _from, address _to, uint256 _value) {
        require(msg.sender == controller);
        require(_from != 0x0);
        require(_to != 0x0);
        _balances[_from] = _balances[_from] - amount;
        _balances[_to] = _balances[_to] + amount;
    }
    function controllerRedeem(address _tokenHolder, uint256 _value) {
        require(msg.sender == controller);
        require(_tokenHolder != 0x0);
        _totalSupply = _totalSupply - _value;
        _balances[_tokenHolder] = _balances[_tokenHolder] - _value;
        totalBalance -= _value;
    }
    function mint(address account, uint256 amount) public {
        require(account != 0x0);
        _totalSupply = _totalSupply + amount;
        _balances[account] = _balances[account] + amount;
        totalBalance += amount;
    }
    function burn(address account, uint256 amount) public {
        require(account != 0x0);
        _totalSupply = _totalSupply - value;
        _balances[account] = _balances[account] - value;
        totalBalance -= amount;
    }
}