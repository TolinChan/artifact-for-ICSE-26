import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine
## state

statemachine = smart_contract_state_machine('melon')
BASE_UNITS = 10 ** 18
NFLATION_ENABLE_DATE = 1551398400
NITIAL_TOTAL_SUPPLY = 932613 * BASE_UNITS
EARLY_MINTABLE_AMOUNT = 300600 * BASE_UNITS

statemachine.constants.append(BASE_UNITS)
statemachine.constants.append(NFLATION_ENABLE_DATE)
statemachine.constants.append(NITIAL_TOTAL_SUPPLY)
statemachine.constants.append(EARLY_MINTABLE_AMOUNT)

_balances, _balancesOut = statemachine.add_state("_balances", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
_totalSupply, _totalSupplyOut = statemachine.add_state("_totalSupply", z3.BitVecSort(256))
council, councileOut = statemachine.add_state("council", z3.BitVecSort(256))
deployer, deployerOut = statemachine.add_state("deployer", z3.BitVecSort(256))
nextMinting, nextMintingOut = statemachine.add_state("nextMinting", z3.BitVecSort(256))
initialSupplyMinted, initialSupplyMintedOIut = statemachine.add_state("endRate", z3.BoolSort())
## transaction
now = statemachine.nowOut
# changeCouncil
_newCouncil = z3.BitVec('rate',256)
statemachine.add_tr(
    tr_name = "changeCouncil",
    parameters = (_newCouncil,),
    guard = True,
    transfer_func = z3.And(councileOut == _newCouncil,
    )
)
# setInitialRate
_initialReceiver = z3.BitVec('_initialReceiver',256)
statemachine.add_tr(
    tr_name = "mintInitialSupply",
    parameters = (rate,),
    guard = True,
    transfer_func = z3.And(_totalSupplyOut == _totalSupply + INITIAL_TOTAL_SUPPLY,
                    _balancesOut == z3.Update(_balances,_initialReceiver,_balances[_initialReceiver]+INITIAL_TOTAL_SUPPLY)
    )
)
# mintInflation

statemachine.add_once()
p = z3.BitVec('p',256)
statemachine.set_init(z3.And(
    z3.ForAll(p, _balances[p]==0),
    council == 0,
    deployer == 0,
    initialSupplyMinted == 0,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []
r1 = z3.Implies(func("changeCouncil"), prev(councile) != councile)


properties.append(r1)

## traces
positive_traces = []
positive_traces.append(
    [
        ('changeCouncil', now == 1, _newCouncil == 0x114514),
    ]
)

statemachine.cegis(properties, positive_traces, None)