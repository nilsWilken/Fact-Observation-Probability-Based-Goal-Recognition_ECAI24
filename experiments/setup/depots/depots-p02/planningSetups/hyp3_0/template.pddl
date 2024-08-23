(define (problem depotprob0) (:domain depots)
(:objects
	depot0 depot1 - depot
	distributor0 - distributor
	truck0 truck1 - truck
	pallet0 pallet1 pallet2 - pallet
	crate0 crate1 crate2 crate3 crate4 crate5 - crate
	hoist0 hoist1 hoist2 - hoist)
(:init
	(AT pallet0 depot0)
	(clear crate4)
	(AT pallet1 depot1)
	(clear crate5)
	(AT pallet2 distributor0)
	(clear crate0)
	(AT truck0 depot1)
	(AT truck1 distributor0)
	(AT hoist0 depot0)
	(available hoist0)
	(AT hoist1 depot1)
	(available hoist1)
	(AT hoist2 distributor0)
	(available hoist2)
	(AT crate0 distributor0)
	(on crate0 pallet2)
	(AT crate1 depot0)
	(on crate1 pallet0)
	(AT crate2 depot0)
	(on crate2 crate1)
	(AT crate3 depot0)
	(on crate3 crate2)
	(AT crate4 depot0)
	(on crate4 crate3)
	(AT crate5 depot1)
	(on crate5 pallet1)
)

(:goal (and
		<HYPOTHESIS>
	)
))
