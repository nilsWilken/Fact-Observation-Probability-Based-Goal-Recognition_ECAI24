(define (problem depotsproblem) 
(:domain depots)
(:objects
	depot0 depot1 depot2 - depot
	distributor0 distributor1 distributor2 - distributor
	truck0 truck1 truck2 - truck
	pallet0 pallet1 pallet2 pallet3 pallet4 pallet5 - pallet
	crate0 crate1 crate2 - crate
	hoist0 hoist1 hoist2 hoist3 hoist4 hoist5 - hoist)
(:init
	(AT pallet0 depot0)
	(clear pallet0)
	(AT pallet1 depot1)
	(clear pallet1)
	(AT pallet2 depot2)
	(clear crate2)
	(AT pallet3 distributor0)
	(clear pallet3)
	(AT pallet4 distributor1)
	(clear pallet4)
	(AT pallet5 distributor2)
	(clear crate1)
	(AT truck0 depot2)
	(AT truck1 distributor1)
	(AT truck2 distributor0)
	(AT hoist0 depot0)
	(available hoist0)
	(AT hoist1 depot1)
	(available hoist1)
	(AT hoist2 depot2)
	(available hoist2)
	(AT hoist3 distributor0)
	(available hoist3)
	(AT hoist4 distributor1)
	(available hoist4)
	(AT hoist5 distributor2)
	(available hoist5)
	(AT crate0 depot2)
	(on crate0 pallet2)
	(AT crate1 distributor2)
	(on crate1 pallet5)
	(AT crate2 depot2)
	(on crate2 crate0)
)

(:goal (and
		<HYPOTHESIS>
	)
))
