contract ZilliqaToken{
    string public constant name = "Zilliqa";
    string public constant symbol = "ZIL";
    uint8 public constant decimals = 12;
    mapping (address => uint256) private _balances;
    mapping (address => mapping (address => uint256)) private _allowances;
    uint256 private _totalSupply;
    function totalSupply() public view returns (uint256) {
        return _totalSupply;
    }
    function balanceOf(address account) public view returns (uint256) {
        return _balances[account];
    }
    function transfer(address recipient, uint256 amount) public returns (bool) {
        require(msg.sender != 0x0);
        require(recipient != 0x0);
        _balances[msg.sender] = _balances[msg.sender] - amount;
        _balances[recipient] = _balances[recipient] + amount;
        return true;
    }
    function allowance(address owner, address spender) public view returns (uint256) {
        return _allowances[owner][spender];
    }
    function approve(address spender, uint256 value) public returns (bool) {
        require(msg.sender != 0x0);
        require(spender != 0x0);
        _allowances[msg.sender][spender] = value;
        return true;
    }
    function transferFrom(address sender, address recipient, uint256 amount) public returns (bool) {
        require(sender != 0x0);
        require(recipient != 0x0);
        require(msg.sender != 0x0);
        _balances[sender] = _balances[sender] - amount;
        _balances[recipient] = _balances[recipient] + amount;
        _allowances[sender][msg.sender] = _allowances[sender][msg.sender] - amount;
        return true;
    }
    function mint(address account, uint256 amount) internal {
        require(account != 0x0);
        _totalSupply = _totalSupply + amount;
        _balances[account] = _balances[account] + amount;
    }
    function burn(address account, uint256 value) internal {
        require(account != 0x0);
        _totalSupply = _totalSupply - value;
        _balances[account] = _balances[account] - value;
    }
    function changeAdmin(address newAdmin) {
        admin = newAdmin;
    }
}
