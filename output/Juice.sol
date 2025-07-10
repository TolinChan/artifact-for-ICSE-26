// SPDX-License-Identifier: MIT
pragma solidity 0.8.20;

interface IERC20 {
    function transfer(address to, uint256 amount) external returns (bool);
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
}

contract JuiceStaking {
    address public owner;
    IERC20 public Juice;

    uint256 public totalStaked;
    uint256 public rewardPerSecond;
    uint256 public lastRewardUpdate;
    uint256 public stakingEnd;
    uint256 public rewardPerShare;
    uint256 constant PRECISION = 1e18;

    struct StakeInfo {
        uint256 amount;
        uint256 rewardDebt;
        uint256 endTime;
        bool unstaked;
    }

    mapping(address => StakeInfo) public stakes;

    event Staked(address indexed user, uint256 amount);
    event Unstaked(address indexed user, uint256 amount);
    event RewardClaimed(address indexed user, uint256 amount);
    event StakingStarted(uint256 startTime, uint256 endTime, uint256 rewards);

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    constructor(address _juiceToken) {
        owner = msg.sender;
        Juice = IERC20(_juiceToken);
    }

    function stake(uint256 amount, uint256 weeksLocked) external {
        require(weeksLocked >= 1, "Minimum 1 week lock");
        require(block.timestamp < stakingEnd, "Staking period ended");
        require(stakes[msg.sender].amount == 0, "Already staking");

        updatePool();

        Juice.transferFrom(msg.sender, address(this), amount);
        totalStaked += amount;

        stakes[msg.sender] = StakeInfo({
            amount: amount,
            rewardDebt: (amount * rewardPerShare) / PRECISION,
            endTime: block.timestamp + (weeksLocked * 7 days),
            unstaked: false
        });

        emit Staked(msg.sender, amount);
    }

    function unstake() external {
        StakeInfo storage userStake = stakes[msg.sender];
        require(userStake.amount > 0 && !userStake.unstaked, "No active stake");
        require(block.timestamp >= userStake.endTime, "Lock period not ended");

        updatePool();

        uint256 pending = ((userStake.amount * rewardPerShare) / PRECISION) - userStake.rewardDebt;
        uint256 amountToSend = userStake.amount + pending;

        userStake.unstaked = true;
        totalStaked -= userStake.amount;

        Juice.transfer(msg.sender, amountToSend);

        emit Unstaked(msg.sender, amountToSend);
    }

    function claimReward() external {
        StakeInfo storage userStake = stakes[msg.sender];
        require(userStake.amount > 0 && !userStake.unstaked, "No active stake");

        updatePool();

        uint256 pending = ((userStake.amount * rewardPerShare) / PRECISION) - userStake.rewardDebt;
        require(pending > 0, "No rewards");

        userStake.rewardDebt += pending;

        Juice.transfer(msg.sender, pending);

        emit RewardClaimed(msg.sender, pending);
    }

    function updatePool() internal {
        if (block.timestamp <= lastRewardUpdate || totalStaked == 0) {
            lastRewardUpdate = block.timestamp;
            return;
        }

        uint256 timeElapsed = block.timestamp < stakingEnd ? block.timestamp - lastRewardUpdate : stakingEnd - lastRewardUpdate;
        uint256 rewards = timeElapsed * rewardPerSecond;
        rewardPerShare += (rewards * PRECISION) / totalStaked;

        lastRewardUpdate = block.timestamp;
    }

    function startStaking(uint256 rewardTokens, uint256 durationDays) external onlyOwner {
        require(stakingEnd == 0, "Already started");
        Juice.transferFrom(msg.sender, address(this), rewardTokens);

        lastRewardUpdate = block.timestamp;
        stakingEnd = block.timestamp + (durationDays * 1 days);
        rewardPerSecond = rewardTokens / (stakingEnd - block.timestamp);

        emit StakingStarted(block.timestamp, stakingEnd, rewardTokens);
    }

    function rescueRemainingTokens(address receiver) external onlyOwner {
        uint256 contractBalance = Juice.balanceOf(address(this));
        Juice.transfer(receiver, contractBalance - totalStaked);
    }
}
