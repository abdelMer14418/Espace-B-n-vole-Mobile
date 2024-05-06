package com.example.espacebenevole.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espacebenevole.databinding.FragmentSlideshowBinding
import com.example.espacebenevole.Ticket
import com.example.espacebenevole.TicketsAdapter
import org.json.JSONArray
import org.json.JSONObject

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        binding.recyclerViewTickets.layoutManager = LinearLayoutManager(context)

        binding.buttonSubmit.setOnClickListener {
            submitTicket()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchTickets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun submitTicket() {
        val title = binding.editTextTitle.text?.toString()?.trim()
        val description = binding.editTextDescription.text?.toString()?.trim()

        if (title.isNullOrEmpty() || description.isNullOrEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = Volley.newRequestQueue(context)
        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/tickets"

        val jsonBody = JSONObject().apply {
            put("title", title)
            put("description", description)
        }

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener<String> { response ->
                Toast.makeText(context, "Ticket submitted successfully!", Toast.LENGTH_SHORT).show()
                binding.editTextTitle.text?.clear()
                binding.editTextDescription.text?.clear()
                fetchTickets()
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Failed to submit ticket: ${error.toString()}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
                val token = sharedPreferences.getString("AuthToken", "")
                return mapOf("auth" to token!!, "Content-Type" to "application/json")
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                return jsonBody.toString().toByteArray(Charsets.UTF_8)
            }
        }
        queue.add(stringRequest)
    }


    /*private fun submitTicket() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = Volley.newRequestQueue(context)
        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/tickets"

        val jsonBody = JSONObject()
        jsonBody.put("title", title)
        jsonBody.put("description", description)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener<String> { response ->
                Toast.makeText(context, "Ticket submitted successfully!", Toast.LENGTH_SHORT).show()
                binding.editTextTitle.text.clear()
                binding.editTextDescription.text.clear()
                fetchTickets() // Refresh tickets list after submission
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Failed to submit ticket: ${error.toString()}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
                val token = sharedPreferences.getString("AuthToken", "")
                headers["auth"] = token!! // Use 'auth' header as specified
                headers["Content-Type"] = "application/json"
                return headers
            }

            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                return jsonBody.toString().toByteArray(Charsets.UTF_8)
            }
        }
        queue.add(stringRequest)
    }*/

    private fun fetchTickets() {
        val queue = Volley.newRequestQueue(context)
        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/tickets"

        val jsonArrayRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                val ticketsArray = JSONArray(response)
                val ticketsList = ArrayList<Ticket>()
                for (i in 0 until ticketsArray.length()) {
                    val ticket = ticketsArray.getJSONObject(i)
                    ticketsList.add(Ticket(
                        ticket.getInt("id"),
                        ticket.getString("title"),
                        ticket.getString("description"),
                        ticket.getString("handled")
                    ))
                }
                binding.recyclerViewTickets.adapter = TicketsAdapter(ticketsList)
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Failed to fetch tickets: ${error.toString()}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
                val token = sharedPreferences.getString("AuthToken", "")
                return mapOf("auth" to token!!)
            }
        }
        queue.add(jsonArrayRequest)
    }
}
