contract Crowdfunding {
    mapping(address => uint256) deposits;
    enum State {OPEN, SUCCESS, REFUND}
    State state;
    uint256 raised;
    uint256 goal;
    uint256 closeTime;
    uint256 totalDeposits;
    constructor(address payable b) public {
        state = State.OPEN;
        raised = 0;
        goal = 10000;
        closeTime = 998877;
        totalDeposits = 0;
    }
    function invest() payable public{
        require(now <= closeTime);
        require(raised < goal);
        raised += msg.value;
        deposits[msg.sender] += msg.value;
        totalDeposits += msg.value;
    }
	function close_success() public{
        require(raised >= goal);
        state = State.SUCCESS;
	}
    function close_refund() public{
        require(now > closeTime);
        require(raised < goal);
        state = State.REFUND;
    }
    function withdraw() public {
        require(state == State.SUCCESS);
        totalDeposits = 0;
    }
    function claimRefund(address payable p) public {
        require(state == State.REFUND);
        totalDeposits = totalDeposits - deposits[p];
        deposits[p] = 0;
    }
}