grammar Logo ; 

@header {package fr.utc.parsing;}

FLOAT : [0-9][0-9]*('.'[0-9]+)? ;
WS : [ \t\r\n]+ -> skip ;
COMMENT1 : '//' .*? [\r\n]+ -> skip;
COMMENT2 : '/*' .*? '*/' -> skip;

programme :
 liste_instructions  
;

liste_instructions :   
 (instruction)+    
;

instruction :
   'av' expr # av
 | 're' expr # re
 | 'td' expr # td
 | 'tg' expr # tg
 | 'lc'      # lc
 | 'bc'      # bc
 | 'fpos' '[' expr expr ']' # fpos
 | 'fcc' expr # fcc
 | 'fcap' expr #fcap
; 

expr :
   FLOAT         # float
 | '(' expr ')'  # parenthese 
;

