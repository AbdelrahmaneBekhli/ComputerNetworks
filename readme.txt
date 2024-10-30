Computer Networks Coursework 2023/2024
Submission by:
Abdelrahmane Bekhli
Student ID: 220011666
Email: abdelrahmane.bekhli@city.ac.uk

Project Description:
This project implements a distributed key-value storage system using Java sockets. It consists of two main components: a FullNode and a TemporaryNode.


=============================== BUILD & COMPILE ===================================
1. Download Java
2. Download the code's zip folder
2. Unzip the folder
3. Run CMD or PowerShell
4. Compile the files using javac *.java
5. Run the commands found below

=============================== TEMPORARY NODE ===================================

acts as a client by sending requests to fullNodes.
	Usage:
		store:
			format:
				java CmdLineStore <nodeName> <nodeAddress:nodePortNumber> <key> <value>
			example:
				java CmdLineStore abdelrahmane.bekhli@city.ac.uk:MyCoolImplementation,1.41,test-node-1 10.0.0.234:20201 Hello World!
		get:
			format:
				java CmdLineGet <nodeName> <nodeAddress:nodePortNumber> <key>
			example:
				java CmdLineGet abdelrahmane.bekhli@city.ac.uk:MyCoolImplementation,1.41,test-node-1 10.0.0.234:20201 Hello
		net cat: 
			format:
				nc <ipAddress> <portNumber>
				<START message>
				<any request>
			example:
				nc 10.0.0.234 20201
				START 1 max.smith@city.ac.uk:MyCoolImplementation,1.41,test-node-2
				ECHO?
			
	Requests:
		START? - a start message informing a node it connected to it 
			format: 
				START 1 <nodeName>
			example:
				START 1 abdelrahmane.bekhli@city.ac.uk:MyCoolImplementation,1.41,test-node-1
		PUT? - stores a key-value pair in the node 
			format: 
				PUT <number of keys> <number of values> 
				<key>
				<value>
			example:
				PUT 1 1
				Hello
				World!
		GET? - retrieves value of a specific key
			format:
				GET? <number of keys>
				key
			example:
				GET? 1
				Hello
		ECHO? - a message to check if a node is still active
			format:
				ECHO?
		NOTIFY? a message to notify a node of another nodes so they can be added to the network map
			format:
				NOTIFY?
				<nodename>
				<nodeAddress:nodePortNumber>
			example:
				NOTIFY?
				max.smith@city.ac.uk:MyCoolImplementation,1.41,test-node-2
				10.0.0.222:1234
		NEAREST? - returns the nearest 3 nodes to a specific hash key
			format:
				NEAREST? <hashedKey>
			example:
				NEAREST? 0f0139b167bb7b4a416b8f6a7e0daa7e24a08172b9892171e5fdc615bb7f999b

			END - terminates the socket connection between 2 nodes
				format:
					END <reason>
				example:
					END time-out

=============================== FULL NODE ===================================

acts as both a server and client, its main role is to handle reequests recieved. 
At the start of the fullNode it will scan other nodes exising on ports between its port number +- 500 then add them to its network map.

	Usage: 
		format: 
			java CmdLineFullNode <nodeName> <nodeAddress:nodePortNumber> <nodeAddress> <nodePortNumber>

		example:
			java CmdLineFullNode abdelrahmane@city.ac.uk:FullNode 10.0.0.234:20201 10.0.0.234 20201

	handling requests:
		START? - a start message informing a node it connected to it 
			format: 
				START 1 <nodeName>
			example:
				START 1 abdelrahmane.bekhli@city.ac.uk:MyCoolImplementation,1.41,test-node-1
		PUT? - replies with whether the key value pair were stored
			format: 
				SUCCESS
				or
				FAILED
		GET? - send the value of the given key if found
			format:
				VALUE <number of values>
				<values>
				or 
				NOPE
			example:
				VALUE 1
				WORLD!
		ECHO? - replies with message to show its active
			format:
				OHCE
		NOTIFY? adds the node to its network map
			format:
				NOTIFIED
		NEAREST? - returns the nearest 3 nodes to a specific hash key
			format:
				NODES <numberOfNodes>
			example:
				NODES 3
				max.smith@city.ac.uk:MyCoolImplementation,1.41,test-node-34
				10.0.0.145:3432
				john.brown@city.ac.uk:MyCoolImplementation,1.41,test-node-14
				10.0.0.454:4123
				jack.jamie@city.ac.uk:MyCoolImplementation,1.41,test-node-65
				10.0.0.743:3532

		END or invalid request - terminates the socket connection between 2 nodes
			format:
				END <reason>
			example:
				END time-out

Afterall each command/request should successfully give the correct output
			
		