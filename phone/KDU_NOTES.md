
## Pos 1 - Divider Channel B
## Pos 2 - Channel Memory Slot Top
```
0000 0000
^^^^ ^^^^ - Channel Mode 
  				0000 0000 - Frequency Mode selected
  				0000 0001 - Slot 01
  				1000 0000 - Slot 128
```

## Pos 3 - Channel Memory Slot Bottom
```
0000 0000
^^^^ ^^^^ - Channel Mode 
  				0000 0000 - Frequency Mode selected
  				0000 0001 - Slot 01
  				1000 0000 - Slot 128
```

## Pos 4 - Top Transmisison Bar | Frequency Slot Select
```
0000 0000
        ^ - Frequency Slot Selection, Top (01), Bottom (10)
  ^^ ^^   - Bottom, Transmission Bar
  				1010 - Full bar
  				0001 - Empty Bar
  				1111 - Dashes
```
## POS 5 Bottom transmission Bar | Frequency Cycle Open

```
0000 0000
       ^^ - Reverse Frequency Open, Off (01), On (10)
  ^^ ^^   - Bottom, Transmission Bar
  				1010 - Full bar
  				0001 - Empty Bar
  				1111 - Dashes
```

## POS 6 Rx/Tx Symbol, Battery

```
0000 0000
   ^ ^^^^ - Battery Symbol
   				Show Battery  (1 0000)
   				Full Battery  (X 0110)
   				Empty Battery (X 0001)
 ^^       - Recieve/Transmit symbol - (01), R (10), T (11)

```

## Pos 7 - Volume Controls
```
0000 0000
       ^^ -- Determines wether it is Electron (01) or VULOS(10) Volume Control
 ^^^ ^^	  -- Volume Control Bar 
 				10101 > Full Volume, 
 				00000 > Lowest Volume
```

## POS 8 - Mic type, R-CTCSS
```
0000 0000
       ^^ - R-CTC, -- (01), T (10)
     ^^   - CT Rx/Tx, -- (01), CT (10)
  ^^      - Mic Type, MOI (01), DYM(10)
```

## Pos 9 - Voice mode (PT)
```
0000 0000
       ^^ -- PT Mode, -- (01), PT (10) - 
     ^^   -- AM Band, -- (01), AM (10) - AM Reciever band
  ^^      -- ES Mode, -- (01), ES (10) - External Speaker
```

## Pos 10 - Freq shift | Radio Mode | Power
```
0000 0000
       ^^ -- Tx Power state, Low(01), Middle (10), High(11)
     ^^   -- Radio Mode RPT (10), TRF(01)
  ^^      -- Frequency Shift Direction TYPE (01), + (10), - (11)
```

## Pos 11 - Squelch | Lock, Unlock
```
0000 0000 
       ^^ -- Determines the key Lock state, Locked (10/11), Unlocked (01)
  ^^ ^^	  -- Squelch
  				1010 > SQL9
  				0001 > SQL0
```

## Pos 12 - Keypad Light
```
0000 0000
        ^ -- Determines the Keypad Light, Off (0), On (1)
```
