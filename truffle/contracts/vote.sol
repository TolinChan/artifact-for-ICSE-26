contract Voting {
    mapping(uint32 => uint32) votes;
    mapping(address => bool) isVoter;
    mapping(address => bool) hasVoted;
    mapping(uint32 => bool) wins;
    bool hasWinner;
    uint32 quorum;
    function vote (uint32 proposal) public {
        require(isVoter[msg.sender]);
        require(!hasVoted[msg.sender]);
        require(!hasWinner);
        votes[proposal] += 1;
        hasVoted[msg.sender] = true;
        if (votes[proposal] > quorum) {
            wins[proposal] = true;
            hasWinner = true;
        }
    }
}