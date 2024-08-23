(define (domain driverlog)
  (:requirements :strips)
  (:types obj - object)
  (:predicates 	(obj ?obj)
	       	(truck ?truck)
               	(location ?loc)
		(driver ?d)
		(AT ?obj ?loc)
		(in ?obj1 ?obj)
		(driving ?d ?v)
		(link ?x ?y) (path ?x ?y)
		(empty ?v)
)


(:action load-truck
  :parameters
   (?obj
    ?truck
    ?loc)
  :precondition
   (and (obj ?obj) (truck ?truck) (location ?loc)
   (AT ?truck ?loc) (AT ?obj ?loc))
  :effect
   (and (not (AT ?obj ?loc)) (in ?obj ?truck)))

(:action unload-truck
  :parameters
   (?obj
    ?truck
    ?loc)
  :precondition
   (and (obj ?obj) (truck ?truck) (location ?loc)
        (AT ?truck ?loc) (in ?obj ?truck))
  :effect
   (and (not (in ?obj ?truck)) (AT ?obj ?loc)))

(:action board-truck
  :parameters
   (?driver
    ?truck
    ?loc)
  :precondition
   (and (driver ?driver) (truck ?truck) (location ?loc)
   (AT ?truck ?loc) (AT ?driver ?loc) (empty ?truck))
  :effect
   (and (not (AT ?driver ?loc)) (driving ?driver ?truck) (not (empty ?truck))))

(:action disembark-truck
  :parameters
   (?driver
    ?truck
    ?loc)
  :precondition
   (and (driver ?driver) (truck ?truck) (location ?loc)
        (AT ?truck ?loc) (driving ?driver ?truck))
  :effect
   (and (not (driving ?driver ?truck)) (AT ?driver ?loc) (empty ?truck)))

(:action drive-truck
  :parameters
   (?truck
    ?loc-from
    ?loc-to
    ?driver)
  :precondition
   (and (truck ?truck) (location ?loc-from) (location ?loc-to) (driver ?driver) 
   (AT ?truck ?loc-from)
   (driving ?driver ?truck) (link ?loc-from ?loc-to))
  :effect
   (and (not (AT ?truck ?loc-from)) (AT ?truck ?loc-to)))

(:action walk
  :parameters
   (?driver
    ?loc-from
    ?loc-to)
  :precondition
   (and (driver ?driver) (location ?loc-from) (location ?loc-to)
	(AT ?driver ?loc-from) (path ?loc-from ?loc-to))
  :effect
   (and (not (AT ?driver ?loc-from)) (AT ?driver ?loc-to)))

 
)
