getindex(H, [H | T],StartingIndex,StartingIndex).
getindex(H, [X | T],StartingIndex,ElementIndex) :-
%    next_number(StartingIndex,NextIndex),
    NextIndex is StartingIndex +1,
	getindex(H,T,NextIndex,ElementIndex).