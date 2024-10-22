(define (problem ferryProblem)
(:domain ferry)
(:objects l0 l1 l2 
          c0 c1 c2 c3 c4 c5 c6 c7 c8 c9 c10 - obj
)
(:init
(location l0)
(location l1)
(location l2)
(car c0)
(car c1)
(car c2)
(car c3)
(car c4)
(car c5)
(car c6)
(car c7)
(car c8)
(car c9)
(car c10)
(not-eq l0 l1)
(not-eq l1 l0)
(not-eq l0 l2)
(not-eq l2 l0)
(not-eq l1 l2)
(not-eq l2 l1)
(empty-ferry)
(AT c0 l0)
(AT c1 l0)
(AT c2 l1)
(AT c3 l0)
(AT c4 l0)
(AT c5 l0)
(AT c6 l2)
(AT c7 l1)
(AT c8 l1)
(AT c9 l1)
(AT c10 l2)
(at-ferry l2)
)
(:goal
(and
	<HYPOTHESIS>
)
)
)