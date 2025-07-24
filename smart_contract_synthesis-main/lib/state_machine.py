from lib.bmc import extract_model
from lib.ts import Ts
import z3
import itertools
import time

def contains(x, e):
    return x.__repr__() == e.__repr__() or any([contains(x, c) for c in e.children()])


class smart_contract_state_machine:
    def __init__(self, name):
        self.name = name
        self.states = {}
        self.prev_states = {}
        self.once = {}
        self.transitions = []
        self.condition_guards = {}
        self.candidate_condition_guards = {}
        self.tr_parameters = {}
        self.transfer_func = {}
        self.constants = []
        self.ts = Ts(name)
        self.now_state = None
        self.now, self.nowOut = self.add_state('now', z3.BitVecSort(256))
        self.func, self.funcOut = self.add_state('func', z3.StringSort())

        self.tracetable = {}

    def add_state(self, state_name, type):
        state, stateOut = self.ts.add_var(type, name = state_name)
        prev_state, prev_stateOut = self.ts.add_var(type, name = "prev_" + state_name)
        if state_name != 'func' and state_name[:5] != "once_" and state_name != 'now':
            self.states[state_name] = (state, stateOut)
            self.prev_states[state_name] = (prev_state, prev_stateOut)
        return state, stateOut

    def prev(self, state):
        return self.prev_states[state.__str__()]

    def add_tr(self, tr_name, parameters, guard, transfer_func):
        self.transitions.append(tr_name)
        self.once[tr_name] = self.add_state("once_"+tr_name, z3.BoolSort())
        self.tr_parameters[tr_name] = parameters
        self.condition_guards[tr_name] = guard
        self.candidate_condition_guards[tr_name] = []
        transfer_func = z3.And(transfer_func, self.funcOut == tr_name, self.once[tr_name][1] == True)
        for state in self.states:
            if state == 'now' or state == 'func':
                continue
            transfer_func = z3.simplify(z3.And(transfer_func, self.prev(self.states[state][0])[1] == self.states[state][0]))
            if not contains(self.states[state][1], transfer_func):
                transfer_func = z3.simplify(z3.And(transfer_func, self.states[state][1] == self.states[state][0]))
        # print(transfer_func)
        self.transfer_func[tr_name] = transfer_func

    def add_once(self):
        for tr in self.transitions:
            for once in self.once:
                if once != tr:
                    # print(once, self.once[once][0])
                    self.transfer_func[tr] = z3.And(self.transfer_func[tr], self.once[once][1] == self.once[once][0])

    def clear_guards(self):
        for i in self.condition_guards.keys():
            self.condition_guards[i] = z3.BoolVal(True)

    def change_guard(self, tr_name, *new_guard):
        if tr_name not in self.transitions:
            print("Transition not found!")
            return False
        else:
            self.condition_guards[tr_name] = z3.simplify(z3.And(*new_guard))
            return True
        
    def add_guard(self, tr_name, *new_guard):
        if tr_name not in self.transitions:
            print("Transition not found!")
            return False
        else:
            self.condition_guards[tr_name] = z3.simplify(z3.And(self.condition_guards[tr_name], *new_guard))
            return True

    def set_init(self, init_state):
        self.ts.Init = z3.And(init_state, self.now == 0, self.func == 'init')
        for once in self.once.values():
            self.ts.Init = z3.simplify(z3.And(self.ts.Init, once[0] == False))

    def transfer(self, tr_name, candidates, next, *parameters):
        success = z3.And(self.now_state, self.condition_guards[tr_name], self.nowOut > self.now, z3.And(*parameters))
        # print(success)
        s = z3.Solver()
        s.add(success)
        result = s.check()
        if result == z3.unsat:
            return None
        else:
            s = z3.Solver()
            s.add(z3.And(self.now_state, self.transfer_func[tr_name], z3.And(*parameters)))
            # print(z3.And(self.now_state, self.transfer_func[tr_name], z3.And(*parameters)))
            result = s.check()
            m = s.model()
            self.now_state = z3.BoolVal(True)
            for v in self.states.values():
                self.now_state = z3.And(self.now_state, v[0] == m[v[1]])
            self.now_state = z3.simplify(self.now_state)
            s = z3.Solver()
            # print(tr_name)
            s.add(self.now_state)
            s.add(next[1:])
            result = s.check()
            # print(result, tr_name)
            if result == z3.sat:
                m = s.model()
                # print(m)
                newline = []
                for c in candidates[next[0]]:
                    # print(c, m.eval(c))
                    # if m.eval(c).__str__() != 'True' and m.eval(c).__str__() != 'False':
                    #     print(c, m.eval(c))
                    # if c.__str__() == "highestbid < value1":
                    #     print(c, m, m.eval(c))
                    newline.append(m.eval(c))
            else:
                print("error")
            return newline
    
    def simulate(self, trace, candidates):
        res = []
        self.now_state = self.ts.Init
        s = z3.Solver()
        # print(trace)
        s.add(self.now_state)
        s.add(trace[0][1:])
        # print(self.now_state, trace[0][1:])
        result = s.check()
        if result == z3.sat:
            m = s.model()
            newline = []
            for c in candidates[trace[0][0]]:
                # if c.__str__() == "now' < 998877":
                #     print(c, m, m.eval(c))
                newline.append(m.eval(c))
        
        i = 0
        for tr_name, *parameters in trace:
            # print(tr_name, newline)
            newline = [tr_name] + newline
            res.append(newline)
            if i == len(trace) - 1:
                break
            newline = self.transfer(tr_name, candidates, trace[i+1], parameters)
            i += 1
        return res
        
    def bmc(self, property):
        import lib.bmc
        lib.bmc.index = 0
        self.ts.Tr = z3.BoolVal(False)
        for tr in self.transitions:
            self.ts.Tr = z3.simplify(z3.Or(self.ts.Tr, z3.And(self.transfer_func[tr], self.condition_guards[tr], self.nowOut > self.now)))
        xs = [v[0] for v in self.states.values()] + [v[0] for v in self.prev_states.values()] + [v[0] for v in self.once.values()] + [self.func] + [self.now]
        xns = [v[1] for v in self.states.values()] + [v[1] for v in self.prev_states.values()] + [v[1] for v in self.once.values()] + [self.funcOut] + [self.nowOut]
        fvs = []
        for p in self.tr_parameters.values():
            if p != None:
                for v in p:
                    fvs.append(v)
        # print(fvs)
        # print(self.ts.Init)
        # print(self.ts.Tr)
        # print(property)
        model = lib.bmc.bmc(self.ts.Init, self.ts.Tr, property, fvs, xs, xns)
        if model != None:
            # print(model)
            rd = extract_model(model,'func')
            # print(rd)
            trace = []
            for i in range(1, len(rd)-2):
                # print(rd[i])
                tr = rd[i]['func'].__str__()[1:-1]
                rule = [tr, self.nowOut == rd[i]['now']]
                # print(tr)
                if self.tr_parameters[tr] != None:
                    for j in self.tr_parameters[tr]:
                        if j.__str__() in rd[i].keys():
                            rule.append(j == rd[i][j.__str__()])
                        else:
                            # print("Error: parameter not found!", i, j.__str__())
                            # print(j, type(rd[i-1][j.__str__()]))
                            rule.append(j == rd[i-1][j.__str__()])
                trace.append(tuple(rule))
            return trace
        else:
            # print("No model found!")
            return None
        

    def generate_candidate_guards(self, predicates, array):
        candidate_guards = {}
        for tr in self.transitions:
            candidate_guards[tr] = []
            s = self.constants + [v[0] for v in self.states.values()] + list(self.tr_parameters[tr]) + [self.nowOut]
            if array:
                array_enum = []
                for xs in s:
                    if z3.is_array(xs):
                        for index in s:
                            if (z3.is_bv(index) or z3.is_int(index)) and index.__str__() != "now'":
                                # print(xs, index)
                                try:
                                    array_enum.append(xs[index])
                                except:
                                    # print("Error")
                                    pass
                        
                s += array_enum
            # print(tr, s)
            for ls in range(len(s)):
                # print(ls, z3.is_bool(ls))
                if z3.is_bool(s[ls]):
                    # print("jsdrfghdjksfhguidfghikdus",ls)
                    candidate_guards[tr].append(s[ls])
                    candidate_guards[tr].append(z3.Not(s[ls]))
                for rs in range(ls + 1, len(s)):
                    if isinstance(s[ls], (int, float)) and isinstance(s[rs], (int, float)):
                        continue
                    elif z3.is_array(s[ls]) or z3.is_array(s[rs]):
                        continue
                    elif z3.is_bool(s[rs]):
                        continue
                    else:
                        for predicate in predicates:
                            # print(s[ls], s[rs])
                            try:
                                if predicate == "<":
                                    g = s[ls] < s[rs]
                                elif predicate == "<=":
                                    g = s[ls] <= s[rs]
                                elif predicate == ">":
                                    g = s[ls] > s[rs]
                                elif predicate == ">=":
                                    g = s[ls] >= s[rs]
                                elif predicate == "=":
                                    g = s[ls] == s[rs]
                                else:
                                    print("predicate not supportted")
                                # if(s[rs].__str__() == "now'" and s[ls].__str__() == "998877"):
                                #     print(tr, g)
                                candidate_guards[tr].append(g)
                            except:
                                # print("unmatch")
                                pass
        
        self.clear_guards()
        maxlen = 0
        for tr in self.candidate_condition_guards:
            self.candidate_condition_guards[tr] = []
            for i in range(len(candidate_guards[tr])):
                self.candidate_condition_guards[tr].append(z3.Const(tr+'_'+str(i), z3.BoolSort()))
        # for tr in self.transitions:
        #     if len(candidate_guards[tr]) > maxlen:
        #         maxlen = len(candidate_guards[tr])
        #     pass
        # print(maxlen)
        return candidate_guards
    
    # def synthesize_one_guard(self, negative_trace, positive_traces):
    #     self.clear_guards()
    #     result_guard = []
    #     for tr in self.transitions:
    #         for g in self.candidate_condition_guards[tr]:
    #             self.add_guard(tr, g)
    #             # print(tr, g)
    #             # print(statemachine.condition_guards)
    #             if self.simulate(negative_trace, show_log=False) == 'reject':
    #                 # all_accept = True
    #                 # for ptrace in positive_traces:
    #                 #     if self.simulate(ptrace, show_log=False) == 'reject':
    #                 #         all_accept = False
    #                 #         break
    #                 # if all_accept:
    #                 #     result_guard.append([tr, g])
    #                 result_guard.append([tr, g])
    #             self.clear_guards()
    #     return result_guard
    
    # def synthesize_old(self, properties, positive_traces, simulate_before_bmc = True):
    #     possible_guards = [[]]
    #     iter = 0
    #     sum_verify_time = 0
    #     sum_synthesize_time = 0
    #     while iter < 100:
    #         print("iter", iter)
    #         iter += 1
    #         best_guard = None
    #         max_verified_num = -1
    #         negative_traces = None
    #         print("verifying......")
    #         T1 = time.time()
    #         all_ntraces = []
    #         for guards in possible_guards:
    #             self.clear_guards()
    #             for g in guards:
    #                 self.add_guard(g[0], g[1])
    #             if simulate_before_bmc:
    #                 reject_all_ntraces = True
    #                 if all_ntraces != []:
    #                     for ntrace in all_ntraces:
    #                         if self.simulate(ntrace, show_log=False) == 'accept':
    #                             reject_all_ntraces = False
    #                             break
    #                 if not reject_all_ntraces:
    #                     continue
    #             ntraces = []
    #             verifiedpnum = 0
    #             print("|",end='')
    #             for r in properties:
    #                 ntrace = self.bmc(z3.Not(r))
    #                 if ntrace == None:
    #                     print("√",end='')
    #                     verifiedpnum += 1
    #                     continue
    #                 else:
    #                     print("×",end='')
    #                     ntraces.append(ntrace)
    #                     if simulate_before_bmc:
    #                         all_ntraces.append(ntrace)
    #             if verifiedpnum == len(properties):
    #                 print("|")
    #                 print("all properties verified!")
    #                 print("average verify time:%ss" % (sum_verify_time/iter))
    #                 print("average synthesize time:%ss" % (sum_synthesize_time/(iter-1)))
    #                 return guards
    #             if verifiedpnum > max_verified_num:
    #                 max_verified_num = verifiedpnum
    #                 best_guard = guards
    #                 negative_traces = ntraces
    #         print("|")
    #         print("best guard:", end="")
    #         print(best_guard)
    #         print("negative_traces:", end="")
    #         print(negative_traces)
    #         T2 = time.time()
    #         sum_verify_time += T2 - T1
    #         print("synthsizing......")
    #         result_guards = []
    #         num = 0
    #         for ntrace in negative_traces:
    #             result_guard = self.synthesize_one_guard(ntrace, positive_traces)
    #             result_guards.append(result_guard)
    #         result_guards_cartesian = list(itertools.product(*result_guards))
    #         print("synthesized guard:")
    #         print(result_guards_cartesian)
    #         possible_guards = []
    #         for guards in result_guards_cartesian:
    #             possible_guards.append(best_guard + list(guards))
    #         T3 = time.time()
    #         sum_synthesize_time += T3 - T2
    #         print("----------------------------------------------")


    def synthesize(self, pos, neg, candidates):
        s = z3.Solver()
        approvePos = z3.BoolVal(True)
        for postrace in pos:
            approveT = z3.BoolVal(True)
            for tr_res in postrace:
                tr = tr_res[0]
                approvetx = z3.BoolVal(True)
                for i in range(1, len(tr_res)):
                    approvetx = z3.And(approvetx, z3.Implies(self.candidate_condition_guards[tr][i-1], tr_res[i]))
                approveT = z3.And(approveT, approvetx)
            approvePos = z3.And(approvePos, approveT)
        approveNeg = z3.BoolVal(True)
        for negtrace in neg:
            approveT = z3.BoolVal(True)
            for tr_res in negtrace:
                tr = tr_res[0]
                approvetx = z3.BoolVal(True)
                for i in range(1, len(tr_res)):
                    approvetx = z3.And(approvetx, z3.Implies(self.candidate_condition_guards[tr][i-1], tr_res[i]))
                approveT = z3.And(approveT, approvetx)
            approveNeg = z3.And(approveNeg, z3.Not(approveT))
        # print(approveNeg)
        s.add(approvePos)
        s.add(approveNeg)
        result = s.check()
        if result == z3.sat:
            self.clear_guards()
            m = s.model()
            # print(m)
            for tr in self.transitions:
                for c in range(len(candidates[tr])):
                    if m[self.candidate_condition_guards[tr][c]]:
                        self.add_guard(tr, candidates[tr][c])
            # print(self.condition_guards)
        else:
            print("No solution found!")
    def cegis(self, properties, positive_traces, candidate_guard, array = True):
        printing = True
        # printing = False
        synthesis_time = 0
        verification_time = 0
        if candidate_guard == None:
            candidate_guard = self.generate_candidate_guards(["<", "<=", ">", ">=", "="], array)
        else:
            for tr in self.candidate_condition_guards:
                self.candidate_condition_guards[tr] = []
                for i in range(len(candidate_guard)):
                    self.candidate_condition_guards[tr].append(z3.Const(tr+'_'+str(i), z3.BoolSort()))
        # return 
        pos = []
        neg = []
        T0 = time.time()
        T1 = time.time()
        for ptrace in positive_traces:
            res = self.simulate(ptrace, candidate_guard)
            print("trace:")
            print(ptrace)
            print("candidate_guard:")
            print(candidate_guard)
            print("res:")
            print(res)
            pos.append(res)
        T2 = time.time()
        synthesis_time += T2 - T1
        # print(pos)
        # for posi in pos:
        #     print(posi[0], len(posi))
        iter = 0
        while True:
            iter += 1
            if printing:
                print("iter", iter, end="| ")
            # print("pos:", pos)
            # print("neg:", neg)
            ##sample one concrete contract
            T1 = time.time()
            self.synthesize(pos, neg, candidate_guard)
            T2 = time.time()
            synthesis_time += T2 - T1            
            if printing:
                print(self.condition_guards)
            ##verify the properties
            T1 = time.time()
            new_ntraces = []
            for p in properties:
                ntrace = self.bmc(z3.Not(p))
                if ntrace == None:
                    if printing:
                        print("√", end="")
                else:
                    new_ntraces.append(ntrace)
                    if printing:
                        print("×", end="")
            if printing:
                print()
            if new_ntraces == []:
                if printing:
                    print("all properties verified!")
                break
            T2 = time.time()
            verification_time += T2 - T1
            self.clear_guards()
            T1 = time.time()
            for negtrace in new_ntraces:
                if printing:
                    print(negtrace)
                neg.append(self.simulate(negtrace, candidate_guard))
            T2 = time.time()
            synthesis_time += T2 - T1
        T3 = time.time()
        print(self.name, "| Time cost:%ss" % (T3 - T0), end="| ")
        print("Synthesis time:%ss" % synthesis_time, end="| ")
        print("Verification time:%ss" % verification_time, end="| ")
        print("iterations:", iter)
        if printing:
            print(self.condition_guards)
        return