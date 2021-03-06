%% Configuration/Constants
%% How To Run
% Before running the MATLAB script, make sure `analysis_file` variable points
% the analysis.json file generated by analysis (eclipse-project)
%% Configuration
%%

% the file containing analysis output
clear % we *always* want to start from a clean slate, otherwise it can give us garbage

analysis_file = 'biometric/analysis.json';
costs_file = 'sample-costs.json';
bit_length = 32; % Un-used now, but eventually when costs file will contain
% costs for multiple bitlengths, This param will specify the bitlength for
% which we want the solver to calculate optimal assignment, the solver will
% then use the costs corresponding to this bitlength in its calculations

% this protocol always has Infinite costs for everything, it comes handy in
% benchmarking i.e. when we want to find cost for Y-only or B-only
global infinite_cost_protocol;
infinite_cost_protocol = "infinite_cost_protocol";
global non_mpc_node_cost;
non_mpc_node_cost = 1; % this cost cannot be zero 

json_costs = get_json_costs(costs_file, bit_length);

%% Preprocessing
[all_def_uses, all_vars, nodes_vector, num_edges, node_lookup] = load_analysis(analysis_file);

% %% Build and Solve IP (best 2 out of 3)
% [summary_assignment, summary_cost, num_conversions] = do_best_2_out_of_3(json_costs, all_def_uses, all_vars, nodes_vector, num_edges, node_lookup);
% fprintf('\n%s - %s\n', analysis_file, costs_file);

% Just the B only or Y only costs
[fval_b_only, fval_y_only] = get_b_only_and_y_only_costs(json_costs, all_def_uses, all_vars, nodes_vector, num_edges);
nf = java.text.DecimalFormat;
b_cost = char(nf.format(fval_b_only));
y_cost = char(nf.format(fval_y_only));
[summary_assignment, summary_cost, num_conversions] = do_best_2_out_of_3(json_costs, all_def_uses, all_vars, nodes_vector, num_edges, node_lookup);
fprintf('\nY-only Cost: %s', y_cost);
fprintf('\nB-only Cost: %s', b_cost);
fprintf('\n%s - %s\n', analysis_file, costs_file);

%% Functions
%%

function [fval_b_only, fval_y_only] = get_b_only_and_y_only_costs(json_costs, all_def_uses, all_vars, nodes_vector, num_edges)
    global infinite_cost_protocol;
    % Force the assignments as either B only or Y only, then print the costs. This comes in handy for benchmarks
    [~, fval_b_only] = build_and_solve_ip("b", infinite_cost_protocol, json_costs, all_def_uses, all_vars, nodes_vector, num_edges);
    [~, fval_y_only] = build_and_solve_ip("y", infinite_cost_protocol, json_costs, all_def_uses, all_vars, nodes_vector, num_edges);
end

function [summary_assignment, summary_cost, num_conversions] = do_best_2_out_of_3(json_costs, all_def_uses, all_vars, nodes_vector, num_edges, node_lookup)
    [x_ab, fval_ab] = build_and_solve_ip("a", "b", json_costs, all_def_uses, all_vars, nodes_vector, num_edges);
    [x_ay, fval_ay] = build_and_solve_ip("a", "y", json_costs, all_def_uses, all_vars, nodes_vector, num_edges);
    [x_by, fval_by] = build_and_solve_ip("b", "y", json_costs, all_def_uses, all_vars, nodes_vector, num_edges);

    min_protocol_a = "a";
    min_protocol_b = "b";
    min_x = x_ab;
    min_fval = fval_ab;

    if(min_fval > fval_ay)
        min_protocol_a = "a";
        min_protocol_b = "y";
        min_x = x_ay;
        min_fval = fval_ay;
    end

    if(min_fval > fval_by)
        min_protocol_a = "b";
        min_protocol_b = "y";
        min_x = x_by;
        min_fval = fval_by;
    end

    %% Output the results
    [summary_assignment, summary_cost, num_conversions] = print_output(min_x, min_fval, min_protocol_a, min_protocol_b, all_def_uses, all_vars, nodes_vector, node_lookup, json_costs);
end

function json_costs = get_json_costs(costs_file_path, bit_length)
    all_json = jsondecode(fileread(costs_file_path));
    costs_for_bit_length = all_json.("x" + bit_length); % MATLAB makes keys such as "32" => "x32".
    json_costs = costs_for_bit_length;
