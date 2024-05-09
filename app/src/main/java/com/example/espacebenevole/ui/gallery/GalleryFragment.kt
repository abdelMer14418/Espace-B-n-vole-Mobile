package com.example.espacebenevole.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espacebenevole.LoginActivity
import com.example.espacebenevole.databinding.FragmentGalleryBinding
import com.example.espacebenevole.ui.home.Event
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import org.json.JSONArray
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private var eventsList: List<Event> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAuthenticationAndFetchEvents()
        setupCalendarClickListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAuthenticationAndFetchEvents() {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            redirectToLogin()
        } else {
            fetchEvents(token)
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

    private fun fetchEvents(token: String) {
        val queue = Volley.newRequestQueue(context)
        val url = "https://projet-annuel-paoli.koyeb.app/api/index.php/volunteer/planning"

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                val eventsArray = JSONArray(response)
                eventsList = eventsArray.toEventList()
                updateCalendar(eventsList)
            },
            Response.ErrorListener { error ->
                handleVolleyError(error)
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("auth" to token)
            }
        }
        queue.add(stringRequest)
    }

    private fun handleVolleyError(error: VolleyError) {
        if (error.networkResponse?.statusCode == 401) {
            redirectToLogin()
        } else {
            Toast.makeText(context, "Erreur réseau: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupCalendarClickListener() {
        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            if (selected) {
                displayEventDetails(date)
            }
        }
    }

    private fun displayEventDetails(date: CalendarDay) {
        val eventsOnThisDay = eventsList.filter {
            val startDate = LocalDate.parse(it.startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atStartOfDay().toLocalDate()
            val endDate = LocalDate.parse(it.endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atStartOfDay().toLocalDate()
            date.date.isAfter(startDate.minusDays(1)) && date.date.isBefore(endDate.plusDays(1))
        }

        if (eventsOnThisDay.isNotEmpty()) {
            showEventDetailsDialog(eventsOnThisDay)
        } else {
            Toast.makeText(context, "Pas d'évènement à cette date!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEventDetailsDialog(events: List<Event>) {
        val eventDetails = events.joinToString(separator = "\n\n") { event ->
            "Titre: ${event.title}\nDébut: ${event.startDate}\nFin: ${event.endDate}\nAdresse: ${event.address}"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Détails")
            .setMessage(eventDetails)
            .setPositiveButton("Fermer") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun updateCalendar(eventsList: List<Event>) {
        eventsList.forEach { event ->
            val startDateTime = parseDateTime(event.startDate)
            val endDateTime = parseDateTime(event.endDate)
            if (startDateTime != null && endDateTime != null) {
                val startDate = startDateTime.toLocalDate()
                val endDate = endDateTime.toLocalDate()
                binding.calendarView.addDecorator(EventDecorator(startDate, endDate))
            }
        }
    }

    private fun parseDateTime(dateTimeStr: String): LocalDateTime? {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return try {
            LocalDateTime.parse(dateTimeStr, formatter)
        } catch (e: DateTimeParseException) {
            Log.e("DateParsing", "Failed to parse date: $dateTimeStr", e)
            null
        }
    }

    inner class EventDecorator(private val startDate: LocalDate, private val endDate: LocalDate) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            return day.date in startDate..endDate
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(5f, ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)))
        }
    }

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
}
/*private fun fetchEvents2() {
        val jsonData = """
        [
            {
                "id": 1,
                "title": "Charity Bake Sale",
                "startDate": "2024-09-10 08:00:00",
                "endDate": "2024-09-12 12:00:00",
                "address": "123 Cupcake Way"
            },
            {
                "id": 2,
                "title": "Book Drive",
                "startDate": "2024-12-10 09:00:00",
                "endDate": "2024-12-12 15:00:00",
                "address": "456 Library Ave"
            }
        ]
        """
        try {
            val eventsArray = JSONArray(jsonData)
            eventsList = eventsArray.toEventList()
            Toast.makeText(context, "Events loaded from JSON.", Toast.LENGTH_LONG).show()
            updateCalendar(eventsList)
        } catch (e: JSONException) {
            Toast.makeText(context, "Failed to parse JSON data: ${e.toString()}", Toast.LENGTH_LONG).show()
        }
    }*/