grammar Logo ; 

@header {package fr.utc.parsing;}

FLOAT : ('-')?[0-9][0-9]*('.'[0-9]+)? ;
WS : [ \t\r\n]+ -> skip ;
COMMENT1 : '//' .*? [\r\n]+ -> skip;
COMMENT2 : '/*' .*? '*/' -> skip;
VAR : ':'[A-Za-z][A-Za-z0-9]*;
NAME : [A-Za-z][A-Za-z0-9]*;
COMPARATOR : ('<' | '>');

procedure: 
 'pour' NAME VAR* liste_instructions 'fin' # declarationProcedure
;

programme :
 procedure* liste_instructions  
;

liste_instructions :   
 (instruction)+    
;

instruction :
   'av' expr # av
 | 'td' expr # td
 | 'tg' expr # tg
 | 're' expr # re
 | 'lc' # lc
 | 'bc' # bc
 | 'fpos' '['expr expr']' # fpos
 | 'fcc' expr # fcc
 | 'fcap' expr #fcap 
 | 'store' # store
 | 'move' # move
 | 'repete' expr '[' liste_instructions ']' # repete
 | 'donne' '"'NAME expr # affectation
 | 'si' predicat '[' liste_instructions ']' ('[' liste_instructions ']')? # if
 | 'tantque' predicat '[' liste_instructions ']' # tantque
 | NAME '(' expr* ')' # executeProcedure
 | 'rends' expr # retourFonction
; 

predicat : 
 expr COMPARATOR expr #booleanComparaison
;

expr:
 expr ('*' | '/' ) expr # mult
 | expr ('+' | '-' ) expr # sum
 | FLOAT #float
 | '(' expr ')' #parenthese
 | 'hasard' '(' expr ')' # hasard
 | 'cos' '(' expr ')' # cos
 | 'sin' '(' expr ')' # sin
 | 'loop' # loop
 | VAR # variables
 | expr COMPARATOR expr # comparaison
 | NAME '(' expr* ')' # executeFonction
;