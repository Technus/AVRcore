.def input=R20
.def temp=R21
.def check=R22

start:
;ldi input,1
;push input;1 is prime!
;ldi input,2
;push input;2 is prime!
;ldi input,3
;push input;3 is prime!
;ldi input,5
;push input;5 is prime!

;etc. would be boring
;lets start checking

ldi input,0

addi input,0 ; change this to change starting postion
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
addi input,0 ; change this to change starting postion
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
addi input,0 ; change this to change starting postion
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
lsl input
addi input,0xff ; change this to change starting postion

nextInput:
addi input,2;add 2

checkStart:
ldi check,1;set to 1
              
checkNext:
addi check,2;increment by 2 check
;cuz no n%2==0 needs to be checked

mov temp,check;init temp

compute:
add temp,check
cp temp,input;compare (temp-input)
brlo -compute;back to compute if lower
  ;- denotes relative address

breq -nextInput
    ;this check failed cuz mod==0

checkIfTestedEnough:
mov temp,input;get input
lsr temp;shift right
 ;cuz cannot be factored
 ;by more than half..
cp temp,check;test check limit
brlo -checkNext;back to testing

push input;we got prime

.equ SP = 61
in temp,SP
andi temp,0xff
cpi temp,245;255-(desired count)
breq -done

jmp nextInput;check next number

done:
break
