import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine
## state

statemachine = smart_contract_state_machine('Mana')
TOTAL_SHARE = 100
CROWDSALE_SHARE = 40
FOUNDATION_SHARE = 60
statemachine.constants.append(ENDTIME)
statemachine.constants.append(TOTAL_SHARE)
statemachine.constants.append(CROWDSALE_SHARE)
statemachine.constants.append(FOUNDATION_SHARE)



preferentialRate, preferentialRateOut = statemachine.add_state("preferentialRate", z3.BitVecSort(256))
initialRate, initialRateOut = statemachine.add_state("initialRate", z3.BitVecSort(256))
buyerRate, buyerRateOut = statemachine.add_state("buyerRate", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
endRate, endRateOut = statemachine.add_state("endRate", z3.BitVecSort(256))
## transaction
now = statemachine.nowOut
buyer = z3.BitVec('buyer',256)
rate = z3.BitVec('rate',256)
statemachine.add_tr(
    tr_name = "setBuyerRate",
    parameters = (buyer, rate),
    guard = True,
    transfer_func = z3.And(buyerRateOut == z3.Update(buyerRate,buyer,rate),
    )
)
# setInitialRate
rate = z3.BitVec('rate',256)
statemachine.add_tr(
    tr_name = "setInitialRate",
    parameters = (rate,),
    guard = True,
    transfer_func = z3.And(initialRateOut == rate,
    )
)
# setEndRate
rate = z3.BitVec('rate',256)
statemachine.add_tr(
    tr_name = "setEndRate",
    parameters = (rate,),
    guard = True,
    transfer_func = z3.And(endRateOut == rate,
    )
)
# buytokens
buyer = z3.BitVec('buyer',256)
value = z3.BitVec('value',256)
statemachine.add_tr(
    tr_name = "buytokens",
    parameters = (buyer, value),
    guard = True,
    transfer_func = z3.And(buyerRate[buyer] > 0,
    )
)

statemachine.add_once()
p = z3.BitVec('p',256)
statemachine.set_init(z3.And(
    z3.ForAll(p, buyerRate[p]==0),
    initialRate == 0,
    endRate == 0,
    preferentialRate == 0,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []
r1 = z3.Implies(func("setBuyerRate"), prev(buyerRate) != buyerRate)
r2 = z3.Implies(func("setInitialRate"), prev(initialRate) != initialRate)
r3 = z3.Implies(func("setEndRate"), prev(endRate) != endRate)
r4 = z3.Implies(func("buytokens"), buyerRate[statemachine.sender] > 0)


properties.append(r1)
properties.append(r2)
properties.append(r3)
properties.append(r4)

## traces
positive_traces = []
positive_traces.append(
    [
        ('setBuyerRate', now == 1, buyer == 0x114514, rate == 100),
        ('setBuyerRate', now == 2, buyer == 0x1919810, rate == 120),
        ('setBuyerRate', now == 3, buyer == 0x1919810, rate == 140),
        ('setBuyerRate', now == 4, buyer == 0x1919810, rate == 160),
        ('setInitialRate', now == 5, rate == 180),
        ('setEndRate', now == 6, rate == 200),
        ('buytokens', now == 7, buyer == 0x114514, value == 100),
        ('buytokens', now == 8, buyer == 0x1919810, value == 120),
        ('buytokens', now == 9, buyer == 0x1919810, value == 140),
        ('buytokens', now == 10, buyer == 0x1919810, value == 160),
        ('buytokens', now == 11, buyer == 0x1919810, value == 180),
        ('buytokens', now == 12, buyer == 0x1919810, value == 200),
    ]
)

statemachine.cegis(properties, positive_traces, None)