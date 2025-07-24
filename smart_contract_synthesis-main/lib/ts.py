import z3

class Ts(object):
    """A transition system

    Example usage
    >>> T = Ts('Ts0')
    >>> x, x_out = T.add_var(z3.IntSort(), name='x')
    >>> T.Init = x <= 0
    >>> T.Tr = z3.And(x < 5, x_out == x + 1)
    >>> T.Bad = x >= 10
    >>> T                                   #doctest: +NORMALIZE_WHITESPACE
    Transition System: Ts0
        Init: v_0 <= 0
        Bad: v_0 >= 10
        Tr: And(v_0 < 5, v_out_0 == v_0 + 1)
    """
    def __init__(self, name='Ts'):
        # string name
        self.name = name
        # state variables (pair of input and output)
        self._vars = []
        # inputs
        self._inputs = []
        # a map from optional names to state variables
        self._named_vars = dict()

        # maps state variable index to optional name
        self._var_names = list()

        # Transition relation
        self.Tr = z3.BoolVal(True)
        # Initial condition
        self.Init = z3.BoolVal(True)
        # Bad states
        self.Bad = z3.BoolVal(False)

    # def add_tr(self, f):
    #     assert(type(f).__name__ == 'BoolRef')
    #     self.Tr.append(f)

    def add_var(self, sort, name=None):
        '''Add a state variable of a given sort. Returns a pair (pre, post)
           of a pre- and post- state versions of the variable
        '''
        pre, post = self._new_var_name(name=name)
        v_in = z3.Const(pre, sort)
        v_out = z3.Const(post, sort)
        self._vars.append((v_in, v_out))
        self._var_names.append(name)
        if name is not None:
            self._named_vars[name] = (v_in, v_out) 

        return (v_in, v_out)

    def get_var(self, idx):
        """Returns a pair of pre- and post-variables with a given index or name

        If idx is not an int it is interpreted as a name.
        Otherwise, it is interpreted as a variable index.

        >>> T = Ts('Ts0')
        >>> x, x_out = T.add_var(z3.IntSort(), name='x')
        >>> y, y_out = T.add_var(z3.IntSort(), name='y')
        >>> x
        v_0

        >>> T.get_var(1)
        (v_1, v_out_1)

        >>> T.get_var('x')
        (v_0, v_out_0)

        """
        if isinstance(idx, int):
            return self._vars[idx]
        elif idx in self._named_vars:
            return self._named_vars[idx]
        return None

    def get_var_name(self, idx):
        if idx < len(self._var_names):
            return self._var_names[idx]
        return None

    def get_pre_var(self, idx):
        """Returns a pre-state variable with a given name/index"""
        res = self.get_var(idx)
        if res is not None:
            return res[0]
        return None

    def get_pre_vars(self, vars):
        """Returns a tuple of pre-state variables with given names"""
        return (self.get_pre_var(v) for v in vars.split())

    def get_post_var(self, idx):
        """Returns a post-state variable with a given name"""
        res = self.get_var(idx)
        if res is not None:
            return res[1]
        return None

    def add_input(self, sort, name=None):
        '''Add an input of a given sort'''
        v = z3.Const(self._new_input_name(name=name), sort)
        self._inputs.append(v)
        return v

    def inputs(self):
        return self._inputs
    def pre_vars(self):
        return [u for (u, v) in self._vars]
    def post_vars(self):
        return [v for (u, v) in self._vars]
    def vars(self):
        return self.pre_vars() + self.post_vars()
    def pre_post_vars(self):
        return self._vars
    def all(self):
        return self.vars() + self.inputs()
    def sig(self):
        return [v.sort() for (u, v) in self._vars]
    def to_post(self, e):
        '''Rename expression over pre-state variables to post-state variables

        >>> T = Ts('Ts0')
        >>> x, x_out = T.add_var(z3.IntSort(), 'x')
        >>> y, y_out = T.add_var(z3.IntSort(), 'y')
        >>> e = x > y
        >>> T.to_post(x > y)
        v_out_0 > v_out_1
        '''
        return z3.substitute(e, *self._vars)
    
    def to_prev(self, e):
        '''Rename expression over post-state variables to pre-state variables

        >>> T = Ts('Ts0')
        >>> x, x_out = T.add_var(z3.IntSort(), 'x')
        >>> y, y_out = T.add_var(z3.IntSort(), 'y')
        >>> e = x > y
        >>> T.to_post(x > y)
        v_out_0 > v_out_1
        '''
        reversed_vars = [(v, u) for (u, v) in self._vars]
        return z3.substitute(e, *reversed_vars)

    def _new_input_name(self, name=None):
        if name is not None:
            return str(name)
        else:
            return self._mk_input_name(len(self._inputs))

    def _mk_input_name(self, idx):
        return 'i_' + str(idx)

    def _new_var_name(self, name=None):
        if name is not None:
            assert name not in self._named_vars
            assert str(name) not in self._named_vars
            return str(name), str(name) + "'" 
        else:
            idx = len(self._vars)
            return self._mk_var_name(idx), self._mk_post_var_name(idx)

    def _mk_var_name(self, idx):
        return 'v_' + str(idx)
    def _mk_post_var_name(self, idx):
        return 'v_out_' + str(idx)

    def __repr__(self):
        return 'Transition System: ' + self.name + '\n' + \
            '\tInit: ' + str(self.Init) + '\n' + \
            '\tBad: ' + str(self.Bad) + '\n' + \
            '\tTr: ' + str(self.Tr)

    def __str__(self):
        return repr(self)

    def filter_pre_vars(self, f):
        f_vars = get_vars(f)
        vars = filter(lambda x: x[0] in f_vars, self._vars)
        return [u for (u,v) in vars]

    def filter_post_vars(self, f):
        f_vars = get_vars(f)
        vars = filter(lambda x: x[0] in f_vars, self._vars)
        return [v for (u,v) in vars]
