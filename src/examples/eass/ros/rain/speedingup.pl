%%%helpers
%%%ismember
ismember(H, [H | T]).
ismember(H, [X | T]) :-
	ismember(H,T).

getindex(H, [H | T],StartingIndex,StartingIndex).
getindex(H, [X | T],StartingIndex,ElementIndex) :-
    next_number(StartingIndex,NextIndex),
	getindex(H,T,NextIndex,ElementIndex).

append([],Ltwo,Ltwo).
append([HLone | Tlone],Ltwo,[HLone | LoneRest]):-
    append(Tlone,Ltwo,LoneRest).


insert_in_list_next_to(LocElem,InsertElem,[],[]).
insert_in_list_next_to(LocElem,InsertElem,[LocElem | T],[LocElem | NewT]):-
    add_to_list(InsertElem,T,NewT).
insert_in_list_next_to(LocElem,InsertElem,[H | T],[H | Rest]):-
    insert_in_list_next_to(LocElem,InsertElem,T,Rest).
% find all the specs in not goals list that
% b or v annots do not satisfy
specs_in_not_goals_not_satisfied([],Vannots,Bannots,[]).

specs_in_not_goals_not_satisfied([avoid(Loc) | T],Vannots,Bannots,[avoid(Loc) | NotSatisfied]):-
    unknown(avoid(Loc)),
    ismember(visit(Loc,Tl),Vannots),
    specs_in_not_goals_not_satisfied(T,Vannots,Bannots,NotSatisfied).

specs_in_not_goals_not_satisfied([avoid(Loc) | T],Vannots,Bannots,NotSatisfied):-
    specs_in_not_goals_not_satisfied(T,Vannots,Bannots,NotSatisfied).

specs_in_not_goals_not_satisfied([before(Bone,Btwo) | T],Vannots,Bannots,[before(Bone,Btwo) |NotSatisfied]):-
    unknown(before(Bone,Btwo)),
    \+ismember(before(Bone,Btwo),Bannots),
    specs_in_not_goals_not_satisfied(T,Vannots,Bannots,NotSatisfied).

specs_in_not_goals_not_satisfied([before(Bone,Btwo) | T],Vannots,Bannots,NotSatisfied):-
    specs_in_not_goals_not_satisfied(T,Vannots,Bannots,NotSatisfied).

specs_in_not_goals_not_satisfied([H|T],Vannots,Bannots,NotSatisfied):-
        specs_in_not_goals_not_satisfied(T,Vannots,Bannots,NotSatisfied).


%%%unique
getunique([],[]).
getunique([H | T],Ul):-
    ismember(H,T),
    getunique(T,Ul).
getunique([H | T], [H | Ul]):-
    getunique(T,Ul).
%%%combine specifications
combined_specifications(CSpecs):-
    specification("Required",RSpec),
    specification("Preferred",PSpec),
    append(RSpec,PSpec,CSpecs).

update_visit_time_from_time(StartTime,TimeIndex,[],[]).
update_visit_time_from_time(StartTime,StartTime,[visit(Loc,StartTime) | Tail],[visit(Loc,NewTimeIndex) | Rest]):-
    next_number(StartTime,NewTimeIndex),
    update_visit_time_from_time(StartTime,NewTimeIndex,Tail,Rest).
update_visit_time_from_time(StartTime,StartTime,[visit(Loc,TimeIndex) | Tail],[visit(Loc,TimeIndex) | Rest]):-
    update_visit_time_from_time(StartTime,StartTime,Tail,Rest).
update_visit_time_from_time(StartTime,TimeIndex,[visit(Loc,TimeIndex)| Tail],[visit(Loc,NewTimeIndex) | Rest]):-
    next_number(TimeIndex,NewTimeIndex),
    update_visit_time_from_time(StartTime,TimeIndex,Tail,Rest).

replace_first_visit_in_visitslist(Loc,TimeIndex,NewTimeIndex,[],[]).
replace_first_visit_in_visitslist(Loc,TimeIndex,NewTimeIndex,[visit(Loc,TimeIndex) | T],[visit(Loc,NewTimeIndex) | T]).
replace_first_visit_in_visitslist(Loc,TimeIndex,NewTimeIndex,[H | T],[H | NewTail]):-
    replace_first_visit_in_visitslist(Loc,TimeIndex,NewTimeIndex,T,NewTail).

