plans([
    plan(visit(l1),at(l0),1,[move_to(l1)],[visit(l1),avoid(l5)]),
    plan(visit(l2),at(l0),1,[move_to(l2)],[visit(l2),avoid(l5)]),
    plan(visit(l3),at(l0),1,[move_to(l3)],[visit(l3),avoid(l5)]),
    plan(visit(l1), at(l2),2,[move_to(l1)],[visit(l1),avoid(l5)]),
    plan(visit(l2),at(l1),2,[move_to(l2)],[visit(l2),avoid(l5)]),
    plan(visit(l3),at(l2),2,[move_to(l3)],[visit(l3),avoid(l5)]),
    plan(visit(l2),at(l1),3,[move_to(l3),move_to(l2)],[visit(l3),avoid(l5)])
]).

specification("Required",[before(l3,l4),visit(l1),visit(l2),visit(l3)]).
specification("Preferred",[avoid(l5),before(l1,l2),before(l2,l3)]).

% a function that returns all plans with the same destination
get_plans_for_initial_location(Loc,[],[]).
get_plans_for_initial_location(Loc,[plan(Goal,at(Loc),Index,Actions,Annotations)|T],
                                [plan(Goal,at(Loc),Index,Actions,Annotations)|PlanSet]):-
                                get_plans_for_initial_location(Loc,T,PlanSet).
get_plans_for_initial_location(Loc,[H|T],PlanSet):-
                                get_plans_for_initial_location(Loc,T,PlanSet).


get_plans_for_goal(Goal,[],[]).
get_plans_for_goal(Goal,[plan(Goal,at(Loc),Index,Actions,Annotations)|T],
                                [plan(Goal,at(Loc),Index,Actions,Annotations)|PlanSet]):-
                                get_plans_for_goal(Goal,T,PlanSet).
get_plans_for_goal(Goal,[H|T],PlanSet):-
                                get_plans_for_goal(Goal,T,PlanSet).


before(l1,l2).

% so if believe you have not visited loc 1 thats when  you call this part
% otherwise you just call the after part because you believe you have visited loc1
% and so nothing else matters
check_before([],before(Loc1,Loc2)).
check_before([move_to(Loc1)|T],before(Loc1,Loc2)):- dif(Loc1,Loc2).
%//            check_before_after(T,before(Loc1,Loc2));
check_before([move_to(Loc2)|T],before(Loc1,Loc2)):- dif(Loc1,Loc2),false.
check_before([move_to(Loc)|T],before(Loc1,Loc2)):-
            dif(Loc,Loc2),
            dif(Loc,Loc1),
            check_before(T,before(Loc1,Loc2)).
%check_before([],before(Loc1,Loc2)).
%check_before([move_to(Loc1)|T],before(Loc1,Loc2)):-
%            check_before_after(T,before(Loc1,Loc2)).
%check_before([move_to(Loc2)|T],before(Loc1,Loc2)):-false.
%check_before([move_to(Loc)|T],before(Loc1,Loc2)):-
%            dif(Loc,Loc2),
%            check_before(T,before(Loc1,Loc2)).
%
%check_before_after([],before(Loc1,Loc2)).
%check_before_after([move_to(Loc2)|T],before(Loc1,Loc2)).
%check_before_after([move_to(Loc)|T],before(Loc1,Loc2)):-
%            dif(Loc,Loc2),
%            dif(Loc,Loc1),
%            check_before_after(T,before(Loc1,Loc2)).

%so now we want to annotate a plan
%given a plan P we go over it to check before
%if it returns true we add the annotation to it
annotate_plan_with_before(plan(Goal,Dest,Index,Actions,Annots),before(Loc1,Loc2)):-
%,before(Loc1,Loc2) ):-
    check_before(Actions,before(Loc1,Loc2)).


%//so you have a plan and the annotations list
%// you just go over each before and annotate it
%// hmmm so we just need a reasoning rule
annotate_plan_with_befores(Plan,[],[]).
annotate_plan_with_befores(Plan,[HBSpecs | TBSpecs],[HBSpecs | UAnnots]):-
%[BAnnot | Annots]):-
    annotate_plan_with_before(Plan,HBSpecs),
%    ,BAnnot),
    annotate_plan_with_befores(Plan,TBSpecs,UAnnots).
