## Restriction #1: `z == 1`

First of all, let's notice that `z` write-accessed only once, in statement `B.4`.
As there's no prior conditional statements, statements `B.3` and `B.4` will be present in any execution trace in this exact order maybe with some other statements between them.
What will be the value after `B.4` execution solely depends on `b_z` value which is assingned only in `B.3` from value of `z`.
From uniqness of aforementioned assignments we can derive that at the end of any valid execution trace `z == 1` must be true.  

## Restriction #2: `0 <= x <= 1`

Secondly, let's consider changes of `x` variable. Write-accesses are statements `B.2` and `C.4`.
If we take a closer look, statements `C.3` and `C.4` would be executed only when `y` has a value of `2`, so let's take a look at `y`'s value throghout the exection.  
`y` is write acessed only once - in statement `A.3` where its value composed as sum of `a_x` and `a_z`, which as we can see from statements `A.1` and `A.2` are local copies of `x` and `z` in some moment in time (not necesseraly relevant).  
As can be seen from aforementioned assignments to `x`, throughout execution of program `x` can only increase or decrease by one from its previous value and each operation can occur only once. 
So absoute change of `x` is limited to `1` which considering `x`'s starting value of `0` leaves us with restrictions on `x` in form of `-1 <= x <= 1`.  
As we've already shown, `z` could equal to `0` or `1` only. 
From that we can derive that predicate in `C.2` is satisfied if and only if at the moments of `A.1` and `A.2` `x` and `z` have had values of `1` and `1` respectively.
If `x` has the value of `1` then `B.2` has already taken its action and between `C.2` and `C.3` no changes could be made to value of `x` (`C.4` can't take place before `C.3`). 
That means that `c_x` can't take any value other than `1` therefore, after execution of `C.4` `x` will be assigned to `0`.  
From the fact that `C.4` is the only statement that potentially could push `x`'s value to negatives, we can conclude that `x >= 0`. 
As was shown before `x <= 1` from which we conclude `0 <= x <= 1`  

### Nota bene:

Let's note that we've shown one more fact: if `y < 2` then `x == 1`. 
If `y < 2` holds true at the end of execution, then it hold throughout the executions due to the fact that there's only one write-access to `y`.
Hence, in any execution trace that satisfies this condition `C.4` can not occur. 
That limits us to only one `x` write-access, which in turn makes `B.1`-`B.2` sequence (again, maybe with something inbetween) effectively increment `x`'s value by one.
Considering that at the start of exection `x` is set to `0`, that leaves us with `1` as one an only possible value of the `x` variable, given that `y < 2` hold true.

## Restiriction #3: `0 <= y <= 2`

From already concluded `0 <= x <= 1` and `0 <= z <= 1`. 
We also know that `y` is the sum of `x` and `z` values in some moment in time.
From all of this trivially follows that `0 <= y <= 2`.

## Traces:

Let's list all tuples of values that our restrictions leave us with and their corresponding traces.

| X | Y | Z | Trace                                                                       |
|---|---|---|-----------------------------------------------------------------------------|
| 0 | 2 | 1 | `B.1 -> B.2 -> B.3 -> B.4 -> A.1 -> A.2 -> A.3 -> C.1 -> C.2 -> C.3 -> C.4` |
| 1 | 0 | 1 | `A.1 -> A.2 -> A.3 -> B.1 -> B.2 -> B.3 -> B.4 -> C.1 -> C.2`               |
| 1 | 1 | 1 | `B.1 -> B.2 -> A.1 -> A.2 -> A.3 -> B.3 -> B.4 -> C.1 -> C.2`               |
| 1 | 2 | 1 | `C.1 -> C.2 -> B.1 -> B.2 -> B.3 -> B.4 -> A.1 -> A.2 -> A.3`               |

