import random
from pathlib import Path
import os
import re
import glob

# ========== Configuration ==========
TRACE_LENGTH = 1000  # Length of generated trace
ACCOUNTS = ['0xA1', '0xB2', '0xC3', '0xD4']  # Available accounts

# ========== Parse Solidity Functions ==========
def parse_solidity_functions(sol_file):
    with open(sol_file, 'r', encoding='utf-8') as f:
        code = f.read()
    pattern = re.compile(r'function\s+(\w+)\s*\(([^)]*)\).*?(public|external)')
    functions = []
    for match in pattern.finditer(code):
        name = match.group(1)
        params = match.group(2)
        param_types = []
        if params.strip():
            for p in params.split(','):
                t = p.strip().split(' ')[0]
                param_types.append(t)
        functions.append({'name': name, 'param_types': param_types})
    return functions

# ========== Extended Temporal Properties Parsing ==========
def parse_temporal_properties(prop_file):
    """Parse temporal properties file, supporting complex logical expressions"""
    properties = {
        'invariants': [],      # Invariants: □(condition)
        'safety': [],          # Safety properties
        'liveness': [],        # Liveness properties
        'temporal_ops': [],    # Temporal operators
        'variables': set(),    # Variable set
        'predicates': set(),   # Predicate set
        'constraints': []      # Constraint conditions
    }
    
    if not os.path.exists(prop_file):
        return properties
    
    with open(prop_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Parse comments and properties
    lines = content.split('\n')
    for line in lines:
        line = line.strip()
        if not line or line.startswith('//'):
            continue
        
        # Extract property expressions
        prop = extract_property_expression(line)
        if prop:
            analyze_property(prop, properties)
    
    return properties

def extract_property_expression(line):
    """Extract property expressions"""
    # Remove comments
    if '//' in line:
        line = line.split('//')[0].strip()
    
    # Find □(expression) format
    if '□(' in line and line.endswith(')'):
        start = line.find('□(') + 2
        end = line.rfind(')')
        if start < end:
            return line[start:end]
    
    # Find other property formats
    if '→' in line or '∧' in line or '∨' in line or '¬' in line:
        return line
    
    return None

def analyze_property(expression, properties):
    """Analyze property expressions"""
    # Extract variables and predicates
    extract_variables_and_predicates(expression, properties)
    
    # Classify property types
    if '□(' in expression or '□' in expression:
        properties['invariants'].append(expression)
    elif '♦' in expression:
        properties['liveness'].append(expression)
    elif '→' in expression:
        properties['safety'].append(expression)
    
    # Extract constraint conditions
    extract_constraints(expression, properties)
    
    # Extract temporal operators
    extract_temporal_operators(expression, properties)

def extract_variables_and_predicates(expression, properties):
    """Extract variables and predicates"""
    # Match function call format: functionName(param1, param2, ...)
    func_pattern = r'([a-zA-Z_][a-zA-Z0-9_]*)\s*\(([^)]*)\)'
    matches = re.findall(func_pattern, expression)
    
    for func_name, params in matches:
        properties['predicates'].add(func_name)
        # Extract parameters as variables
        param_list = [p.strip() for p in params.split(',') if p.strip()]
        for param in param_list:
            if param.isalpha() and param not in ['true', 'false', 'and', 'or', 'not']:
                properties['variables'].add(param)
    
    # Extract other variables (single letters or words)
    var_pattern = r'\b([a-zA-Z_][a-zA-Z0-9_]*)\b'
    vars = re.findall(var_pattern, expression)
    for var in vars:
        if var not in ['true', 'false', 'and', 'or', 'not', '□', '♦', '→', '∧', '∨', '¬']:
            properties['variables'].add(var)

def extract_constraints(expression, properties):
    """Extract constraint conditions"""
    constraints = []
    
    # Numerical constraints
    if '≤' in expression or '<=' in expression:
        constraints.append('comparison')
    if '≥' in expression or '>=' in expression:
        constraints.append('comparison')
    if '==' in expression or '=' in expression:
        constraints.append('equality')
    if '!=' in expression or '≠' in expression:
        constraints.append('inequality')
    
    # Logical constraints
    if '∧' in expression or 'and' in expression:
        constraints.append('conjunction')
    if '∨' in expression or 'or' in expression:
        constraints.append('disjunction')
    if '¬' in expression or 'not' in expression:
        constraints.append('negation')
    if '→' in expression or '->' in expression:
        constraints.append('implication')
    
    properties['constraints'].extend(constraints)

def extract_temporal_operators(expression, properties):
    """Extract temporal operators"""
    if '□' in expression:
        properties['temporal_ops'].append('always')
    if '♦' in expression:
        properties['temporal_ops'].append('eventually')
    if '○' in expression:
        properties['temporal_ops'].append('next')
    if 'U' in expression:
        properties['temporal_ops'].append('until')

# ========== Random Parameter Generation ==========
def random_param(param_type, exclude=None):
    if 'address' in param_type:
        choices = [acc for acc in ACCOUNTS if acc != exclude] if exclude else ACCOUNTS
        return random.choice(choices)
    elif 'uint' in param_type or 'int' in param_type:
        return str(random.randint(1, 100))
    elif 'bool' in param_type:
        return random.choice(['true', 'false'])
    else:
        return '0'

# ========== Contract State Management ==========
class ContractState:
    def __init__(self, properties):
        self.properties = properties
        self.balances = {}  # Account balances
        self.allowances = {}  # Authorization amounts (owner, spender) -> amount
        self.total_supply = 1000000  # Total supply
        self.withdraw_counts = {}  # Withdrawal counts
        self.votes = set()  # Voted accounts
        self.bids = {}  # Bidding records
        self.highest_bid = 0  # Highest bid
        self.highest_bidder = None  # Highest bidder
        self.auction_ended = False  # Whether auction has ended
        self.crowdfunding_closed = False  # Whether crowdfunding has ended
        self.raised_funds = 0  # Raised funds
        self.target_funds = 1000000  # Target funds
        self.operation_counts = {}  # Operation count statistics
        
        # Initialize some account balances
        for i, account in enumerate(ACCOUNTS[:5]):
            self.balances[account] = random.randint(1000, 10000)
    
    def is_valid(self, func, params):
        """Check if operation is valid"""
        # Basic validation
        if not self._basic_validation(func, params):
            return False
        
        # Property-based validation
        if not self._property_based_validation(func, params):
            return False
        
        return True
    
    def _basic_validation(self, func, params):
        """Basic validation"""
        if func['name'] == 'transfer':
            if len(params) < 3:
                return False
            sender, to, value = params[0], params[1], params[2]
            try:
                value_int = int(value)
                return (self.balances.get(sender, 0) >= value_int and 
                       value_int > 0 and sender != to)
            except (ValueError, TypeError):
                return False
        return True
    
    def _property_based_validation(self, func, params):
        """Property-based validation"""
        properties = self.properties
        
        # Check invariants
        for invariant in properties.get('invariants', []):
            if not self._check_invariant(func, params, invariant):
                return False
        
        # Check safety properties
        for safety in properties.get('safety', []):
            if not self._check_safety(func, params, safety):
                return False
        
        return True
    
    def _check_invariant(self, func, params, invariant):
        """Check invariants"""
        # Total supply should not be less than total balances
        if 'totalSupply' in invariant and 'totalBalances' in invariant:
            total_balances = sum(self.balances.values())
            if total_balances > self.total_supply:
                return False
        
        # Balances should be non-negative
        if 'balanceOf' in invariant and ('≥' in invariant or '>=' in invariant):
            for balance in self.balances.values():
                if balance < 0:
                    return False
        
        # Authorization amount check
        if 'allowance' in invariant and 'transferFrom' in invariant:
            if func['name'] == 'transferFrom' and len(params) >= 3:
                from_acc, to_acc, value = params[0], params[1], params[2]
                try:
                    value_int = int(value)
                    allowance = self.allowances.get((from_acc, to_acc), 0)
                    if value_int > allowance:
                        return False
                except (ValueError, TypeError):
                    return False
        
        return True
    
    def _check_safety(self, func, params, safety):
        """Check safety state"""
        # Withdrawal count limit
        if 'withdrawCount' in safety and ('≤' in safety or '<=' in safety):
            if func['name'] == 'withdraw':
                account = params[0] if params else None
                if account and self.withdraw_counts.get(account, 0) >= 1:
                    return False
        
        # Vote uniqueness
        if 'vote' in safety and '=' in safety:
            if func['name'] == 'vote':
                voter = params[0] if params else None
                if voter and voter in self.votes:
                    return False
        
        # Auction highest bid
        if 'highestBid' in safety and 'bid' in safety:
            if func['name'] == 'bid':
                if len(params) >= 2:
                    bidder, amount = params[0], params[1]
                elif len(params) >= 1:
                    bidder, amount = params[0], "0"
                else:
                    return True  # Skip check if no parameters
                try:
                    amount_int = int(amount)
                    if amount_int <= self.highest_bid and bidder != self.highest_bidder:
                        return False
                except (ValueError, TypeError):
                    return False
        
        return True
    
    def update(self, func, params):
        """Update state"""
        if func['name'] == 'transfer':
            if len(params) >= 3:
                sender, to, value = params[0], params[1], params[2]
                try:
                    value_int = int(value)
                    self.balances[sender] = self.balances.get(sender, 0) - value_int
                    self.balances[to] = self.balances.get(to, 0) + value_int
                except (ValueError, TypeError):
                    pass
        
        elif func['name'] == 'transferFrom':
            if len(params) >= 3:
                from_acc, to_acc, value = params[0], params[1], params[2]
                try:
                    value_int = int(value)
                    self.balances[from_acc] = self.balances.get(from_acc, 0) - value_int
                    self.balances[to_acc] = self.balances.get(to_acc, 0) + value_int
                    # Reduce authorization amount
                    key = (from_acc, to_acc)
                    self.allowances[key] = self.allowances.get(key, 0) - value_int
                except (ValueError, TypeError):
                    pass
        
        elif func['name'] == 'approve':
            if len(params) >= 3:
                owner, spender, amount = params[0], params[1], params[2]
                try:
                    amount_int = int(amount)
                    self.allowances[(owner, spender)] = amount_int
                except (ValueError, TypeError):
                    pass
        
        elif func['name'] == 'mint':
            if len(params) >= 2:
                account, amount = params[0], params[1]
                try:
                    amount_int = int(amount)
                    self.balances[account] = self.balances.get(account, 0) + amount_int
                    self.total_supply += amount_int
                except (ValueError, TypeError):
                    pass
        
        elif func['name'] == 'burn':
            if len(params) >= 2:
                account, amount = params[0], params[1]
                try:
                    amount_int = int(amount)
                    self.balances[account] = self.balances.get(account, 0) - amount_int
                    self.total_supply -= amount_int
                except (ValueError, TypeError):
                    pass
        
        elif func['name'] == 'withdraw':
            if params:
                account = params[0]
                self.withdraw_counts[account] = self.withdraw_counts.get(account, 0) + 1
        
        elif func['name'] == 'vote':
            if params:
                voter = params[0]
                self.votes.add(voter)
        
        elif func['name'] == 'bid':
            if len(params) >= 2:
                bidder, amount = params[0], params[1]
                try:
                    amount_int = int(amount)
                    if amount_int > self.highest_bid:
                        self.highest_bid = amount_int
                        self.highest_bidder = bidder
                    self.bids[bidder] = amount_int
                except (ValueError, TypeError):
                    pass
        
        # Update operation statistics
        self.operation_counts[func['name']] = self.operation_counts.get(func['name'], 0) + 1

# ========== Smart Parameter Generation ==========
def smart_param_generation(func, state):
    """Smart parameter generation, considering current state"""
    if func['name'] == 'transfer':
        # Select accounts with balance as sender
        valid_senders = [acc for acc in ACCOUNTS if state.balances.get(acc, 0) > 0]
        if not valid_senders:
            return None
        sender = random.choice(valid_senders)
        to = random.choice([acc for acc in ACCOUNTS if acc != sender])
        max_value = state.balances[sender]
        value = str(random.randint(1, max(1, max_value // 2)))  # Use half of balance
        return [sender, to, value]
    elif func['name'] == 'transferFrom':
        # Select accounts with balance and authorization
        valid_pairs = []
        for from_acc in ACCOUNTS:
            for to_acc in ACCOUNTS:
                if from_acc != to_acc:
                    balance = state.balances.get(from_acc, 0)
                    allowance = state.allowances.get((from_acc, to_acc), 0)
                    if balance > 0 and allowance > 0:
                        valid_pairs.append((from_acc, to_acc, min(balance, allowance)))
        if not valid_pairs:
            return None
        from_acc, to_acc, max_value = random.choice(valid_pairs)
        value = str(random.randint(1, max(1, max_value // 2)))
        return [from_acc, to_acc, value]
    elif func['name'] == 'approve':
        owner = random.choice(ACCOUNTS)
        spender = random.choice([acc for acc in ACCOUNTS if acc != owner])
        amount = str(random.randint(10, 200))
        return [owner, spender, amount]
    elif func['name'] == 'mint':
        account = random.choice(ACCOUNTS)
        amount = str(random.randint(100, 1000))
        return [account, amount]
    elif func['name'] == 'burn':
        # Select accounts with balance
        valid_accounts = [acc for acc in ACCOUNTS if state.balances.get(acc, 0) > 0]
        if not valid_accounts:
            return None
        account = random.choice(valid_accounts)
        max_amount = state.balances[account]
        amount = str(random.randint(1, max(1, max_amount // 3)))
        return [account, amount]
    elif func['name'] in ['balanceOf', 'allowance', 'totalSupply']:
        if func['name'] == 'balanceOf':
            return [random.choice(ACCOUNTS)]
        elif func['name'] == 'allowance':
            owner = random.choice(ACCOUNTS)
            spender = random.choice([acc for acc in ACCOUNTS if acc != owner])
            return [owner, spender]
        else:  # totalSupply
            return []
    else:
        # Generic parameter generation
        return [random_param(t) for t in func['param_types']]

# ========== Business Pattern Insertion ==========
def insert_business_patterns(trace, state, current_time):
    """Insert typical business patterns"""
    patterns = [
        # Typical approve -> transferFrom flow
        [
            ("approve", lambda: [random.choice(ACCOUNTS), random.choice(ACCOUNTS), "100"]),
            ("transferFrom", lambda: [random.choice(ACCOUNTS), random.choice(ACCOUNTS), "50"])
        ],
        # Multi-step transfer chain
        [
            ("transfer", lambda: [random.choice(ACCOUNTS), random.choice(ACCOUNTS), "30"]),
            ("transfer", lambda: [random.choice(ACCOUNTS), random.choice(ACCOUNTS), "20"]),
            ("transfer", lambda: [random.choice(ACCOUNTS), random.choice(ACCOUNTS), "10"])
        ],
        # mint -> transfer -> burn flow
        [
            ("mint", lambda: [random.choice(ACCOUNTS), "500"]),
            ("transfer", lambda: [random.choice(ACCOUNTS), random.choice(ACCOUNTS), "200"]),
            ("burn", lambda: [random.choice(ACCOUNTS), "100"])
        ]
    ]
    
    # 30% probability to insert business patterns
    if random.random() < 0.3 and len(trace) < 950:  # Ensure not exceeding total length
        pattern = random.choice(patterns)
        pattern_trace = []
        for func_name, param_gen in pattern:
            params = param_gen()
            # Create temporary func object for validation
            temp_func = {'name': func_name, 'param_types': []}
            if state.is_valid(temp_func, params):
                pattern_trace.append(f"{func_name}({format_params(temp_func, params)})@{current_time};")
                state.update(temp_func, params)
                current_time += 1
        if pattern_trace:
            trace.extend(pattern_trace)
            return current_time
    return current_time

# ========== Dynamic Weight Adjustment ==========
def adaptive_weighted_choice(functions, recent_ops):
    """Dynamically adjust weights based on recent operations"""
    weights = []
    for f in functions:
        base_weight = 5 if f['name'] in ['transfer', 'transferFrom'] else 1
        # If a function appears too frequently in recent operations, reduce its weight
        recent_count = recent_ops.count(f['name'])
        adjusted_weight = max(1, base_weight - recent_count)
        weights.append(adjusted_weight)
    return random.choices(functions, weights=weights, k=1)[0]

# ========== Operation Diversity Guarantee ==========
def ensure_operation_diversity(trace, functions):
    """Ensure operation diversity"""
    used_ops = set()
    for op in trace:
        if '//' not in op and '@' in op:  # Skip comment lines
            used_ops.add(op.split('(')[0])
    
    # If some operation types are missing, increase their weight
    missing_ops = [f for f in functions if f['name'] not in used_ops]
    if missing_ops:
        return random.choice(missing_ops)
    return None

# ========== Generate Trace ==========
def generate_trace(functions, trace_length, state):
    trace = []
    tries = 0
    trace_count = 0
    current_trace = []
    current_time = 0
    recent_ops = []  # Record recent operation types
    
    while len(trace) < trace_length and tries < trace_length * 100:
        # Check operation diversity
        diverse_func = ensure_operation_diversity(current_trace, functions)
        if diverse_func:
            func = diverse_func
        else:
            func = adaptive_weighted_choice(functions, recent_ops[-10:])  # Consider last 10 operations
        
        # Smart parameter generation
        params = smart_param_generation(func, state)
        if params is None:
            tries += 1
            continue
        
        # State validation
        if not state.is_valid(func, params):
            tries += 1
            continue
        
        # Format parameters as key=value format
        param_str = format_params(func, params)
        current_trace.append(f"{func['name']}({param_str})@{current_time};")
        current_time += 1
        recent_ops.append(func['name'])
        
        # Insert typical business patterns
        current_time = insert_business_patterns(current_trace, state, current_time)
        
        # Every 10-20 operations form a trace sequence
        if len(current_trace) >= random.randint(10, 20) or len(trace) + len(current_trace) >= trace_length:
            if current_trace:
                trace_count += 1
                trace.append(f"// Trace {trace_count}: {generate_trace_description(current_trace)}")
                trace.extend(current_trace)
                trace.append("")  # Empty line separator
                current_trace = []
                current_time = 0
        
        state.update(func, params)
        tries = 0
    
    # Handle remaining trace
    if current_trace:
        trace_count += 1
        trace.append(f"// Trace {trace_count}: {generate_trace_description(current_trace)}")
        trace.extend(current_trace)
    
    return trace

def format_params(func, params):
    """Format parameters as key=value format compatible with parser"""
    if func['name'] == 'transfer':
        return f"from={params[0]}, to={params[1]}, value={params[2]}"
    elif func['name'] == 'transferFrom':
        # Fix: transferFrom should have 4 parameters: sender, from, to, value
        if len(params) >= 3:
            sender = params[0]
            from_acc = params[1] 
            to_acc = params[2]
            value = params[3] if len(params) > 3 else '50'
            return f"sender={sender}, from={from_acc}, to={to_acc}, value={value}"
        else:
            return f"sender={params[0]}, from={params[0]}, to={params[1]}, value=50"
    elif func['name'] == 'approve':
        return f"owner={params[0]}, spender={params[1]}, amount={params[2]}"
    elif func['name'] == 'mint':
        return f"account={params[0]}, amount={params[1]}"
    elif func['name'] == 'burn':
        return f"account={params[0]}, amount={params[1]}"
    elif func['name'] == 'balanceOf':
        return f"account={params[0]}"
    elif func['name'] == 'allowance':
        return f"owner={params[0]}, spender={params[1]}"
    elif func['name'] == 'totalSupply':
        return ""
    elif func['name'] == 'vote':
        if len(params) >= 2:
            return f"voter={params[0]}, proposal={params[1]}"
        elif len(params) >= 1:
            return f"voter={params[0]}, proposal=1"
        else:
            return f"voter=0xA1, proposal=1"
    elif func['name'] == 'bid':
        if len(params) >= 2:
            return f"bidder={params[0]}, amount={params[1]}"
        elif len(params) >= 1:
            return f"bidder={params[0]}, amount=100"
        else:
            return f"bidder=0xA1, amount=100"
    elif func['name'] == 'withdraw':
        if len(params) >= 2:
            return f"bidder={params[0]}, amount={params[1]}"
        elif len(params) >= 1:
            return f"bidder={params[0]}, amount=100"
        else:
            return f"bidder=0xA1, amount=100"
    elif func['name'] == 'endAuction':
        return ""
    elif func['name'] == 'auctionEnd':
        return ""
    elif func['name'] == 'ansfer':  # Handle the incorrect function name from synthesis
        if len(params) >= 2:
            return f"from={params[0]}, to={params[1]}, value={params[2] if len(params) > 2 else '100'}"
        elif len(params) >= 1:
            return f"from={params[0]}, to=0xB2, value=100"
        else:
            return f"from=0xA1, to=0xB2, value=100"
    elif func['name'] == 'ansferFrom':  # Handle the incorrect function name from synthesis
        if len(params) >= 2:
            return f"sender={params[0]}, from={params[0]}, to={params[1]}, value={params[2] if len(params) > 2 else '50'}"
        elif len(params) >= 1:
            return f"sender={params[0]}, from={params[0]}, to=0xB2, value=50"
        else:
            return f"sender=0xA1, from=0xA1, to=0xB2, value=50"
    else:
        # Generic format
        param_names = ['param1', 'param2', 'param3', 'param4', 'param5']
        formatted = []
        for i, param in enumerate(params):
            if i < len(param_names):
                formatted.append(f"{param_names[i]}={param}")
        return ", ".join(formatted)

def generate_trace_description(trace_ops):
    """Generate trace description"""
    op_types = [op.split('(')[0] for op in trace_ops]
    unique_ops = list(set(op_types))
    if len(unique_ops) <= 3:
        return ", ".join(unique_ops)
    else:
        return f"{len(unique_ops)} different operations"

# ========== Main Process ==========
def batch_generate():
    """Generate traces for all contracts in output directory"""
    sol_files = glob.glob('output/*.sol')
    for sol_file in sol_files:
        name = Path(sol_file).stem
        prop_file = f'synthesis-benchmark/{name}/temporal_properties.txt'
        trace_file = f'test_trace/{name}/example_traces.txt'
        if not Path(prop_file).exists():
            print(f"Property file does not exist, skipping: {prop_file}")
            continue
        print(f"Processing: {name}")
        functions = parse_solidity_functions(sol_file)
        properties = parse_temporal_properties(prop_file)
        state = ContractState(properties)
        trace = generate_trace(functions, TRACE_LENGTH, state)
        os.makedirs(os.path.dirname(trace_file), exist_ok=True)
        with open(trace_file, 'w', encoding='utf-8') as f:
            for line in trace:
                f.write(line + '\n')
        print(f"Generated {len(trace)} traces, saved to {trace_file}")

def generate_single_trace():
    """Generate trace for a single contract.sol file in current directory"""
    sol_file = 'contract.sol'
    if not os.path.exists(sol_file):
        print(f"Contract file does not exist: {sol_file}")
        return False
    
    print(f"Processing contract.sol")
    functions = parse_solidity_functions(sol_file)
    
    if not functions:
        print("No functions found in contract.sol")
        return False
    
    # Create a simple state for trace generation
    properties = {
        'invariants': [],
        'safety': [],
        'liveness': [],
        'temporal_ops': [],
        'variables': set(),
        'predicates': set(),
        'constraints': []
    }
    state = ContractState(properties)
    
    trace = generate_trace(functions, TRACE_LENGTH, state)
    trace_file = 'example_traces.txt'
    
    with open(trace_file, 'w', encoding='utf-8') as f:
        for line in trace:
            f.write(line + '\n')
    
    print(f"Trace written to {trace_file}")
    return True

if __name__ == '__main__':
    # Check if we're in a directory with contract.sol
    if os.path.exists('contract.sol'):
        generate_single_trace()
    else:
        batch_generate()
