package nl.endran.minireactor.plant

data class Slug(val type: String, val payload: String, val id: String)

data class StartClient(val plantId: String, val ipAddress: String, val port: Int)
data class ClientStarted(val plantId: String, val ipAddress: String, val port: Int)
data class StopClient(val plantId: String, val ipAddress: String, val port: Int)
data class ClientStopped(val plantId: String, val ipAddress: String, val port: Int)

data class OpenServer(val plantId: String, val port: Int)
data class ServerOpened(val plantId: String, val port: Int)
data class CloseServer(val plantId: String)
data class ServerClosed(val plantId: String)

enum class ConnectionState { CONNECTED, DISCONNECTED }
data class ClientConnectionState(val clientId: String, val connectionState: ConnectionState)
data class ServerConnectionState(val connectionState: ConnectionState)
