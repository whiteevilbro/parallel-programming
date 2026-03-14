## First
`x == 1, r_y == 0, r_z == 0`  

`A.1 -> A.2 -> A.3 -> A.4 -> B.1 -> B.2 -> B.3 -> B.4 -> A.5 -> B.5`  
**Note:** First eight statemenst could be arranged in any order as long as `X.i` stands before `X.(i+1)`, 
because only kind of multi-thread interaction is reading from `x` and no writes ocuur. 
Given local context (`a_x == y == b_x == z == 0`) two last statements could also be rearranged with no consecuences.

## Second
`x == 2, r_y == 0, r_z == 1`  

`A.1 -> A.2 -> A.3 -> A.4 -> B.1 -> B.2 -> B.3 -> A.5 -> B.4 -> B.5`

**Note:** Similarly, first seven statements could be rearranged. 
Also, `B.2` must come before `A.5`, but `B.3` and `A.5` must come in this order, 
otherwise `B.5` wouldn't be executed and maximum value of `x` is limited to `1`

## Third
Let's assume that scenario `x == 1, r_y == 0, r_z == 1` is reachable.  
From `r_z == 1` and `r_z = z;` (the only mention of `r_z`) we can derive that at some moment local variable `z` had value `1`. 
Starting value of `z` is `-1` which is, obviously, not `1`, so it had to change its value.
Only write access to `z` is statement `B.4`. 
From that we can get that at the moment of `B.4` `x` had a value of `1`.  
Starting value of `x` is `0`, so for it to become `1` there should be write-access to `x` before `B.4`.
Let's list all statemets that write to `x`: `A.5`, `B.5`.  
If `B.5` comes before `B.4` then we violate order of execution and our assumption was false.  
From now, we assume that `B.5` can't be executed before `B.4`.  
Then `A.5` must come before `B.4` in our execution sequence and `B.5` must come after (we can show that `B.5` must come immediately after, but it is unnecessary).  
We know that after `B.4` `z` is set to `1`. 
We know that there's no other write-access to `z` occuring between `B.4` and `B.5`.
From that we derive that after execution of `B.5` `x` is set to `2`.
For our assumed scenario to be reachable we need to set `x` to `1`, 
but, as we showed earlier, all other write-accesses to `x` occur before `B.4` and, as the result, before `B.5`.
So there's no execution order where `x` could be set to `1`, therefore our premise is false and aforementioned scenario couldn't be reached.
