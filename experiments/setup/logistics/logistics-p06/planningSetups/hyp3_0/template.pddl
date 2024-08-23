(define (problem logistics-pb6)
(:domain logistics)
(:objects
 apn1 apn2 apn3 apn4 apn5 apn6 - airplane
 apt1 apt2 apt3 apt4 apt5 apt6 - airport
 pos66 pos55 pos44 pos33 pos21 pos22 pos23 pos11 pos12 pos13 - location
 cit2 cit1 cit3 cit4 cit5 cit6 - city
 tru2 tru1 tru3 tru4 tru5 - truck
 obj55 obj44 obj33 obj23 obj22 obj21 obj13 obj12 obj11 obj00 - package)

(:init 
	(AT apn1 apt2) (AT apn2 apt1) (AT apn3 apt1) (AT tru1 pos11) (AT tru3 pos12) (AT tru4 pos13) (AT tru2 pos22) 
	(AT obj11 pos13) (AT obj12 pos13) (AT obj13 pos11)
	(AT obj21 pos21) (AT obj22 pos21) (AT obj23 pos22)
	(AT obj55 pos13) (AT obj44 pos12) (AT obj33 pos11)
	(in-city apt1 cit1) (in-city pos11 cit1) (in-city pos12 cit1) (in-city pos13 cit1)
	(in-city apt2 cit2) (in-city pos21 cit2) (in-city pos22 cit2) (in-city pos23 cit2)
	(in-city pos33 cit4) (in-city pos44 cit4) (in-city pos55 cit4) (in-city pos66 cit5)
	(in-city apt3 cit3) (in-city apt4 cit4) (in-city apt5 cit5) (in-city apt6 cit6)
)

(:goal 
(and 
	<HYPOTHESIS>
)
))