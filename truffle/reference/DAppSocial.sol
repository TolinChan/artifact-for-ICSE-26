// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

interface IERC20 {
    function transfer(address to, uint256 amount) external returns (bool);
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
}

contract DAppSocialPoolModel {

    mapping(address => uint256) private _nativeBalances;
    mapping(address => mapping(address => uint256)) private _tokenBalances;
    mapping(address => bool) private _supportedTokens;
    mapping(address => bool) private _adminList;
    address private _owner;

    modifier onlyOwner() {
        require(msg.sender == _owner, "Not owner");
        _;
    }

    modifier adminOnly() {
        require(_adminList[msg.sender], "Not admin");
        _;
    }

    modifier validAmount(uint256 amount) {
        require(amount > 0, "Amount > 0");
        _;
    }

    constructor() {
        _owner = msg.sender;
        _adminList[msg.sender] = true;
    }

    function depositNative() external payable validAmount(msg.value) {
        _nativeBalances[msg.sender] += msg.value;
    }

    function depositTokens(address token, uint256 amount) external validAmount(amount) {
        require(_supportedTokens[token], "Token unsupported");
        IERC20(token).transferFrom(msg.sender, address(this), amount);
        _tokenBalances[token][msg.sender] += amount;
    }

    function withdrawNative(uint256 amount) external validAmount(amount) {
        require(_nativeBalances[msg.sender] >= amount, "Insufficient balance");
        unchecked {
            _nativeBalances[msg.sender] -= amount;
        }
        (bool success,) = payable(msg.sender).call{value: amount}("");
        require(success, "ETH transfer failed");
    }

    function withdrawTokens(address token, uint256 amount) external validAmount(amount) {
        require(_supportedTokens[token], "Token unsupported");
        require(_tokenBalances[token][msg.sender] >= amount, "Insufficient balance");
        unchecked {
            _tokenBalances[token][msg.sender] -= amount;
        }
        IERC20(token).transfer(msg.sender, amount);
    }

    function addSupportedToken(address token) external adminOnly {
        _supportedTokens[token] = true;
    }

    function removeSupportedToken(address token) external adminOnly {
        _supportedTokens[token] = false;
    }

    function addAdmin(address admin) external onlyOwner {
        _adminList[admin] = true;
    }

    function removeAdmin(address admin) external onlyOwner {
        _adminList[admin] = false;
    }

    function transferTokens(address token, address from, address to, uint256 amount) external adminOnly validAmount(amount) {
        require(_tokenBalances[token][from] >= amount, "Insufficient balance");
        unchecked {
            _tokenBalances[token][from] -= amount;
        }
        _tokenBalances[token][to] += amount;
    }

    function transferNative(address from, address to, uint256 amount) external adminOnly validAmount(amount) {
        require(_nativeBalances[from] >= amount, "Insufficient balance");
        unchecked {
            _nativeBalances[from] -= amount;
        }
        _nativeBalances[to] += amount;
    }

    function getTokenBalance(address token, address user) external view returns (uint256) {
        return _tokenBalances[token][user];
    }

    function getNativeBalance(address user) external view returns (uint256) {
        return _nativeBalances[user];
    }
}