replace_first_plan_in_plans(Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,[],[]).
replace_first_plan_in_plans(Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,[plan(Goal,Source,Index,Actions,Vannots,Bannots) | T],[plan(Goal,Source,Index,Actions,UVannots,UBannots) | T]).
replace_first_plan_in_plans(Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,[H | T],[H | Rest]):-
    replace_first_plan_in_plans(Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,T,Rest).

%%% step 1
%%% strip locs from specs
get_second_location_from_before(before(Lone,Ltwo),Ltwo).
strip_locations_from_specs_wrapper(CSpecs,Locs):-
    strip_locations_from_specs(CSpecs,LocsNotUnique),
    getunique(LocsNotUnique,Locs).

strip_locations_from_spec(visit(Loc),[Loc]).
strip_locations_from_spec(avoid(Loc),[Loc]).
strip_locations_from_spec(before(Bone,Btwo),Blist):-
    append([Bone],[Btwo],Blist).
strip_locations_from_spec(maintain(Property),[]).


strip_locations_from_specs([],[]).
strip_locations_from_specs([H | T],LocsN):-
    strip_locations_from_spec(H,SpecLocs),
    strip_locations_from_specs(T,Locs),
    append(SpecLocs,Locs,LocsN).


% step 2
% annotate plans with visit, before
%we have the list of locations we care about
%if we have a move_to/move to something in our list
%we add the visit annotation
get_visit_annotations2(Index,[],Locs,[]).
get_visit_annotations2(Index,[move_to(HLoc)|Tactions],Locs,[visit(HLoc,Index) | OtherAnnots]):-
    ismember(HLoc,Locs),
    next_number(Index,NewIndex),
    get_visit_annotations2(NewIndex,Tactions,Locs,OtherAnnots).
get_visit_annotations2(Index,[move_to(HLoc)|Tactions],Locs, OtherAnnots):-
    next_number(Index,NewIndex),
    get_visit_annotations2(NewIndex,Tactions,Locs,OtherAnnots).

get_visit_annotationsInit(at(InitLoc),Actions,Locs,[visit(InitLoc,0) | OtherAnnots]):-
    ismember(InitLoc,Locs),
    get_visit_annotations2(1,Actions,Locs,OtherAnnots).

get_visit_annotationsInit(at(InitLoc),Actions,Locs,OtherAnnots):-
    get_visit_annotations2(1,Actions,Locs,OtherAnnots).


get_visit_annotations(Actions,[],[]).
get_visit_annotations(Actions,[Hloc | Tloc], [visit(Hloc,Index) | OtherAnnots]):-
    getindex(move_to(Hloc),Actions,1,Index),
    get_visit_annotations(Actions,Tloc,OtherAnnots).
get_visit_annotations(Actions,[Hloc | Tloc],  OtherAnnots):-
    get_visit_annotations(Actions,Tloc,OtherAnnots).
get_visit_annotations(at(InitLoc),Actions,Locs,[visit(InitLoc,0) | OtherAnnots]):-
    ismember(InitLoc,Locs),
    get_visit_annotations(Actions,Locs,OtherAnnots).
get_visit_annotations(at(InitLoc),Actions,Locs, OtherAnnots):-
    get_visit_annotations(Actions,Locs,OtherAnnots).

update_visit_annotations_for_loc(Loc,Actions,OldVisits,[visit(Loc,Index) | OldVisits]):-
    getindex(move_to(Loc),Actions,1,Index).
update_visit_annotations_for_loc(Loc,Actions,OldVisits, OldVisits).

get_loc_index_in_actions(Loc,Actions,ActionIndex):-
    getindex(move_to(Loc),Actions,1,ActionIndex).


get_list_first_elem([H | T],H).
get_list_second_elem([H | T], Second):-
    get_list_first_elem(T,Second).

parse_before(before(BList),B1,B2):-
    get_list_first_elem(BList,B1),
    get_list_second_elem(BList,B2).

% lets go befores from just a list
% so we assume we have a unique list of visits
% with indices
generate_befores_for_plan([],VisitsList,[]).

