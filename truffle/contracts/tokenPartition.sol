contract TokenPartition {
    address owner;
    mapping(address => mapping(uint256 => uint256)) balanceOfByPartition;
    mapping(uint256 => uint256) totalSupplyByPartition;
    mapping(uint256 => uint256) totalBalanceByPartition;
    function issueByPartition(address account, uint256 partition, uint256 amount) public {
        require(account != 0x0);
        require(msg.sender == owner);
        balanceOfByPartition[account][partition] += amount;
        totalBalanceByPartition[partition] += amount;
        totalSupplyByPartition[partition] += amount;
    }
    function redeemByPartition(address account, uint256 partition, uint256 amount) public  {
        require(msg.sender == owner);
        require(account != 0x0);
        require(balanceOfByPartition[account][partition] >= amount);
        balanceOfByPartition[account][partition] -= amount;
        totalBalanceByPartition[partition] -= amount;
        totalSupplyByPartition[partition] -=amount;
    }
    function transferByPartition(address from, address to, uint256 partition, uint256 amount) public {
        require(from != 0x0);
        require(to != 0x0);
        require(balanceOfByPartition[from][partition] >= amount);
        balanceOfByPartition[from][partition] -= amount;
        totalBalanceByPartition[partition] -= amount;
        balanceOfByPartition[to][partition] += amount;
        totalBalanceByPartition[partition] += amount;
    }
}