import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine

# state
statemachine = smart_contract_state_machine('PaymentSplitter')
_shares, _sharesOut = statemachine.add_state("_shares", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
_released, _releasedOut = statemachine.add_state("_released", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
_totalShares, _totalSharesOut = statemachine.add_state("_totalShares", z3.BitVecSort(256))
_totalReleased, _totalReleasedOut = statemachine.add_state("_totalReleased", z3.BitVecSort(256))


## transaction
z3.BitVec('p',256)
now = statemachine.nowOut

account = z3.BitVec('account',256)
payment = z3.BitVec('payment',256)

statemachine.add_tr(
    tr_name = "release",
    parameters = (value, sender),
    guard = True
    transfer_func = z3.And(_totalReleasedOut == _totalReleased+payment,
                        )
)

statemachine.add_once()

statemachine.set_init(z3.And(
    z3.ForAll(p, _shares[p]==0),
    z3.ForAll(p, _released[p]==0),
    _totalShares == 0,
    _totalReleased == 0,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []


r3 = z3.Implies(func("release"), _totalReleased > prev(_totalReleased))

properties.append(r2)

## traces
positive_traces = []
positive_traces.append(
    [
        ('release', now == 1, account == 0x114514, payment == 100),
    ]
)
statemachine.cegis(properties, positive_traces, None)