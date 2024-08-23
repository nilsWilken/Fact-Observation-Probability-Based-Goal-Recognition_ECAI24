(define (domain campus)
	
	(:requirements :strips :typing)
	(:types place)

	(:constants
		bank watson_theater hayman_theater davis_theater jones_theater
		bookmark_cafe library cbs psychology_bldg angazi_cafe tav - place
	)
	(:predicates 
		(AT ?p - place )
		(banking)
		(lecture-1-taken)
		(lecture-2-taken)
		(lecture-3-taken)
		(lecture-4-taken)
		(group-meeting-1)
		(group-meeting-2)
		(group-meeting-3)
		(coffee)
		(breakfast)
		(lunch)
	)

	(:functions
		(total-cost) - number
	)
	(:action MOVE
		:parameters( ?src - place ?dst - place )
		:precondition (and (AT ?src ) )
		:effect ( and
				(AT ?dst)
				(increase (total-cost) 1)
				(not (AT ?src))
			)
	)
	(:action ACTIVITY-BANKING
		:parameters()
		:precondition (and (AT bank))
		:effect (and
				(banking)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-TAKE-LECTURE-1
		:parameters()
		:precondition (and (AT watson_theater))
		:effect (and
				(lecture-1-taken)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-TAKE-LECTURE-2
		:parameters()
		:precondition (and (AT hayman_theater) (breakfast) (lecture-1-taken))
		:effect (and
				(lecture-2-taken)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-TAKE-LECTURE-3
		:parameters()
		:precondition (and (AT davis_theater) (group-meeting-2) (banking))
		:effect	(and
				(lecture-3-taken)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-TAKE-LECTURE-4
		:parameters()
		:precondition (and (AT jones_theater) (lecture-3-taken))
		:effect (and
				(lecture-4-taken)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-GROUP-MEETING-1
		:parameters()
		:precondition (and (AT bookmark_cafe) (lecture-1-taken) (breakfast))
		:effect (and
				(group-meeting-1)
				(increase (total-cost) 1)
			) 
	)
	(:action ACTIVITY-GROUP-MEETING-1
		:parameters()
		:precondition (and (AT library) (lecture-1-taken) (breakfast))
		:effect (and
				(group-meeting-1)
				(increase (total-cost) 1)
			) 
	)
	(:action ACTIVITY-GROUP-MEETING-1
		:parameters()
		:precondition (and (AT cbs) (lecture-1-taken) (breakfast))
		:effect (and
				(group-meeting-1)
				(increase (total-cost) 1)
			) 
	)
	(:action ACTIVITY-GROUP-MEETING-2
		:parameters()
		:precondition (and (AT library))
		:effect (and
				(group-meeting-2)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-GROUP-MEETING-2
		:parameters()
		:precondition (and (AT cbs))
		:effect (and
				(group-meeting-2)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-GROUP-MEETING-2
		:parameters()
		:precondition (and (AT psychology_bldg))
		:effect (and
				(group-meeting-2)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-GROUP-MEETING-3
		:parameters()
		:precondition (and (AT angazi_cafe) (lecture-4-taken))
		:effect (and
				(group-meeting-3)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-GROUP-MEETING-3
		:parameters()
		:precondition (and (AT psychology_bldg) (lecture-4-taken))
		:effect (and
				(group-meeting-3)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-COFFEE
		:parameters()
		:precondition (and (AT tav) (lecture-2-taken) (group-meeting-1))
		:effect (and
				(coffee)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-COFFEE
		:parameters ()
		:precondition (and (AT angazi_cafe) (lecture-2-taken) (group-meeting-1))
		:effect (and
				(coffee)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-COFFEE
		:parameters ()
		:precondition (and (AT bookmark_cafe) (lecture-2-taken) (group-meeting-1))
		:effect (and
				(coffee)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-BREAKFAST
		:parameters()
		:precondition (and (AT tav))
		:effect (and
				(breakfast)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-BREAKFAST
		:parameters ()
		:precondition (and (AT angazi_cafe))
		:effect (and
				(breakfast)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-BREAKFAST
		:parameters ()
		:precondition (and (AT bookmark_cafe))
		:effect (and
				(breakfast)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-LUNCH
		:parameters ()
		:precondition (and (AT tav))
		:effect (and
				(lunch)
				(increase (total-cost) 1)
			)
	)
	(:action ACTIVITY-LUNCH
		:parameters ()
		:precondition (and (AT bookmark_cafe))
		:effect (and
				(lunch)
				(increase (total-cost) 1)
			)
	)
)
