(define (problem pb7_large)
(:domain zenotravel)
(:objects
	plane1
	plane2
	plane3
	person1
	person2
	person3
	person4
	person5
	person6
	person7
	person8
	city0
	city1
	city2
	city3
	city4
	city5
	fl0
	fl1
	fl2
	fl3
	fl4
	fl5
	fl6 - obj
	)
(:init
	(AT plane1 city2)
	(aircraft plane1)
	(fuellevel plane1 fl2)
	(AT plane2 city4)
	(aircraft plane2)
	(fuellevel plane2 fl5)
	(AT plane3 city3)
	(aircraft plane3)
	(fuellevel plane3 fl2)
	(AT person1 city5)
	(person person1)
	(AT person2 city3)
	(person person2)
	(AT person3 city4)
	(person person3)
	(AT person4 city2)
	(person person4)
	(AT person5 city5)
	(person person5)
	(AT person6 city2)
	(person person6)
	(AT person7 city1)
	(person person7)
	(AT person8 city5)
	(person person8)
	(city city0)
	(city city1)
	(city city2)
	(city city3)
	(city city4)
	(city city5)
	(next fl0 fl1)
	(next fl1 fl2)
	(next fl2 fl3)
	(next fl3 fl4)
	(next fl4 fl5)
	(next fl5 fl6)
	(flevel fl0)
	(flevel fl1)
	(flevel fl2)
	(flevel fl3)
	(flevel fl4)
	(flevel fl5)
	(flevel fl6)
)
(:goal (and
	<HYPOTHESIS>
))
)