generate_befores_for_plan([before(Bone,Btwo) | T],VisitsList,PlanBeforesList):-
    visited(Btwo,VBtwo),
    generate_befores_for_plan(T,VisitsList,PlanBeforesList).

generate_befores_for_plan([before(Bone,Btwo) | T],VisitsList,[before(Bone,Btwo) | PlanBeforesList]):-
    visited(Bone,VBone),
    generate_befores_for_plan(T,VisitsList,PlanBeforesList).

generate_befores_for_plan([before(Bone,Btwo) | T],VisitsList,[before(Bone,Btwo) |PlanBeforesList]):-
    \+ismember(visit(Btwo,Pbtwo),VisitsList),
    generate_befores_for_plan(T,VisitsList,PlanBeforesList).

generate_befores_for_plan([before(Bone,Btwo) | T],VisitsList,[before(Bone,Btwo) |PlanBeforesList]):-
    ismember(visit(Btwo,Pbtwo),VisitsList),
    ismember(visit(Bone,Pbone),VisitsList),
    Pbone < Pbtwo,
    generate_befores_for_plan(T,VisitsList,PlanBeforesList).

generate_befores_for_plan([before(Bone,Btwo) | T],VisitsList,PlanBeforesList):-
    generate_befores_for_plan(T,VisitsList,PlanBeforesList).

%just using the knowledge we have
%generate a befores list from the locations we care about
before_satisfied(Locone,Loctwo):-
    visited(Loctwo,Tltwo),
    visited(Locone,Tlone),
    Tlone < Tltwo.

before_satisfied(Locone,Loctwo):-
    \+visited(Loctwo,Tltwo),
    visited(Locone,Tlone).

before_violated(Locone,Loctwo):-
    visited(Locone,Tlone),
    visited(Loctwo,Tltwo),
    Tltwo < Tlone.

before_violated(Locone,Loctwo):-
    \+visited(Locone,Tlone),
    visited(Loctwo,Tltwo).

before_unknown(Locone,Loctwo):-
    \+before_satisfied(Locone,Loctwo),
    \+before_violated(Locone,Loctwo).

avoid_violated(Loc):-
    visited(Loc,Tl).

avoid_unknown(Loc):-
    \+avoid_violated(Loc).

visit_satisfied(Loc):-
    visited(Loc,Tl).

visit_unknown(Loc):-
    \+visit_satisfied(Loc).

unknown(before(Bone,Btwo)):-
    before_unknown(Bone,Btwo).

unknown(avoid(Loc)):-
    avoid_unknown(Loc).

unknown(visit(Loc)):-
    visit_unknown(Loc).

unknown(maintain(Property)).

satisfied(before(Bone,Btwo)):-
    before_satisfied(Bone,Btwo).

satisfied(avoid(Loc)):-
    \+visited(Loc, Tl).

satisfied(visit(Loc)):-
    visit_satisfied(Loc).

violated(before(Bone,Btwo)):-
    before_violated(Bone,Btwo).

violated(avoid(Loc)):-
    avoid_violated(Loc).


befores_for_locs(Marker,[],Others,[]).

befores_for_locs(Marker,[HLoc | TLocs],[],OtherBefores):-
    befores_for_locs(first,TLocs, TLocs,OtherBefores).

befores_for_locs(Marker,[HLoc | TLocs],[HLoc | OTLocs],OtherBefores):-
    befores_for_locs(Marker,[HLoc | TLocs], OTLocs,OtherBefores).

befores_for_locs(first,[HLoc | TLocs],[HTLoc | OTLocs],[before(HLoc,HTLoc) | OtherBefores]):-
    befores_for_locs(second,[HLoc | TLocs],[HTLoc | OTLocs],OtherBefores).

befores_for_locs(second,[HLoc | TLocs],[HTLoc | OTLocs],[before(HTLoc,HLoc) | OtherBefores]):-
    befores_for_locs(first,[HLoc | TLocs],OTLocs,OtherBefores).

generate_all_possible_befores_wrapper(Locs,Locs,AllBefores):-
    befores_for_locs(first,Locs,Locs,AllBefores).


