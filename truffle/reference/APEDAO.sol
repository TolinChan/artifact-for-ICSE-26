// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

interface IERC20 {
    function transfer(address recipient, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
    function approve(address spender, uint256 amount) external returns (bool);
}

interface IPancakeRouter {
    function swapExactTokensForTokensSupportingFeeOnTransferTokens(
        uint256 amountIn,
        uint256 amountOutMin,
        address[] calldata path,
        address to,
        uint256 deadline
    ) external;
}

interface IPancakeFactory {
    function createPair(address tokenA, address tokenB) external returns (address pair);
}

contract APE2 {
    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;
    uint256 public totalSupply;

    string public name = "APEDAO";
    string public symbol = "APEDAO";
    uint8 public decimals = 18;

    address public owner;

    IERC20 public USDT;
    IPancakeRouter public router;
    address public pair;

    address public receiveAddress;

    uint256 public taxLp = 2;
    uint256 public taxNFT = 2;
    uint256 public taxChildCoin = 1;

    bool inSwap;

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    modifier lockSwap() {
        inSwap = true;
        _;
        inSwap = false;
    }

    constructor(address _usdt, address _router, address _receiveAddress) {
        owner = msg.sender;
        USDT = IERC20(_usdt);
        router = IPancakeRouter(_router);
        receiveAddress = _receiveAddress;

        pair = IPancakeFactory(router.factory()).createPair(_usdt, address(this));

        _mint(msg.sender, 10000 * 10**18);

        approve(address(router), type(uint256).max);
    }

    function _mint(address to, uint256 amount) internal {
        totalSupply += amount;
        balanceOf[to] += amount;
        emit Transfer(address(0), to, amount);
    }

    function approve(address spender, uint256 amount) public returns (bool) {
        allowance[msg.sender][spender] = amount;
        emit Approval(msg.sender, spender, amount);
        return true;
    }

    function transfer(address to, uint256 amount) public returns (bool) {
        _transfer(msg.sender, to, amount);
        return true;
    }

    function transferFrom(address from, address to, uint256 amount) public returns (bool) {
        require(allowance[from][msg.sender] >= amount, "Allowance exceeded");
        allowance[from][msg.sender] -= amount;
        _transfer(from, to, amount);
        return true;
    }

    function _transfer(address from, address to, uint256 amount) internal {
        require(balanceOf[from] >= amount, "Insufficient balance");

        uint256 fee = 0;

        if (from == pair || to == pair) {
            fee = amount * (taxLp + taxNFT + taxChildCoin) / 100;
            balanceOf[address(this)] += fee;
        }

        balanceOf[from] -= amount;
        balanceOf[to] += (amount - fee);

        emit Transfer(from, to, amount - fee);

        if (!inSwap && balanceOf[address(this)] > 0) {
            swapTokensForUsdt(balanceOf[address(this)]);
        }
    }

    function swapTokensForUsdt(uint256 tokenAmount) internal lockSwap {
        address[] memory path = new address[](2);
        path[0] = address(this);
        path[1] = address(USDT);

        router.swapExactTokensForTokensSupportingFeeOnTransferTokens(
            tokenAmount,
            0,
            path,
            receiveAddress,
            block.timestamp
        );

        balanceOf[address(this)] = 0;
    }

    function setTax(uint256 _taxLp, uint256 _taxNFT, uint256 _taxChildCoin) external onlyOwner {
        taxLp = _taxLp;
        taxNFT = _taxNFT;
        taxChildCoin = _taxChildCoin;
    }

    function rescueTokens(address token, uint256 amount) external onlyOwner {
        IERC20(token).transfer(owner, amount);
    }

    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
}
