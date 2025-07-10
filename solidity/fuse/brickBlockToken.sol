contract BrickBlockToken {
  struct OwnerTuple {
    address p;
    bool _valid;
  }
  struct PausedTuple {
    bool p;
    bool _valid;
  }
  struct InitialSupplyTuple {
    uint a;
    bool _valid;
  }
  struct BalancesTuple {
    uint a;
    bool _valid;
  }
  struct SuccessorAddressTuple {
    address p;
    bool _valid;
  }
  struct AllowedTuple {
    uint a;
    bool _valid;
  }
  struct BonusDistributionAddressTuple {
    address p;
    bool _valid;
  }
  struct BonusShareTuple {
    uint n;
    bool _valid;
  }
  struct TokenSaleActiveTuple {
    bool b;
    bool _valid;
  }
  struct FountainContractAddressTuple {
    address p;
    bool _valid;
  }
  struct DeadTuple {
    bool b;
    bool _valid;
  }
  mapping(address=>BalancesTuple) balances;
  PausedTuple paused;
  TokenSaleActiveTuple tokenSaleActive;
  InitialSupplyTuple initialSupply;
  OwnerTuple owner;
  SuccessorAddressTuple successorAddress;
  mapping(address=>mapping(address=>AllowedTuple)) allowed;
  BonusDistributionAddressTuple bonusDistributionAddress;
  BonusShareTuple bonusShare;
  FountainContractAddressTuple fountainContractAddress;
  DeadTuple dead;
  event Approve(address owner,address operator,uint n);
  event FountainContractAddress(address p);
  event IncreaseAllowance(address o,address s,uint d);
  event DecreaseAllowance(address o,address s,uint a);
  event Pause();
  event TransferOwnership(address old,address _new);
  event Unpause();
  event DistributeTokens(address p,uint v);
  event Upgrade(address p);
  event FinalizeTokenSale();
  event Burn(address p,uint n);
  event TransferFrom(address operator,address from,address to,uint a);
  event BonusDistributionAddress(address p);
  event Transfer(address from,address to,uint a);
  constructor() public {
    updatePausedOnInsertConstructor_r10();
    updateOnceEndSaleOnInsertConstructor_r4();
    updateOwnerOnInsertConstructor_r42();
    updateOnceUpgradeOnInsertConstructor_r18();
    updateOnceUnpausedOnInsertConstructor_r41();
  }
  function upgrade(address p) public    {
      bool r37 = updateUpgradeOnInsertRecv_upgrade_r37(p);
      if(r37==false) {
        revert("Rule condition failed");
      }
  }
  function finalizeTokenSale() public    {
      bool r30 = updateFinalizeTokenSaleOnInsertRecv_finalizeTokenSale_r30();
      if(r30==false) {
        revert("Rule condition failed");
      }
  }
  function approve(address s,uint n) public    {
      bool r47 = updateIncreaseAllowanceOnInsertRecv_approve_r47(s,n);
      bool r33 = updateApproveOnInsertRecv_approve_r33(s,n);
      bool r13 = updateDecreaseAllowanceOnInsertRecv_approve_r13(s,n);
      if(r47==false && r33==false && r13==false) {
        revert("Rule condition failed");
      }
  }
  function decreaseApproval(address s,uint a) public    {
      bool r1 = updateDecreaseAllowanceOnInsertRecv_decreaseApproval_r1(s,a);
      if(r1==false) {
        revert("Rule condition failed");
      }
  }
  function distributeTokens(address p,uint v) public    {
      bool r27 = updateDistributeTokensOnInsertRecv_distributeTokens_r27(p,v);
      if(r27==false) {
        revert("Rule condition failed");
      }
  }
  function unpause() public    {
      bool r3 = updateUnpauseOnInsertRecv_unpause_r3();
      if(r3==false) {
        revert("Rule condition failed");
      }
  }
  function evacuate(address p) public    {
      bool r12 = updateBurnOnInsertRecv_evacuate_r12(p);
      if(r12==false) {
        revert("Rule condition failed");
      }
  }
  function increaseApproval(address s,uint a) public    {
      bool r34 = updateIncreaseAllowanceOnInsertRecv_increaseApproval_r34(s,a);
      if(r34==false) {
        revert("Rule condition failed");
      }
  }
  function transferFrom(address from,address to,uint a) public    {
      bool r23 = updateTransferFromOnInsertRecv_transferFrom_r23(from,to,a);
      if(r23==false) {
        revert("Rule condition failed");
      }
  }
  function changeFountainContractAddress(address p) public    {
      bool r19 = updateFountainContractAddressOnInsertRecv_changeFountainContractAddress_r19(p);
      if(r19==false) {
        revert("Rule condition failed");
      }
  }
  function transfer(address to,uint a) public    {
      bool r20 = updateTransferOnInsertRecv_transfer_r20(to,a);
      if(r20==false) {
        revert("Rule condition failed");
      }
  }
  function updateTransferFromOnInsertRecv_transferFrom_r23(address f,address r,uint n) private   returns (bool) {
      address s = msg.sender;
      uint a = allowed[f][s].a;
      uint m = balances[f].a;
      if(r!=address(0) && n<=m && n<=a) {
        emit TransferFrom(s,f,r,n);
        allowed[s][f].a -= n;
        emit Transfer(f,r,n);
        balances[f].a -= n;
        balances[r].a += n;
        return true;
      }
      return false;
  }
  function updateDistributeTokensOnInsertRecv_distributeTokens_r27(address a,uint n) private   returns (bool) {
      bool b = tokenSaleActive.b;
      address s = owner.p;
      if(s==msg.sender) {
        if(b==true && a!=address(0) && a!=s) {
          emit DistributeTokens(a,n);
          address s = address(this);
          emit Transfer(s,a,n);
          balances[s].a -= n;
          balances[a].a += n;
          return true;
        }
      }
      return false;
  }
  function updateTransferOnInsertRecv_transfer_r20(address r,uint n) private   returns (bool) {
      address s = msg.sender;
      if(false==paused.p) {
        uint m = balances[s].a;
        if(r!=address(0) && n<=m) {
          emit Transfer(s,r,n);
          balances[s].a -= n;
          balances[r].a += n;
          return true;
        }
      }
      return false;
  }
  function updateOnceEndSaleOnInsertConstructor_r4() private    {
      // Empty()
  }
  function updateFountainContractAddressOnInsertRecv_changeFountainContractAddress_r19(address a) private   returns (bool) {
      address thisAddr = address(this);
      address s = owner.p;
      if(s==msg.sender) {
        if(a!=s && thisAddr!=a) {
          fountainContractAddress = FountainContractAddressTuple(a,true);
          emit FountainContractAddress(a);
          return true;
        }
      }
      return false;
  }
  function updateIncreaseAllowanceOnInsertRecv_approve_r47(address o,uint n) private   returns (bool) {
      address s = msg.sender;
      uint m = allowed[o][s].a;
      if(n>m) {
        uint d = n-m;
        emit IncreaseAllowance(o,s,d);
        allowed[o][s].a += d;
        return true;
      }
      return false;
  }
  function updateOnceUpgradeOnInsertConstructor_r18() private    {
      // Empty()
  }
  function updateuintByint(uint x,int delta) private   returns (uint) {
      int convertedX = int(x);
      int value = convertedX+delta;
      uint convertedValue = uint(value);
      return convertedValue;
  }
  function updateDecreaseAllowanceOnInsertRecv_approve_r13(address o,uint n) private   returns (bool) {
      address s = msg.sender;
      uint m = allowed[o][s].a;
      if(n<m) {
        uint d = m-n;
        emit DecreaseAllowance(o,s,d);
        allowed[o][s].a -= d;
        return true;
      }
      return false;
  }
  function updateFinalizeTokenSaleOnInsertRecv_finalizeTokenSale_r30() private   returns (bool) {
      address fa = fountainContractAddress.p;
      address s = msg.sender;
      if(true==tokenSaleActive.b) {
        address a = bonusDistributionAddress.p;
        if(s==owner.p) {
          if(a!=address(0) && fa!=address(0)) {
            emit FinalizeTokenSale();
            uint s = initialSupply.a;
            uint bs = bonusShare.n;
            address p = address(this);
            address bda = bonusDistributionAddress.p;
            uint n = (s*bs)/100;
            emit Transfer(p,bda,n);
            balances[p].a -= n;
            balances[bda].a += n;
            tokenSaleActive = TokenSaleActiveTuple(false,true);
            return true;
          }
        }
      }
      return false;
  }
  function updatePausedOnInsertConstructor_r10() private    {
      paused = PausedTuple(true,true);
  }
  function updateBurnOnInsertRecv_evacuate_r12(address p) private   returns (bool) {
      address s = msg.sender;
      if(true==dead.b) {
        if(s==successorAddress.p) {
          uint n = balances[p].a;
          emit Burn(p,n);
          emit Transfer(p,address(0),n);
          balances[p].a -= n;
          balances[address(0)].a += n;
          return true;
        }
      }
      return false;
  }
  function updateOnceUnpausedOnInsertConstructor_r41() private    {
      // Empty()
  }
  function updateDecreaseAllowanceOnInsertRecv_decreaseApproval_r1(address s,uint m) private   returns (bool) {
      address o = msg.sender;
      uint n = allowed[o][s].a;
      if(n>=m) {
        uint d = n-m;
        emit DecreaseAllowance(o,s,d);
        allowed[o][s].a -= d;
        return true;
      }
      return false;
  }
  function updateIncreaseAllowanceOnInsertRecv_increaseApproval_r34(address s,uint m) private   returns (bool) {
      address o = msg.sender;
      emit IncreaseAllowance(o,s,m);
      allowed[o][s].a += m;
      return true;
      return false;
  }
  function updateApproveOnInsertRecv_approve_r33(address o,uint n) private   returns (bool) {
      address s = msg.sender;
      emit Approve(s,o,n);
      return true;
      return false;
  }
  function updateUnpauseOnInsertRecv_unpause_r3() private   returns (bool) {
      address s = msg.sender;
      bool b = paused.p;
      if(false==dead.b) {
        if(s==owner.p) {
          if(b==true) {
            emit Unpause();
            paused = PausedTuple(false,true);
            return true;
          }
        }
      }
      return false;
  }
  function updateUpgradeOnInsertRecv_upgrade_r37(address p) private   returns (bool) {
      address s = owner.p;
      if(s==msg.sender) {
        if(p!=address(0)) {
          emit Upgrade(p);
          paused = PausedTuple(true,true);
          dead = DeadTuple(true,true);
          return true;
        }
      }
      return false;
  }
  function updateOwnerOnInsertConstructor_r42() private    {
      address s = msg.sender;
      owner = OwnerTuple(s,true);
  }
}