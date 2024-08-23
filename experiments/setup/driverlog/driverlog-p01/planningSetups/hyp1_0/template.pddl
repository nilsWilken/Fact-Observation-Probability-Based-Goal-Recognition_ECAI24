(define (problem driverLogProblem)
	(:domain driverlog)
	(:objects
	driver1
	driver2
	driver3
	truck1
	truck2
	package1
	package2
	package3
	package4
	package5
	s0
	s1
	s2
	p0-1
	p0-2
	p1-2 - obj
	)
	(:init
	(AT driver1 s1)
	(driver driver1)
	(AT driver2 s0)
	(driver driver2)
	(AT driver3 s0)
	(driver driver3)
	(AT truck1 s1)
	(empty truck1)
	(truck truck1)
	(AT truck2 s1)
	(empty truck2)
	(truck truck2)
	(AT package1 s0)
	(obj package1)
	(AT package2 s0)
	(obj package2)
	(AT package3 s2)
	(obj package3)
	(AT package4 s2)
	(obj package4)
	(AT package5 s1)
	(obj package5)
	(location s0)
	(location s1)
	(location s2)
	(location p0-1)
	(location p0-2)
	(location p1-2)
	(path s0 p0-1)
	(path p0-1 s0)
	(path s1 p0-1)
	(path p0-1 s1)
	(path s0 p0-2)
	(path p0-2 s0)
	(path s2 p0-2)
	(path p0-2 s2)
	(path s1 p1-2)
	(path p1-2 s1)
	(path s2 p1-2)
	(path p1-2 s2)
	(link s0 s1)
	(link s1 s0)
	(link s0 s2)
	(link s2 s0)
	(link s1 s2)
	(link s2 s1)
)
	(:goal (and
		<HYPOTHESIS>
	))
)