package nl.endran.minireactor.distributed

import org.craftsmenlabs.socketoutlet.client.SocketOutletClient
import org.craftsmenlabs.socketoutlet.server.SocketOutletServer

data class NetworkMessage(val type: String, val payload: String, val id: String)
data class HubMessage(val sender: String, val networkMessage: NetworkMessage)

data class StartClient(val plantId: String, val ipAddress: String, val port: Int)
data class ClientStarted(val plantId: String, val ipAddress: String, val port: Int, val client: SocketOutletClient)
data class StopClient(val plantId: String, val ipAddress: String, val port: Int)
data class ClientStopped(val plantId: String, val ipAddress: String, val port: Int)

data class OpenServer(val plantId: String, val port: Int)
data class ServerOpened(val plantId: String, val server: SocketOutletServer)
data class CloseServer(val plantId: String)
data class ServerClosed(val plantId: String)

enum class ConnectionState { CONNECTED, DISCONNECTED }
data class ServerToClientConnectionEvent(val clientId: String, val connectionState: ConnectionState)
data class ClientToServerConnectionEvent(val ipAddress: String, val port: Int, val client: SocketOutletClient, val connectionState: ConnectionState)
