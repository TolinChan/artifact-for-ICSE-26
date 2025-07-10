// SPDX-License-Identifier: BUSL-1.1
pragma solidity 0.8.19;

interface IERC20 {
    function transfer(address to, uint256 amount) external returns (bool);
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
    function approve(address spender, uint256 amount) external returns (bool);
}

interface ILockupPlans {
    function createPlan(address recipient, address token, uint256 amount, uint256 start, uint256 cliff, uint256 rate, uint256 period) external;
}

interface IVestingPlans {
    function createPlan(address recipient, address token, uint256 amount, uint256 start, uint256 cliff, uint256 rate, uint256 period, address vestingAdmin, bool transferable) external;
}

contract ClaimCampaigns {
    address private donationCollector;

    enum TokenLockup { Unlocked, Locked, Vesting }

    struct ClaimLockup {
        address tokenLocker;
        uint256 start;
        uint256 cliff;
        uint256 period;
        uint256 periods;
    }

    struct Campaign {
        address manager;
        address token;
        uint256 amount;
        uint256 end;
        TokenLockup tokenLockup;
    }

    mapping(bytes16 => Campaign) public campaigns;
    mapping(bytes16 => ClaimLockup) public claimLockups;
    mapping(bytes16 => bool) public usedIds;

    event CampaignStarted(bytes16 indexed id, Campaign campaign);
    event ClaimLockupCreated(bytes16 indexed id, ClaimLockup claimLockup);
    event CampaignCancelled(bytes16 indexed id);
    event TokensDonated(bytes16 indexed id, address donationCollector, address token, uint256 amount, address tokenLocker);

    constructor(address _donationCollector) {
        donationCollector = _donationCollector;
    }

    function createUnlockedCampaign(
        bytes16 id,
        Campaign memory campaign,
        uint256 donationAmount
    ) external {
        require(!usedIds[id], 'in use');
        require(campaign.token != address(0), '0_address');
        require(campaign.manager != address(0), '0_manager');
        require(campaign.amount > 0, '0_amount');
        require(campaign.end > block.timestamp, 'end error');
        require(campaign.tokenLockup == TokenLockup.Unlocked, 'locked');

        usedIds[id] = true;
        IERC20(campaign.token).transferFrom(msg.sender, address(this), campaign.amount + donationAmount);

        if (donationAmount > 0) {
            IERC20(campaign.token).transfer(donationCollector, donationAmount);
            emit TokensDonated(id, donationCollector, campaign.token, donationAmount, address(0));
        }

        campaigns[id] = campaign;
        emit CampaignStarted(id, campaign);
    }

    function createLockedCampaign(
        bytes16 id,
        Campaign memory campaign,
        ClaimLockup memory claimLockup,
        uint256 donationAmount
    ) external {
        require(!usedIds[id], 'in use');
        require(campaign.token != address(0), '0_address');
        require(campaign.manager != address(0), '0_manager');
        require(campaign.amount > 0, '0_amount');
        require(campaign.end > block.timestamp, 'end error');
        require(campaign.tokenLockup != TokenLockup.Unlocked, '!locked');
        require(claimLockup.tokenLocker != address(0), 'invalid locker');

        usedIds[id] = true;
        IERC20(campaign.token).transferFrom(msg.sender, address(this), campaign.amount + donationAmount);

        if (donationAmount > 0) {
            IERC20(campaign.token).transfer(donationCollector, donationAmount);
            emit TokensDonated(id, donationCollector, campaign.token, donationAmount, claimLockup.tokenLocker);
        }

        claimLockups[id] = claimLockup;
        IERC20(campaign.token).approve(claimLockup.tokenLocker, campaign.amount);

        campaigns[id] = campaign;
        emit ClaimLockupCreated(id, claimLockup);
        emit CampaignStarted(id, campaign);
    }

    function cancelCampaign(bytes16 campaignId) external {
        Campaign memory campaign = campaigns[campaignId];
        require(campaign.manager == msg.sender, '!manager');
        delete campaigns[campaignId];
        delete claimLockups[campaignId];
        IERC20(campaign.token).transfer(msg.sender, campaign.amount);
        emit CampaignCancelled(campaignId);
    }

    function changeDonationcollector(address newCollector) external {
        require(msg.sender == donationCollector);
        donationCollector = newCollector;
    }
}
