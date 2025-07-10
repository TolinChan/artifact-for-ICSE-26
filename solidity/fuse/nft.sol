contract Nft {
  struct OwnerOfTuple {
    address p;
    bool _valid;
  }
  struct OwnerTuple {
    address p;
    bool _valid;
  }
  struct ApprovedTuple {
    bool b;
    bool _valid;
  }
  struct LatestTransferTuple {
    address from;
    address to;
    uint time;
    bool _valid;
  }
  struct ApprovalTuple {
    address p;
    bool b;
    bool _valid;
  }
  struct ExistsTuple {
    bool b;
    bool _valid;
  }
  struct IsApprovedForAllTuple {
    bool b;
    bool _valid;
  }
  struct BalanceOfTuple {
    uint n;
    bool _valid;
  }
  mapping(uint=>OwnerOfTuple) ownerOf;
  mapping(uint=>mapping(address=>ApprovedTuple)) approved;
  mapping(uint=>LatestTransferTuple) latestTransfer;
  mapping(address=>mapping(uint=>ApprovalTuple)) approval;
  mapping(uint=>ExistsTuple) exists;
  mapping(address=>mapping(address=>IsApprovedForAllTuple)) isApprovedForAll;
  mapping(address=>BalanceOfTuple) balanceOf;
  OwnerTuple owner;
  event Transfer(uint tokenId,address from,address to,uint time);
  event Approval(address o,uint tokenId,address p,bool b);
  event IsApprovedForAll(address owner,address operator,bool b);
  constructor() public {
    updateOwnerOnInsertConstructor_r10();
  }
  function getApproved(uint tokenId,address p) public view  returns (bool) {
      bool b = approved[tokenId][p].b;
      return b;
  }
  function getOwnerOf(uint tokenId) public view  returns (address) {
      address p = ownerOf[tokenId].p;
      return p;
  }
  function getIsApprovedForAll(address owner,address operator) public view  returns (bool) {
      bool b = isApprovedForAll[owner][operator].b;
      return b;
  }
  function transfer(address to,uint tokenId) public    {
      bool r9 = updateTransferOnInsertRecv_transfer_r9(to,tokenId);
      if(r9==false) {
        revert("Rule condition failed");
      }
  }
  function mint(uint tokenId,address to) public    {
      bool r3 = updateTransferOnInsertRecv_mint_r3(tokenId,to);
      if(r3==false) {
        revert("Rule condition failed");
      }
  }
  function getExists(uint tokenId) public view  returns (bool) {
      bool b = exists[tokenId].b;
      return b;
  }
  function setApprovalForAll(address operator,bool _approved) public    {
      bool r2 = updateIsApprovedForAllOnInsertRecv_setApprovalForAll_r2(operator,_approved);
      if(r2==false) {
        revert("Rule condition failed");
      }
  }
  function burn(uint tokenId) public    {
      bool r12 = updateTransferOnInsertRecv_burn_r12(tokenId);
      if(r12==false) {
        revert("Rule condition failed");
      }
  }
  function setApproval(uint tokenId,address p,bool b) public    {
      bool r1 = updateApprovalOnInsertRecv_setApproval_r1(tokenId,p,b);
      if(r1==false) {
        revert("Rule condition failed");
      }
  }
  function transferFrom(address from,address to,uint tokenId) public    {
      bool r7 = updateTransferOnInsertRecv_transferFrom_r7(from,to,tokenId);
      bool r6 = updateTransferOnInsertRecv_transferFrom_r6(from,to,tokenId);
      if(r7==false && r6==false) {
        revert("Rule condition failed");
      }
  }
  function getBalanceOf(address p) public view  returns (uint) {
      uint n = balanceOf[p].n;
      return n;
  }
  function updateIsApprovedForAllOnInsertRecv_setApprovalForAll_r2(address o,bool b) private   returns (bool) {
      address p = msg.sender;
      isApprovedForAll[p][o] = IsApprovedForAllTuple(b,true);
      emit IsApprovedForAll(p,o,b);
      return true;
      return false;
  }
  function updateTransferOnInsertRecv_transferFrom_r6(address s,address r,uint tokenId) private   returns (bool) {
      uint time = block.timestamp;
      if(true==approved[tokenId][s].b) {
        if(r!=address(0)) {
          emit Transfer(tokenId,s,r,time);
          uint _max = latestTransfer[tokenId].time;
          if(time>_max) {
            latestTransfer[tokenId] = LatestTransferTuple(s,r,time,true);
            address r0__ = latestTransfer[tokenId].from;
            address r0_p = latestTransfer[tokenId].to;
            uint r0__ = latestTransfer[tokenId].time;
            if(r0_p!=address(0)) {
              if(r0_p==ownerOf[tokenId].p) {
                ownerOf[tokenId] = OwnerOfTuple(address(address(0)),false);
              }
              address r0_p = approval[r0_p][tokenId].p;
              bool b = approval[r0_p][tokenId].b;
              if(b==approved[tokenId][r0_p].b) {
                approved[tokenId][r0_p] = ApprovedTuple(false,false);
              }
              balanceOf[r0_p].n -= 1;
            }
            if(r!=address(0)) {
              ownerOf[tokenId] = OwnerOfTuple(r,true);
              address r4_o = ownerOf[tokenId].p;
              address r = approval[r4_o][tokenId].p;
              bool b = approval[r4_o][tokenId].b;
              if(b==approved[tokenId][r].b) {
                approved[tokenId][r] = ApprovedTuple(false,false);
              }
              address r = approval[r][tokenId].p;
              bool b = approval[r][tokenId].b;
              approved[tokenId][r] = ApprovedTuple(b,true);
              address r5_p = ownerOf[tokenId].p;
              balanceOf[r5_p].n -= 1;
              balanceOf[r].n += 1;
            }
            address r11__ = latestTransfer[tokenId].from;
            address r11_to = latestTransfer[tokenId].to;
            uint r11__ = latestTransfer[tokenId].time;
            if(r11_to!=address(0)) {
              if(true==exists[tokenId].b) {
                exists[tokenId] = ExistsTuple(false,false);
              }
            }
            if(r!=address(0)) {
              exists[tokenId] = ExistsTuple(true,true);
            }
          }
          return true;
        }
      }
      return false;
  }
  function updateOwnerOnInsertConstructor_r10() private    {
      address s = msg.sender;
      owner = OwnerTuple(s,true);
  }
  function updateTransferOnInsertRecv_transfer_r9(address r,uint tokenId) private   returns (bool) {
      uint time = block.timestamp;
      address s = msg.sender;
      if(s==ownerOf[tokenId].p) {
        if(r!=address(0)) {
          emit Transfer(tokenId,s,r,time);
          uint _max = latestTransfer[tokenId].time;
          if(time>_max) {
            latestTransfer[tokenId] = LatestTransferTuple(s,r,time,true);
            address r0__ = latestTransfer[tokenId].from;
            address r0_p = latestTransfer[tokenId].to;
            uint r0__ = latestTransfer[tokenId].time;
            if(r0_p!=address(0)) {
              if(r0_p==ownerOf[tokenId].p) {
                ownerOf[tokenId] = OwnerOfTuple(address(address(0)),false);
              }
              address r0_p = approval[r0_p][tokenId].p;
              bool b = approval[r0_p][tokenId].b;
              if(b==approved[tokenId][r0_p].b) {
                approved[tokenId][r0_p] = ApprovedTuple(false,false);
              }
              balanceOf[r0_p].n -= 1;
            }
            if(r!=address(0)) {
              ownerOf[tokenId] = OwnerOfTuple(r,true);
              address r4_o = ownerOf[tokenId].p;
              address r = approval[r4_o][tokenId].p;
              bool b = approval[r4_o][tokenId].b;
              if(b==approved[tokenId][r].b) {
                approved[tokenId][r] = ApprovedTuple(false,false);
              }
              address r = approval[r][tokenId].p;
              bool b = approval[r][tokenId].b;
              approved[tokenId][r] = ApprovedTuple(b,true);
              address r5_p = ownerOf[tokenId].p;
              balanceOf[r5_p].n -= 1;
              balanceOf[r].n += 1;
            }
            address r11__ = latestTransfer[tokenId].from;
            address r11_to = latestTransfer[tokenId].to;
            uint r11__ = latestTransfer[tokenId].time;
            if(r11_to!=address(0)) {
              if(true==exists[tokenId].b) {
                exists[tokenId] = ExistsTuple(false,false);
              }
            }
            if(r!=address(0)) {
              exists[tokenId] = ExistsTuple(true,true);
            }
          }
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
  function updateApprovalOnInsertRecv_setApproval_r1(uint tokenId,address p,bool b) private   returns (bool) {
      address o = msg.sender;
      if(o==ownerOf[tokenId].p) {
        approval[o][tokenId] = ApprovalTuple(p,b,true);
        emit Approval(o,tokenId,p,b);
        address r4_p = approval[o][tokenId].p;
        bool r4_b = approval[o][tokenId].b;
        if(o==ownerOf[tokenId].p) {
          if(r4_b==approved[tokenId][r4_p].b) {
            approved[tokenId][r4_p] = ApprovedTuple(false,false);
          }
        }
        if(o==ownerOf[tokenId].p) {
          approved[tokenId][p] = ApprovedTuple(b,true);
        }
        return true;
      }
      return false;
  }
  function updateTransferOnInsertRecv_transferFrom_r7(address s,address r,uint tokenId) private   returns (bool) {
      address o = msg.sender;
      uint time = block.timestamp;
      if(s==ownerOf[tokenId].p) {
        if(true==isApprovedForAll[s][o].b) {
          if(r!=address(0)) {
            emit Transfer(tokenId,s,r,time);
            uint _max = latestTransfer[tokenId].time;
            if(time>_max) {
              latestTransfer[tokenId] = LatestTransferTuple(s,r,time,true);
              address r0__ = latestTransfer[tokenId].from;
              address r0_p = latestTransfer[tokenId].to;
              uint r0__ = latestTransfer[tokenId].time;
              if(r0_p!=address(0)) {
                if(r0_p==ownerOf[tokenId].p) {
                  ownerOf[tokenId] = OwnerOfTuple(address(address(0)),false);
                }
                address r0_p = approval[r0_p][tokenId].p;
                bool b = approval[r0_p][tokenId].b;
                if(b==approved[tokenId][r0_p].b) {
                  approved[tokenId][r0_p] = ApprovedTuple(false,false);
                }
                balanceOf[r0_p].n -= 1;
              }
              if(r!=address(0)) {
                ownerOf[tokenId] = OwnerOfTuple(r,true);
                address r4_o = ownerOf[tokenId].p;
                address r = approval[r4_o][tokenId].p;
                bool b = approval[r4_o][tokenId].b;
                if(b==approved[tokenId][r].b) {
                  approved[tokenId][r] = ApprovedTuple(false,false);
                }
                address r = approval[r][tokenId].p;
                bool b = approval[r][tokenId].b;
                approved[tokenId][r] = ApprovedTuple(b,true);
                address r5_p = ownerOf[tokenId].p;
                balanceOf[r5_p].n -= 1;
                balanceOf[r].n += 1;
              }
              address r11__ = latestTransfer[tokenId].from;
              address r11_to = latestTransfer[tokenId].to;
              uint r11__ = latestTransfer[tokenId].time;
              if(r11_to!=address(0)) {
                if(true==exists[tokenId].b) {
                  exists[tokenId] = ExistsTuple(false,false);
                }
              }
              if(r!=address(0)) {
                exists[tokenId] = ExistsTuple(true,true);
              }
            }
            return true;
          }
        }
      }
      return false;
  }
  function updateTransferOnInsertRecv_mint_r3(uint tokenId,address to) private   returns (bool) {
      address s = msg.sender;
      uint time = block.timestamp;
      if(s==owner.p) {
        if(false==exists[tokenId].b) {
          if(to!=address(0)) {
            emit Transfer(tokenId,address(0),to,time);
            uint _max = latestTransfer[tokenId].time;
            if(time>_max) {
              latestTransfer[tokenId] = LatestTransferTuple(address(0),to,time,true);
              address r0__ = latestTransfer[tokenId].from;
              address r0_p = latestTransfer[tokenId].to;
              uint r0__ = latestTransfer[tokenId].time;
              if(r0_p!=address(0)) {
                if(r0_p==ownerOf[tokenId].p) {
                  ownerOf[tokenId] = OwnerOfTuple(address(address(0)),false);
                }
                address r0_p = approval[r0_p][tokenId].p;
                bool b = approval[r0_p][tokenId].b;
                if(b==approved[tokenId][r0_p].b) {
                  approved[tokenId][r0_p] = ApprovedTuple(false,false);
                }
                balanceOf[r0_p].n -= 1;
              }
              if(to!=address(0)) {
                ownerOf[tokenId] = OwnerOfTuple(to,true);
                address r4_o = ownerOf[tokenId].p;
                address to = approval[r4_o][tokenId].p;
                bool b = approval[r4_o][tokenId].b;
                if(b==approved[tokenId][to].b) {
                  approved[tokenId][to] = ApprovedTuple(false,false);
                }
                address to = approval[to][tokenId].p;
                bool b = approval[to][tokenId].b;
                approved[tokenId][to] = ApprovedTuple(b,true);
                address r5_p = ownerOf[tokenId].p;
                balanceOf[r5_p].n -= 1;
                balanceOf[to].n += 1;
              }
              address r11__ = latestTransfer[tokenId].from;
              address r11_to = latestTransfer[tokenId].to;
              uint r11__ = latestTransfer[tokenId].time;
              if(r11_to!=address(0)) {
                if(true==exists[tokenId].b) {
                  exists[tokenId] = ExistsTuple(false,false);
                }
              }
              if(to!=address(0)) {
                exists[tokenId] = ExistsTuple(true,true);
              }
            }
            return true;
          }
        }
      }
      return false;
  }
  function updateTransferOnInsertRecv_burn_r12(uint tokenId) private   returns (bool) {
      address s = msg.sender;
      uint time = block.timestamp;
      if(s==owner.p) {
        if(true==exists[tokenId].b) {
          address p = ownerOf[tokenId].p;
          emit Transfer(tokenId,p,address(0),time);
          uint _max = latestTransfer[tokenId].time;
          if(time>_max) {
            latestTransfer[tokenId] = LatestTransferTuple(p,address(0),time,true);
            address r0__ = latestTransfer[tokenId].from;
            address r0_p = latestTransfer[tokenId].to;
            uint r0__ = latestTransfer[tokenId].time;
            if(r0_p!=address(0)) {
              if(r0_p==ownerOf[tokenId].p) {
                ownerOf[tokenId] = OwnerOfTuple(address(address(0)),false);
              }
              address r0_p = approval[r0_p][tokenId].p;
              bool b = approval[r0_p][tokenId].b;
              if(b==approved[tokenId][r0_p].b) {
                approved[tokenId][r0_p] = ApprovedTuple(false,false);
              }
              balanceOf[r0_p].n -= 1;
            }
            address r11__ = latestTransfer[tokenId].from;
            address r11_to = latestTransfer[tokenId].to;
            uint r11__ = latestTransfer[tokenId].time;
            if(r11_to!=address(0)) {
              if(true==exists[tokenId].b) {
                exists[tokenId] = ExistsTuple(false,false);
              }
            }
          }
          return true;
        }
      }
      return false;
  }
}