end

function [all_def_uses, all_vars, nodes_vector, num_edges, node_lookup] = load_analysis(analysis_file)
    json = jsondecode(fileread(analysis_file));
    all_def_uses = json.('def_use');

    all_vars = fields(all_def_uses);
    num_vars = numel(all_vars);
    node_map = containers.Map('KeyType', 'uint32', 'ValueType', 'char'); % map of all nodes (id -> instruction)
    num_edges = 0;
    for i = 1:num_vars
        def_use = all_def_uses.(all_vars{i});
        num_uses = numel(def_use.uses);
        def = def_use.def;
        node_map(def.id.index) = def.id.unit;
        node_lookup(def.id.index) = struct('index', def.id.index, 'unit', def.id.unit ...
            , 'weight', def.weight, 'parallel_param', def.parallel_param, 'node_type', def.node_type);
        % for each use, we can define an edge from 'def' to 'use', for 'k'
        % uses, we have 'k' edges
        num_edges = num_edges + num_uses;
        for j = 1:num_uses
            use = def_use.uses(j);
            node_map(use.id.index) = use.id.unit;
            node_lookup(use.id.index) = struct('index', use.id.index, 'unit', use.id.unit ...
            , 'weight', use.weight, 'parallel_param', use.parallel_param, 'node_type', use.node_type);
        end
    end

    % lets put all nodes in the def-use graph into a vector
    nodes_vector = (cell2mat(node_map.keys()));
end

