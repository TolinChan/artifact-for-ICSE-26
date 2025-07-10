contract ERC1155 {
    mapping(uint256 => mapping(address => uint256)) private _balances;
    mapping(uint256 => uint256) private totalBalances;
    mapping(uint256 => uint256) private totalSupply;
    mapping(address => mapping(address => bool)) private _operatorApprovals;
    function balanceOf(address account, uint256 id) public view virtual override returns (uint256) {
        require(account != 0x0);
        return _balances[id][account];
    }
    function setApprovalForAll(address operator, bool approved) public {
        require(msg.sender != operator);
        _operatorApprovals[msg.sender][operator] = approved;
    }
    function isApprovedForAll(address account, address operator) public view returns (bool) {
        return _operatorApprovals[account][operator];
    }

    function safeTransferFrom(address from, address to, uint256 id, uint256 amount, bytes memory data) public {
        require(from == msg.sender || _operatorApprovals[account][operator]);
        require(_balances[id][from] >= amount);
        require(to != 0x0);
        address operator = msg.sender;
        _balances[id][from] = _balances[id][from] - amount;
        totalBalances[id] -= amount;
        _balances[id][to] += amount;
        totalBalances[id] += amount;
    }
    function _mint(address to, uint256 id, uint256 amount, bytes memory data) internal{
        require(to != 0x0);
        _balances[id][to] += amount;
        totalBalances[id] += amount;
        totalSupply[id] += amount;
    }

    function _burn(address from, uint256 id, uint256 amount) internal {
        require(from != 0x0);
        require(_balances[id][from] >= amount);
        _balances[id][from] = _balances[id][from] - amount;
        totalBalances[id] -= amount;
        totalSupply[id] -= amount;
    }
    function _setApprovalForAll(address owner, address operator, bool approved) internal {
        require(owner != operator);
        _operatorApprovals[owner][operator] = approved;
    }
}