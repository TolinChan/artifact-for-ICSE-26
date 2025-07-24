import z3,sys

index = 0

def pure_name(name):
    if "|" in name:
        return name[:name.index('|')]
    else:
        return name
    
def fresh(round, s, name):
    # print("s = ",s)
    global index
    index += 1
    return z3.Const(name+"|r%d|i%d" % (round, index), s)

def zipp(xs, ys):
    # print("xs:", xs)
    # print("ys:", ys)
    return [p for p in zip(xs, ys)]

def bmc(init, trans, goal, fvs, xs, xns):
    s = z3.Solver()
    s.set("timeout", 2000)
    s.add(init)
    count = 0
    # print("iteration ", end = "")
    while count<=7:
        # print(count, end = "", flush=True)
        count += 1
        p = fresh(count, z3.BoolSort(), "P")
        s.add(z3.Implies(p, goal))
        
        res = s.check(p)
        # print(res)
        if z3.sat == res:
            # print(s.model())
            return s.model()
        s.add(trans)
        ys = [fresh(count, x.sort(), pure_name(x.__str__())) for x in xs]
        nfvs = [fresh(count, x.sort(), pure_name(x.__str__())) for x in fvs]
        # print("before:", trans)
        trans = z3.substitute(trans, 
                           zipp(xns + xs + fvs, ys + xns + nfvs))
        # print("zip:", zipp(xns + xs + fvs, ys + xns + nfvs))
        # print("after:", trans)
        goal = z3.substitute(goal, zipp(xs, xns))
        xs, xns, fvs = xns, ys, nfvs
        # sys.stdout.flush()

def extract_model(model, var=None):
    rd = {}
    maxrd = -1
    for v in model:
        name = v.__str__()
        if "|" in name:
            round = int(name[name.index("|r")+2:name.index("|i")]) + 1
            maxrd = max(maxrd, round)
            index = int(name[name.index("|i")+2:])
            pname = pure_name(name)
            # print(pname, pname[-1])
            if pname[-1] == "'":
                pname = pname[:-1]
            # print(pname)
            if round not in rd.keys():
                rd[round] = {}
            rd[round][pname] = model[v]
            # print(round, index)
        else:
            if name[-1] == "'":
                if 1 not in rd.keys():
                    rd[1] = {}
                rd[1][name[:-1]] = model[v]
            else:
                if 0 not in rd.keys():
                    rd[0] = {}
                rd[0][name] = model[v]
    # print(rd)
    # for i in range(maxrd-1):
    #     print("round {i}:".format(i=i))
    #     if var == None:
    #         print(rd[i])
    #     else:
    #         if var in rd[i].keys():
    #             print(rd[i][var])
    #         else:
    #             print(rd[i][var+"'"])
    return rd