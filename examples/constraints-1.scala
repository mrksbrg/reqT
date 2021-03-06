val m = Model(Feature("x") has Prio(1)) 
val cs = Seq(Feature("y").Prio::{1 to 5}, Feature("y").Prio #> 3)
m.impose(cs)
m.impose(cs).satisfy

val m1 = Model(
  Release("r1") precedes Release("r2"),
  Feature("x") precedes Feature("y"))

// how to build a release planning satisfaction problem out if this??:
  
val nRel = (m1 / Release).entities.size

val orderDomain = {1 to nRel}

val cs1 = Seq(
  Release("r1").Order :: orderDomain,
  Release("r2").Order :: orderDomain,
  Feature("x").Order :: orderDomain,
  Feature("y").Order :: orderDomain,
  Release("r2").Order #> Release("r1").Order,
  Feature("y").Order #> Feature("x").Order)

val m1cs1 = Model() impose cs1 satisfy
  
val m2 = Model(
  Release("r1") has Order(1),
  Release("r2") has Order(2),
  Feature("x") has Order(1),
  Feature("y") has Order(2)) 

assert(m2 == m1cs1)  
  
val m3 = Model(
  Release("r1") precedes Release("r2"),
  Feature("x") precedes Feature("y"),
  Release("r1") requires Feature("y"),  //requires => implements/releases/includes??
  Release("r2") requires Feature("x")) 
  
