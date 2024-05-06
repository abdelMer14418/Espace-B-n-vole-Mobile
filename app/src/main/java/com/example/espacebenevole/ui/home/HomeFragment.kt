package com.example.espacebenevole.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espacebenevole.R
import com.example.espacebenevole.databinding.FragmentHomeBinding
import org.json.JSONArray

data class Event(
    val id: Int,
    val title: String,
    val startDate: String,
    val endDate: String,
    val address: String
)

fun JSONArray.toEventList(): List<Event> {
    val list = mutableListOf<Event>()
    for (i in 0 until length()) {
        val jsonObject = getJSONObject(i)
        list.add(
            Event(
                id = jsonObject.getInt("id"),
                title = jsonObject.getString("title"),
                startDate = jsonObject.getString("startDate"),
                endDate = jsonObject.getString("endDate"),
                address = jsonObject.getString("address")
            )
        )
    }
    return list
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        checkAuthenticationAndFetchEvents()
        return binding.root
    }

    private fun checkAuthenticationAndFetchEvents() {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
        } else {
            fetchAndDisplayEvents(token)
        }
    }

    private fun getToken(): String? {
        val sharedPreferences = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("AuthToken", null)
    }

    private fun redirectToLogin() {
        Toast.makeText(requireContext(), "Votre session s'est expirée, veuillez vous reconnecter!", Toast.LENGTH_LONG).show()
        // Handle redirection to login; possibly via Navigation Component or starting a new Activity
    }

    private fun fetchAndDisplayEvents(token: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val urlPublic = "https://projet-annuel-paoli.koyeb.app/api/index.php/public/activities"
        val urlRegistered = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/planning"

        // Requête pour les événements publics
        val publicEventsRequest = JsonArrayRequest(Request.Method.GET, urlPublic, null,
            Response.Listener<JSONArray> { responsePublic ->
                val publicEvents = responsePublic.toEventList()

                // Requête pour les événements enregistrés avec en-tête d'authentification correct
                val registeredEventsRequest = object : JsonArrayRequest(Method.GET, urlRegistered, null,
                    Response.Listener<JSONArray> { responseRegistered ->
                        val registeredEvents = responseRegistered.toEventList()
                        updateEventsPage(publicEvents, registeredEvents)
                    },
                    Response.ErrorListener { error ->
                        Toast.makeText(requireContext(), "Erreur lors de la récupération des évènements dans les quelles vous etes inscrits!", Toast.LENGTH_LONG).show()
                    }) {
                    override fun getHeaders(): Map<String, String> {
                        return mapOf("auth" to token)
                    }
                }
                queue.add(registeredEventsRequest)
            },
            Response.ErrorListener { error ->
                Toast.makeText(requireContext(), "Erreur de la récupération des évènements disponibles!", Toast.LENGTH_LONG).show()
            }
        )
        queue.add(publicEventsRequest)
    }


    private fun updateEventsPage(publicEvents: List<Event>?, registeredEvents: List<Event>?) {
        val container = binding.eventsContainer
        container.removeAllViews()

        publicEvents?.forEach { event ->
            val eventView = LayoutInflater.from(context).inflate(R.layout.event_card, container, false)
            val buttonParticipate = eventView.findViewById<Button>(R.id.buttonParticipate)
            val isRegistered = registeredEvents?.any { it.id == event.id } ?: false

            eventView.findViewById<TextView>(R.id.activityName).text = event.title
            eventView.findViewById<TextView>(R.id.activityStart).text = "Début : ${event.startDate}"
            eventView.findViewById<TextView>(R.id.activityEnd).text = "Fin : ${event.endDate}"
            eventView.findViewById<TextView>(R.id.activityLocation).text = "Lieu : ${event.address}"

            buttonParticipate.text = if (isRegistered) "Vous êtes déjà inscrit" else "Participer"
            buttonParticipate.isEnabled = !isRegistered
            buttonParticipate.setOnClickListener {
                if (!isRegistered) {
                    registerForEvent(event.id, buttonParticipate)
                }
            }

            container.addView(eventView)
        }
    }

    private fun registerForEvent(eventId: Int, button: Button) {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
            return
        }

        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/activities/$eventId/register"
        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> {
                button.text = "Vous êtes déjà inscrit"
                button.isEnabled = false
                Toast.makeText(context, "Enregistrement bien accompli!", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Erreur lors de l'enregistrement!", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("auth" to token)
            }
        }
        Volley.newRequestQueue(context).add(stringRequest)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
