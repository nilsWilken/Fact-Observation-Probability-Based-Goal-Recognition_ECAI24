(define (problem depotsproblem) 
(:domain depots)
(:objects
	depot0 depot1 - depot
	distributor0 distributor1 - distributor
	truck0 truck1 - truck
	pallet0 pallet1 pallet2 pallet3 - pallet
	crate0 crate1 - crate
	hoist0 hoist1 hoist2 hoist3 - hoist)
(:init
	(AT pallet0 depot0)
	(clear crate1)
	(AT pallet1 depot1)
	(clear crate0)
	(AT pallet2 distributor0)
	(clear pallet2)
	(AT pallet3 distributor1)
	(clear pallet3)
	(AT truck0 depot0)
	(AT truck1 distributor0)
	(AT hoist0 depot0)
	(available hoist0)
	(AT hoist1 depot1)
	(available hoist1)
	(AT hoist2 distributor0)
	(available hoist2)
	(AT hoist3 distributor1)
	(available hoist3)
	(AT crate0 depot1)
	(on crate0 pallet1)
	(AT crate1 depot0)
	(on crate1 pallet0)
)

(:goal (and
		<HYPOTHESIS>
	)
))