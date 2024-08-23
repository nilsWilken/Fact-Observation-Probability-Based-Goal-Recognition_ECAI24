(define (domain ferry)
    (:types obj - object)
   (:predicates (not-eq ?x ?y)
		(car ?c)
		(location ?l)
		(at-ferry ?l)
		(AT ?c ?l)
		(empty-ferry)
		(on ?c))

   (:action sail
       :parameters  (?from ?to)
       :precondition (and (not-eq ?from ?to) 
                          (location ?from) (location ?to) (at-ferry ?from))
       :effect (and  (at-ferry ?to)
		     (not (at-ferry ?from))))


   (:action board
       :parameters (?car ?loc)
       :precondition  (and  (car ?car) (location ?loc)
			    (AT ?car ?loc) (at-ferry ?loc) (empty-ferry))
       :effect (and (on ?car)
		    (not (AT ?car ?loc)) 
		    (not (empty-ferry))))

   (:action debark
       :parameters  (?car  ?loc)
       :precondition  (and  (car ?car) (location ?loc)
			    (on ?car) (at-ferry ?loc))
       :effect (and (AT ?car ?loc)
		    (empty-ferry)
		    (not (on ?car)))))