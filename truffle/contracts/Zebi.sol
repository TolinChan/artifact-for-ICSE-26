contract ZebiMainCrowdsale {
    uint256 currentYearMinted;
    uint256 calenderYearMintCap;
    uint256 calenderYearStart;
    uint256 calenderYearEnd;
    uint256 vestedMintStartTime;
    uint256 zebiZCOShare;
    uint256 crowdsaleZCOCap;
    uint256 transStartTime;
    ZebiCoinCrowdsale public zcc;
    ZebiCoinTempMgr public tempMngr;
    uint64 public tokenDecimals;
    uint256 public startTime;
    uint256 public endTime;
    uint256 public goldListPeriod;
    uint256 public postGoldPeriod;
    uint256 public minTransAmount;
    uint256 public ethCap;
    mapping(address => uint256) mainContribution;
    mapping(address => bool) mainCancelledList;
    uint256 goldPeriodCap;
    bool goldListPeriodFlag;
    mapping(address=>uint256) goldListContribution;
    mapping(address => bool) goldList;
    mapping(address => bool) kycAcceptedList;
    address public wallet;
    bool public withinRefundPeriod;
    mapping(address => uint256) preSaleRefundsInMainSale;
    uint256 public tokens;
    uint256 public weiAmount;
    uint256 public ETHtoZWeirate;
    uint256 public mainWeiRaised;
    function ZebiMainCrowdsale(uint256 _startTime, uint256 _endTime, uint256 _ETHtoZWeirate, address _wallet,uint256 _minTransAmount,uint256 _ethCap, address tokenAddress, address presaleAddress,address tempMngrAddress,uint256 _goldListPeriod,uint256 _postGoldPeriod,uint256 _goldPeriodCap,uint256 _vestedMintStartTime,uint256 _calenderYearStart) public {
        require(_startTime >= now);
        require(_endTime >= _startTime);
        require(_ETHtoZWeirate > 0);
        require(_wallet != 0x0);
        token = ZebiCoin(tokenAddress);
        zcc = ZebiCoinCrowdsale(presaleAddress);
        startTime = _startTime;
        endTime = _endTime;
        ETHtoZWeirate = _ETHtoZWeirate;
        wallet = _wallet;
        minTransAmount = _minTransAmount;
        tokenDecimals = token.decimals();
        ethCap = _ethCap;
        tempMngr=ZebiCoinTempMgr(tempMngrAddress);
        goldListPeriod=_goldListPeriod;
        postGoldPeriod=_postGoldPeriod;
        zebiZCOShare=SafeMath.mul(500000000,(10**tokenDecimals));
        crowdsaleZCOCap=zebiZCOShare;
        goldPeriodCap=_goldPeriodCap;
        calenderYearMintCap = SafeMath.div((zebiZCOShare.mul(2)),8);
        vestedMintStartTime=_vestedMintStartTime;
        calenderYearStart=_calenderYearStart;
        calenderYearEnd=(calenderYearStart+1 years )- 1;
    }
    function viewCancelledList(address participant) public view returns (bool){
        return mainCancelledList[participant];
    }
    function viewGoldList(address participant) public view returns (bool){
        return goldList[participant];
    }
    function addToGoldList (address _participant) external returns (bool) {
        goldList[_participant] = true;
        return true;
    }
    function removeFromGoldList(address _participant) external returns (bool) {
        goldList[_participant]=false;
        return true;
    }
    function viewKYCAccepted(address participant) public view returns (bool) {
        return kycAcceptedList[participant];
    }
    function addToKYCList (address _participant) external returns (bool) {
        kycAcceptedList[_participant] = true;
        return true;
    }
    function removeFromKYCList (address _participant) external returns (bool){
        kycAcceptedList[_participant]=false;
    }
    function viewPreSaleRefundsInMainSale(address participant) public view returns(uint256){
        return preSaleRefundsInMainSale[participant];
    }
    function buyTokens(address beneficiary) public payable {
        transStartTime=now;
        require(goldList[beneficiary] || kycAcceptedList[beneficiary]);
        goldListPeriodFlag=false;
        uint256 extraEth=0;
        weiAmount = msg.value;
        if((msg.value>ethCap.sub(mainWeiRaised)) && !goldListPeriodFlag){
            weiAmount=ethCap.sub(mainWeiRaised);
            extraEth=(msg.value).sub(weiAmount);
        }
        tokens = getTokenAmount(weiAmount);
        mainWeiRaised = mainWeiRaised.add(weiAmount);
        token.mint(beneficiary, tokens);
        mainContribution[beneficiary] = mainContribution[beneficiary].add(weiAmount);
        if(goldListPeriodFlag){
            goldListContribution[beneficiary] = goldListContribution[beneficiary].add(weiAmount);
        }
        if(extraEth>0){
            beneficiary.transfer(extraEth);
        }
    }
    function getTokenAmount(uint256 weiAmount1) public view returns(uint256) {

        uint256 number = SafeMath.div((weiAmount1.mul(ETHtoZWeirate)),(1 ether));
        uint256 volumeBonus;
        uint256 timeBonus;
        if(number >= 400000000000000)
        {
            volumeBonus = SafeMath.div((number.mul(25)),100);
        }
        else if(number>= 150000000000000) {
            volumeBonus = SafeMath.div((number.mul(20)),100);
        }
        else if(number>= 80000000000000) {
            volumeBonus = SafeMath.div((number.mul(15)),100);
        }
        else if(number>= 40000000000000) {
            volumeBonus = SafeMath.div((number.mul(10)),100);
        }
        else if(number>= 7500000000000) {
            volumeBonus = SafeMath.div((number.mul(5)),100);
        }
        else{
            volumeBonus=0;
        }
        if(goldListPeriodFlag){
            timeBonus = SafeMath.div((number.mul(15)),100);
        }
        else if(transStartTime <= startTime + postGoldPeriod){
            timeBonus = SafeMath.div((number.mul(10)),100);
        }
        else{
            timeBonus=0;
        }
        number=number+timeBonus+volumeBonus;
        return number;
    }
    function enableRefundPeriod() external onlyOwner{
        withinRefundPeriod = true;
    }
    function disableRefundPeriod() external onlyOwner{
        withinRefundPeriod = false;
    }
    function viewContribution(address participant) public view returns(uint256){
        return mainContribution[participant];
    }
    function validPurchase() internal view returns (bool) {
        bool withinPeriod = transStartTime >= startTime && transStartTime <= endTime;
        bool validAmount = msg.value >= minTransAmount;
        bool withinEthCap = ((ethCap.sub(mainWeiRaised))>0);
        bool goldPeriodValid=true;
        if(transStartTime <= (startTime + goldListPeriod)){
            goldPeriodValid=(goldList[msg.sender])&&(goldListContribution[msg.sender]+msg.value <= goldPeriodCap);
            goldListPeriodFlag=true;
        }
        return withinPeriod && validAmount && withinEthCap && goldPeriodValid;
    }
    function mintvestedTokens (address partnerAddress,uint256 zweitokens) external returns(bool){
        require(zweitokens <= zebiZCOShare && zweitokens > 0);
        require(partnerAddress != 0x0);
        require(now >= vestedMintStartTime);
        uint256 currentYearCounter = (now - calenderYearStart) / (1 years);
        if(now > calenderYearEnd && currentYearCounter >= 1){
            currentYearMinted = 0;
            calenderYearStart = calenderYearEnd+((currentYearCounter-1) * 1 years) + 1;
            calenderYearEnd = (calenderYearStart + 1 years ) - 1;
        }
        currentYearMinted = currentYearMinted+zweitokens;
        zebiZCOShare = zebiZCOShare - zweitokens;
    }
    function refund() external {
        require(withinRefundPeriod);
        require(mainCancelledList[msg.sender]);
        require((mainContribution[msg.sender] > 0) && token.balanceOf(msg.sender)>0);
        mainContribution[msg.sender] = 0;
        msg.sender.transfer(refundBalance);
    }
    function forcedRefund(address _from) external {
        require(mainCancelledList[_from]);
        require(mainContribution[_from] > 0);
        require(token.balanceOf(_from) > 0);
        uint256 presaleContribution = zcc.viewContribution(_from);
        uint256 refundBalance = mainContribution[_from] + presaleContribution;
        uint256 preSaleRefundTemp = tempMngr.viewPreSaleRefunds(_from);
        uint256 preSaleRefundMain = presaleContribution - preSaleRefundTemp;
        refundBalance = refundBalance - preSaleRefundTemp;
        refundBalance = refundBalance - preSaleRefundsInMainSale[_from];
        preSaleRefundsInMainSale[_from] = preSaleRefundMain;
        mainContribution[_from] = 0;
    }
}
