import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine

# state
statemachine = smart_contract_state_machine('MRV')
state, stateOut = statemachine.add_state("state", z3.BitVecSort(2))
deposits, depositsOut = statemachine.add_state("deposits", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
totalDeposits, totalDepositsOut = statemachine.add_state("totalDeposits", z3.BitVecSort(256))
raised, raisedOut = statemachine.add_state("raised", z3.BitVecSort(256))

GOAL = 10000
CLOSETIME = 998877


OPEN = 0
SUCCESS = 1
REFUND = 2

statemachine.constants.append(GOAL)
statemachine.constants.append(CLOSETIME)
statemachine.constants.append(OPEN)
statemachine.constants.append(SUCCESS)
statemachine.constants.append(REFUND)

## transaction
z3.BitVec('p',256)
now = statemachine.nowOut

sender = z3.BitVec('sender',256)
value = z3.BitVec('amount',256)

statemachine.add_tr(
    tr_name = "invest",
    parameters = (value, sender),
    guard = True,
    transfer_func = z3.And(raisedOut == raised + value,
                           depositsOut == z3.Update(deposits,sender,deposits[sender]+value),
                           totalDepositsOut == totalDeposits+value,
                        )
)

statemachine.add_tr(
    tr_name = "close_success",
    parameters = (),
    guard = True,
    transfer_func = z3.And(
                           stateOut == SUCCESS,
                        )
)

statemachine.add_tr(
    tr_name = "close_refund",
    parameters = (),
    guard = True,
    transfer_func = z3.And(stateOut == REFUND,
                        )
)

p = z3.BitVec('p',256)
statemachine.add_tr(
    tr_name = "claimrefund",
    parameters = (p, ),
    guard = True,
    transfer_func = z3.And(depositsOut == z3.Update(deposits,p,0),
                           totalDepositsOut == totalDeposits - deposits[p],
                        )
)


statemachine.add_tr(
    tr_name = "withdraw",
    parameters = (),
    guard = True,
    transfer_func = z3.And(totalDepositsOut == 0,
                        )
)

statemachine.add_once()

statemachine.set_init(z3.And(
    z3.ForAll(p, deposits[p]==0),
    raised == 0,
    totalDeposits == 0,
    state == OPEN,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []

r2 = z3.Not(z3.And(once('withdraw'), once('claimrefund')))

r3 = z3.Implies(prev(raised) >= GOAL, z3.Not(func("invest")))

properties.append(r2)
properties.append(r3)

## traces
positive_traces = []

positive_traces.append(
    [
        ('invest', now == 1, sender == 0x114514, value == 100),
        ('invest', now == 2, sender == 0x114515, value == GOAL),
        ('close_success', now == 3),
        ('withdraw', now == 4)
    ]
)
positive_traces.append(
    [
        ('invest', now == 1, sender == 0x114514, value == 100),
        ('invest', now == 2, sender == 0x114515, value == GOAL),
        ('close_success', now == 11200),
        ('withdraw', now == 11234)
    ]
)

positive_traces.append(
    [
        ('invest', now == 1, sender == 0x114514, value == 100),
        ('invest', now == 2, sender == 0x114515, value == GOAL),
        ('close_success', now == CLOSETIME + 1),
        ('withdraw', now == CLOSETIME + 2)
    ]
)

positive_traces.append(
    [
        ('invest', now == 1, sender == 0x114514, value == 100),
        ('invest', now == 2, sender == 0x114515, value == 100),
        ('close_refund', now == CLOSETIME + 1),
        ('claimrefund', now == CLOSETIME + 2, p == 0x114514)
    ]
)

positive_traces.append(
    [
        ('invest', now == 1, sender == 0x114514, value == 100),
        ('invest', now == 2, sender == 0x114515, value == GOAL),
        ('close_success', now == 3),
    ]
)

statemachine.cegis(properties, positive_traces, None)