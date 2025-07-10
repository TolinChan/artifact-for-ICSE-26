// SPDX-License-Identifier: MIT
pragma solidity 0.8.7;

contract SheepFarm {
    struct Village {
        uint256 gems;
        uint256 money;
        uint256 yield;
        uint256 timestamp;
        uint8 warehouse;
        uint8 truck;
        uint8[6] sheeps;
    }

    mapping(address => Village) public villages;

    uint256 public totalSheeps;
    uint256 public totalInvested;

    address public manager;
    address public marketWallet;

    bool public initialized;

    modifier onlyOwner() {
        require(msg.sender == manager, "Not owner");
        _;
    }

    modifier isInitialized() {
        require(initialized, "Not initialized");
        _;
    }

    constructor(address _marketWallet) {
        manager = msg.sender;
        marketWallet = _marketWallet;
    }

    function initialize() external onlyOwner {
        initialized = true;
    }

    function register() external isInitialized {
        Village storage user = villages[msg.sender];
        require(user.timestamp == 0, "Already registered");
        user.gems = 10;
        user.timestamp = block.timestamp;
    }

    function buyGems() external payable isInitialized {
        Village storage user = villages[msg.sender];
        require(user.timestamp > 0, "Register first");
        uint256 gems = msg.value / 5e14;
        require(gems > 0, "Not enough ETH");
        user.gems += gems;
        totalInvested += msg.value;
        payable(marketWallet).transfer(msg.value / 10); // 10% fee
    }

    function collectYield() public isInitialized {
        Village storage user = villages[msg.sender];
        require(user.timestamp > 0, "Register first");
        uint256 hrs = (block.timestamp - user.timestamp) / 3600;
        if (hrs > getWarehouseCapacity(user.warehouse)) {
            hrs = getWarehouseCapacity(user.warehouse);
        }
        user.money += hrs * user.yield;
        user.timestamp = block.timestamp;
    }

    function upgradeFarm(uint256 farmId) external isInitialized {
        require(farmId < 6, "Invalid farmId");
        Village storage user = villages[msg.sender];
        collectYield();
        uint256 sheepCount = user.sheeps[farmId] + 1;
        uint256 cost = getSheepPrice(farmId, sheepCount);
        require(user.gems >= cost, "Insufficient gems");
        user.gems -= cost;
        user.yield += getSheepYield(farmId, sheepCount);
        user.sheeps[farmId] = uint8(sheepCount);
        totalSheeps++;
    }

    function upgradeWarehouse() external isInitialized {
        Village storage user = villages[msg.sender];
        collectYield();
        require(user.warehouse < 4, "Max level reached");
        uint256 cost = (user.warehouse + 1) * 2000;
        require(user.gems >= cost, "Insufficient gems");
        user.gems -= cost;
        user.warehouse += 1;
    }

    function upgradeTruck() external isInitialized {
        Village storage user = villages[msg.sender];
        collectYield();
        require(user.truck < 3, "Max level reached");
        uint256 cost = (user.truck + 1) * 10000;
        require(user.gems >= cost, "Insufficient gems");
        user.gems -= cost;
        user.truck += 1;
    }

    function withdraw(uint256 wool) external isInitialized {
        Village storage user = villages[msg.sender];
        collectYield();
        require(user.money >= wool, "Insufficient wool");
        user.money -= wool;
        uint256 amount = wool * 5e12;
        uint256 fee = amount / 50; // 2% fee
        payable(marketWallet).transfer(fee);
        payable(msg.sender).transfer(amount - fee);
    }

    function getWarehouseCapacity(uint8 level) internal pure returns (uint256) {
        return 24 + (level * 6);
    }

    function getSheepPrice(uint256 farmId, uint256 sheepId) internal pure returns (uint256) {
        uint256[6][10] memory prices = [
            [400,4000,12000,24000,40000,60000],
            [600,6000,18000,36000,60000,90000],
            [900,9000,27000,54000,90000,135000],
            [1350,13000,40000,81000,135000,202000],
            [2000,20000,60000,121000,202000,303000],
            [3000,30000,91000,182000,303000,455000],
            [4500,45000,136000,273000,455000,683000],
            [6800,68000,205000,410000,683000,1025000],
            [10000,102000,307000,615000,1025000,1537000],
            [15000,154000,461000,922000,1537000,2300000]
        ];
        require(sheepId <= 10, "Invalid sheepId");
        return prices[sheepId-1][farmId];
    }

    function getSheepYield(uint256 farmId, uint256 sheepId) internal pure returns (uint256) {
        uint256[6][10] memory yields = [
            [5,56,179,382,678,762],
            [8,85,272,581,1030,1142],
            [12,128,413,882,1564,1714],
            [18,195,628,1340,2379,2570],
            [28,297,954,2035,3620,3856],
            [42,450,1439,3076,5506,5783],
            [63,675,2159,4614,8259,8675],
            [95,1013,3238,6921,12389,13013],
            [142,1519,4857,10382,18583,19519],
            [213,2278,7285,15572,27874,29278]
        ];
        require(sheepId <= 10, "Invalid sheepId");
        return yields[sheepId-1][farmId];
    }

    function setMarketWallet(address _marketWallet) external onlyOwner {
        marketWallet = _marketWallet;
    }
    
    receive() external payable {}
}
