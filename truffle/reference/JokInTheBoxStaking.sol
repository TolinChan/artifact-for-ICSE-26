// SPDX-License-Identifier: MIT
pragma solidity ^0.8.17;

interface IERC20 {
    function transfer(address recipient, uint256 amount) external returns (bool);
    function transferFrom(address sender, address recipient, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
}

contract JokInTheBoxStaking {
    address public owner;
    IERC20 public jokToken;
    address public stakingSigner;
    uint256 public ethTax = 5;
    uint256 public maxPercentage = 10;
    uint256 public totalStaked;

    struct Stake {
        bool unstaked;
        uint256 amountStaked;
        uint256 lockPeriod;
        uint256 stakedDay;
        uint256 unstakedDay;
    }

    mapping(address => Stake[]) public stakes;
    mapping(address => uint256) public nonce;

    mapping(uint256 => bool) public validLockPeriods;

    event NewStake(address indexed staker, uint256 amount, uint256 timestamp, uint256 lockPeriod);
    event Withdraw(address indexed staker, uint256 amount, bool inETH);
    event Unstake(address indexed staker, uint256 amount, uint256 timestamp);

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    constructor(address _jokToken, address _stakingSigner) {
        owner = msg.sender;
        jokToken = IERC20(_jokToken);
        stakingSigner = _stakingSigner;
        validLockPeriods[1] = true;
        validLockPeriods[30] = true;
        validLockPeriods[60] = true;
        validLockPeriods[90] = true;
    }

    function stake(uint256 amount, uint256 lockPeriod) external {
        require(validLockPeriods[lockPeriod], "Invalid lock period");
        stakes[msg.sender].push(Stake(false, amount, lockPeriod, block.timestamp / 1 days, 0));
        totalStaked += amount;
        jokToken.transferFrom(msg.sender, address(this), amount);
        emit NewStake(msg.sender, amount, block.timestamp, lockPeriod);
    }

    function unstake(uint256 stakeIndex) external {
        Stake storage s = stakes[msg.sender][stakeIndex];
        require(!s.unstaked, "Already unstaked");
        require(block.timestamp / 1 days >= s.stakedDay + s.lockPeriod, "Lock period active");

        s.unstaked = true;
        s.unstakedDay = block.timestamp / 1 days;
        totalStaked -= s.amountStaked;

        require(jokToken.transfer(msg.sender, s.amountStaked), "Transfer failed");
        emit Unstake(msg.sender, s.amountStaked, block.timestamp);
    }

    function withdraw(uint256 earnings, bool inETH, uint8 v, bytes32 r, bytes32 s) external {
        require(isValidSignature(msg.sender, earnings, inETH, nonce[msg.sender], v, r, s), "Invalid signature");
        nonce[msg.sender]++;

        if (inETH) {
            require(earnings <= address(this).balance * maxPercentage / 100, "Over max percentage");
            uint256 fee = earnings * ethTax / 100;
            earnings -= fee;
            payable(msg.sender).transfer(earnings);
        } else {
            require(earnings <= jokToken.balanceOf(address(this)) * maxPercentage / 100, "Over max percentage");
            require(jokToken.transfer(msg.sender, earnings), "Transfer failed");
        }
        emit Withdraw(msg.sender, earnings, inETH);
    }

    function isValidSignature(address user, uint256 amount, bool inETH, uint256 userNonce, uint8 v, bytes32 r, bytes32 s) internal view returns (bool) {
        bytes32 messageHash = keccak256(abi.encodePacked(address(this), user, amount, inETH, userNonce));
        bytes32 prefixedHash = keccak256(abi.encodePacked("\x19Ethereum Signed Message:\n32", messageHash));
        return ecrecover(prefixedHash, v, r, s) == stakingSigner;
    }

    // Admin functions
    function setValidLockPeriod(uint256 period, bool isValid) external onlyOwner {
        validLockPeriods[period] = isValid;
    }

    function setEthTax(uint256 _ethTax) external onlyOwner {
        require(_ethTax <= 100, "Invalid tax");
        ethTax = _ethTax;
    }

    function setMaxPercentage(uint256 _maxPercentage) external onlyOwner {
        require(_maxPercentage <= 100, "Invalid percentage");
        maxPercentage = _maxPercentage;
    }

    function setJokToken(address _jokToken) external onlyOwner {
        jokToken = IERC20(_jokToken);
    }

    function setStakingSigner(address _stakingSigner) external onlyOwner {
        stakingSigner = _stakingSigner;
    }

    receive() external payable {}
}
