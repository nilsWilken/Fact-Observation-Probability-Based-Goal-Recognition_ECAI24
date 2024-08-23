;; logistics domain Typed version.

(define (domain logistics)
  (:requirements :strips :typing) 
  (:types city place physobj - object
          package vehicle - physobj
          truck airplane - vehicle
          airport location - place
  )
  (:predicates 
	(in-city ?loc - place ?city - city) 
	(AT ?obj - physobj ?loc - place) 
	(in ?pkg - package ?veh - vehicle)
  )
  (:action load-truck
    :parameters (?pkg - package ?truck - truck ?loc - place)
    :precondition (and (AT ?truck ?loc) (AT ?pkg ?loc))
    :effect (and (not (AT ?pkg ?loc)) (in ?pkg ?truck))
  )
  (:action load-airplane
   :parameters (?pkg - package ?airplane - airplane ?loc - place)
   :precondition (and (AT ?pkg ?loc) (AT ?airplane ?loc))
   :effect (and (not (AT ?pkg ?loc)) (in ?pkg ?airplane))
  )
  (:action unload-truck
   :parameters (?pkg - package ?truck - truck ?loc - place)
   :precondition (and (AT ?truck ?loc) (in ?pkg ?truck))
   :effect (and (not (in ?pkg ?truck)) (AT ?pkg ?loc))
  )
  (:action unload-airplane
   :parameters (?pkg - package ?airplane - airplane ?loc - place)
   :precondition (and (in ?pkg ?airplane) (AT ?airplane ?loc))
   :effect (and (not (in ?pkg ?airplane)) (AT ?pkg ?loc))
  )
  (:action drive-truck
   :parameters (?truck - truck ?loc_from - place ?loc_to - place ?city - city)
   :precondition (and (not (= ?loc_from ?loc_to)) (AT ?truck ?loc_from) (in-city ?loc_from ?city) (in-city ?loc_to ?city))
   :effect (and (not (AT ?truck ?loc_from)) (AT ?truck ?loc_to))
  )
  (:action fly-airplane
   :parameters (?airplane - airplane ?loc_from - airport ?loc_to - airport)
   :precondition (and (not (= ?loc_from ?loc_to)) (AT ?airplane ?loc_from))
   :effect (and (not (AT ?airplane ?loc_from)) (AT ?airplane ?loc_to))
  )
)