function [x, fval] = build_and_solve_ip(protocol1, protocol2, json_costs, all_def_uses, all_vars, nodes_vector, num_edges)
    num_vars = numel(all_vars);
    num_nodes = length(nodes_vector); 
    node_index_map = containers.Map('KeyType', 'uint32', 'ValueType', 'uint32'); % map of all nodes (id -> index)
    for i = 1:num_nodes
        node_index_map(nodes_vector(i)) = i;
    end
    % - for each node `k`, we have 2 variables `a_k` and `y_k`. 
    % - for each edge `e`, we have 2 variables `a_e` and `y_e`.
    total_vars = (num_nodes * 2) + (num_edges * 2);

    % - for each node `k`, we hae an inequality constraint `a_k + y_k >= 1`
    % - for each edge `e`, we have 2 ineqaulity constarints: `a_e >= a_d - a_u` 
    % and `y_e >= y_d - y_u`

    total_ineqs = num_nodes + num_edges * 2;
    %% Create Matrices for Linear Program
    %%
    A = zeros(total_ineqs, total_vars); % constraint matrix
    b = zeros(total_ineqs, 1); % constraint matrix row should be less-than-or-equal-to the value in `b` 
    A_eq = [];
    b_eq = [];
    f = zeros(total_vars, 1); % costs
    lb = zeros(total_vars, 1); % lower bound
    ub = ones(total_vars, 1); % upper bound
    intcon = 1:total_vars; % integer constraints

    % populate A, b and f
    row = 1;
    col = 1;
    for i = 1:num_vars
        def_use = all_def_uses.(all_vars{i});
        num_uses = numel(def_use.uses);
        def = def_use.def;
        def_index = node_index_map(def.id.index);
        %fprintf('%d def index = %d, a_d = %d, y_d = %d\n', def.id.index, def_index, (2*def_index)-1, 2*def_index);
        % add cost for def node
        a_d = (2*def_index)-1;
        y_d = (2*def_index);
        
        %        a_d + y_d >= 1  
        % =>    -a_d - y_d <= -1
        A(row, a_d) = -1;
        A(row, y_d)  = -1;
        b(row) = -1;
        row = row + 1;
        % note: we don't bother with array weight here because that only applies to conversions
        f(a_d) = get_cost(protocol1, def.node_type, def.parallel_param, json_costs) * def.weight;
        f(y_d) = get_cost(protocol2, def.node_type, def.parallel_param, json_costs) * def.weight;
        % debug output
        %fprintf('def: %s, type = %s, cost(%s) = %10d, cost(%s) = %10d\n', def.id.unit, ...
        %    def.node_type, protocol1, f(a_d), protocol2, f(y_d));
        
        a2y_edges = zeros(num_uses, 1);
        y2a_edges = zeros(num_uses, 1);
        for j = 1:num_uses
            use = def_use.uses(j);
            use_index = node_index_map(use.id.index);
            %fprintf('%d use index = %d, a_u = %d, y_y = %d\n', use.id.index, use_index, (2*use_index)-1, 2*use_index);
            a_u = (2*use_index)-1;
            y_u = (2*use_index);
            
            %        a_u + y_u >= 1  
            % =>    -a_u - y_u <= -1
            A(row, a_u) = -1;
            A(row, y_u)  = -1;
            b(row) = -1;
            row = row + 1;
            % note: we don't bother with array weight here because that only applies to conversions
            f(a_u) = get_cost(protocol1, use.node_type, use.parallel_param, json_costs) * use.weight;
            f(y_u) = get_cost(protocol2, use.node_type, use.parallel_param, json_costs) * use.weight;
            
            % debug output
            %fprintf('use: %s, type = %s, cost(%s) = %10d, cost(%s) = %10d\n', use.id.unit, ...
            %    use.node_type, protocol1, f(a_u), protocol2, f(y_u));
           
            % conversion constraints
            a2y_e = (2 * num_nodes) + col;
            a2y_edges(j) = a2y_e;
            % def is the one that gets converted, hence we use def.array_weight
            a2y_e_cost = get_conversion_cost(protocol1, protocol2, def.array_weight, ...
                use.conversion_parallel_param, json_costs);
            f(a2y_e) =  a2y_e_cost * use.conversion_weight * def.array_weight;
            col = col + 1;
            
            y2a_e = (2 * num_nodes) + col;
            y2a_edges(j) = y2a_e;
            % def is the one that gets converted, hence we use def.array_weight
            y2a_e_cost = get_conversion_cost(protocol2, protocol1, def.array_weight, ...
                use.conversion_parallel_param, json_costs);
            f(y2a_e) =  y2a_e_cost * use.conversion_weight * def.array_weight;
            col = col + 1;
            
            % debug output
            %fprintf('array_weight: %10d, conversion_parallel_param: %10d, conversion_weight: %10d\n' ...
            %    ,def.array_weight, use.conversion_parallel_param, use.conversion_weight); 
            %fprintf('edge: %s2%s = %10d, total: %10d. \n', protocol1, ...
            %    protocol2, a2y_e_cost, f(a2y_e));
            %fprintf('edge: %s2%s = %10d, total: %10d. \n', protocol2, ...
            %    protocol1, y2a_e_cost, f(y2a_e));
            %fprintf('edge: %s2%s = %d, %s2%s = %d\n', protocol1, protocol2, f(a2y_e), ...
            %    protocol2, protocol1, f(y2a_e));
            
            %                   y2a_e >= a_u - a_d  
            % =>    y2a_e - a_u + a_d >= 0
            % =>   -y2a_e + a_u - a_d <= 0
            % y2a_e is representative of all edges so far in y2a_edges
            for ei = 1:j
                A(row, y2a_edges(ei)) = -1;
            end
            A(row, a_u)   =  1;
            A(row, a_d)   = -1;
            b(row) = 0;
            row = row + 1;
            
            %                   a2y_e >= y_u - y_d
            % =>    a2y_e - y_u + y_d >= 0
            % =>   -a2y_e + y_u - y_d <= 0
            % a2y_e is representative of all edges so far in a2y_edges
            for ei = 1:j
                A(row, a2y_edges(ei)) = -1;
            end
            A(row, y_u)   =  1;
            A(row, y_d)   = -1;
            b(row) = 0;
            row = row + 1;
        end
    end

    %% Solve IP
    [x, fval] = intlinprog(f, intcon, A, b, A_eq, b_eq, lb, ub);
end