generate_befores_from_beliefs([],[],[],[]).
generate_befores_from_beliefs([before(Bone,Btwo) | T],UnknownBefores,[before(Bone,Btwo) | SatisfiedBefores],ViolatedBefores):-
    before_satisfied(Bone,Btwo),
    generate_befores_from_beliefs(T,UnknownBefores,SatisfiedBefores,ViolatedBefores).
generate_befores_from_beliefs([before(Bone,Btwo) | T],UnknownBefores,SatisfiedBefores,[before(Bone,Btwo) | ViolatedBefores]):-
    before_violated(Bone,Btwo),
    generate_befores_from_beliefs(T,UnknownBefores,SatisfiedBefores,ViolatedBefores).

generate_befores_from_beliefs([before(Bone,Btwo) | T],[before(Bone,Btwo)  | UnknownBefores],SatisfiedBefores,ViolatedBefores):-
    generate_befores_from_beliefs(T,UnknownBefores,SatisfiedBefores,ViolatedBefores).

%so for each location, check whether the before has been satisfied, violated or unknown
generate_befores_from_beliefs(Locs,UnknownBefores,SatisfiedBefores,ViolatedBefores,AllPossibleBefores):-
    generate_all_possible_befores_wrapper(Locs,Locs,AllPossibleBefores),
    generate_befores_from_beliefs(AllPossibleBefores,UnknownBefores,SatisfiedBefores,ViolatedBefores).

update_befores_from_beliefs(CurrentlyUnknownBefores,CurrentlySatB,CurrentViolB,UnknownBefores,SatisfiedBefores,ViolatedBefores):-
    generate_befores_from_beliefs(CurrentlyUnknownBefores,UnknownBefores,SatisfiedBeforesN,ViolatedBeforesN),
    append(SatisfiedBeforesN,CurrentlySatB,SatisfiedBefores),
    append(ViolatedBeforesN,CurrentViolB,ViolatedBefores).

get_annotations(UnknownBefores,at(Loc),Actions,Start,Locs,VisitAnnots,BeforesAnnots):-
    get_visit_annotationsInit(at(Loc),Actions,Locs,VisitAnnots),
    generate_befores_for_plan(UnknownBefores,VisitAnnots,BeforesAnnots).


gather_allplan_goals_wrapper(Plans,GoalsList):-
    gather_allplan_goals(Plans,NonUniqueGoalsList),
    getunique(NonUniqueGoalsList,GoalsList).
gather_allplan_goals([],[]).
gather_allplan_goals([plan(Goal,Source,Index,Actions) | TPlans],[Goal | PGoals]):-
    gather_allplan_goals(TPlans,PGoals).


annotate_all_plans(Start,Locs,AllPossibleBefores,[],[]).
annotate_all_plans(Start,Locs,AllPossibleBefores,[plan(Goal,Source,Index,Actions) | TPlans],[plan(Goal,Source,Index,Actions,VisitAnnots,BeforesAnnots) | TUpdatedPlans]):-
    get_annotations(AllPossibleBefores,Source,Actions,Start,Locs,VisitAnnots,BeforesAnnots),
    annotate_all_plans(Start,Locs,AllPossibleBefores,TPlans,TUpdatedPlans).

update_before_annotations([],[]).
update_before_annotations([plan(Goal,at(Loc),Index,Actions,VisitAnnots,OBeforesAnnots) | TPlans],[plan(Goal,at(Loc),Index,Actions,VisitAnnots,BeforesAnnots) | TUpdatedPlans]):-
    generate_befores_for_plan(OBeforesAnnots,VisitAnnots,BeforesAnnots),
    update_before_annotations(TPlans,TUpdatedPlans).

