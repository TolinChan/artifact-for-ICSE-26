contract Auction {
    address payable public beneficiary;
    uint public auctionEndTime;
    address public highestBidder;
    uint public highestBid;
    mapping(address => uint) pendingReturns;
    bool ended;
    constructor() {
        beneficiary = 0x998877aa;
        auctionEndTime = 998877;
    }
    function bid() public payable {
        require(block.timestamp < auctionEndTime);
        require(msg.value > highestBid);
        pendingReturns[highestBidder] += highestBid;
        highestBidder = msg.sender;
        highestBid = msg.value;
    }
    function withdraw() public returns (bool) {
        uint amount = pendingReturns[msg.sender];
        if (amount > 0) {
            pendingReturns[msg.sender] = 0;
            if (!payable(msg.sender).send(amount)) {
                pendingReturns[msg.sender] = amount;
                return false;
            }
        }
        return true;
    }
    function auctionEnd() public {
        require(block.timestamp > auctionEndTime);
        require(!ended);
        ended = true;
        beneficiary.transfer(highestBid);
    }
}