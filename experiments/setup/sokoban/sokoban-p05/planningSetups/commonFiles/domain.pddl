(define (domain sokoban)
(:requirements :typing)
(:types LOC DIR BOX)
(:predicates 
             (at-robot ?l - LOC)
             (AT ?o - BOX ?l - LOC)
             (adjacent ?l1 - LOC ?l2 - LOC ?d - DIR) 
             (clear ?l - LOC)
)

(:action move
	:parameters (?from - LOC ?to - LOC ?dir - DIR)
	:precondition (and (clear ?to) (at-robot ?from) (adjacent ?from ?to ?dir))
	:effect (and (at-robot ?to) (not (at-robot ?from))))
             
(:action push
	:parameters  (?rloc - LOC ?bloc - LOC ?floc - LOC ?dir - DIR ?b - BOX)
	:precondition (and (at-robot ?rloc) (AT ?b ?bloc) (clear ?floc)
	           (adjacent ?rloc ?bloc ?dir) (adjacent ?bloc ?floc ?dir))

	:effect (and (at-robot ?bloc) (AT ?b ?floc) (clear ?bloc)
             (not (at-robot ?rloc)) (not (AT ?b ?bloc)) (not (clear ?floc))))
)