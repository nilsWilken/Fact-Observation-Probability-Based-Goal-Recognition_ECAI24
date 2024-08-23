(define (problem logistics-04-0)
(:domain logistics)
(:objects
 apn1 - airplane
 apt1 apt2 - airport
 pos21 pos22 pos23 pos11 pos12 pos13 - location
 cit2 cit1 - city
 tru2 tru1 - truck
 obj23 obj22 obj21 obj13 obj12 obj11 - package)

(:init 
	(AT apn1 apt2) (AT tru1 pos11) (AT tru2 pos22) 
	(AT obj11 pos11) (AT obj12 pos12) (AT obj13 pos13)
	(AT obj21 pos21) (AT obj22 pos22) (AT obj23 pos23)
	(in-city apt1 cit1) (in-city pos11 cit1) (in-city pos12 cit1) (in-city pos13 cit1)
	(in-city apt2 cit2) (in-city pos21 cit2) (in-city pos22 cit2) (in-city pos23 cit2)
)

(:goal (and 
	<HYPOTHESIS>
	)
)
)
