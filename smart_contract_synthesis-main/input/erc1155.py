import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine
## state

statemachine = smart_contract_state_machine('erc1155')
totalSupply, totalSupplyOut = statemachine.add_state("totalSupply", z3.BitVecSort(256))
totalBalance, totalBalanceOut = statemachine.add_state("totalBalance", z3.BitVecSort(256))
balanceOf, balanceOfOut = statemachine.add_state("balanceOf", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
allowance, allowanceOut = statemachine.add_state("allowance", z3.ArraySort(z3.BitVecSort(256), z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256))))

statemachine.constants.append(0)
statemachine.constants.append(1155)

## transaction
now = statemachine.nowOut
account1 = z3.BitVec('account1',256)
amount1 = z3.BitVec('amount1',256)

statemachine.add_tr(
    tr_name = "mint",
    parameters = (account1, amount1),
    guard = True,
    transfer_func = z3.And(totalSupplyOut == totalSupply + amount1,
                           balanceOfOut == z3.Update(balanceOf,account1,balanceOf[account1]+amount1),
                           totalBalanceOut == totalBalance + amount1,
                        )
)

sender1 = z3.BitVec('sender1',256)
to = z3.BitVec('to',256)
value = z3.BitVec('value',256)

statemachine.add_tr(
    tr_name = "transfer",
    parameters = (to, value, sender1),
    guard = True,
    transfer_func = z3.And(balanceOfOut == z3.Update(z3.Update(balanceOf,to,balanceOf[to]+value),sender1,balanceOf[sender1]-value),
                            )
)

spender = z3.BitVec('spender',256)
sender2 = z3.BitVec('sender2',256)
value2 = z3.BitVec('value2',256)

statemachine.add_tr(
    tr_name = "approve",
    parameters = (spender, value2, sender2),
    guard = True,
    transfer_func = z3.And(allowanceOut == z3.Update(allowance,sender2,z3.Update(allowance[sender2],spender,value2)),
                        )
)

froM = z3.BitVec('from',256)
to2 = z3.BitVec('to2',256)
value3 = z3.BitVec('value3',256)
sender3 = z3.BitVec('sender3',256)

statemachine.add_tr(
    tr_name = "transferFrom",
    parameters = (froM, to2, value3),
    guard = True,
    transfer_func = z3.And(balanceOfOut == z3.Update(z3.Update(balanceOf,to2,balanceOf[to2]+value3),froM,balanceOf[froM]-value3),
                            allowanceOut == z3.Update(allowance,froM,z3.Update(allowance[froM],sender3,allowance[froM][sender3]-value3)),
                            )
)

sender4 = z3.BitVec('sender4',256)
value4 = z3.BitVec('value4',256)

statemachine.add_tr(
    tr_name = "burn",
    parameters = (sender4, value4),
    guard = True,
    transfer_func = z3.And(balanceOfOut == z3.Update(balanceOf,sender4,balanceOf[sender4]-value4),
                            totalSupplyOut == totalSupply - value4,
                            totalBalanceOut == totalBalance - value4,
                            )
)





statemachine.add_once()
p = z3.BitVec('p',256)
q = z3.BitVec('q',256)
statemachine.set_init(z3.And(
    totalBalance == 0,
    totalSupply == 0,
    z3.ForAll(p, balanceOf[p] == 0),
    z3.ForAll(p, z3.ForAll(q, allowance[p][q] == 0)),
    ))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []

r1 = z3.Implies(func('burn'), totalSupply < prev(totalSupply))
# r2 = z3.Implies(func('mint'), balanceOf[0] == 0)
r2 = z3.Implies(func('mint'), totalSupply > prev(totalSupply))

properties.append(r1)
properties.append(r2)
# properties.append(r3)

## traces
positive_traces = []
positive_traces.append(
    [
        ('transfer', now == 1, to == 0x114515, value == 20, sender1 == 0x114514),
        ('burn', now == 2, sender4 == 0x114514, value4 == 10),
        ('approve', now == 5, spender == 0x114518, value2 == 10, sender2 == 0x114514),
        ('transferFrom', now == 6, froM == 0x114514, to2 == 0x114515, value3 == 10, sender3 == 0x114518),
    ]
)


statemachine.cegis(properties, positive_traces, None)