update_visit_annotations(Loc,[],[]).
update_visit_annotations(Loc,[plan(Goal,at(InitLoc),Index,Actions,OVisitAnnots,BeforesAnnots) | TPlans], [plan(Goal,at(InitLoc),Index,Actions,VisitAnnots,BeforesAnnots) | TUpdatedPlans]):-
    update_visit_annotations_for_loc(Loc,Actions,OVisitAnnots,VisitAnnots),
    update_visit_annotations(Loc,TPlans,TUpdatedPlans).

 replace_first_plan_in_plans_revise_for_loc(Loc,Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,[],[]).
 replace_first_plan_in_plans_revise_for_loc(Loc,Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,[plan(Goal,Source,Index,Actions,Vannots,Bannots) | T],[plan(Goal,Source,Index,Actions,UVannots,UBannots) | Rest]):-
    replace_first_plan_in_plans_revise_for_loc(Loc,Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,T,Rest).
 replace_first_plan_in_plans_revise_for_loc(Loc,Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,[plan(Sgoal,Ssource,Sindex,SActions,Svannots,Sbannots) | T],[plan(Sgoal,Ssource,Sindex,SActions,NSvannots,Sbannots) | Rest]):-
    update_visit_annotations_for_loc(Loc,SActions,Svannots,NSvannots),
     replace_first_plan_in_plans_revise_for_loc(Loc,Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,T,Rest).
%step 3
%now we've annotated our plans
%so we can select plans we want
%the steps are
%we want to get all plans for our current location
%then we want to filter from these plans
%all plans that have goals that are part of our specification
%so given a list of plans, our specification and the location
%we return a list of "valid plans"
plans_from_loc_with_any_specification(Loc,Specs,[],[]).
plans_from_loc_with_any_specification(Loc,Specs,[plan(Goal,at(Loc),Index,Actions,Vannots,Bannots) | PlanT],[plan(Goal,at(Loc),Index,Actions,Vannots,Bannots) | LPlanT]):-
    unknown(Goal),
    ismember(Goal,Specs),
    plans_from_loc_with_any_specification(Loc,Specs,PlanT,LPlanT).
plans_from_loc_with_any_specification(Loc,Specs,[plan(Goal,at(Loctwo),Index,Actions,Vannots,Bannots) | PlanT], LPlanT):-
    plans_from_loc_with_any_specification(Loc,Specs,PlanT,LPlanT).


subtract_secondlist_from_firstlist([],GoalsList,[]).
subtract_secondlist_from_firstlist([HSpec | TSpec],GoalsList,NotGoalsList):-
    ismember(HSpec,GoalsList),
    subtract_secondlist_from_firstlist(TSpec,GoalsList,NotGoalsList).
subtract_secondlist_from_firstlist([HSpec | TSpec],GoalsList,[HSpec | NotGoalsList]):-
    subtract_secondlist_from_firstlist(TSpec,GoalsList,NotGoalsList).


plan_exists(Goal,Source,Index,Actions,Vannots,Bannots):-
    plans(Plans),
    plan_exists(Plans,Goal,Source,Index,Actions,Vannots,Bannots).

plan_exists([plan(Goal,Source,Index,Actions,Vannots,Bannots) | T],Goal,Source,Index,Actions,Vannots,Bannots).
plan_exists([H | T],Goal,Source,Index,Actions,Vannots,Bannots):-
    plan_exists(T,Goal,Source,Index,Actions,Vannots,Bannots).


get_plans_for_spec(S,[],[]).
% avoid is the absence of a visit
% so if you see visit loc then you dont add it
get_plans_for_spec(maintain(Property),Plans,Plans).
get_plans_for_spec(avoid(Loc), [plan(Goal,Source,Ind,Actions,Vannots,Bannots) | TP], Res) :-
	ismember(visit(Loc,Tl),Vannots),
	get_plans_for_spec(avoid(Loc),TP,Res).
%if you dont see visit loc then you can add it
get_plans_for_spec(avoid(Loc),[plan(Goal,Source,Ind,Actions,Vannots,Bannots) | TP], [plan(Goal,Source,Ind,Actions,Vannots,Bannots) | Res]):-
	get_plans_for_spec(avoid(Loc),TP,Res).
%visit is self-explanatory
%see it , add it
get_plans_for_spec(visit(Loc),[plan(Goal,Source,Ind,Actions,Vannots,Bannots) | TP], [plan(Goal,Source,Ind,Actions,Vannots,Bannots) | Res] ):-
    ismember(visit(Loc,Tl),Vannots),
    get_plans_for_spec(visit(Loc),TP,Res).
%dont see it
%dont add it
get_plans_for_spec(visit(Loc),[plan(Goal,Source,Ind,Actions,Vannots,Bannots) | TP], Res):-
	get_plans_for_spec(visit(Loc),TP,Res).

