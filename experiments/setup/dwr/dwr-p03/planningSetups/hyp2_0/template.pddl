(define (problem dwrProblem)
  (:domain dwr)
  (:objects
   r1 - robot
   l1 l2 l3 l4 - location
   k1 k2 k3 - crane
   p1 p2 p3 - pile
   ca cb cc cd ce cf pallet - container)
   
   (:init
     (adjacent l1 l2)
     (adjacent l2 l1)
   (adjacent l2 l3)
   (adjacent l3 l2)
   (adjacent l3 l4)
   (adjacent l4 l3)
   
   (attached p1 l1)
   (attached p2 l3)
   (attached p3 l4)
   
   (belong k1 l1)
   (belong k2 l3)
   (belong k3 l4)
   
   (AT r1 l2)
   (unloaded r1)
   (occupied l2)
   
   (in ca p1)
   (in cb p1)
   (in cc p1)
   (in cd p1)
   (in ce p1)
   
   (on ce cd)
   (on cd cc)
   (on cc cb)
   (on cb ca)
   (on ca pallet)
   (top ce p1)
   (empty k1)
   
   (empty k3)
   (holding k2 cf)
   
   (top pallet p2)
   (top pallet p3)
   )
   
   (:goal
     (and   
        <HYPOTHESIS>
     )
  )
)