function [summary_assignment, summary_cost, num_conversions] = print_output(x, fval, protocol1, protocol2, all_def_uses, all_vars, nodes_vector, node_lookup, json_costs)
    num_vars = numel(all_vars);
    num_nodes = length(nodes_vector);
    fprintf('\n------------- OUTPUT ----------------\n')
    fprintf('Total Cost of the Program: %f', fval);
    fprintf('\nNODES:\n');
    protocol1_insts = 0;
    protocol2_insts = 0;
    global non_mpc_node_cost;
    num_gates = 0;
    for k = 1:num_nodes
        %fprintf('nodes_vector[%d] = %d, a: %d, u: %d\n', k, nodes_vector(k), 2*k-1, 2*k);
        a_k = (2*k) - 1;
        y_k = (2*k);
        
        node_idx = nodes_vector(k);
        node = node_lookup(node_idx);
        num_gates = num_gates + node.weight;
        method = '';
        node_cost = 0;
        if(x(a_k)==1 && x(y_k)==1)
            method = protocol1 + " AND " + protocol2; 
            node_cost1 = get_cost(protocol1, node.node_type, node.parallel_param, json_costs);
            node_cost2 = get_cost(protocol2, node.node_type, node.parallel_param, json_costs);
            if(node_cost1 ~= non_mpc_node_cost && node_cost2 ~= non_mpc_node_cost) % non mpc nodes should not count towards assignment
                protocol1_insts = protocol1_insts + 1;
                protocol2_insts = protocol2_insts + 1;
                node_cost = (node_cost1 + node_cost2) * node.weight;
            end
        elseif(x(a_k)==1)
            method = "      " + protocol1;
            node_cost1 = get_cost(protocol1, node.node_type, node.parallel_param, json_costs);
            if(node_cost1 ~= non_mpc_node_cost) % non mpc nodes should not count towards assignment
                protocol1_insts = protocol1_insts + 1;
                node_cost = node_cost1 * node.weight;
            end
        elseif(x(y_k) == 1)
            method = "      " + protocol2;
            node_cost2 = get_cost(protocol2, node.node_type, node.parallel_param, json_costs);
            if(node_cost2 ~= non_mpc_node_cost) % non mpc nodes should not count towards assignment
                protocol2_insts = protocol2_insts + 1;
                node_cost = node_cost2 * node.weight;
            end
        end
        
        if(node_cost > 0)
            fprintf('%s: [%d:%s, cost: %d, w: %d, par:%d] %s\n', method, node_idx, node.node_type, node_cost, node.weight, node.parallel_param, node.unit);
        end
    end
    fprintf('Total Nodes (Linearized) %d \n', num_gates);
    fprintf('\nEDGES:\n');
    col = 1;
    conversions_needed = 0;
    for i = 1:num_vars
        def_use = all_def_uses.(all_vars{i});
        num_uses = numel(def_use.uses);
        def = def_use.def;
        def_cost = get_cost(protocol1, def.node_type, def.parallel_param, json_costs);
        for j = 1:num_uses
            use = def_use.uses(j);
            a2y_e = (2 * num_nodes) + col;
            col = col + 1;
            y2a_e = (2 * num_nodes) + col;
            col = col + 1;
            
            conversion = '';
    %         if(x(a2y_e) == 0 && x(y2a_e) == 0)
    %             conversion = 'NONE';
            if(x(a2y_e) == 1)
                % non mpc node such as pseudo-phi and assignment do propagate def use chains, therefore, their def might need
                % conversion from one sharing to the other
                % if(def_cost == non_mpc_node_cost)
                %     error('\nERROR: %s should not need conversion (non-mpc-node)\n', def.id.unit);
                % end
                conversions_needed = conversions_needed + 1;
                conversion = protocol1 + "2" + protocol2;
                conversion_cost = get_conversion_cost(protocol1, protocol2, def.array_weight, ...
                        use.conversion_parallel_param, json_costs);
                conversion_cost_total = conversion_cost * use.conversion_weight * def.array_weight;
                fprintf('%s:    for def: %s put conversion before: %s\n', conversion, ...
                def.id.unit, use.conversion_point.unit);
                fprintf('conversion_cost: %s, use.conversion_parallel_param: %s, def.array_weight: %s, use.conversion_weight: %s \n', ...
                    num2str(conversion_cost), num2str(use.conversion_parallel_param), num2str(def.array_weight), ...
                    num2str(use.conversion_weight));
                fprintf('conversion_cost_total = %s (convesrion_cost * def.array_weight * use.conversion_weight)\n\n', ...
                    num2str(conversion_cost_total));
            end
            if(x(y2a_e) == 1)
                % non mpc node such as pseudo-phi and assignment do propagate def use chains, therefore, their def might need
                % conversion from one sharing to the other
                % if(def_cost == non_mpc_node_cost)
                %     error('\nERROR: %s should not need conversion (non-mpc-node)\n', def.id.unit);
                % end
                conversions_needed = conversions_needed + 1;
                conversion = protocol2 + "2" + protocol1;
                conversion_cost = get_conversion_cost(protocol2, protocol1, def.array_weight, ...
                        use.conversion_parallel_param, json_costs);
                conversion_cost_total = conversion_cost * use.conversion_weight * def.array_weight;
                fprintf('%s:    for def: %s put conversion before: %s\n', conversion, ...
                def.id.unit, use.conversion_point.unit);
                fprintf('conversion_cost: %s, use.conversion_parallel_param: %s, def.array_weight: %s, use.conversion_weight: %s \n', ...
                    num2str(conversion_cost), num2str(use.conversion_parallel_param), num2str(def.array_weight), ...
                    num2str(use.conversion_weight));
                fprintf('conversion_cost_total = %s (convesrion_cost * def.array_weight * use.conversion_weight)\n\n', ...
                    num2str(conversion_cost_total));
            end
        end
    end
    if(conversions_needed == 0)
        fprintf("NO CONVERSIONS NEEDED \n");
    end

    fprintf("\n\nSummary:\n")
    abbrev_assignment = '';
    if(protocol1_insts == 0)
        abbrev_assignment = protocol2 + " only";
    end

    if(protocol2_insts == 0)
        abbrev_assignment = protocol1 + " only";
    end

    if(protocol1_insts ~= 0 && protocol2_insts ~= 0)
        abbrev_assignment = protocol1 + "+" + protocol2;
    end

    nf = java.text.DecimalFormat;
    cost_str = char(nf.format(fval));
    summary_assignment = abbrev_assignment;
    summary_cost = cost_str;
    num_conversions = conversions_needed;


    fprintf('\tAssignment: %s (%d conversions)\n', abbrev_assignment, conversions_needed);
    fprintf('\tTotal MPC Nodes: %d\n', protocol1_insts + protocol2_insts);
    fprintf('\t %s Nodes: %d\n', protocol1, protocol1_insts);
    fprintf('\t %s Nodes: %d\n', protocol2, protocol2_insts);
    fprintf('\tCost: %s\n\n', cost_str);
