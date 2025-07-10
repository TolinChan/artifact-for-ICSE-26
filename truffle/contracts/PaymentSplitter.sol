contract PaymentSplitter {
    uint256 private _totalShares;
    uint256 private _totalReleased;
    mapping(address => uint256) private _shares;
    mapping(address => uint256) private _released;
    address[] private _payees;
    function totalShares() public view returns (uint256) {
        return _totalShares;
    }
    function totalReleased() public view returns (uint256) {
        return _totalReleased;
    }
    function shares(address account) public view returns (uint256) {
        return _shares[account];
    }
    function released(address account) public view returns (uint256) {
        return _released[account];
    }
    function payee(uint256 index) public view returns (address) {
        return _payees[index];
    }
    function releasable(address account) public view returns (uint256) {
        uint256 totalReceived = address(this).balance + _totalReleased;
        return (totalReceived * _shares[account]) / _totalShares - _released[account];
    }
    function release(address payable account) public virtual {
        require(_shares[account] > 0);
        _released[account] += payment;
        _totalReleased += payment;
        account.transfer(payment);
    }
    function _pendingPayment(address account, uint256 totalReceived, uint256 alreadyReleased) private view returns (uint256) {
        return (totalReceived * _shares[account]) / _totalShares - alreadyReleased;
    }
    function _addPayee(address account, uint256 shares_) private {
        require(account != 0x0);
        require(shares_ > 0);
        require(_shares[account] == 0,);
        _payees.push(account);
        _shares[account] = shares_;
        _totalShares = _totalShares + shares_;
    }
}