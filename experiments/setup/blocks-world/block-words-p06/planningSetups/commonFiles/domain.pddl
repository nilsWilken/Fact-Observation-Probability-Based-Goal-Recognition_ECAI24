;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; 4 Op-blocks world
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (domain BLOCKS)
  (:requirements :strips :typing :equality)
  (:types block)
  (:predicates (ON ?x ?y - block)
	       (ONTABLE ?x - block)
	       (CLEAR ?x - block)
	       (HANDEMPTY)
	       (HOLDING ?x - block)
	       )

  (:action PICK-UP
	     :parameters (?x - block)
	     :precondition (and (CLEAR ?x) (ONTABLE ?x) (HANDEMPTY))
	     :effect
	     (and (not (ONTABLE ?x))
		   (not (CLEAR ?x))
		   (not (HANDEMPTY))
		   (HOLDING ?x)))

  (:action PUT-DOWN
	     :parameters (?x - block)
	     :precondition (HOLDING ?x)
	     :effect
	     (and (not (HOLDING ?x))
		   (CLEAR ?x)
		   (HANDEMPTY)
		   (ONTABLE ?x)))
  (:action STACK
	     :parameters (?x ?y - block)
	     :precondition (and (HOLDING ?x) (CLEAR ?y) (not (= ?x ?y)))
	     :effect
	     (and (not (HOLDING ?x))
		   (not (CLEAR ?y))
		   (CLEAR ?x)
		   (HANDEMPTY)
		   (ON ?x ?y)))
  (:action UNSTACK
	     :parameters (?x ?y - block)
	     :precondition (and (ON ?x ?y) (CLEAR ?x) (HANDEMPTY) (not (= ?x ?y)))
	     :effect
	     (and (HOLDING ?x)
		   (CLEAR ?y)
		   (not (CLEAR ?x))
		   (not (HANDEMPTY))
		   (not (ON ?x ?y)))))
