(define (problem pb5)
(:domain blocks)
(:objects F A K H G E D I C J B - block)
(:init (CLEAR B) (CLEAR J) (CLEAR C) (ONTABLE I) (ONTABLE D) (ONTABLE E)
 (ON B G) (ON G H) (ON H K) (ON K A) (ON A F) (ON F I) (ON J D) (ON C E)
 (HANDEMPTY))
(:goal (and
<HYPOTHESIS>
))
)