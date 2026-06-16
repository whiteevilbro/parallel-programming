It's hard to say something about correctness of implementation.  
Only notable things in this case are:
- In case of an exception inside task callable, thread self-modifies future object. 
Potentially, there could've been a race, but only other write-access to this field is invoked if and only if the "worker" thread had died.
- There could be a situation where task callable has alreay exited (completed), but Future is not considered completed.
This latency caused by the handling of exceptions, that task can cause.
- JoinFuture::get - there's a `join` before the read-access to `result`, so no visibility problems here.