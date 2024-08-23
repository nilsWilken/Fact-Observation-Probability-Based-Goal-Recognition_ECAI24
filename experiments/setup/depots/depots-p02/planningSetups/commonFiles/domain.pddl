(define (domain depots)
(:requirements :strips :typing)
(:types place locatable - object
	depot distributor - place
        truck hoist surface - locatable
        pallet crate - surface)

(:predicates (AT ?x - locatable ?y - place)
             (on ?x - crate ?y - surface)
             (in ?x - crate ?y - truck)
             (lifting ?x - hoist ?y - crate)
             (available ?x - hoist)
             (clear ?x - surface))
	
(:action drive
  :parameters (?x - truck ?y - place ?z - place) 
  :precondition (and (AT ?x ?y))
  :effect (and (not (AT ?x ?y)) (AT ?x ?z)))

(:action lift
  :parameters (?x - hoist ?y - crate ?z - surface ?p - place)
  :precondition (and (AT ?x ?p) (available ?x) (AT ?y ?p) (on ?y ?z) (clear ?y))
  :effect (and (not (AT ?y ?p)) (lifting ?x ?y) (not (clear ?y)) (not (available ?x)) (clear ?z) (not (on ?y ?z))))

(:action drop 
  :parameters (?x - hoist ?y - crate ?z - surface ?p - place)
  :precondition (and (AT ?x ?p) (AT ?z ?p) (clear ?z) (lifting ?x ?y))
  :effect (and (available ?x) (not (lifting ?x ?y)) (AT ?y ?p) (not (clear ?z)) (clear ?y)(on ?y ?z)))

(:action load
  :parameters (?x - hoist ?y - crate ?z - truck ?p - place)
  :precondition (and (AT ?x ?p) (AT ?z ?p) (lifting ?x ?y))
  :effect (and (not (lifting ?x ?y)) (in ?y ?z) (available ?x)))

(:action unload 
  :parameters (?x - hoist ?y - crate ?z - truck ?p - place)
  :precondition (and (AT ?x ?p) (AT ?z ?p) (available ?x) (in ?y ?z))
  :effect (and (not (in ?y ?z)) (not (available ?x)) (lifting ?x ?y)))
)