end

%% Costs Functions (for Uniform Parallelization)

% returns cost for a single execution
% return value should be multiplied, if needed, with weight by the caller 
function cost = get_cost(protocol, node_type, parallel_param, json_costs)
    global infinite_cost_protocol;
    
    if(protocol == infinite_cost_protocol)
        cost = cast(Inf, 'uint64');
    else
        cost1 = get_cost2(protocol, node_type, parallel_param, json_costs);
        cost = cast(cost1, 'uint64');
    end
end
function cost = get_cost2(protocol, node_type, parallel_param, json_costs)
    global non_mpc_node_cost;
    if(node_type == "IN" || node_type == "OUT" || node_type == "PSEUDO_PHI") % special nodes
        cost = non_mpc_node_cost;
        return;
    end
    if(node_type == "OTHER" || node_type == "SIMPLE_ASSIGN")
        cost = non_mpc_node_cost;
        return;
    end
    
    % 		OTHER(0),
    % 		// AbstractStmtSwitch
    % 		SIMPLE_ASSIGN(1),
    % 		// AbstractJimpleValueSwitch
    % 		ADD(201), AND(202), CMP(203), DIV(204), EQ(205), GE(206), GT(207), LE(208), LT(209), MUL(210), NE(211), OR(
    % 				212), REM(213), SHL(214), SHR(215), SUB(216), USHR(217), XOR(218), NEG(219)
    % 		// AbstractShimpleValueSwitch
    % 		MUX(301);
    costs_map = containers.Map('KeyType', 'char', 'ValueType', 'char');
    costs_map('ADD') = 'add';
    costs_map('AND') = 'and';
    costs_map('CMP') = ''; % not supported
    costs_map('DIV') = ''; % not supported
    costs_map('EQ') = 'eq';
    costs_map('GE') = 'ge';
    costs_map('GT') = 'gt';
    costs_map('LE') = 'le';
    costs_map('LT') = 'lt';
    costs_map('MUL') = 'mul';
    costs_map('NE') = 'ne';
    costs_map('OR') = 'or';
    costs_map('REM') = 'rem';
    costs_map('SHL') = 'shl';
    costs_map('SHR') = 'shr'; % signed shift is not supported, we'll just assume this is an unsigned shift
    costs_map('SUB') = 'sub';
    costs_map('USHR') = 'shr';
    costs_map('XOR') = 'xor';
    costs_map('NEG') = ''; % not supported
    costs_map('MUX') = 'mux';
    
    if(isKey(costs_map, node_type) == false)
        error('%s is not a key in costs_map\n', node_type);
    end
    
    if(costs_map(node_type) == "")
        error('Error. \ncannot find nodetype: %s in costs_map.\n',node_type);
    end
    
    
    json_field = costs_map(node_type);
    %fprintf('%s -> %s\n', node_type, json_field);
    
    if(isfield(json_costs, json_field) == false)
        error('%s is NOT a field in json costs\n', json_field);
    end
    
    if(isfield(json_costs.(json_field), protocol) == false)
        %fprintf('protocol %s does not implement %s, assigning infinite cost\n', protocol, json_field);
        cost = Inf;
        return;
    end
    
    costs_array = json_costs.(json_field).(protocol);
    cost = get_appropriate_cost(parallel_param, costs_array);