annotate_plan_with_befores(Plan,[HBSpecs | TBSpecs], UAnnots):-
    annotate_plan_with_befores(Plan,TBSpecs,UAnnots).

annotate_plans_with_befores([],BSpecs,[]).
annotate_plans_with_befores([plan(Goal,Dest,Index,Actions,Annots) | TPlans],BSpecs,[plan(Goal,Dest,Index,Actions,NewAnnots) | NewPlans]):-
    annotate_plan_with_befores(plan(Goal,Dest,Index,Actions,Annots),BSpecs,AddAnnots),
    append(Annots,AddAnnots,NewAnnots),
    annotate_plans_with_befores(TPlans,BSpecs,NewPlans).

ismember(H, [H | T]).
ismember(H, [X | T]) :-
	ismember(H,T).
%//helper stuff for plans
get_plans_for_spec(S,[],[]).

get_plans_for_spec(S, [plan(G,X,Ind,Actions,HP) | TP], [plan(G,X,Ind,Actions,HP) | Res]) :-
	ismember(S,HP),
	get_plans_for_spec(S,TP,Res).

get_plans_for_spec(S,[plan(G,X,Ind,Actions,HP) | TP], Res) :-
	get_plans_for_spec(S,TP,Res).


%//more helper things
check_plans_for_spec(L,[],L).
check_plans_for_spec(L,NL,NL).

%//getting plans with weakening
get_plans_for_speclist_weakening([],Plans,Plans).

get_plans_for_speclist_weakening([H|T],Plans,PossiblePlans):-
	get_plans_for_spec(H,Plans,PlanSet),
	check_plans_for_spec(Plans,PlanSet,NewPlanSet),
	get_plans_for_speclist_weakening(T,NewPlanSet,PossiblePlans).

%testplanstuff(Annots):-
%annotate_plan_with_befores(plan(visit(l1),at(l0),1,[move_to(l1)],[visit(l1),avoid(l5)]),[before(l3,l4),before(l1,l2),before(l2,l3)],Annots).
%
%
%[plan(visit(l1),at(l0),1,[move_to(l1),],[visit(l1),avoid(l5),before(l3,l4),before(l1,l2),before(l2,l3),]),plan(visit(l2),at(l0),1,[move_to(l2),],[visit(l2),avoid(l5),before(l3,l4),before(l1,l2),before(l2,l3),]),plan(visit(l3),at(l0),1,[move_to(l3),],[visit(l3),avoid(l5),before(l3,l4),before(l1,l2),before(l2,l3),]),]

%[before(l3,l4),visit(l1),avoid(l5),before(l1,l2),before(l2,l3),]
get_plan_goals_list([],[]).
get_plan_goals_list([plan(Goal,Source,Index,Actions,Annots) | TPlans], GoalsList):-
    get_plan_goals_list(TPlans,GoalsList),
    ismember(Goal,GoalsList).
get_plan_goals_list([plan(Goal,Source,Index,Actions,Annots) | TPlans], [Goal |GoalsList]):-
    get_plan_goals_list(TPlans,GoalsList).

break_required_specs_into_goals_and_not_goals([],GoalsList,[]).
break_required_specs_into_goals_and_not_goals([HSpec | TSpec],GoalsList,NotGoalsList):-
    ismember(HSpec,GoalsList),
    break_required_specs_into_goals_and_not_goals(TSpec,GoalsList,NotGoalsList).
break_required_specs_into_goals_and_not_goals([HSpec | TSpec],GoalsList,[HSpec | NotGoalsList]):-
    break_required_specs_into_goals_and_not_goals(TSpec,GoalsList,NotGoalsList).

%testplanstuff(GoalsList,NotGoalsList):-
%    plans(PlanList),
%    specification("Required",RSpecs),
%    get_plan_goals_list(PlanList,GoalsList),
%    break_required_specs_into_goals_and_not_goals(RSpecs,GoalsList,NotGoalsList).

testplanstuff(NewPlans):-
    annotate_plans_with_befores([plan(visit(l1),at(l0),1,[move_to(l1)],[visit(l1),avoid(l5)]),plan(visit(l2),at(l0),1,[move_to(l2)],[visit(l2),avoid(l5)]),plan(visit(l3),at(l0),1,[move_to(l3)],[visit(l3),avoid(l5)])],[before(l3,l4),before(l1,l2),before(l2,l3)],NewPlans).
