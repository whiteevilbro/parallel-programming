### Speedup

Let's assume that we have a task, that takes time `t`.
We can change performance of some part `ζ` of our task by the scale of `ξ`.
We can calculate total "speedup" `s` of our change using the following formula.

```
t = (1-ζ)t + ζt
t' = (1-ζ)t + ζξt
s = t/t' = 1/((1-ζ) + ζξ)
```
---

**Note:** `0 <= ζ <= 1`, `0 <= ξ`

### First question

Using derived formula, setting the values `ζ` to 10<sup>-1</sup>, `ξ` to 10<sup>-4</sup>.
From that we get s = 1/(0.9 + 10<sup>-5</sup>)

**Answer:** `s == 1/(0.90001)`

### Second question

We derive `ξ == (1 - s(1 - ζ))/(ζs)`
Given `s == 3` and `ζ = 5/6`, `ξ = 0.2`.
As `ξ` is time-scaling factor, we actually need ξ<sup>-1</sup> which is equal to `5`

**Answer:** `5`

### Third question
```
s == 10
ξ = 0.01  
ζ == (1-s)/(s*(ξ-1))
->
ζ == -9 / (10 * -0.99) = 1 / (1.1) = 10/11

```

To achive such scenario we need to have a program, which is `1/11` serial and `10/11` parallel, working on 100 CPUs