// SPDX-License-Identifier: MIT
pragma solidity ^0.8.6;

interface IERC20 {
    function balanceOf(address account) external view returns (uint256);
    function transfer(address recipient, uint256 amount) external returns (bool);
    function approve(address spender, uint256 amount) external returns (bool);
}

interface IUniswapV2Router02 {
    function swapExactTokensForTokensSupportingFeeOnTransferTokens(
        uint256 amountIn,
        uint256 amountOutMin,
        address[] calldata path,
        address to,
        uint256 deadline
    ) external;

    function addLiquidity(
        address tokenA,
        address tokenB,
        uint256 amountADesired,
        uint256 amountBDesired,
        uint256 amountAMin,
        uint256 amountBMin,
        address to,
        uint256 deadline
    ) external;

    function factory() external pure returns (address);
}

interface IUniswapV2Factory {
    function createPair(address tokenA, address tokenB) external returns (address pair);
}

contract GPU {
    IERC20 USDT;
    address public uniswapV2Pair;
    IUniswapV2Router02 public uniswapV2Router;
    address public owner;
    uint256 public totalSupply = 10**24;
    address public tokenOwner;
    address _baseToken = 0x55d398326f99059fF775485246999027B3197955;
    mapping(address => uint256) balances;
    mapping(address => bool) public isExcludedFromFeesVip;

    constructor(address _tokenOwner) {
        owner = msg.sender;
        tokenOwner = _tokenOwner;
        uniswapV2Router = IUniswapV2Router02(0x10ED43C718714eb63d5aA57B78B54704E256024E);
        uniswapV2Pair = IUniswapV2Factory(uniswapV2Router.factory()).createPair(address(this), _baseToken);
        USDT = IERC20(_baseToken);

        balances[tokenOwner] = totalSupply;

        USDT.approve(address(uniswapV2Router), type(uint256).max);
        isExcludedFromFeesVip[address(this)] = true;
        isExcludedFromFeesVip[tokenOwner] = true;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    function transfer(address to, uint256 amount) external returns (bool) {
        _transfer(msg.sender, to, amount);
        return true;
    }

    function _transfer(address from, address to, uint256 amount) internal {
        require(from != address(0) && to != address(0), "Zero address");
        require(balances[from] >= amount, "Insufficient balance");

        uint256 fee = 0;

        if (!isExcludedFromFeesVip[from] && !isExcludedFromFeesVip[to]) {
            if (to == uniswapV2Pair || from == uniswapV2Pair) {
                fee = amount / 100; // 1% fee
                balances[address(this)] += fee;
            }
        }

        balances[from] -= amount;
        balances[to] += (amount - fee);

        if (balances[address(this)] > totalSupply / 2000) {
            uint256 contractTokenBalance = balances[address(this)];
            swapTokensForUsdt(contractTokenBalance / 2);
            uint256 usdtBalance = USDT.balanceOf(address(this));
            addLiquidityUsdt(usdtBalance, contractTokenBalance / 2);
            balances[address(this)] = 0;
        }
    }

    function balanceOf(address account) public view returns (uint256) {
        return balances[account];
    }

    function swapTokensForUsdt(uint256 tokenAmount) private {
        address[] memory path = new address[](2);
        path[0] = address(this);
        path[1] = _baseToken;

        uniswapV2Router.swapExactTokensForTokensSupportingFeeOnTransferTokens(
            tokenAmount,
            0,
            path,
            tokenOwner,
            block.timestamp
        );
    }

    function addLiquidityUsdt(uint256 usdtAmount, uint256 tokenAmount) private {
        uniswapV2Router.addLiquidity(
            _baseToken,
            address(this),
            usdtAmount,
            tokenAmount,
            0,
            0,
            tokenOwner,
            block.timestamp
        );
    }

    function rescueToken(address tokenAddress, uint256 tokens) external onlyOwner {
        IERC20(tokenAddress).transfer(owner, tokens);
    }
}