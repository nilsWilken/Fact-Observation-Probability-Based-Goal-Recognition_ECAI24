(define (problem DLOG-3-3-7)
  (:domain driverlog)
  (:objects
  driver1
  driver2
  driver3
  truck1
  truck2
  truck3
  package1
  package2
  package3
  package4
  package5
  package6
  package7
  s0
  s1
  s2
  p1-0
  p2-0
  p2-1 - obj
  )
  (:init
  (AT driver1 s2)
  (driver driver1)
  (AT driver2 s0)
  (driver driver2)
  (AT driver3 s1)
  (driver driver3)
  (AT truck1 s2)
  (empty truck1)
  (truck truck1)
  (AT truck2 s2)
  (empty truck2)
  (truck truck2)
  (AT truck3 s2)
  (empty truck3)
  (truck truck3)
  (AT package1 s0)
  (obj package1)
  (AT package2 s1)
  (obj package2)
  (AT package3 s0)
  (obj package3)
  (AT package4 s0)
  (obj package4)
  (AT package5 s1)
  (obj package5)
  (AT package6 s2)
  (obj package6)
  (AT package7 s2)
  (obj package7)
  (location s0)
  (location s1)
  (location s2)
  (location p1-0)
  (location p2-0)
  (location p2-1)
  (path s1 p1-0)
  (path p1-0 s1)
  (path s0 p1-0)
  (path p1-0 s0)
  (path s2 p2-0)
  (path p2-0 s2)
  (path s0 p2-0)
  (path p2-0 s0)
  (path s2 p2-1)
  (path p2-1 s2)
  (path s1 p2-1)
  (path p2-1 s1)
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
