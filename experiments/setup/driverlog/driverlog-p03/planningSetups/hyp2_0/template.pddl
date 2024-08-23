(define (problem driverLogProblem)
	(:domain driverlog)
	(:objects
	driver1
	driver2
	truck1
	truck2
	package1
	package2
	s0
	s1
	s2
	p1-0
	p1-2 - obj
	)
	(:init
	(AT driver1 s2)
	(driver driver1)
	(AT driver2 s2)
	(driver driver2)
	(AT truck1 s0)
	(empty truck1)
	(truck truck1)
	(AT truck2 s0)
	(empty truck2)
	(truck truck2)
	(AT package1 s0)
	(obj package1)
	(AT package2 s0)
	(obj package2)
	(location s0)
	(location s1)
	(location s2)
	(location p1-0)
	(location p1-2)
	(path s1 p1-0)
	(path p1-0 s1)
	(path s0 p1-0)
	(path p1-0 s0)
	(path s1 p1-2)
	(path p1-2 s1)
	(path s2 p1-2)
	(path p1-2 s2)
	(link s0 s1)
	(link s1 s0)
	(link s0 s2)
	(link s2 s0)
	(link s2 s1)
	(link s1 s2)
)
	(:goal (and
		<HYPOTHESIS>
	))
)