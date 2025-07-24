import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine
## state

statemachine = smart_contract_state_machine('auction')
ENDTIME = 998877
statemachine.constants.append(ENDTIME)
highestbid, highestbidOut = statemachine.add_state("highestbid", z3.BitVecSort(256))
highestbidder, highestbidderOut = statemachine.add_state("highestbidder", z3.BitVecSort(256))
pendingReturns, pendingReturnsOut = statemachine.add_state("pendingReturns", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
ended, endedOut = statemachine.add_state("ended", z3.BoolSort())
withdrawCount, withdrawCountOut = statemachine.add_state("withdrawCount", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))

## transaction
now = statemachine.nowOut
sender1 = z3.BitVec('sender1',256)
value1 = z3.BitVec('value1',256)
statemachine.add_tr(
    tr_name = "bid",
    parameters = (sender1, value1),
    guard = True,
    transfer_func = z3.And(pendingReturnsOut == z3.Update(pendingReturns,highestbidder,pendingReturns[highestbidder]+highestbid), 
                   highestbidderOut == sender1,
                     highestbidOut == value1,
                   )
)

sender2 = z3.BitVec('sender2',256)
statemachine.add_tr(
    tr_name = "withdraw",
    parameters = (sender2,),
    guard = True,
    transfer_func = z3.And(pendingReturnsOut == z3.Update(pendingReturns,sender2,0), 
                   withdrawCountOut == z3.Update(withdrawCount,sender2,withdrawCount[sender2]+1),
                   )
)

statemachine.add_tr(
    tr_name = "end",
    parameters = (),
    guard = True,
    transfer_func = z3.And(endedOut == True,
    )
)

statemachine.add_once()
p = z3.BitVec('p',256)
statemachine.set_init(z3.And(
    z3.ForAll(p, pendingReturns[p]==0),
    z3.ForAll(p, withdrawCount[p]==0),
    highestbid == 0,
    highestbidder == 0,
    ended == False,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []
r1 = z3.Implies(func("end"), prev(ended) == False)
r3 = z3.Implies(func("bid"), highestbid > prev(highestbid))
r2 = z3.Implies(func("bid"), now < ENDTIME)
r4 = z3.Implies(func("end"), now > ENDTIME)

properties.append(r1)
properties.append(r2)
properties.append(r3)
properties.append(r4)

## traces
positive_traces = []
positive_traces.append(
    [
        ('bid', now == 1, sender1 == 0x114514, value1 == 100),
        ('bid', now == 2, sender1 == 0x114514, value1 == 120),
        ('bid', now == 3, sender1 == 0x114514, value1 == 140),
        ('bid', now == 4, sender1 == 0x114514, value1 == 160),
        ('withdraw', now == 5, sender2 == 0x114514),
        ('end', now == 999988,),
        
    ]
)


statemachine.cegis(properties, positive_traces, None)