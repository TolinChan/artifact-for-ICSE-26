import z3
import sys
sys.path.insert(1, './')
from lib.state_machine import smart_contract_state_machine
## state

statemachine = smart_contract_state_machine('voting')

QUORUM = 10
votes, votesOut = statemachine.add_state("votes", z3.ArraySort(z3.BitVecSort(256), z3.BitVecSort(256)))
isVoter, isVoterOut = statemachine.add_state("isVoter", z3.ArraySort(z3.BitVecSort(256), z3.BoolSort()))
hasVoted, hasVotedOut = statemachine.add_state("hasVoted", z3.ArraySort(z3.BitVecSort(256), z3.BoolSort()))
wins, winsOut = statemachine.add_state("wins", z3.ArraySort(z3.BitVecSort(256), z3.BoolSort()))
hasVinner, hasVinnerOut = statemachine.add_state("hasVinner", z3.BoolSort())
winner, winnerOut = statemachine.add_state("winner", z3.BitVecSort(256))
## transaction
now = statemachine.nowOut
sender = z3.BitVec('sender',256)
proposal = z3.BitVec('proposal',256)

statemachine.add_tr(
    tr_name = "vote",
    parameters = (proposal, sender),
    guard = True,
    transfer_func = z3.And(hasVinnerOut == True,
                            hasVotedOut == z3.Update(hasVoted,sender,True),
                            winnerOut == proposal,
                            )
)

statemachine.add_once()
p = z3.BitVec('p',256)
statemachine.set_init(z3.And(
    z3.ForAll(p, votes[p]==0),
    z3.ForAll(p, isVoter[p]==True),
    z3.ForAll(p, hasVoted[p]==False),
    z3.ForAll(p, wins[p]==False),
    hasVinner == False,
))

## properties
def prev(state):
    return statemachine.prev(state)[0]

def func(name):
    return statemachine.func == name

def once(funcname):
    return statemachine.once[funcname][0]

properties = []
r1 = z3.Implies(func("vote"), prev(hasVinner) == False)

properties.append(r1)

## traces
positive_traces = []
positive_traces.append(
    [
        ('vote', now == 1, proposal == 0x114514, sender == 0x114514),
    ]
)


statemachine.cegis(properties, positive_traces, None)