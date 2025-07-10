// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

abstract contract ERC20 {
    mapping(address => uint256) internal _balances;
    uint256 public totalSupply;

    event Transfer(address indexed from, address indexed to, uint256 value);

    constructor(string memory name_, string memory symbol_) {}

    function _mint(address account, uint256 amount) internal virtual {
        totalSupply += amount;
        _balances[account] += amount;
        emit Transfer(address(0), account, amount);
    }

    function _transfer(address from, address to, uint256 amount) internal virtual {
        require(_balances[from] >= amount, "Insufficient balance");
        _balances[from] -= amount;
        _balances[to] += amount;
        emit Transfer(from, to, amount);
    }
}

contract SSS is ERC20 {
    uint256 constant TOTAL_SUPPLY = 555_555_555_555_555 * 10**18;

    address public owner;
    address public communityAddress;
    address public devTaxReceiverAddress;

    uint256 public buyTaxPercent = 200;
    uint256 public sellTaxPercent = 200;

    bool public limitEnabled = true;
    uint256 public maxAmountPerTx = TOTAL_SUPPLY * 5 / 10000;
    uint256 public maxAmountPerAccount = TOTAL_SUPPLY * 5 / 10000;

    mapping(address => bool) public liquidityPools;
    mapping(address => bool) public unlimiteds;
    mapping(address => bool) public excludeFromTaxes;

    constructor(address community, address devTaxReceiver) ERC20("SSS", "SSS") {
        owner = msg.sender;
        communityAddress = community;
        devTaxReceiverAddress = devTaxReceiver;

        excludeFromTaxes[msg.sender] = true;
        excludeFromTaxes[address(this)] = true;

        _mint(msg.sender, TOTAL_SUPPLY);
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    function transfer(address to, uint256 amount) external {
        _transferWithTax(msg.sender, to, amount);
    }

    function _transferWithTax(address from, address to, uint256 amount) internal {
        require(!limitEnabled || amount <= maxAmountPerTx || unlimiteds[from], "Transfer limit");

        uint256 tax = 0;
        if (liquidityPools[from] && !excludeFromTaxes[to]) {
            tax = amount * buyTaxPercent / 10000;
        } else if (liquidityPools[to] && !excludeFromTaxes[from]) {
            tax = amount * sellTaxPercent / 10000;
        }

        uint256 amountAfterTax = amount - tax;

        require(!limitEnabled || unlimiteds[to] || (_balances[to] + amountAfterTax) <= maxAmountPerAccount, "Balance limit");

        if (tax > 0) {
            uint256 halfTax = tax / 2;
            _transfer(from, communityAddress, halfTax);
            _transfer(from, devTaxReceiverAddress, tax - halfTax);
        }

        _transfer(from, to, amountAfterTax);
    }

    function setLiquidityPool(address pool, bool isPool) external onlyOwner {
        liquidityPools[pool] = isPool;
    }

    function setUnlimited(address addr, bool isUnlimited) external onlyOwner {
        unlimiteds[addr] = isUnlimited;
    }

    function setExcludeFromTax(address addr, bool exclude) external onlyOwner {
        excludeFromTaxes[addr] = exclude;
    }

    function setLimitConfig(uint256 _maxAmountPerTx, uint256 _maxAmountPerAccount) external onlyOwner {
        maxAmountPerTx = _maxAmountPerTx;
        maxAmountPerAccount = _maxAmountPerAccount;
    }

    function setLimitEnabled(bool enabled) external onlyOwner {
        limitEnabled = enabled;
    }
}
