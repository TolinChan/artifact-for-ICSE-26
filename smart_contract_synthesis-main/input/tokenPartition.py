import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine
## state

statemachine = smart_contract_state_machine('tokenpartition')
statemachine.constants.append(0)
balanceOfByPartition, balanceOfByPartitionOut = statemachine.add_state("balanceOfByPartition", z3.ArraySort(z3.BitVecSort(256), z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256))))
totalSupplyByPartition, totalSupplyByPartitionOut = statemachine.add_state("totalSupplyByPartition", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
totalBalanceByPartition, totalBalanceByPartitionOut = statemachine.add_state("totalBalanceByPartition", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
caller, callerOut = statemachine.add_state("caller", z3.BitVecSort(256))
## transaction
now = statemachine.nowOut
account1 = z3.BitVec('account1',256)
partition1 = z3.BitVec('partition1',256)
amount1 = z3.BitVec('amount1',256)

statemachine.add_tr(
    tr_name = "issueByPartition",
    parameters = (partition1, amount1, account1),
    guard = True,
    transfer_func = z3.And(totalSupplyByPartitionOut == z3.Update(totalSupplyByPartition,partition1,totalSupplyByPartition[partition1]+amount1),
                            totalBalanceByPartitionOut == z3.Update(totalBalanceByPartition,partition1,totalBalanceByPartition[partition1]+amount1),
                            callerOut == account1,
                         )
)

account2 = z3.BitVec('account2',256)
partition2 = z3.BitVec('partition2',256)
amount2 = z3.BitVec('amount2',256)

statemachine.add_tr(
    tr_name = "redeemByPartition",
    parameters = (partition2, amount2, account2),
    guard = True,
    transfer_func = z3.And(totalSupplyByPartitionOut == z3.Update(totalSupplyByPartition,partition2,totalSupplyByPartition[partition2]-amount2),
                            totalBalanceByPartitionOut == z3.Update(totalBalanceByPartition,partition2,totalBalanceByPartition[partition2]-amount2),
                            callerOut == account2,
                         )
)

froM = z3.BitVec('froM',256)
to = z3.BitVec('to',256)
partition3 = z3.BitVec('partition3',256)
amount3 = z3.BitVec('amount3',256)

statemachine.add_tr(
    tr_name = "transferByPartition",
    parameters = (partition3, froM, to, amount3),
    guard = True,
    transfer_func = z3.And(caller == froM,
                         )
)

statemachine.add_once()
p = z3.BitVec('p',256)
q = z3.BitVec('q',256)
statemachine.set_init(z3.And(
    z3.ForAll(p, totalSupplyByPartition[p]==0),
    z3.ForAll(p, totalBalanceByPartition[p]==0),
    z3.ForAll(p, z3.ForAll(q, balanceOfByPartition[p][q]==0)),
    caller == 0x114514,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []
r1 = z3.Implies(func("issueByPartition"), caller!=0)
r2 = z3.Implies(func("redeemByPartition"), caller!=0)

properties.append(r1)
properties.append(r2)

## traces
positive_traces = []
positive_traces.append(
    [
        ('issueByPartition', now == 1, partition1 == 0, amount1 == 100, account1 == 0x114514),
        ('issueByPartition', now == 2, partition1 == 10, amount1 == 5, account1 == 5),
        ('issueByPartition', now == 3, partition1 == 1, amount1 == 100, account1 == 0x114514),
    ]
)


statemachine.cegis(properties, positive_traces, None)