(define (problem depotprob0) (:domain depots)
(:objects
  depot0 - depot
  distributor0 - distributor
  truck0 truck1 - truck
  pallet0 pallet1 - pallet
  crate0 crate1 crate2 crate3 crate4 crate5 crate6 crate7 - crate
  hoist0 hoist1 - hoist)
(:init
  (AT pallet0 depot0)
  (clear crate5)
  (AT pallet1 distributor0)
  (clear crate7)
  (AT truck0 distributor0)
  (AT truck1 distributor0)
  (AT hoist0 depot0)
  (available hoist0)
  (AT hoist1 distributor0)
  (available hoist1)
  (AT crate0 distributor0)
  (on crate0 pallet1)
  (AT crate1 distributor0)
  (on crate1 crate0)
  (AT crate2 depot0)
  (on crate2 pallet0)
  (AT crate3 distributor0)
  (on crate3 crate1)
  (AT crate4 depot0)
  (on crate4 crate2)
  (AT crate5 depot0)
  (on crate5 crate4)
  (AT crate6 distributor0)
  (on crate6 crate3)
  (AT crate7 distributor0)
  (on crate7 crate6)
)

(:goal (and
  <HYPOTHESIS>
))
)
