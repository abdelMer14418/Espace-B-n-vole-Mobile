package com.example.espacebenevole.ui.slideshow

import android.content.Intent
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
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espacebenevole.LoginActivity
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
        checkAuthenticationAndFetchTickets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAuthenticationAndFetchTickets() {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
        } else {
            fetchTickets(token)
        }
    }

    private fun getToken(): String? {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        return sharedPreferences.getString("AuthToken", null)
    }

    private fun redirectToLogin() {
        Toast.makeText(requireContext(), "Votre session s'est expirée, veuillez vous reconnecter!", Toast.LENGTH_LONG).show()
        val intent = Intent(activity, LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    private fun submitTicket() {
        val title = binding.editTextTitle.text?.toString()?.trim()
        val description = binding.editTextDescription.text?.toString()?.trim()

        if (title.isNullOrEmpty() || description.isNullOrEmpty()) {
            Toast.makeText(context, "Veuillez remplir tous les champs!", Toast.LENGTH_SHORT).show()
            return
        }

        val token = getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
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
                Toast.makeText(context, "Ticket envoyé avec succès!", Toast.LENGTH_SHORT).show()
                binding.editTextTitle.text?.clear()
                binding.editTextDescription.text?.clear()
                fetchTickets(token)
            },
            Response.ErrorListener { error ->
                handleVolleyError(error)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("auth" to token, "Content-Type" to "application/json")
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

    private fun fetchTickets(token: String) {
        val queue = Volley.newRequestQueue(context)
        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/tickets"

        val jsonArrayRequest = object : JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONArray> { response ->
                val ticketsList = mutableListOf<Ticket>()
                for (i in 0 until response.length()) {
                    val ticketJson = response.getJSONObject(i)
                    val ticket = Ticket(
                        ticketJson.getInt("id"),
                        ticketJson.getString("title"),
                        ticketJson.getString("description"),
                        ticketJson.getString("handled")
                    )
                    ticketsList.add(ticket)
                }
                binding.recyclerViewTickets.adapter = TicketsAdapter(ticketsList)
            },
            Response.ErrorListener { error ->
                handleVolleyError(error)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("auth" to token)
            }
        }
        queue.add(jsonArrayRequest)
    }

    private fun handleVolleyError(error: VolleyError) {
        if (error.networkResponse?.statusCode == 401) {
            redirectToLogin()
        } else {
            Toast.makeText(context, "Erreur réseau: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }
}