// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

interface IERC20 {
    function transfer(address to, uint256 amount) external returns (bool);
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
}

interface IERC721 {
    function safeTransferFrom(address from, address to, uint256 tokenId) external;
    function approve(address spender, uint256 tokenId) external;
    function ownerOf(uint256 tokenId) external view returns (address);
}

interface IERC721MintBurn {
    function mint(address to) external;
    function burn(uint256 tokenId) external;
}

interface iERC721CheckAuth {
    function isAuthorized(address owner, address spender, uint256 tokenId) external view returns (bool);
}

contract P404Token {
    string public name;
    string public symbol;
    uint8 public constant decimals = 18;
    uint256 public totalSupply;

    address public immutable erc721;
    address public owner;
    address public feeTo;

    uint256 public constant TRANSFORM_PRICE = 10000 * 10 ** 18;
    uint256 public constant TRANSFORM_LOSE_RATE = 200; // 2%

    mapping(address => uint256) private balances;

    event Transfer(address indexed from, address indexed to, uint256 value);
    event FromTokenToNFT(address indexed from, uint256 amount);
    event FromNFTToToken(address indexed from, uint256 tokenId);

    constructor(string memory _name, string memory _symbol, address _erc721, address _feeTo) {
        name = _name;
        symbol = _symbol;
        erc721 = _erc721;
        feeTo = _feeTo;
        owner = msg.sender;
        uint256 initialSupply = 260000000 * 10 ** 18;
        balances[msg.sender] = initialSupply;
        totalSupply = initialSupply;
        emit Transfer(address(0), msg.sender, initialSupply);
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    function balanceOf(address account) public view returns (uint256) {
        return balances[account];
    }

    function isValidTokenId(uint256 tokenId) public pure returns (bool) {
        return tokenId < 30001;
    }

    function transfer(address to, uint256 value) public returns (bool) {
        _transfer(msg.sender, to, value);
        return true;
    }

    function transferFrom(address from, address to, uint256 value) public returns (bool) {
        if (isValidTokenId(value)) {
            require(iERC721CheckAuth(erc721).isAuthorized(from, msg.sender, value), "Not authorized");
            IERC721(erc721).safeTransferFrom(from, to, value);
            if (to == address(this)) {
                _erc721ToErc20(value, from);
            }
        } else {
            _transfer(from, to, value);
        }
        return true;
    }

    function approve(address spender, uint256 tokenId) public {
        IERC721(erc721).approve(spender, tokenId);
    }

    function _transfer(address from, address to, uint256 value) internal {
        require(balances[from] >= value, "Insufficient balance");
        balances[from] -= value;
        balances[to] += value;
        emit Transfer(from, to, value);

        if (to == address(this)) {
            _erc20ToErc721(value, from);
        }
    }

    function _erc20ToErc721(uint256 amount, address sender) internal {
        require(amount >= TRANSFORM_PRICE && amount % TRANSFORM_PRICE == 0, "Invalid amount");
        uint256 nfts = amount / TRANSFORM_PRICE;
        uint256 realCost = nfts * TRANSFORM_PRICE;
        balances[address(this)] -= realCost;
        totalSupply -= realCost;

        IERC721MintBurn minter = IERC721MintBurn(erc721);
        for (uint256 i = 0; i < nfts; i++) {
            minter.mint(sender);
        }
        emit FromTokenToNFT(sender, amount);
    }

    function _erc721ToErc20(uint256 tokenId, address sender) internal {
        IERC721MintBurn burner = IERC721MintBurn(erc721);
        burner.burn(tokenId);
        uint256 amountToMint = (TRANSFORM_PRICE * (10000 - TRANSFORM_LOSE_RATE)) / 10000;
        balances[sender] += amountToMint;
        totalSupply += amountToMint;
        emit Transfer(address(0), sender, amountToMint);
        emit FromNFTToToken(sender, tokenId);
    }

    function ownerOf(uint256 tokenId) public view returns (address) {
        return IERC721(erc721).ownerOf(tokenId);
    }

    function mint(address to, uint256 amount) public {
        require(msg.sender == erc721, "Only NFT contract");
        balances[to] += amount;
        totalSupply += amount;
        emit Transfer(address(0), to, amount);
    }

    function setFeeTo(address newFeeTo) external onlyOwner {
        feeTo = newFeeTo;
    }

    receive() external payable {
        payable(feeTo).transfer(msg.value);
    }
}
