contract BNB{
    string public name;
    string public symbol;
    uint8 public decimals;
    uint256 public totalSupply;
	address public owner;
    uint256 totalBalance;
    uint256 freezeTotal;
    mapping (address => uint256) public balanceOf;
	mapping (address => uint256) public freezeOf;
    mapping (address => mapping (address => uint256)) public allowance;
    function transfer(address _to, uint256 _value) public {
        require(_to != 0x0);
		require(_value > 0);
        require(balanceOf[msg.sender] >= _value);
        require(balanceOf[_to] + _value >= balanceOf[_to]);
        balanceOf[msg.sender] = balanceOf[msg.sender] -  _value;
        totalBalance = totalBalance - _value;
        balanceOf[_to] = balanceOf[_to] + _value;
        totalBalance = totalBalance + _value;
    }
    function approve(address _spender, uint256 _value) public {
		require(_value > 0);
        allowance[msg.sender][_spender] = _value;
        return true;
    }
    function transferFrom(address _from, address _to, uint256 _value) public returns (bool success) {
        require(_to != 0x0);
		require(_value > 0); 
        require(balanceOf[_from] >= _value);
        require(balanceOf[_to] + _value >= balanceOf[_to]);
        require(_value <= allowance[_from][msg.sender]);
        balanceOf[_from] = balanceOf[_from] - _value;
        totalBalance = totalBalance - _value;
        balanceOf[_to] = balanceOf[_to] + _value;
        totalBalance = totalBalance + _value;
        allowance[_from][msg.sender] = allowance[_from][msg.sender] - _value;
        return true;
    }
    function burn(uint256 _value) public returns (bool success) {
        require(balanceOf[msg.sender] >= _value);
		require(_value > 0); 
        balanceOf[msg.sender] = balanceOf[msg.sender] - _value;
        totalBalance = totalBalance - _value;
        totalSupply = totalSupply - _value;
        return true;
    }
	function freeze(uint256 _value) public returns (bool success) {
        require(balanceOf[msg.sender] >= _value);
		require(_value > 0); 
        balanceOf[msg.sender] = balanceOf[msg.sender] - _value;
        totalBalance = totalBalance - _value;
        freezeOf[msg.sender] = freezeOf[msg.sender] + _value;
        freezeTotal = freezeTotal + _value;
        return true;
    }
	function unfreeze(uint256 _value) public returns (bool success) {
        require(freezeOf[msg.sender] >= _value);
		require(_value > 0); 
        freezeOf[msg.sender] = freezeOf[msg.sender] - _value;
        freezeTotal = freezeTotal - _value;
        balanceOf[msg.sender] = balanceOf[msg.sender] + _value;
        totalBalance = totalBalance + _value;
        return true;
    }
	function withdrawEther(uint256 amount) public {
		require(msg.sender == owner);
		payable(owner).transfer(amount);
	}
}