// SPDX-License-Identifier: MIT
pragma solidity ^0.6.12;

interface IERC20 {
    function balanceOf(address account) external view returns (uint256);
    function transfer(address recipient, uint256 amount) external returns (bool);
    function transferFrom(address sender, address recipient, uint256 amount) external returns (bool);
}

interface IERC721 {
    function ownerOf(uint256 tokenId) external view returns (address);
    function safeTransferFrom(address from, address to, uint256 tokenId) external;
}

contract Pool {
    IERC20 public pledgeToken;
    IERC20 public rewardToken;
    IERC721 public nft;

    address public owner;

    uint256 public totalStaked;
    uint256 public nftRewardMultiplier;

    struct UserInfo {
        uint256 amount;
        uint256 nftCount;
        uint256 rewardDebt;
    }

    mapping(address => UserInfo) public userInfo;

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    constructor(address _pledgeToken, address _rewardToken, address _nft, uint256 _nftRewardMultiplier) public {
        owner = msg.sender;
        pledgeToken = IERC20(_pledgeToken);
        rewardToken = IERC20(_rewardToken);
        nft = IERC721(_nft);
        nftRewardMultiplier = _nftRewardMultiplier;
    }

    function stake(uint256 amount) external {
        require(amount > 0, "Amount 0");
        pledgeToken.transferFrom(msg.sender, address(this), amount);
        UserInfo storage user = userInfo[msg.sender];

        user.amount += amount;
        totalStaked += amount;

        user.rewardDebt = pendingReward(msg.sender);
    }

    function withdraw(uint256 amount) external {
        UserInfo storage user = userInfo[msg.sender];
        require(user.amount >= amount, "Insufficient");

        uint256 reward = pendingReward(msg.sender);
        if (reward > 0) {
            rewardToken.transfer(msg.sender, reward);
        }

        user.amount -= amount;
        totalStaked -= amount;
        pledgeToken.transfer(msg.sender, amount);

        user.rewardDebt = pendingReward(msg.sender);
    }

    function stakeNft(uint256 tokenId) external {
        require(nft.ownerOf(tokenId) == msg.sender, "Not owner");
        nft.safeTransferFrom(msg.sender, address(this), tokenId);

        userInfo[msg.sender].nftCount += 1;
        userInfo[msg.sender].rewardDebt = pendingReward(msg.sender);
    }

    function unstakeNft(uint256 tokenId) external {
        require(nft.ownerOf(tokenId) == address(this), "Not staked");

        nft.safeTransferFrom(address(this), msg.sender, tokenId);
        userInfo[msg.sender].nftCount -= 1;
        userInfo[msg.sender].rewardDebt = pendingReward(msg.sender);
    }

    function pendingReward(address userAddr) public view returns (uint256) {
        UserInfo storage user = userInfo[userAddr];
        uint256 nftBonus = user.nftCount * nftRewardMultiplier;
        return user.amount + nftBonus - user.rewardDebt;
    }

    function rescueTokens(address token, uint256 amount) external onlyOwner {
        IERC20(token).transfer(owner, amount);
    }

    function rescueNft(uint256 tokenId) external onlyOwner {
        nft.safeTransferFrom(address(this), owner, tokenId);
    }
}