%before is self-explanatory
%see it , add it
get_plans_for_spec(before(Bone,Btwo),[plan(Goal,Source,Ind,Actions,Vannots,Bannots) | TP], [plan(Goal,Source,Ind,Actions,Vannots,Bannots) | Res] ):-
    ismember(before(Bone,Btwo),Bannots),
    get_plans_for_spec(before(Bone,Btwo),TP,Res).
%dont see it
%dont add it
get_plans_for_spec(before(Bone,Btwo),[plan(Goal,Source,Ind,Actions,Vannots,Bannots) | TP], Res):-
	get_plans_for_spec(before(Bone,Btwo),TP,Res).


% now from these plans we want to find valid plans
find_valid_plans(NotGoalsList,Preferred,Plans,PossiblePlans):-
    append(NotGoalsList,Preferred,SpecsForPlans),
    find_valid_plans(SpecsForPlans,Plans,PossiblePlans).

%getting plans with weakening
find_valid_plans([],Plans,Plans).
find_valid_plans([H|T],Plans,PossiblePlans):-
    unknown(H),
	get_plans_for_spec(H,Plans,PlanSet),
	check_plans_for_spec(Plans,PlanSet,NewPlanSet),
	find_valid_plans(T,NewPlanSet,PossiblePlans).

find_valid_plans([H|T],Plans,PossiblePlans):-
	find_valid_plans(T,Plans,PossiblePlans).

check_plans_for_spec(L,[],L).
check_plans_for_spec(L,NL,NL).

get_first_plan([plan(Goal,Source,Index,Actions,Vannots,Bannots) | Rest],plan(Goal,Source,Index,Actions,Vannots,Bannots)).

% what do I do for maintains
% if its a maintain, I just ignore it

specs_unknown_satisfied_violated([],[],[],[]).
specs_unknown_satisfied_violated([visit(Loc)|T],UnknownR,[visit(Loc) | SatisfiedR],ViolatedR):-
    visited(Loc,Tl),
    specs_unknown_satisfied_violated(T,UnknownR,SatisfiedR,ViolatedR).
specs_unknown_satisfied_violated([before(Bone,Btwo)|T],UnknownR,[before(Bone,Btwo) | SatisfiedR],ViolatedR):-
    satisfied(before(Bone,Btwo)),
    specs_unknown_satisfied_violated(T,UnknownR,SatisfiedR,ViolatedR).
specs_unknown_satisfied_violated([before(Bone,Btwo) |T],UnknownR,SatisfiedR,[before(Bone,Btwo) | ViolatedR]):-
    violated(before(Bone,Btwo)),
    specs_unknown_satisfied_violated(T,UnknownR,SatisfiedR,ViolatedR).
specs_unknown_satisfied_violated([avoid(Loc) |T],UnknownR,SatisfiedR,[avoid(Loc) | ViolatedR]):-
    avoid_violated(Loc),
    specs_unknown_satisfied_violated(T,UnknownR,SatisfiedR,ViolatedR).
specs_unknown_satisfied_violated([H|T],[H | UnknownR],SatisfiedR,ViolatedR):-
    unknown(H),
    specs_unknown_satisfied_violated(T,UnknownR,SatisfiedR,ViolatedR).


% given a list of specs, and a percept we want to see if we could violate any of these specs
% can_violate(Near(Percept),Specs,CouldViolate)
% if its a before([l1,l2]) we can violate if percept is l2 and we have not visited l1
% if its an avoid(l1) we can violate if l1 = percept
% if its a visit we cant really violate it so we cool so far
can_violate(near(Loc),[],[]).
can_violate(near(Btwo),[before(Bone,Btwo) | TSpecs],[before(Bone,Btwo) | Rest]):-
%    parse_before(before(Blist),Bone,Loc),
    \+visited(Bone,Tb),
    can_violate(near(Btwo),TSpecs,Rest).

can_violate(near(Btwo),[before(Bone,Btwo) | TSpecs], Rest):-
    can_violate(near(Btwo),TSpecs,Rest).

can_violate(near(Loc),[avoid(Loc) | TSpecs], [avoid(Loc) | Rest]):-
    can_violate(near(Loc),TSpecs,Rest).

