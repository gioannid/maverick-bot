# maverick-bot
A poker bot framework supporting many different engines.

This is heavy WIP so not much to say hereby... An example dialog with poker bot follows below.

<pre>
MAVERICK_1.22/150322

Initializing test subsystem with 4 players, running for 200 rounds.
Searching for models under path E:\Backups\poker\maverick\data\models
No changes will be committed to player models on disk
SimExpertBot fear factor at 0.6
Maverick listening to port 6789
Seed used by random number generator: 1647281583520
BNModel: Loading player model: $generic
BNPlayerModeller [$generic]: Loading net for round 1...
HistogramActionModel: Loading player model: $generic
Opening data file E:\Backups\poker\maverick\data\models\$generic\HistogramActionModel.raw
Hero is on seat: 3
Accepted connection from /127.0.0.1:60444
>>> FROM MAVERICK_1.22/150322
1/ <<< FROM TESTER Hero
1/ Stop-loss value at 0%
1/ Simbot using ExpertGame simulator
Accepted connection from /192.168.1.44:56575
>>> HELLO FROM MAVERICK v0.1
<<< NEWGAME 500 2 0 1 HELL201288 99800 johnidis 25000


--- NEW GAME --- 1 ---
B> HELL201288     99800
 > johnidis       25000
-----------------------
null: Your Hole Cards Are: 1c 1c
>>> OK
<<< BLIND 0 200
Small blind
action observed: BLIND 200
>>> OK
<<< BLIND 1 500
Big blind
action observed: STRADDLE 500
>>> OK
<<< DEAL 1 JC 8D
     johnidis:   Jc 8d
johnidis: Your Hole Cards Are: Jc 8d
>>> OK
<<< CALL 0 500
action observed: CALL 500
>>> OK
<<< ACTION? 1
Hand: [Jc-8d] 
is Button : false
player: p1
suits equal: false
preflop index: 123
action node : 10
preflop file: p1_strategy_10.txt
table : p1_strategy_10.txt
trying Strategy_FellOmen_2/1.zip
table found: 1/p1_strategy_10.txt
Prob[raise call fold] : 0.91074 0.0892601 0.0
random number = 0.5044304089022477
>>> RAISE 500
<<< RAISE 1 1000
action observed: RAISE 1000
>>> OK
<<< RAISE 0 1500
action observed: RAISE 1500
>>> OK
<<< ACTION? 1
Hand: [Jc-8d] 
is Button : false
player: p1
suits equal: false
preflop index: 123
action node : 8
preflop file: p1_strategy_8.txt
table : p1_strategy_8.txt
trying Strategy_FellOmen_2/1.zip
table found: 1/p1_strategy_8.txt
Prob[raise call fold] : 0.0319196 0.96808 0.0
random number = 0.3481890302915096
>>> CALL 500
<<< FLOP QS JH 8S
board:  Qs Jh 8s
flop wt: 0.20162901435924138
flop board index = 9
flop Hand Strength : 0.967622571692877 flop potential: 0.11555555555555555 flop negative potential : 0.15360584815651737
>>> OK
<<< ACTION? 1
rollout Hand Strength: 0.822731477588092 flop index: 165
flop file: p1_strategy-19_5.txt
table : p1_strategy-19_5.txt
trying Strategy_FellOmen_2/1.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/2.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/3.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/4.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/5.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/6.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/7.zip
reached end of zip file before table was found
>>> ERROR java.lang.NullPointerException
<<< CALL 1 0
action observed: FOLD
>>> OK
<<< RAISE 0 500
action observed: RAISE 500
>>> OK
<<< ACTION? 1
rollout Hand Strength: 0.822731477588092 flop index: 165
flop file: p1_strategy-19_9.txt
table : p1_strategy-19_9.txt
trying Strategy_FellOmen_2/1.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/2.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/3.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/4.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/5.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/6.zip
reached end of zip file before table was found
trying Strategy_FellOmen_2/7.zip
reached end of zip file before table was found
>>> ERROR java.lang.NullPointerException
<<< CALL 1 1000
call action observed: CALL 1000
position of action : 1
node of action: 9 Terminal Index : 7
>>> OK
<<< RAISE 0 1500
action observed: RAISE 1500
>>> OK
<<< FOLD 1
action observed: FOLD
>>> WARNING GAME IS OVER
<<< FOLD 0
action observed: FOLD
>>> OK
<<< WINNER 0
>>> OK
<<< NEWGAME 500 2 1 2 HELL201288 101900 johnidis 22300
</pre>
