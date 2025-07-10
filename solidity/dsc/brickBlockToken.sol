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
  function transferFrom(address from,address to,uint a) public    {
      bool r23 = updateTransferFromOnInsertRecv_transferFrom_r23(from,to,a);
      if(r23==false) {
        revert("Rule condition failed");
      }
  }
  function approve(address s,uint n) public    {
      bool r33 = updateApproveOnInsertRecv_approve_r33(s,n);
      bool r13 = updateDecreaseAllowanceOnInsertRecv_approve_r13(s,n);
      bool r47 = updateIncreaseAllowanceOnInsertRecv_approve_r47(s,n);
      if(r33==false && r13==false && r47==false) {
        revert("Rule condition failed");
      }
  }
  function increaseApproval(address s,uint a) public    {
      bool r34 = updateIncreaseAllowanceOnInsertRecv_increaseApproval_r34(s,a);
      if(r34==false) {
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
  function updateAllowanceTotalOnInsertIncreaseAllowance_r15(address o,address s,uint n) private    {
      int delta0 = int(n);
      updateAllowedOnIncrementAllowanceTotal_r26(o,s,delta0);
  }
  function updateAllowedOnIncrementSpentTotal_r26(address f,address s,int l) private    {
      int _delta = int(-l);
      uint newValue = updateuintByint(allowed[f][s].a,_delta);
      allowed[f][s].a = newValue;
  }
  function updateTransferOnInsertTransferFrom_r11(address s,address r,uint n) private    {
      updateTotalInOnInsertTransfer_r25(r,n);
      updateTotalOutOnInsertTransfer_r14(s,n);
      emit Transfer(s,r,n);
  }
  function updateDecreaseTotalOnInsertDecreaseAllowance_r29(address f,address s,uint n) private    {
      int delta0 = int(n);
      updateAllowedOnIncrementDecreaseTotal_r26(f,s,delta0);
  }
  function updateDeadOnInsertUpgrade_r43() private    {
      dead = DeadTuple(true,true);
  }
  function updateSpentTotalOnInsertTransferFrom_r5(address f,address s,uint n) private    {
      int delta0 = int(n);
      updateAllowedOnIncrementSpentTotal_r26(f,s,delta0);
  }
  function updateOnceEndSaleOnInsertConstructor_r4() private    {
      // Empty()
  }
  function updateTotalOutOnInsertTransfer_r14(address p,uint n) private    {
      int delta0 = int(n);
      updateBalancesOnIncrementTotalOut_r21(p,delta0);
  }
  function updateTransferOnInsertBurn_r2(address s,uint n) private    {
      updateTotalOutOnInsertTransfer_r14(s,n);
      updateTotalInOnInsertTransfer_r25(address(0),n);
      emit Transfer(s,address(0),n);
  }
  function updateTotalInOnInsertTransfer_r25(address p,uint n) private    {
      int delta0 = int(n);
      updateBalancesOnIncrementTotalIn_r21(p,delta0);
  }
  function updateDistributeTokensOnInsertRecv_distributeTokens_r27(address a,uint n) private   returns (bool) {
      bool b = tokenSaleActive.b;
      address s = owner.p;
      if(s==msg.sender) {
        if(b==true && a!=address(0) && a!=s) {
          updateTransferOnInsertDistributeTokens_r40(a,n);
          emit DistributeTokens(a,n);
          return true;
        }
      }
      return false;
  }
  function updateuintByint(uint x,int delta) private   returns (uint) {
      int convertedX = int(x);
      int value = convertedX+delta;
      uint convertedValue = uint(value);
      return convertedValue;
  }
  function updateAllowedOnIncrementDecreaseTotal_r26(address f,address s,int d) private    {
      int _delta = int(-d);
      uint newValue = updateuintByint(allowed[f][s].a,_delta);
      allowed[f][s].a = newValue;
  }
  function updatePausedOnInsertConstructor_r10() private    {
      paused = PausedTuple(true,true);
  }
  function updateDecreaseAllowanceOnInsertRecv_decreaseApproval_r1(address s,uint m) private   returns (bool) {
      address o = msg.sender;
      uint n = allowed[o][s].a;
      if(n>=m) {
        uint d = n-m;
        updateDecreaseTotalOnInsertDecreaseAllowance_r29(o,s,d);
        emit DecreaseAllowance(o,s,d);
        return true;
      }
      return false;
  }
  function updateBalancesOnIncrementTotalOut_r21(address p,int o) private    {
      int _delta = int(-o);
      uint newValue = updateuintByint(balances[p].a,_delta);
      balances[p].a = newValue;
  }
  function updateTokenSaleActiveOnInsertFinalizeTokenSale_r28() private    {
      tokenSaleActive = TokenSaleActiveTuple(false,true);
  }
  function updatePausedOnInsertUnpause_r48() private    {
      paused = PausedTuple(false,true);
  }
  function updateBalancesOnIncrementTotalIn_r21(address p,int i) private    {
      int _delta = int(i);
      uint newValue = updateuintByint(balances[p].a,_delta);
      balances[p].a = newValue;
  }
  function updateIncreaseAllowanceOnInsertRecv_approve_r47(address o,uint n) private   returns (bool) {
      address s = msg.sender;
      uint m = allowed[o][s].a;
      if(n>m) {
        uint d = n-m;
        updateAllowanceTotalOnInsertIncreaseAllowance_r15(o,s,d);
        emit IncreaseAllowance(o,s,d);
        return true;
      }
      return false;
  }
  function updateUnpauseOnInsertRecv_unpause_r3() private   returns (bool) {
      address s = msg.sender;
      bool b = paused.p;
      if(false==dead.b) {
        if(s==owner.p) {
          if(b==true) {
            updatePausedOnInsertUnpause_r48();
            emit Unpause();
            return true;
          }
        }
      }
      return false;
  }
  function updateTransferOnInsertDistributeTokens_r40(address p,uint n) private    {
      address s = address(this);
      updateTotalOutOnInsertTransfer_r14(s,n);
      updateTotalInOnInsertTransfer_r25(p,n);
      emit Transfer(s,p,n);
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
  function updateOnceUpgradeOnInsertConstructor_r18() private    {
      // Empty()
  }
  function updateAllowedOnIncrementAllowanceTotal_r26(address f,address s,int n) private    {
      int _delta = int(n);
      uint newValue = updateuintByint(allowed[f][s].a,_delta);
      allowed[f][s].a = newValue;
  }
  function updateApproveOnInsertRecv_approve_r33(address o,uint n) private   returns (bool) {
      address s = msg.sender;
      emit Approve(s,o,n);
      return true;
      return false;
  }
  function updateIncreaseAllowanceOnInsertRecv_increaseApproval_r34(address s,uint m) private   returns (bool) {
      address o = msg.sender;
      updateAllowanceTotalOnInsertIncreaseAllowance_r15(o,s,m);
      emit IncreaseAllowance(o,s,m);
      return true;
      return false;
  }
  function updateUpgradeOnInsertRecv_upgrade_r37(address p) private   returns (bool) {
      address s = owner.p;
      if(s==msg.sender) {
        if(p!=address(0)) {
          updatePausedOnInsertUpgrade_r6();
          updateDeadOnInsertUpgrade_r43();
          emit Upgrade(p);
          return true;
        }
      }
      return false;
  }
  function updatePausedOnInsertUpgrade_r6() private    {
      paused = PausedTuple(true,true);
  }
  function updateDecreaseAllowanceOnInsertRecv_approve_r13(address o,uint n) private   returns (bool) {
      address s = msg.sender;
      uint m = allowed[o][s].a;
      if(n<m) {
        uint d = m-n;
        updateDecreaseTotalOnInsertDecreaseAllowance_r29(o,s,d);
        emit DecreaseAllowance(o,s,d);
        return true;
      }
      return false;
  }
  function updateTransferFromOnInsertRecv_transferFrom_r23(address f,address r,uint n) private   returns (bool) {
      address s = msg.sender;
      uint a = allowed[f][s].a;
      uint m = balances[f].a;
      if(r!=address(0) && n<=m && n<=a) {
        updateTransferOnInsertTransferFrom_r11(f,r,n);
        updateSpentTotalOnInsertTransferFrom_r5(s,f,n);
        emit TransferFrom(s,f,r,n);
        return true;
      }
      return false;
  }
  function updateBurnOnInsertRecv_evacuate_r12(address p) private   returns (bool) {
      address s = msg.sender;
      if(true==dead.b) {
        if(s==successorAddress.p) {
          uint n = balances[p].a;
          updateTransferOnInsertBurn_r2(p,n);
          emit Burn(p,n);
          return true;
        }
      }
      return false;
  }
  function updateTransferOnInsertFinalizeTokenSale_r0() private    {
      uint s = initialSupply.a;
      uint bs = bonusShare.n;
      address p = address(this);
      address bda = bonusDistributionAddress.p;
      uint n = (s*bs)/100;
      updateTotalInOnInsertTransfer_r25(bda,n);
      updateTotalOutOnInsertTransfer_r14(p,n);
      emit Transfer(p,bda,n);
  }
  function updateOwnerOnInsertConstructor_r42() private    {
      address s = msg.sender;
      owner = OwnerTuple(s,true);
  }
  function updateFinalizeTokenSaleOnInsertRecv_finalizeTokenSale_r30() private   returns (bool) {
      address fa = fountainContractAddress.p;
      address s = msg.sender;
      if(true==tokenSaleActive.b) {
        address a = bonusDistributionAddress.p;
        if(s==owner.p) {
          if(a!=address(0) && fa!=address(0)) {
            updateTokenSaleActiveOnInsertFinalizeTokenSale_r28();
            updateTransferOnInsertFinalizeTokenSale_r0();
            emit FinalizeTokenSale();
            return true;
          }
        }
      }
      return false;
  }
  function updateTransferOnInsertRecv_transfer_r20(address r,uint n) private   returns (bool) {
      address s = msg.sender;
      if(false==paused.p) {
        uint m = balances[s].a;
        if(r!=address(0) && n<=m) {
          updateTotalInOnInsertTransfer_r25(r,n);
          updateTotalOutOnInsertTransfer_r14(s,n);
          emit Transfer(s,r,n);
          return true;
        }
      }
      return false;
  }
  function updateOnceUnpausedOnInsertConstructor_r41() private    {
      // Empty()
  }
}