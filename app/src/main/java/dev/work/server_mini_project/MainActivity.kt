package dev.work.server_mini_project

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import dev.work.server_mini_project.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.util.Random


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var host = "100.103.66.179" // Replace with the actual server IP address
    private val port = 12345 // Replace with the actual server port

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setStatusBarColor(this.getResources().getColor(R.color.transparent))

        binding.btnIP.setOnClickListener {
            val etIP = binding.etIp.text.toString()
            host = etIP
            Toast.makeText(this, "IP Updated", Toast.LENGTH_SHORT).show()
        }

        binding.buttonStart.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                startServer()
            }
        }
    }

    private suspend fun startServer() {
        log("Server listening on $host:$port")

        val serverSocket = ServerSocket(port)

        val clientSocket = serverSocket.accept()
        log("Connected to client: ${clientSocket.inetAddress.hostAddress}")

        // Perform Diffie-Hellman key exchange
        val p = 14327 // Prime number
        val g = 100 // Generator

        val private_key = Random().nextInt(p - 1) + 1
        val public_key = pow(g.toDouble(), private_key.toDouble(), p.toDouble()).toInt()

        log("Private key: $private_key")
        log("Public key: $public_key")

        val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        val writer = OutputStreamWriter(clientSocket.getOutputStream())

        // Receive the client's public key
        val their_public_key = reader.readLine().toInt()
        log("Client's public key: $their_public_key")

        // Send the public key to the client
        writer.write(public_key.toString() + "\n")
        writer.flush()

        // Compute the shared secret
        val shared_secret = pow(their_public_key.toDouble(), private_key.toDouble(), p.toDouble()).toInt()
        log("Shared secret: $shared_secret")

        // Receive the encrypted message from the client
        val encryptedMessage = reader.readLine()
        log("Received encrypted message: $encryptedMessage")

        // Decrypt the message using the shared secret
        val decryptedMessage = decryptMessage(encryptedMessage, shared_secret)
        log("Decrypted message: $decryptedMessage")

        // Send a response to the client
        val response = binding.searchEditText.text.toString()
        val encryptedResponse = encryptMessage(response, shared_secret)
        log("Sending response: $response")
        writer.write(encryptedResponse + "\n")
        writer.flush()

        delay(2000)
        // Close the connection
        clientSocket.close()

        serverSocket.close()

        log("Connection closed")
    }

    private fun log(message: String) {
        runOnUiThread {
            binding.textLog.append("$message\n")
        }
    }

    private fun pow(base: Double, exponent: Double, modulus: Double): Double {
        var result = 1.0
        var b = base
        var e = exponent

        while (e > 0) {
            if (e % 2 == 1.0) {
                result = (result * b) % modulus
            }
            e /= 2
            b = (b * b) % modulus
        }

        return result
    }

    private fun encryptMessage(message: String, key: Int): String {
        val encryptedMessage = StringBuilder()
        for (i in message.indices) {
            val encryptedChar = message[i].toInt() xor key
            encryptedMessage.append(encryptedChar.toChar())
        }
        return encryptedMessage.toString()
    }

    private fun decryptMessage(encryptedMessage: String, key: Int): String {
        return encryptMessage(encryptedMessage, key) // XOR encryption is symmetric
    }
}
