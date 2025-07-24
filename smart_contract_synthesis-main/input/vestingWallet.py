import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine
## state

statemachine = smart_contract_state_machine('vestingWallet')
balanceOf, balanceOfOut = statemachine.add_state("balanceOf", z3.ArraySort(z3.BitVecSort(256), z3.IntSort()))
totalSupply, totalSupplyOut = statemachine.add_state("totalSupply", z3.IntSort())
totalBalance, totalBalanceOut = statemachine.add_state("totalBalance", z3.IntSort())


statemachine.constants.append(0)

## transaction
p = z3.BitVec('p',256)
now = statemachine.nowOut

account1 = z3.BitVec('account1',256)
amount1 = z3.Int('amount1')

statemachine.add_tr(
    tr_name = "mint",
    parameters = (account1, amount1),
    guard = True,
    transfer_func = z3.And(totalSupplyOut == totalSupply + amount1,
                           balanceOfOut == z3.Update(balanceOf,account1,balanceOf[account1]+amount1),
                           totalBalanceOut == totalBalance + amount1,
                        )
)

account2 = z3.BitVec('account2',256)
amount2 = z3.Int('amount2')

statemachine.add_tr(
    tr_name = "burn",
    parameters = (account2, amount2),
    guard = True,
    transfer_func = z3.And(totalSupplyOut == totalSupply - amount2,
                           balanceOfOut == z3.Update(balanceOf,account2,balanceOf[account2]-amount2),
                           totalBalanceOut == totalBalance - amount2,
                        )
)

froM = z3.BitVec('from',256)
to = z3.BitVec('to',256)
amount3 = z3.Int('amount3')

statemachine.add_tr(
    tr_name = "transfer",
    parameters = (froM, to, amount3),
    guard = True,
    transfer_func = z3.And(balanceOfOut == z3.Update(balanceOf,froM,balanceOf[froM]-amount3),
                            balanceOfOut == z3.Update(balanceOf,to,balanceOf[to]+amount3),
                            )
)


statemachine.add_once()

statemachine.set_init(z3.And(
    totalBalance == 0,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []

r1 = z3.Implies(func('mint'), totalSupply > prev(totalSupply))
# r2 = z3.Implies(func('mint'), balanceOf[0] == 0)
r2 = z3.Implies(func('burn'), totalSupply < prev(totalSupply))

properties.append(r1)
properties.append(r2)

## traces
positive_traces = []
positive_traces.append(
    [
        ('mint', now == 1, account1 == 0x114514, amount1 == 100),
        ('mint', now == 2, account1 == 0x114514, amount1 == 150),
        ('mint', now == 2, account1 == 1, amount1 == 70),
        ('burn', now == 3, account2 == 0x114514, amount2 == 20),
        ('burn', now == 4, account2 == 0x114514, amount2 == 10),
        ('mint', now == 5, account1 == 2, amount1 == 70),
        ('burn', now == 6, account2 == 2, amount2 == 20),
        ('transfer', now == 7, froM == 0x114516, to == 0x114517, amount3 == 50),
    ]
)


statemachine.cegis(properties, positive_traces, None)