end

function cost = get_conversion_cost(from_protocol, to_protocol, array_weight, ...
    conversion_parallel_param, json_costs)
    global infinite_cost_protocol;
    if(from_protocol == infinite_cost_protocol || to_protocol == infinite_cost_protocol)
        cost = cast(Inf, 'uint64');
        return;
    end
    conversion_costs_map = containers.Map('KeyType', 'char', 'ValueType', 'char');
    conversion_costs_map('a2b') = 'a2b';
    conversion_costs_map('a2y') = 'a2y';
    conversion_costs_map('b2a') = 'b2a';
    conversion_costs_map('b2y') = 'b2y';
    conversion_costs_map('y2a') = 'y2a';
    conversion_costs_map('y2b') = 'y2b';
    
    key = char(from_protocol + "2" + to_protocol);
    if(isKey(conversion_costs_map, key) == false)
        error('%s is not a key in conversion_costs_map\n', key);
    end
    
    field = conversion_costs_map(key);
    %fprintf('%s -> %s\n', key, field);
    
    if(isfield(json_costs, field) == false)
        error('%s is NOT a field in json costs\n', field);
    end
    
    costs_array = json_costs.(field);
    cost = get_appropriate_cost(conversion_parallel_param * array_weight, costs_array);
end

function cost = get_appropriate_cost(parallel_param, costs_array)
    if(parallel_param == 1)
        %display(costs_array);
        cost = costs_array.('x1');
        %fprintf('node is not parallelizable, therefore cost is %s\n', cost);
        return
    end
    
    weight = parallel_param;
    % now we have an array to go through     
    ns = sort(str2num(char(extractAfter(fields(costs_array), "x"))));
    ns_count = length(ns);     
    % extract this_n, if weight is equal to this_n, return it
    % else if weight is greater than it:
    %   - on first iteration, just return the this_n
    %   - otherwise
    %       average prev_n and this_n
    % if weight is greater than average, return prev_n
    % otherwise, this n
    % otherwise assign this_n to prev_n and continue
    
    % handle maximum n
    
    this_n = ns(ns_count);
    if(weight >= this_n)
        cost = costs_array.("x"+this_n);
        %fprintf("weight: %d, n: %d\n", weight, this_n);
        return;
    end
    
    for i = (ns_count-1):-1:1
        prev_n = this_n;
        this_n = ns(i);
        if(weight == this_n)
            cost = costs_array.("x"+this_n);
            %fprintf("weight: %d, n: %d\n", weight, this_n);
            return
        end
        if(weight > this_n)
            avg = (this_n + prev_n) / 2;
            if(weight > avg)
                cost = costs_array.("x"+prev_n);
                %fprintf("weight: %d, n: %d\n", weight, prev_n);
                return
            else
                cost = costs_array.("x"+this_n);
                %fprintf("weight: %d, n: %d\n", weight, this_n);
                return
            end
        end
    end
    %fprintf('returning default cost\n');
    cost = costs_array.('x1');
end
