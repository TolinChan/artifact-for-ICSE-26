// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

interface IERC20 {
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
    function transfer(address to, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
}

contract WIFStaking {
    address public owner;
    address public immutable stakingToken;
    address constant BURN_ADDRESS = 0x000000000000000000000000000000000000dEaD;

    struct Plan {
        uint256 apr;
        uint256 stakeDuration;
        bool conclude;
    }

    struct Staking {
        uint256 amount;
        uint256 stakeAt;
        uint256 endstakeAt;
    }

    mapping(uint256 => Plan) public plans;
    mapping(address => Staking) public stakes;

    uint256 public penalty = 20;

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    constructor(address _stakingToken) {
        owner = msg.sender;
        stakingToken = _stakingToken;

        plans[0] = Plan({apr: 50, stakeDuration: 15 days, conclude: false});
        plans[1] = Plan({apr: 100, stakeDuration: 30 days, conclude: false});
        plans[2] = Plan({apr: 300, stakeDuration: 90 days, conclude: false});
        plans[3] = Plan({apr: 600, stakeDuration: 180 days, conclude: false});
    }

    function stake(uint256 planId, uint256 amount) external {
        require(amount > 0, "Zero amount");
        Plan memory plan = plans[planId];
        require(!plan.conclude, "Plan concluded");
        IERC20(stakingToken).transferFrom(msg.sender, address(this), amount);

        stakes[msg.sender] = Staking({
            amount: amount,
            stakeAt: block.timestamp,
            endstakeAt: block.timestamp + plan.stakeDuration
        });
    }

    function unstake(uint256 planId, uint256 burnRate) external {
        require(burnRate == 10 || burnRate == 25 || burnRate == 40, "Invalid burnRate");
        Staking memory userStake = stakes[msg.sender];
        require(block.timestamp >= userStake.endstakeAt, "Locked");

        uint256 reward = (userStake.amount * plans[planId].apr) / 10000;
        uint256 burnAmount = (reward * burnRate) / 100;

        IERC20(stakingToken).transfer(msg.sender, userStake.amount + reward - burnAmount);
        IERC20(stakingToken).transfer(BURN_ADDRESS, burnAmount);

        delete stakes[msg.sender];
    }

    function claimEarned(uint256 planId, uint256 burnRate) external {
        require(burnRate == 10 || burnRate == 25 || burnRate == 40, "Invalid burnRate");
        Staking storage userStake = stakes[msg.sender];
        require(block.timestamp >= userStake.endstakeAt, "Locked");

        uint256 reward = (userStake.amount * plans[planId].apr) / 10000;
        uint256 burnAmount = (reward * burnRate) / 100;

        IERC20(stakingToken).transfer(msg.sender, reward - burnAmount);
        IERC20(stakingToken).transfer(BURN_ADDRESS, burnAmount);

        userStake.stakeAt = block.timestamp;
        userStake.endstakeAt = block.timestamp + plans[planId].stakeDuration;
    }

    function emergencyWithdraw() external {
        Staking memory userStake = stakes[msg.sender];
        require(userStake.amount > 0, "No stake");

        uint256 burnAmount = (userStake.amount * penalty) / 100;
        IERC20(stakingToken).transfer(msg.sender, userStake.amount - burnAmount);
        IERC20(stakingToken).transfer(BURN_ADDRESS, burnAmount);

        delete stakes[msg.sender];
    }

    function concludeStaking(uint256 planId) external onlyOwner {
        plans[planId].conclude = true;
    }

    function withdrawNativeToken() external onlyOwner {
        payable(msg.sender).transfer(address(this).balance);
    }

    function penaltyWithdraw(uint256 amount) external onlyOwner {
        IERC20(stakingToken).transfer(msg.sender, amount);
    }

    receive() external payable {}
}
