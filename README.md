# SOEN423Project

## To generate the package from idl file:
-  Navigate to java directory `cd src\main\java`
-  Run `idlj -fall corba_fe.idl` 
-  Run `orbd -ORBInitialPort 1234 -ORBInitialHost localhost&`