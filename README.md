ConversationBluetoothNetwork
============================

A bluetooth network for exchanging conversation information.  Each device maintains a BluetoothServerSocket to listen for oncoming connections.  When one is received, it spawns a thread for both incoming messages and outgoing messages and then continues listening for more devices.  Messages are passed into a queue from the main thread into all outgoing threads.  Eventually, machine learning analysis will take place on each phone and pass the data to all phones in the network so every member will have a complete log of the interaction.  
