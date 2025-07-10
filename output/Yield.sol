// SPDX-License-Identifier: BUSL-1.1
pragma solidity >=0.8.13;

interface IERC20 {
    function transfer(address to, uint256 amount) external returns (bool);
    function balanceOf(address account) external view returns (uint256);
}

interface IFYToken is IERC20 {
    function redeem(address to, uint256 amount) external returns (uint256);
    function underlying() external view returns (address);
}

interface IPool is IERC20 {
    function init(address to) external returns (uint256, uint256, uint256);
    function burn(address baseTo, address fyTokenTo, uint256 minBaseOut, uint256 minFYTokenOut) external returns (uint256, uint256, uint256);
    function base() external view returns (IERC20);
    function fyToken() external view returns (IFYToken);
    function maturity() external view returns (uint32);
}

contract Strategy {
    enum State {DEPLOYED, DIVESTED, INVESTED, EJECTED, DRAINED}

    State public state;
    IERC20 public base;
    IFYToken public fyToken;
    IPool public pool;
    uint256 public baseCached;
    uint256 public poolCached;
    uint256 public fyTokenCached;
    uint32 public maturity;

    modifier isState(State target) {
        require(target == state, "Incorrect state");
        _;
    }

    constructor(IFYToken fyToken_) {
        base = IERC20(fyToken_.underlying());
        fyToken = fyToken_;
        state = State.DEPLOYED;
    }

    function _transition(State target, IPool pool_) internal {
        if (target == State.INVESTED) {
            pool = pool_;
            fyToken = pool_.fyToken();
            maturity = pool_.maturity();
        } else {
            delete fyToken;
            delete maturity;
            delete pool;
        }
        state = target;
    }

    function invest(IPool pool_) external isState(State.DIVESTED) {
        uint256 amount = baseCached;
        delete baseCached;
        base.transfer(address(pool_), amount);
        (,, uint256 lpObtained) = pool_.init(address(this));
        poolCached = lpObtained;
        _transition(State.INVESTED, pool_);
    }

    function divest() external isState(State.INVESTED) {
        require(block.timestamp >= maturity, "Not mature");
        uint256 lpTokens = poolCached;
        delete poolCached;
        pool.transfer(address(pool), lpTokens);
        (, uint256 baseFromBurn, uint256 fyTokenFromBurn) = pool.burn(address(this), address(this), 0, 0);
        uint256 baseFromRedeem = fyToken.redeem(address(this), fyTokenFromBurn);
        baseCached = base.balanceOf(address(this));
        _transition(State.DIVESTED, pool);
    }

    function eject() external isState(State.INVESTED) {
        uint256 lpTokens = poolCached;
        delete poolCached;
        try pool.burn(address(this), address(this), 0, 0) returns (uint256 baseReceived, uint256 fyTokenReceived, ) {
            baseCached = baseReceived;
            fyTokenCached = fyTokenReceived;
            if (fyTokenReceived > 0) {
                _transition(State.EJECTED, pool);
            } else {
                _transition(State.DIVESTED, pool);
            }
        } catch {
            pool.transfer(msg.sender, lpTokens);
            _transition(State.DRAINED, pool);
        }
    }

    function buyFYToken(address fyTokenTo, address baseTo) external isState(State.EJECTED) {
        uint256 baseIn = base.balanceOf(address(this)) - baseCached;
        uint256 soldFYToken = baseIn > fyTokenCached ? fyTokenCached : baseIn;
        baseCached += soldFYToken;
        fyTokenCached -= soldFYToken;

        if (fyTokenCached == 0) {
            _transition(State.DIVESTED, pool);
        }

        fyToken.transfer(fyTokenTo, soldFYToken);
        if (soldFYToken < baseIn) {
            base.transfer(baseTo, baseIn - soldFYToken);
        }
    }

    function mint(address to) external isState(State.INVESTED) {
        uint256 deposit = pool.balanceOf(address(this)) - poolCached;
        poolCached += deposit;
        // mint逻辑简化：此处示意，不实现具体ERC20 mint
    }

    function burn(address to) external isState(State.INVESTED) {
        uint256 poolTokensObtained = poolCached; // 直接简化为全部取出
        pool.transfer(to, poolTokensObtained);
        poolCached -= poolTokensObtained;
        // burn逻辑简化：此处示意，不实现具体ERC20 burn
    }

    function restart() external isState(State.DRAINED) {
        baseCached = base.balanceOf(address(this));
        require(baseCached > 0, "No base");
        _transition(State.DIVESTED, pool);
    }
}
