### 2.2.a

Let's assume that both threads enter `critical_section()` statements meaning at some point there exists a subsequnce of type `X -> Y_1 -> ... -> Y_n -> Z` where `X from {A.4, B.6} & Z from {A.4, B.6} & X != Z`, `Y_i not in {A.5, B.7} for every natural i, 1 <= i <= n`  

Let's consider path's to `A.4` in scope of `ThreadA` and value of `A_flag` and `B_flag` on this path.
Any path will be contained within `while (should_continue)` cycle, due to the fact that there's unconditional setting of `A_flag` at the start (`A.1`) and clearing at the end (`A.5`).
As there's no other write-accesses to `A_flag`, at any point between `A.1` and `A.5` `A_flag` will be set, including our `critical_section()` (`A.4`)  
Statements `A.2` `A.3` combined are effectively waiting for `B_flag` to be cleared, from that we get that regardless of the form of the path (`A.1 -> A.2 -> A.4` or `A.1 -> (A.2 -> A.3)xN -> A.4` (`B.i` statements are ommited for clarity)) at the moment of nearest previous `A.2` to `A.4`  predicate `A_flag && !B_flag` holds.

Now let's make similar approach to `B.6` in scope of `ThreadB` (meaning henceforth `A.i` statements are ommited, though insensibly present).
Any path that is interesting for us is contained withing `while (should_continue)` cycle. (any path can be diveded into subpaths by boundaries of that cycle, for us all iterations are independant, anything in `critical_section()` does not concers us, as long as it is containted within itself and doesn't mess with our flags).  
First, we will consider paths in the form of `B.1 -> B.2 -> B.6 -> ...`. This type path is only possible if at the moment of `B.2` `A_flag` is cleared (and `B_flag` is, obviously, set, due to `B.1`)  
Now, lets consider all other paths, which will have form of `B.1 -> (B.2 -> B.3 -> B.4 -> B.5)xN -> B.2 -> B.6 -> ...` where `N >= 1`. 
On these paths statement `B.1` is effectively negated by the first `B.3` statement. 
And we know that at the moment of `B.2` `A_flag` is set. Statement `B.4` is a wait for `A_flag` to be cleared and statent `B.5` returns us to `B.2` in the exact same situation as before, so any path of this type is reduced to paths of previous type.
From that we get that at the moment of the nearest previous `B.2` to `B.6` predicate `!A_flag && B_flag` holds.
**Note:** nearest previous `A.2` to `A.4` and `B.2` to `B.6` will come directly before critical statement in scope of corresponding thread (as can be seen from the structure of traces).

Let's consider first time we entered `ciritical_section()` non-exclusievly and find nearest `A.2` and `B.2` to our `A.4` and `B.6` respectively.
Henceforth, we will be working with them.  
At the moment of `A.2` condition `A_flag && !B_flag` was satisfied.  
At the moment of `B.2` condition `!A_flag && B_flag` was satisfied.  
From that we derive that both `A_flag` and `B_flag` were changed.
Without loss of generality, we can say that `A.2` came before `B.2`.
As we've noted earlier, the very next statement in the scope of `ThreadA` is `A.4`.
By our premise, `A.5` can't come before `B.2`, so can't any other statement from `ThreadA` (otherwise order of execution inside thread is violated).
`A.4` doesn't change any of our flags, so they need to be changed by other statements, which leaves us with `B.x` statements.  
Carefully listing `B.i` statements containig write-accesses (`B.1`, `B.3`, `B.5`, `B.7`) we can notice, that all of them influence only `B_flag` value.
Which, in turn, means that in no trace can be condition `A_flag && !B_flag` could be met at the moment of `A.2`.  
Contradiction. That means our assumption was wrong and this algorithm guerantees mutual exclusion.

### 2.2.b

Let's assume that both out threads hang. Let's list all possible causes^
- Blocking methods
  - `join`s
  - `lock`s
- Loops

Our working threads have no `join`s in them, so this cause is non-viable.
There's only one lock in our program, so its either `ThreadA` has ownership, `ThreadB` has ownership or no one has ownership (`Main` thread does not participate in the competition for this lock at all),
so `lock` waiting is non-viable either (also, all our opertaions with `lock` are embedded into our statements, so we can consider them atomic and non-blocking).  
Only thing left to consider is loops. 
Only place that `ThreadA` could hang is `A.2`-`A.3` loop, because if we're looping in `while(should_continue)` then we execute `critical_sections()` and this is considered "working", not "hanging".
To loop in `A.2`-`A.3` we need to have `B_flag` set every time we execute `A.2`.
Let's note the fact that during this, `A_flag` is set due to he `A.1` and the fact, that `ThreadB` can't modify `A_flag`, which was proven above.  
`ThreadB` can loop inside either of `B.2`-`B.5`, `B.4` or any combination of them.
First, let's see if we can stay in the loop, so we assume we're already looping in `ThreadA`.
That means that `A_flag` is set.  
Let's say that we're a looping in `B.4` cycle. How did we get here? Either from previous `B.4` iteration and then the question arises anew, or from `B.3`.
We can't start execution at `B.4`, so at some moment we've certaintly gone through `B.3`.
That means, that `B_flag` was cleared. As long as `A_flag` is set (which it currently is), we are looping in `B.4` and can't go any further or modify `B.5`.
Given enough time, some luck or some fairness from the scheduler, `ThreadA` will execute `A.2` (we're curretly looping through `A.2`-`A.3`).
From there we will go (is the scope of `ThreadA`) to `A.4`, because `B_flag` has been cleared by `B.3` and hasn't been set since.
That means that we've escaped `A.2`-`A.3` loop, done some work `A.4` and did not hang. Contradiction.  
Next case, we're looping through `B.2`-`B.5` maybe with some (or none) iterations of `B.4`.
As before, we're trying to stay in the loop, so `A.2`-`A.3` loop is already taking its place and `A_flag` is set.
As before, if we're executing `B.3` then we're clearing `B_flag` and for `ThreadA` loop to continue, we need to set it again before next `A.2` exection.
We can set `B_flag` by executin either of `B.5` of `B.7`, but execution of `B.7` means executing `B.6`, which is "work" and we're not hanging,  so it leaves us with `B.5` as out only mean of setting `B_flag` again.
To execute `B.5` we need to go through `B.4` which can be done only by clearing `A_flag`, which cannot be done from neither of `ThreadA` (it's curretly looping in `A.2`-`A.3`) nor `ThreadB` (cannot write to `A_flag`).
So, we cannot set `B_flag` before the next exectuion of `A.2`, therefore we cannot loop in theese particular cycles forever.

#### Notes:
- As we can notice, last statemets in any comlpete trace of our threads, will be `A.5` and `B.7` (due to the nature of `while(should_continue)`).
That means that if (when) any of our thread leaves earlier than the other, its corresponding flag will be cleared.
That will allow other thread to proceed any remaining operations wihtout waiting for other to clear its flag (`A.2`, `B.2`, `B.4`)
- Clarification on "work": in both threads, if we can proceed to `critical_section()` then we will reach `while(should_continue)`.
This check determines if we should continue or should we stop. 
Given that at some moment `should_continue` will become `false`, reaching `critical_section()` after that will guarantee that our thread will end its work and terminate.
- Entirety of this proof does not consider possibility of `critical_section()` or some other code interfiering with `A_flag` and `B_flag` or interrupting `ThreadA` or `ThreadB`.
Without these constraints scenario where `A.2` and `B.4` are both in action and `A_flag && B_flag` is true is trivially constructible.
So is scenario where, for example, `ThreadA` is interrupted right after `A.1` and `ThreadB` currently executes `B.4`. 
Both of proposed scenarios will wait untill the battery runs out, i.e. heat death of the universe.