can_violate(near(Loc),[H | T],Rest):-
    can_violate(near(Loc),T,Rest).


% to revise your annotations you need the ones it can violate
% we can only violate a before or an avoid
% if its an avoid, we add a visit to our list
% if its a before, we are basically visiting l2 before l1, so we need to add a visit too
% and then just repeat the before annots for this plan
% and then just update our plans

revise_annotations(Loc,near(NextLoc),CurrentPlanIndex,Vannots,Bannots,[visit(NextLoc,NextLocIndex) | UpdatedVannots],UpdatedBannots):-
% insert nextloc at current plan index
    next_number(CurrentPlanIndex,IncrementedIndex),
    update_visit_time_from_time(IncrementedIndex,IncrementedIndex,Vannots,UpdatedVannots),
    next_number(CurrentPlanIndex,NextLocIndex),
    generate_befores_for_plan(Bannots,[visit(NextLoc,NextLocIndex) | UpdatedVannots],UpdatedBannots).


replace_plan_annots_in_plans(Plans,Goal,Source,Index,Actions,UVannots,UBannots,NewPlans):-
    replace_first_plan_in_plans(Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,Plans,NewPlans).

replace_plan_annots_in_plans_revise_for_loc(Loc,Plans,Goal,Source,Index,Actions,UVannots,UBannots,NewPlans):-
    replace_first_plan_in_plans_revise_for_loc(Loc,Goal,Source,Index,Actions,Vannots,Bannots,UVannots,UBannots,Plans,NewPlans).


add_to_list(InsertElem,Oldlist,[InsertElem | Oldlist]).


list_has_no_visits([],[]).
list_has_no_visits([visit(Loc) | Tail],[visit(Loc) | OtherTail]):-
    list_has_no_visits(Tail,OtherTail).
list_has_no_visits([H | Tail],OtherTail):- list_has_no_visits(Tail,OtherTail).

speclist_has_maintain([],[]).
speclist_has_maintain([maintain(Property) | T],[maintain(Property) | Rest]):-
    speclist_has_maintain(T,Rest).
speclist_has_maintain([H | T], Rest):-
    speclist_has_maintain(T,Rest).

next_number(X,Y):-
    Y is X+1.


plans([
	plan(visit(tankzone),at(door), 1, [move_to(foyer), move_to(tankzone)]),
	plan(visit(tank2pipe),at(tankzone), 1, [move_to(tank2face),move_to(tank2),move_to(tank2pipe)]),
	plan(visit(tank2pipe),at(tankzone), 2, [move_to(tank1face), move_to(tank1pipe), move_to(tankset),move_to(tank2pipe)]),
	plan(visit(tank2pipe),at(tankzone), 3, [move_to(foyer), move_to(freezone), move_to(stairs),move_to(farwall),move_to(pipes),move_to(tank1pipe),move_to(tankset),move_to(tank2pipe)]),
	plan(visit(tankset),at(tank2pipe),1,[move_to(tankset)]),
	plan(visit(pipes),at(tankset),1,[move_to(pipes)]),
	plan(visit(pipes),at(tank2pipe),1,[move_to(pipes)])
	]).

%B annotate_all_plans(1,Locs,UnknownBefores,Plans,AnnotatedPlans)

%original_specification("Required",[visit(tankzone),visit(tank2pipe),visit(tankset),visit(pipes),avoid(corridor),maintain(radiation(low))])
%original_specification("Preferred",[before(tankzone,tank2pipe),before(tankzone,tankset),before(tankzone,pipes),before(tank2pipe,pipes),before(tankset,pipes),before(tank2pipe,tankset)])
locations_of_interest([tankzone,tank2pipe,tankset,pipes,corridor]).
unknown_befores_list([before(tankzone,tank2pipe),before(tankzone,tankset),before(tankzone,pipes),before(tank2pipe,pipes),before(tankset,pipes),before(tank2pipe,tankset)]).

teststuff(APs):-
    plans(Plans),
    locations_of_interest(Locs),
    unknown_befores_list(UnknownBefores),
    annotate_all_plans(1,Locs,UnknownBefores,Plans,APs).

visited(nothing,0).