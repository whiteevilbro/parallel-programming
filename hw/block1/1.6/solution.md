### First deadlock

As we can see from lines 10 through 18 of `1.out` file
```jstack
"main" #1 prio=5 os_prio=0 cpu=62.50ms elapsed=18.91s allocated=1407K defined_classes=46 tid=0x00000229712db800 nid=0x25ac in Object.wait()  [0x000000c663fff000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@11.0.30/Native Method)
	- waiting on <0x0000000719907ed8> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1300)
	- waiting to re-lock in wait() <0x0000000719907ed8> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1375)
	at Deadlock1.main(Deadlock1.java:3)
```
thread `main` is waiting on `join`, and as it is the one and only thread in this program, it must wait on itself.
If it waits on itself then its trivial cycle in wait-for graph, and, by definition, is a deadlock

### Second deadlock

As we can see from lines 12 through 19,
```jstack
"main" #1 prio=5 os_prio=0 cpu=62.50ms elapsed=21.64s allocated=1437K defined_classes=49 tid=0x000001e9347dc800 nid=0x3dac in Object.wait()  [0x00000052e0cff000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@11.0.30/Native Method)
	- waiting on <0x00000007198e5ff0> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1300)
	- waiting to re-lock in wait() <0x00000007198e5ff0> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1375)
	at Deadlock2.main(Deadlock2.java:23)
```
116 through 127
```jstack
"Thread-0" #14 prio=5 os_prio=0 cpu=0.00ms elapsed=21.57s allocated=0B defined_classes=0 tid=0x000001e958f81000 nid=0x4d28 in Object.wait()  [0x00000052e1fff000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@11.0.30/Native Method)
	- waiting on <0x00000007198e8748> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1300)
	- waiting to re-lock in wait() <0x00000007198e8748> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1375)
	at Deadlock2.lambda$main$2(Deadlock2.java:17)
	at Deadlock2$$Lambda$16/0x0000000840065040.run(Unknown Source)
	at Deadlock2.lambda$main$0(Deadlock2.java:7)
	at Deadlock2$$Lambda$14/0x0000000840064840.run(Unknown Source)
	at java.lang.Thread.run(java.base@11.0.30/Thread.java:829)
```
and 132 through 141
```jstack
"Thread-1" #15 prio=5 os_prio=0 cpu=0.00ms elapsed=21.57s allocated=0B defined_classes=0 tid=0x000001e958f84000 nid=0x30d8 in Object.wait()  [0x00000052e20ff000]
   java.lang.Thread.State: WAITING (on object monitor)
	at java.lang.Object.wait(java.base@11.0.30/Native Method)
	- waiting on <0x00000007198e5ff0> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1300)
	- waiting to re-lock in wait() <0x00000007198e5ff0> (a java.lang.Thread)
	at java.lang.Thread.join(java.base@11.0.30/Thread.java:1375)
	at Deadlock2.lambda$main$1(Deadlock2.java:11)
	at Deadlock2$$Lambda$15/0x0000000840064c40.run(Unknown Source)
	at java.lang.Thread.run(java.base@11.0.30/Thread.java:829)
```
All three of our threads are waiting on `join`s. 
Every wait is an edge in wait-for graph. 
Our edges are oriented, but at any time single thread can `join` (therefore, wait on) only one thread, so outgoing degree of each node in our graph is at most one. 
From this and the fact that there's more than verticies-1 edged we can conclude that if at any point observe all threads waiting, then there must be a deadlock.  
This is exactly a scenario we observe here, hence these threads are deadlocked.