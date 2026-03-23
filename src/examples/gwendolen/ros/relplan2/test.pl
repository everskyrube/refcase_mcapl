ismember(H, [H | T]).
ismember(H, [X | T]) :-
	ismember(H,T).
	
append_([], X, X).
append_([ H | T], X , [ H | W ]) :- 
	append_(T,X,W).

remove_from_list_(Elem,[],[]).
remove_from_list_(Elem,[Elem | L],L).
%remove_from_list_(Elem,[Elem|L],NL):-
%	remove_from_list_(Elem,L,NL).
remove_from_list_(Elem,[NotElem | L],[NotElem | NL]):-
	remove_from_list_(Elem,L,NL).


remove_list_from_list([],L,L).
remove_list_from_list([ToRemoveH | ToRemoveT ],L,NL):-
	remove_from_list_(ToRemoveH,L,NL1),
	remove_list_from_list(ToRemoveT,NL1,NL).
	
remove_annots_from_specs([],Spec,Spec).
remove_annots_from_specs([H | T],Spec, RSpec):-
	ismember(H,Spec),
	remove_from_list_(H,Spec,USpec),
	remove_annots_from_specs(T,USpec,RSpec).
remove_annots_from_specs([H | T],Spec,USpec):-
	remove_annots_from_specs(T,Spec,USpec).

	
get_possible_violations(NotGoalsList,PreferredList,PlanAnnots,PossibleVSpecs):-
	append_(NotGoalsList,PreferredList,SpecsForPlan),
	remove_annots_from_specs(PlanAnnots,SpecsForPlan,PossibleVSpecs).
	
teststuff(PV):-
	get_possible_violations([],[avoid(t2left),before(iP1,t2bottom),before(iP1,tankset),before(iP1,pipes),before(t2bottom,pipes),before(tankset,pipes),before(t2bottom,tankset)],[visit(iP1),avoid(t2left),before(iP1,t2bottom),before(iP1,tankset),before(iP1,pipes),before(tankset,pipes),before(t2bottom,pipes),before(t2bottom,tankset)],PV).