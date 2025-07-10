contract CrowFunding {
  struct TargetTuple {
    uint t;
    bool _valid;
  }
  struct OwnerTuple {
    address p;
    bool _valid;
  }
  struct RaisedTuple {
    uint n;
    bool _valid;
  }
  struct ClosedTuple {
    bool b;
    bool _valid;
  }
  struct BeneficiaryTuple {
    address p;
    bool _valid;
  }
  struct BalanceOfTuple {
    uint n;
    bool _valid;
  }
  TargetTuple target;
  RaisedTuple raised;
  ClosedTuple closed;
  BeneficiaryTuple beneficiary;
  mapping(address=>BalanceOfTuple) balanceOf;
  OwnerTuple owner;
  event Refund(address p,uint n);
  event Invest(address p,uint n);
  event Closed(bool b);
  event Withdraw(address p,uint n);
  constructor(uint t,address b) public {
    updateOnceRefundOnInsertConstructor_r13();
    updateTotalBalanceOnInsertConstructor_r18();
    updateRaisedOnInsertConstructor_r7();
    updateOwnerOnInsertConstructor_r8();
    updateTargetOnInsertConstructor_r15(t);
    updateBeneficiaryOnInsertConstructor_r22(b);
    updateOnceWithdrawOnInsertConstructor_r16();
  }
  function getClosed() public view  returns (bool) {
      bool b = closed.b;
      return b;
  }
  function getRaised() public view  returns (uint) {
      uint n = raised.n;
      return n;
  }
  function refund() public    {
      bool r4 = updateRefundOnInsertRecv_refund_r4();
      if(r4==false) {
        revert("Rule condition failed");
      }
  }
  function close() public    {
      bool r11 = updateClosedOnInsertRecv_close_r11();
      if(r11==false) {
        revert("Rule condition failed");
      }
  }
  function invest() public  payable  {
      bool r5 = updateInvestOnInsertRecv_invest_r5();
      if(r5==false) {
        revert("Rule condition failed");
      }
  }
  function withdraw() public    {
      bool r10 = updateWithdrawOnInsertRecv_withdraw_r10();
      if(r10==false) {
        revert("Rule condition failed");
      }
  }
  function updateClosedOnInsertRecv_close_r11() private   returns (bool) {
      address s = owner.p;
      if(s==msg.sender) {
        closed = ClosedTuple(true,true);
        emit Closed(true);
        return true;
      }
      return false;
  }
  function updateOwnerOnInsertConstructor_r8() private    {
      address p = msg.sender;
      owner = OwnerTuple(p,true);
  }
  function updateOnceWithdrawOnInsertConstructor_r16() private    {
      // Empty()
  }
  function updateOnceRefundOnInsertConstructor_r13() private    {
      // Empty()
  }
  function updateRaisedOnInsertConstructor_r7() private    {
      raised = RaisedTuple(0,true);
  }
  function updateuintByint(uint x,int delta) private   returns (uint) {
      int convertedX = int(x);
      int value = convertedX+delta;
      uint convertedValue = uint(value);
      return convertedValue;
  }
  function updateRefundOnInsertRecv_refund_r4() private   returns (bool) {
      if(true==closed.b) {
        address p = msg.sender;
        uint t = target.t;
        uint r = raised.n;
        uint n = balanceOf[p].n;
        if(r<t && n>0) {
          emit Refund(p,n);
          payable(p).send(n);
          balanceOf[p].n -= n;
          return true;
        }
      }
      return false;
  }
  function updateWithdrawOnInsertRecv_withdraw_r10() private   returns (bool) {
      address p = beneficiary.p;
      uint t = target.t;
      uint r = raised.n;
      if(p==msg.sender) {
        if(r>=t) {
          emit Withdraw(p,r);
          payable(p).send(r);
          return true;
        }
      }
      return false;
  }
  function updateTargetOnInsertConstructor_r15(uint t) private    {
      target = TargetTuple(t,true);
  }
  function updateInvestOnInsertRecv_invest_r5() private   returns (bool) {
      if(false==closed.b) {
        uint s = raised.n;
        uint t = target.t;
        uint n = msg.value;
        address p = msg.sender;
        if(s<t) {
          emit Invest(p,n);
          balanceOf[p].n += n;
          raised.n += n;
          return true;
        }
      }
      return false;
  }
  function updateTotalBalanceOnInsertConstructor_r18() private    {
      // Empty()
  }
  function updateBeneficiaryOnInsertConstructor_r22(address p) private    {
      beneficiary = BeneficiaryTuple(p,true);
  }
}